package com.project.agent_brain_service;

public class Review {
    public String user;
    public double rating; // 1.0 to 5.0
    public String comment;
    public String sentiment; // "Positive", "Negative", "Neutral"

    public Review(String user, double rating, String comment, String sentiment) {
        this.user = user;
        this.rating = rating;
        this.comment = comment;
        this.sentiment = sentiment;
    }
}