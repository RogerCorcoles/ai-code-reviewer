package com.rogercm.aicodereviewer.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.rogercm.aicodereviewer.ui.ReviewToolWindow;

// Declared as <projectService> in plugin.xml — do NOT also add @Service here;
// using both registrations causes getService() to return null.
public final class ReviewService {

    private static final Logger LOG = Logger.getInstance(ReviewService.class);

    private ReviewToolWindow toolWindow;

    public static ReviewService getInstance(Project project) {
        return project.getService(ReviewService.class);
    }

    public void setToolWindow(ReviewToolWindow toolWindow) {
        LOG.info("setToolWindow: " + (toolWindow != null ? "registered" : "null"));
        this.toolWindow = toolWindow;
    }

    public ReviewToolWindow getToolWindow() {
        return toolWindow;
    }
}
