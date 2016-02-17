package com.github.standingsconverter.parser;

import com.github.standingsconverter.entity.*;
import com.google.gson.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CodeforcesAPIParser implements Parser {
    private static final String PROPERTY_CONTEST_ID = "contestId";
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_SECRET = "secret";

    private static final String CF_API_URL = "http://codeforces.com/api";

    @Override
    public Contest parse(String filename) throws IOException {
        Map<String, String> properties = parseProperties(filename);
        String contestId = properties.get(PROPERTY_CONTEST_ID);
        String apiKey = properties.get(PROPERTY_KEY);
        String apiSecret = properties.get(PROPERTY_SECRET);
        SortedMap<String, Object> contestStandingsParameters = new SortedMapBuilder<String, Object>().
                put("contestId", contestId).
                put("showUnofficial", true).
                build();
        JsonObject jsonContestStandings = readJsonFromAPI("contest.standings", contestStandingsParameters, apiKey, apiSecret).getAsJsonObject();
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
        SortedMap<String, Object> contestStatusParameters = new SortedMapBuilder<String, Object>().
                put("contestId", contestId).
                build();
        JsonObject jsonContestStatus = readJsonFromAPI("contest.status", contestStatusParameters, apiKey, apiSecret).getAsJsonObject();
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

    // parameters must be sorted to make private requests
    private JsonElement readJsonFromAPI(String method, SortedMap<String, Object> parameters, String key, String secret) throws IOException {
        if (key != null && secret != null) {
            parameters.put("apiKey", key);
            parameters.put("time", System.currentTimeMillis() / 1000);
        }
        StringBuilder methodWithParams = new StringBuilder();
        methodWithParams.append(method);
        methodWithParams.append('?');
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            if (methodWithParams.charAt(methodWithParams.length() - 1) != '?') {
                methodWithParams.append('&');
            }
            methodWithParams.append(e.getKey());
            methodWithParams.append('=');
            methodWithParams.append(e.getValue());
        }
        if (key != null && secret != null) {
            String rand = String.format("%06x", new Random().nextInt(1 << 24)); // 6 hex digits
            String sha512Hash = sha512(rand, methodWithParams.toString(), secret);
            methodWithParams.append('&');
            methodWithParams.append("apiSig");
            methodWithParams.append('=');
            methodWithParams.append(rand);
            methodWithParams.append(sha512Hash);
        }
        URL url = new URL(CF_API_URL + "/" + methodWithParams);
        URLConnection connection = url.openConnection();
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
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

    private String sha512(String rand, String methodWithParams, String secret) throws UnsupportedEncodingException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String str = rand + "/" + methodWithParams + "#" + secret;
        byte[] bytes = messageDigest.digest(str.getBytes("UTF-8"));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b & 0xFF));
        }
        return result.toString();
    }

    private Map<String, String> parseProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(filename)) {
            properties.load(reader);
        }
        Map<String, String> map = new HashMap<>(properties.size());
        properties.forEach((key, value) -> map.put((String) key, (String) value));
        if (!map.containsKey(PROPERTY_CONTEST_ID)) {
            throw new IllegalArgumentException("Property " + PROPERTY_CONTEST_ID + " is missing");
        }
        if (map.containsKey(PROPERTY_KEY) != map.containsKey(PROPERTY_SECRET)) {
            throw new IllegalArgumentException("Properties " + PROPERTY_KEY + " and " + PROPERTY_SECRET + " must be both presented or both missing");
        }
        return map;
    }

    private static class SortedMapBuilder<K, V> {
        private final SortedMap<K, V> map;

        public SortedMapBuilder() {
            this.map = new TreeMap<>();
        }

        public SortedMapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public SortedMap<K, V> build() {
            return map;
        }
    }
}
