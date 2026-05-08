package com.rogercm.aicodereviewer.model;

public class ReviewResult {

    private final String content;
    private final boolean success;
    private final String errorMessage;

    private ReviewResult(String content, boolean success, String errorMessage) {
        this.content = content;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ReviewResult success(String content) {
        return new ReviewResult(content, true, null);
    }

    public static ReviewResult error(String errorMessage) {
        return new ReviewResult(null, false, errorMessage);
    }

    public String getContent() {
        return content;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
