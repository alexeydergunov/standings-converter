package com.github.standingsconverter.parser;

import com.github.standingsconverter.entity.*;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class CodeforcesAPIParser implements Parser {
    private static final String CF_API_URL = "http://codeforces.com/api/";

    @Override
    public Contest parse(String filename) throws IOException {
        //noinspection UnnecessaryLocalVariable
        String contestId = filename; // TODO make input parameter less confusing

        Map<String, Object> contestStandingsParameters = new MapBuilder<String, Object>().
                put("contestId", contestId).
                put("showUnofficial", true).
                build();
        JsonObject jsonContestStandings = readJsonFromAPI("contest.standings", contestStandingsParameters).getAsJsonObject();
        checkStatus(jsonContestStandings);
        JsonObject jsonContestStandingsResult = jsonContestStandings.getAsJsonObject("result");
        JsonObject jsonContest = jsonContestStandingsResult.getAsJsonObject("contest");
        String contestName = jsonContest.getAsJsonPrimitive("name").getAsString();
        long duration = jsonContest.getAsJsonPrimitive("durationSeconds").getAsLong();
        List<Problem> problems = new ArrayList<>();
        Map<Character, Integer> problemIdMap = new HashMap<>();
        JsonArray jsonProblems = jsonContestStandingsResult.getAsJsonArray("problems");
        for (int i = 0; i < jsonProblems.size(); i++) {
            JsonObject jsonProblem = jsonProblems.get(i).getAsJsonObject();
            char letter = jsonProblem.getAsJsonPrimitive("index").getAsCharacter();
            String name = jsonProblem.getAsJsonPrimitive("name").getAsString();
            Problem problem = new Problem(letter, name);
            problemIdMap.put(letter, problems.size());
            problems.add(problem);
        }
        if (problems.size() != problemIdMap.size()) {
            throw new IllegalStateException("Some problems have equal ids");
        }
        JsonArray jsonRows = jsonContestStandingsResult.getAsJsonArray("rows");
        List<Team> teams = new ArrayList<>();
        Map<String, Integer> teamNameIdMap = new HashMap<>();
        for (int i = 0; i < jsonRows.size(); i++) {
            JsonObject jsonRow = jsonRows.get(i).getAsJsonObject();
            JsonObject jsonParty = jsonRow.getAsJsonObject("party");
            String participantType = jsonParty.getAsJsonPrimitive("participantType").getAsString();
            boolean ghost = jsonParty.getAsJsonPrimitive("ghost").getAsBoolean();
            if (participantType.equals("CONTESTANT") || (participantType.equals("VIRTUAL") && ghost)) {
                String name = getTeamName(jsonParty);
                int teamId = teams.size();
                teamNameIdMap.put(name, teamId);
                teams.add(new Team(teamId, name));
            }
        }
        if (teams.size() != teamNameIdMap.size()) {
            throw new IllegalStateException("Some teams have equal names");
        }
        Map<String, Object> contestStatusParameters = new MapBuilder<String, Object>().
                put("contestId", contestId).
                build();
        JsonObject jsonContestStatus = readJsonFromAPI("contest.status", contestStatusParameters).getAsJsonObject();
        checkStatus(jsonContestStatus);
        List<Submission> submissions = new ArrayList<>();
        JsonArray jsonContestStatusResult = jsonContestStatus.getAsJsonArray("result");
        Map<TeamProblemKey, Integer> attempts = new HashMap<>();
        for (int i = jsonContestStatusResult.size() - 1; i >= 0; i--) {
            JsonObject jsonSubmission = jsonContestStatusResult.get(i).getAsJsonObject();
            JsonObject jsonParty = jsonSubmission.getAsJsonObject("author");
            String participantType = jsonParty.getAsJsonPrimitive("participantType").getAsString();
            boolean ghost = jsonParty.getAsJsonPrimitive("ghost").getAsBoolean();
            if (participantType.equals("CONTESTANT") || (participantType.equals("VIRTUAL") && ghost)) {
                JsonObject jsonProblem = jsonSubmission.getAsJsonObject("problem");
                char letter = jsonProblem.getAsJsonPrimitive("index").getAsCharacter();
                Integer problemId = problemIdMap.get(letter);
                if (problemId == null) {
                    throw new IllegalStateException("Can't find problem " + letter + " in the contest");
                }
                String teamName = getTeamName(jsonParty);
                Integer teamId = teamNameIdMap.get(teamName);
                if (teamId == null) {
                    throw new IllegalStateException("Can't find team " + teamName + " in the contest");
                }
                long relativeTime = jsonSubmission.getAsJsonPrimitive("relativeTimeSeconds").getAsLong();
                if (relativeTime < 0 || relativeTime > duration) {
                    throw new IllegalStateException("Wrong relative time of submission: " + relativeTime + ", duration = " + duration);
                }
                TeamProblemKey key = new TeamProblemKey(teamId, problemId);
                int attempt = attempts.getOrDefault(key, 0) + 1;
                attempts.put(key, attempt);
                Verdict verdict = parseVerdict(jsonSubmission.getAsJsonPrimitive("verdict").getAsString());
                int submissionId = submissions.size();
                Problem problem = problems.get(problemId);
                Team team = teams.get(teamId);
                submissions.add(new Submission(submissionId, team, problem, attempt, relativeTime, verdict));
            }
        }
        Collections.sort(submissions, (o1, o2) -> {
            if (o1.getTime() != o2.getTime()) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
            return Integer.compare(o1.getId(), o2.getId());
        });
        return new Contest(contestName, duration / 60, problems, teams, submissions);
    }

    private void checkStatus(JsonObject jsonParentObject) throws IOException {
        JsonPrimitive jsonStatus = jsonParentObject.getAsJsonPrimitive("status");
        String status = (jsonStatus == null) ? null : jsonStatus.getAsString();
        if (!"OK".equals(status)) {
            throw new IOException("Codeforces API returned status " + status);
        }
    }

    private String getTeamName(JsonObject jsonParty) {
        JsonArray jsonMembers = jsonParty.getAsJsonArray("members");
        StringBuilder nameBuilder = new StringBuilder();
        boolean hasName = jsonParty.has("teamName");
        if (hasName) {
            nameBuilder.append(jsonParty.getAsJsonPrimitive("teamName").getAsString());
        }
        for (int i = 0; i < jsonMembers.size(); i++) {
            if (i == 0) {
                if (hasName) {
                    nameBuilder.append(": ");
                }
            } else {
                nameBuilder.append(", ");
            }
            JsonObject jsonMember = jsonMembers.get(i).getAsJsonObject();
            nameBuilder.append(jsonMember.getAsJsonPrimitive("handle").getAsString());
        }
        return nameBuilder.toString();
    }

    private Verdict parseVerdict(String verdictCode) {
        switch (verdictCode) {
            case "OK": return Verdict.ACCEPTED;
            case "REJECTED": return Verdict.REJECTED;
            case "WRONG_ANSWER": return Verdict.WRONG_ANSWER;
            case "RUNTIME_ERROR": return Verdict.RUNTIME_ERROR;
            case "TIME_LIMIT_EXCEEDED": return Verdict.TIME_LIMIT_EXCEEDED;
            case "MEMORY_LIMIT_EXCEEDED": return Verdict.MEMORY_LIMIT_EXCEEDED;
            case "COMPILATION_ERROR": return Verdict.COMPILATION_ERROR;
            case "PRESENTATION_ERROR": return Verdict.PRESENTATION_ERROR;
            case "IDLENESS_LIMIT_EXCEEDED": return Verdict.IDLENESS_LIMIT_EXCEEDED;
        }
        throw new IllegalArgumentException("Unknown verdict: " + verdictCode);
    }

    private static class TeamProblemKey {
        private final int teamId;
        private final int problemId;

        public TeamProblemKey(int teamId, int problemId) {
            this.teamId = teamId;
            this.problemId = problemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TeamProblemKey that = (TeamProblemKey) o;
            return problemId == that.problemId && teamId == that.teamId;
        }

        @Override
        public int hashCode() {
            int result = teamId;
            result = 31 * result + problemId;
            return result;
        }
    }

    private JsonElement readJsonFromAPI(String method, Map<String, Object> parameters) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(CF_API_URL);
        urlBuilder.append(method);
        urlBuilder.append('?');
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            if (urlBuilder.charAt(urlBuilder.length() - 1) != '?') {
                urlBuilder.append('&');
            }
            urlBuilder.append(e.getKey());
            urlBuilder.append('=');
            urlBuilder.append(e.getValue());
        }
        URL url = new URL(urlBuilder.toString());
        URLConnection connection = url.openConnection();
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                jsonBuilder.append(line).append('\n');
            }
        }
        JsonParser parser = new JsonParser();
        return parser.parse(jsonBuilder.toString());
    }

    private static class MapBuilder<K, V> {
        private final Map<K, V> map;

        public MapBuilder() {
            this.map = new HashMap<>();
        }

        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return map;
        }
    }
}
