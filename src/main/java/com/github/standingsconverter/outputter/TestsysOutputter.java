package com.github.standingsconverter.outputter;

import com.github.standingsconverter.entity.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TestsysOutputter implements Outputter {
    @Override
    public void output(Contest contest, String outputFile) throws IOException {
        List<Problem> problems = contest.getProblems();
        List<Team> teams = contest.getTeams();
        List<Submission> submissions = contest.getSubmissions();
        try (PrintWriter writer = new PrintWriter(new File(outputFile), "UTF-8")) {
            writer.println((char) 0x1A);
            writer.printf("@contest \"%s\"", contest.getName());
            writer.println();
            writer.printf("@contlen %d", contest.getDuration());
            writer.println();
            writer.printf("@problems %d", problems.size());
            writer.println();
            writer.printf("@teams %d", teams.size());
            writer.println();
            writer.printf("@submissions %d", submissions.size());
            writer.println();
            for (Problem problem : problems) {
                writer.println(toString(problem));
            }
            for (Team team : teams) {
                writer.println(toString(team));
            }
            for (Submission submission : submissions) {
                writer.println(toString(submission));
            }
        }
    }

    private static String toString(Problem problem) {
        return String.format("@p %c,%s,20,0", problem.getId(), problem.getName());
    }

    private static String toString(Team team) {
        return String.format("@t %d,0,1,\"%s\"", team.getId(), team.getName());
    }

    private static String toString(Submission submission) {
        return String.format("@s %d,%c,%d,%d,%s", submission.getTeam().getId(), submission.getProblem().getId(), submission.getAttempt(), submission.getTime(), toString(submission.getVerdict()));
    }

    private static String toString(Verdict verdict) {
        // TODO seems that CF doesn't support IL and SV, they are replaced with TL and RT
        switch (verdict) {
            case ACCEPTED: return "OK";
            case REJECTED: return "RJ";
            case WRONG_ANSWER: return "WA";
            case RUNTIME_ERROR: return "RT";
            case TIME_LIMIT_EXCEEDED: return "TL";
            case MEMORY_LIMIT_EXCEEDED: return "ML";
            case COMPILATION_ERROR: return "CE";
            case PRESENTATION_ERROR: return "PE";
            case IDLENESS_LIMIT_EXCEEDED: return "TL";
            case SECURITY_VIOLATION: return "RT";
        }
        throw new IllegalArgumentException("Unknown verdict: " + verdict);
    }
}
