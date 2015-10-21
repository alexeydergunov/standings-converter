package com.github.standingsconverter.entity;

import java.util.List;

public class Contest {
    private final String name;
    private final long duration;
    private final List<Problem> problems;
    private final List<Team> teams;
    private final List<Submission> submissions;

    public Contest(String name, long duration, List<Problem> problems, List<Team> teams, List<Submission> submissions) {
        this.name = name;
        this.duration = duration;
        this.problems = problems;
        this.teams = teams;
        this.submissions = submissions;
    }

    public String getName() {
        return name;
    }

    public long getDuration() {
        return duration;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public List<Submission> getSubmissions() {
        return submissions;
    }
}
