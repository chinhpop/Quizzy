package com.example.quizzy;

import java.util.UUID;

public class FlashCard {
    private String front;
    private String back;
    private String id;

    public FlashCard(String front, String back) {
        this.front = front;
        this.back = back;
        this.id = UUID.randomUUID().toString();
    }

    public FlashCard(String front, String back, String id) {
        this.front = front;
        this.back = back;
        this.id = id;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}