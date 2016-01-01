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

public class PCMSParser implements Parser {
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
        Element standingsElement = document.getDocumentElement();
        Node contestElement = getChild(standingsElement, "contest");
        NamedNodeMap contestElementAttributes = contestElement.getAttributes();
        long duration = Long.parseLong(contestElementAttributes.getNamedItem("length").getTextContent()) / 1000 / 60;
        String name = contestElementAttributes.getNamedItem("name").getTextContent();
        Map<String, Team> teams = parseTeams(getChildren(contestElement, "session"));
        Map<String, Problem> problems = parseProblems(getChild(contestElement, "challenge"));
        Map<Integer, Submission> submissions = parseSubmissions(teams, problems, getChildren(contestElement, "session"));
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

    private Node getChild(Node parent, String name) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeName().equals(name)) {
                return child;
            }
        }
        return null;
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

    private Map<String, Team> parseTeams(List<Node> nodeList) {
        Map<String, Team> map = new HashMap<>();
        Map<String, Integer> ids = new HashMap<>();
        for (Node node : nodeList) {
            NamedNodeMap attributes = node.getAttributes();
            String id = attributes.getNamedItem("alias").getNodeValue();
            String name = attributes.getNamedItem("party").getNodeValue();
            int newID = ids.size();
            ids.put(id, newID);
            Team team = new Team(newID, name);
            map.put(id, team);
        }
        return map;
    }

    private Map<String, Problem> parseProblems(Node parentNode) {
        Map<String, Problem> map = new HashMap<>();
        List<Node> nodeList = getChildren(parentNode, "problem");
        for (Node node : nodeList) {
            NamedNodeMap attributes = node.getAttributes();
            String alias = attributes.getNamedItem("alias").getTextContent();
            String name = attributes.getNamedItem("name").getTextContent();
            Problem problem = new Problem(alias.charAt(0), name);
            map.put(alias, problem);
        }
        return map;
    }

    private Map<Integer, Submission> parseSubmissions(Map<String, Team> teams, Map<String, Problem> problems, List<Node> nodeList) {
        Map<TeamProblemKey, Integer> attempts = new HashMap<>();
        Map<Integer, Submission> map = new HashMap<>();
        int submissionID = 0;
        for (Node node : nodeList) {
            NamedNodeMap teamAttrs = node.getAttributes();
            List<Node> submissionNodes = getChildren(node, "problem");
            for (Node submissionNode : submissionNodes) {
                NamedNodeMap attributes = submissionNode.getAttributes();
                String teamId = teamAttrs.getNamedItem("alias").getNodeValue();
                String problemId = attributes.getNamedItem("alias").getNodeValue();
                List<Node> verdictNodes = getChildren(submissionNode, "run");
                for (Node verdictNode : verdictNodes) {
                    NamedNodeMap verdictAttrs = verdictNode.getAttributes();
                    String verdict = verdictAttrs.getNamedItem("accepted").getNodeValue();
                    long time = Long.parseLong(verdictAttrs.getNamedItem("time").getNodeValue()) / 1000;
                    Team team = teams.get(teamId);
                    Problem problem = problems.get(problemId);
                    TeamProblemKey key = new TeamProblemKey(team.getId(), problem.getId());
                    if (!attempts.containsKey(key)) {
                        attempts.put(key, 1);
                    } else {
                        attempts.put(key, attempts.get(key) + 1);
                    }
                    int attempt = attempts.get(key);
                    Submission submission = new Submission(submissionID, team, problem, attempt, time, verdict.equalsIgnoreCase("yes") ? Verdict.ACCEPTED : Verdict.REJECTED);
                    map.put(submissionID, submission);
                    submissionID++;
                }
            }
        }
        return map;
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
