package com.rogercm.aicodereviewer.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.rogercm.aicodereviewer.services.ReviewService;
import org.jetbrains.annotations.NotNull;

// DumbAware ensures createToolWindowContent() is called immediately even when
// the IDE is still indexing. Without this, the first show() call during dumb
// mode defers initialization and leaves the ContentManager empty.
public class ReviewToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(ReviewToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("createToolWindowContent called for project: " + project.getName());
        try {
            ReviewService service = ReviewService.getInstance(project);
            if (service == null) {
                LOG.error("ReviewService is null — check plugin.xml <projectService> declaration");
                return;
            }

            ReviewToolWindow reviewToolWindow = new ReviewToolWindow();
            service.setToolWindow(reviewToolWindow);

            Content content = ContentFactory.getInstance()
                    .createContent(reviewToolWindow.getContent(), "", false);
            toolWindow.getContentManager().addContent(content);
            LOG.info("Tool window content added successfully");

        } catch (Exception ex) {
            LOG.error("Failed to create tool window content", ex);
        }
    }
}
