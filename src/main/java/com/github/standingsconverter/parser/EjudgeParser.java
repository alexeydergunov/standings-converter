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

public class EjudgeParser implements Parser {
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
        Element runLogElement = document.getDocumentElement();
        long duration = Long.parseLong(runLogElement.getAttribute("duration")) / 60;
        String name = runLogElement.getElementsByTagName("name").item(0).getTextContent();
        Map<Integer, Team> teams = parseTeams(runLogElement.getElementsByTagName("user"));
        Map<Integer, Problem> problems = parseProblems(runLogElement.getElementsByTagName("problem"));
        Map<Integer, Submission> submissions = parseSubmissions(teams, problems, runLogElement.getElementsByTagName("run"));
        List<Team> teamList = new ArrayList<>(teams.values());
        List<Problem> problemList = new ArrayList<>(problems.values());
        List<Submission> submissionList = new ArrayList<>(submissions.values());
        Collections.sort(teamList, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
        Collections.sort(problemList, (o1, o2) -> Integer.compare(o1.getId(), o2.getId()));
        Collections.sort(submissionList, (o1, o2) -> {
            if (o1.getTime() != o2.getTime()) {
                return Long.compare(o1.getTime(), o2.getTime());
            }
            return Integer.compare(o1.getId(), o2.getId());
        });
        return new Contest(name, duration, problemList, teamList, submissionList);
    }

    private Map<Integer, Team> parseTeams(NodeList nodeList) {
        Map<Integer, Team> map = new HashMap<>();
        Map<Integer, Integer> ids = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            int id = Integer.parseInt(attributes.getNamedItem("id").getNodeValue());
            String name = attributes.getNamedItem("name").getNodeValue();
            int newID = ids.size();
            ids.put(id, newID);
            Team team = new Team(newID, name);
            map.put(id, team);
        }
        return map;
    }

    private Map<Integer, Problem> parseProblems(NodeList nodeList) {
        Map<Integer, Problem> map = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            int id = Integer.parseInt(attributes.getNamedItem("id").getNodeValue());
            String letter = attributes.getNamedItem("short_name").getNodeValue();
            String name = attributes.getNamedItem("long_name").getNodeValue();
            Problem problem = new Problem(letter.charAt(0), name);
            map.put(id, problem);
        }
        return map;
    }

    private Map<Integer, Submission> parseSubmissions(Map<Integer, Team> teams, Map<Integer, Problem> problems, NodeList nodeList) {
        Map<TeamProblemKey, Integer> attempts = new HashMap<>();
        Map<Integer, Submission> map = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            int id = Integer.parseInt(attributes.getNamedItem("run_id").getNodeValue());
            long time = Long.parseLong(attributes.getNamedItem("time").getNodeValue());
            int teamId = Integer.parseInt(attributes.getNamedItem("user_id").getNodeValue());
            int problemId = Integer.parseInt(attributes.getNamedItem("prob_id").getNodeValue());
            String verdict = attributes.getNamedItem("status").getNodeValue();
            Team team = teams.get(teamId);
            Problem problem = problems.get(problemId);
            TeamProblemKey key = new TeamProblemKey(teamId, problemId);
            if (!attempts.containsKey(key)) {
                attempts.put(key, 1);
            } else {
                attempts.put(key, attempts.get(key) + 1);
            }
            int attempt = attempts.get(key);
            Submission submission = new Submission(id, team, problem, attempt, time, parseVerdict(verdict));
            map.put(id, submission);
        }
        return map;
    }

    private static Verdict parseVerdict(String s) {
        switch (s) {
            case "OK": return Verdict.ACCEPTED;
            case "CE": return Verdict.COMPILATION_ERROR;
            case "RT": return Verdict.RUNTIME_ERROR;
            case "TL": return Verdict.TIME_LIMIT_EXCEEDED;
            case "PE": return Verdict.PRESENTATION_ERROR;
            case "WA": return Verdict.WRONG_ANSWER;
            case "ML": return Verdict.MEMORY_LIMIT_EXCEEDED;
            case "SE": return Verdict.SECURITY_VIOLATION;
            case "RJ": return Verdict.REJECTED;
        }
        throw new IllegalArgumentException("Unknown verdict: " + s);
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
}
