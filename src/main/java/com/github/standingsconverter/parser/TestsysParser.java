package com.github.standingsconverter.parser;

import com.github.standingsconverter.entity.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestsysParser implements Parser {
	@Override
	public Contest parse(String inputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(inputFile)))) {
            String name = "";
            long duration = 0;
            List<Problem> problems = new ArrayList<>();
            List<Team> teams = new ArrayList<>();
            List<Submission> submissions = new ArrayList<>();
            Map<Character, Problem> problemMap = new HashMap<>();
            Map<Integer, Team> teamMap = new HashMap<>();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (!line.startsWith("@")) {
                    continue;
                }
                line = line.substring(1);
                String type = line.substring(0, line.indexOf(' '));
                switch (type) {
                    case "contest":
                        int firstPos = line.indexOf('"');
                        int lastPos = line.lastIndexOf('"');
                        name = line.substring(firstPos + 1, lastPos);
                        break;
                    case "contlen":
                        duration = Long.parseLong(line.substring(7).trim());
                        break;
                    case "p":
                        String problemDescription = line.substring(1).trim();
                        Problem problem = parseProblem(problemDescription);
                        problems.add(problem);
                        problemMap.put(problem.getId(), problem);
                        break;
                    case "t":
                        String teamDescription = line.substring(1).trim();
                        Team team = parseTeam(teamDescription);
                        teams.add(team);
                        teamMap.put(team.getId(), team);
                        break;
                    case "s":
                        String submissionDescription = line.substring(1).trim();
                        int id = submissions.size();
                        Submission submission = parseSubmission(submissionDescription, problemMap, teamMap, id);
                        submissions.add(submission);
                        break;
                }
            }
            return new Contest(name, duration, problems, teams, submissions);
        }
    }

	private Problem parseProblem(String problemDescription) {
		char id = problemDescription.charAt(0);
		problemDescription = problemDescription.substring(2);
		String name = problemDescription.substring(0, problemDescription.indexOf(','));
		return new Problem(id, name);
	}

	private Team parseTeam(String teamDescription) {
		int id = Integer.parseInt(teamDescription.substring(0, teamDescription.indexOf(',')));
		int firstPos = teamDescription.indexOf('"');
		int lastPos = teamDescription.lastIndexOf('"');
		String name = teamDescription.substring(firstPos + 1, lastPos);
		return new Team(id, name);
	}

	private Submission parseSubmission(String submissionDescription, Map<Character, Problem> problemMap, Map<Integer, Team> teamMap, int id) {
		StringTokenizer tokenizer = new StringTokenizer(submissionDescription, ",");
		int teamID = Integer.parseInt(tokenizer.nextToken());
		Team team = teamMap.get(teamID);
		char problemID = tokenizer.nextToken().charAt(0);
		Problem problem = problemMap.get(problemID);
		int attempt = Integer.parseInt(tokenizer.nextToken());
		long time = Long.parseLong(tokenizer.nextToken());
		Verdict verdict = parseVerdict(tokenizer.nextToken());
		return new Submission(id, team, problem, attempt, time, verdict);
	}

    private Verdict parseVerdict(String verdictCode) {
        switch (verdictCode) {
            case "OK": return Verdict.ACCEPTED;
            case "RJ": return Verdict.REJECTED;
            case "WA": return Verdict.WRONG_ANSWER;
            case "RT": return Verdict.RUNTIME_ERROR;
            case "TL": return Verdict.TIME_LIMIT_EXCEEDED;
            case "ML": return Verdict.MEMORY_LIMIT_EXCEEDED;
            case "CE": return Verdict.COMPILATION_ERROR;
            case "PE": return Verdict.PRESENTATION_ERROR;
        }
        throw new IllegalArgumentException("Unknown verdict: " + verdictCode);
    }
}
