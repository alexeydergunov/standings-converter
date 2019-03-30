package com.github.standingsconverter.parser;

import com.github.standingsconverter.entity.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class YandexContestParser implements Parser {
    @Override
    public Contest parse(String inputFile) throws IOException {
        DocumentBuilder builder;
        Document document;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(new File(inputFile));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
        Element contestLogElement = document.getDocumentElement();

        Node settingsElement = getChild(contestLogElement, "settings");
        Node durationElement = getChild(settingsElement, "duration");
        long duration = parseDuration(durationElement) / 60;
        String name = getChild(settingsElement, "contestName").getTextContent();

        Node usersElement = getChild(contestLogElement, "users");
        Node problemsElement = getChild(contestLogElement, "problems");
        Node eventsElement = getChild(contestLogElement, "events");
        Map<String, Team> teams = parseTeams(getChildren(usersElement, "user"));
        Map<String, Problem> problems = parseProblems(getChildren(problemsElement, "problem"));
        Map<Integer, Submission> submissions = parseSubmissions(teams, problems, getChildren(eventsElement, "submit"));

        List<Team> teamList = new ArrayList<>(teams.values());
        List<Problem> problemList = new ArrayList<>(problems.values());
        List<Submission> submissionList = new ArrayList<>(submissions.values());
        teamList.sort(Comparator.comparingInt(Team::getId));
        problemList.sort(Comparator.comparingInt(Problem::getId));
        submissionList.sort((o1, o2) -> {
            if (o1.getTime() != o2.getTime()) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
            return Integer.compare(o1.getId(), o2.getId());
        });
        return new Contest(name, duration, problemList, teamList, submissionList);
    }

    private Node getChild(Node parent, String name) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeName().equals(name)) {
                return child;
            }
        }
        throw new RuntimeException();
    }

    private List<Node> getChildren(Node parent, String name) {
        NodeList nodeList = parent.getChildNodes();
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeName().equals(name)) {
                result.add(child);
            }
        }
        return result;
    }

    private long parseDuration(Node durationElement) {
        String value = durationElement.getTextContent();
        String[] tokens = value.split(":");
        if (tokens.length != 3) {
            throw new RuntimeException();
        }
        long h = Long.parseLong(tokens[0]);
        long m = Long.parseLong(tokens[1]);
        long s = Long.parseLong(tokens[2]);
        return h * 3600 + m * 60 + s;
    }

    private Map<String, Team> parseTeams(List<Node> userNodes) {
        Map<String, Team> teams = new HashMap<>();
        int teamId = 0;
        for (Node userNode : userNodes) {
            NamedNodeMap attributes = userNode.getAttributes();
            Node participationTypeNode = attributes.getNamedItem("participationType");
            if (participationTypeNode == null) {
                String userId = attributes.getNamedItem("id").getNodeValue();
                String name = attributes.getNamedItem("displayedName").getNodeValue();
                Team team = new Team(teamId, name);
                teams.put(userId, team);
                teamId++;
            }
        }
        return teams;
    }

    private Map<String, Problem> parseProblems(List<Node> problemNodes) {
        Map<String, Problem> problems = new HashMap<>();
        for (Node problemNode : problemNodes) {
            NamedNodeMap attributes = problemNode.getAttributes();
            String idStr = attributes.getNamedItem("title").getNodeValue();
            String name = attributes.getNamedItem("longName").getNodeValue();
            problems.put(idStr, new Problem(idStr.charAt(0), name));
        }
        return problems;
    }

    private Map<Integer, Submission> parseSubmissions(Map<String, Team> teams, Map<String, Problem> problems, List<Node> submitNodes) {
        Map<TeamProblemKey, Integer> attempts = new HashMap<>();
        Map<Integer, Submission> submissions = new HashMap<>();
        int submissionId = 0;
        for (Node submitNode : submitNodes) {
            NamedNodeMap attributes = submitNode.getAttributes();

            String userId = attributes.getNamedItem("userId").getNodeValue();
            String problemTitle = attributes.getNamedItem("problemTitle").getNodeValue();
            Team team = teams.get(userId);
            Problem problem = problems.get(problemTitle);
            if (team == null || problem == null) {
                // probably, it is a hidden participant
                continue;
            }
            TeamProblemKey key = new TeamProblemKey(team.getId(), problem.getId());

            int attempt = attempts.getOrDefault(key, 0) + 1;
            attempts.put(key, attempt);

            long time = Long.parseLong(attributes.getNamedItem("contestTime").getNodeValue()) / 1000;
            String verdict = attributes.getNamedItem("verdict").getNodeValue();

            Submission submission = new Submission(submissionId, team, problem, attempt, time, parseVerdict(verdict));
            submissions.put(submissionId, submission);
            submissionId++;
        }
        return submissions;
    }

    private static Verdict parseVerdict(String s) {
        switch (s) {
            case "CE": return Verdict.COMPILATION_ERROR;
            case "IL": return Verdict.IDLENESS_LIMIT_EXCEEDED;
            case "ML": return Verdict.MEMORY_LIMIT_EXCEEDED;
            case "OK": return Verdict.ACCEPTED;
            case "PE": return Verdict.PRESENTATION_ERROR;
            case "RE": return Verdict.RUNTIME_ERROR;
            case "TL": return Verdict.TIME_LIMIT_EXCEEDED;
            case "WA": return Verdict.WRONG_ANSWER;
        }
        throw new IllegalArgumentException("Unknown verdict: " + s);
    }

    private static class TeamProblemKey {
        private final int teamId;
        private final char problemId;

        private TeamProblemKey(int teamId, char problemId) {
            this.teamId = teamId;
            this.problemId = problemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TeamProblemKey that = (TeamProblemKey) o;
            return teamId == that.teamId && problemId == that.problemId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(teamId, problemId);
        }
    }
}
