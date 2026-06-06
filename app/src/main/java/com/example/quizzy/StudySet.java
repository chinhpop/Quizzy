package com.example.quizzy;

public class StudySet {
    private String id;
    private String title;
    private int termCount;
    private String username;

    public StudySet(String id, String title, int termCount, String username) {
        this.id = id;
        this.title = title;
        this.termCount = termCount;
        this.username = username != null && !username.isEmpty() ? username : "Unknown";

    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getTermCount() {
        return termCount;
    }

    public String getUsername() {
        return username;
    }


    public String getAvatarLetter() {
        if (username == null || username.isEmpty()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }
}