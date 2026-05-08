package com.rogercm.aicodereviewer.api;

import com.rogercm.aicodereviewer.model.ReviewResult;

/**
 * Contract for AI-powered code review.
 * Decouples ReviewCodeAction from the concrete HTTP implementation,
 * making the action testable without a real network call.
 */
public interface CodeReviewClient {
    ReviewResult reviewCode(String code, String language);
}
