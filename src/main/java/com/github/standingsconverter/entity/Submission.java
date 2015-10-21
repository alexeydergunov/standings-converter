package com.github.standingsconverter.entity;

public class Submission {
    private final int id;
    private final Team team;
    private final Problem problem;
    private final int attempt;
    private final long time;
    private final Verdict verdict;

    public Submission(int id, Team team, Problem problem, int attempt, long time, Verdict verdict) {
        this.id = id;
        this.team = team;
        this.problem = problem;
        this.attempt = attempt;
        this.time = time;
        this.verdict = verdict;
    }

    public int getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public Problem getProblem() {
        return problem;
    }

    public int getAttempt() {
        return attempt;
    }

    public long getTime() {
        return time;
    }

    public Verdict getVerdict() {
        return verdict;
    }
}
