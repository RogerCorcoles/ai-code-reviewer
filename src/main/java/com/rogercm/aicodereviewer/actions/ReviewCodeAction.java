package com.rogercm.aicodereviewer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.rogercm.aicodereviewer.api.GroqApiClient;
import com.rogercm.aicodereviewer.model.ReviewResult;
import com.rogercm.aicodereviewer.services.ReviewService;
import com.rogercm.aicodereviewer.ui.ReviewToolWindow;
import org.jetbrains.annotations.NotNull;

public class ReviewCodeAction extends AnAction implements DumbAware {

    private static final Logger LOG = Logger.getInstance(ReviewCodeAction.class);
    private static final String TOOL_WINDOW_ID = "AI Code Review";

    private final GroqApiClient apiClient = new GroqApiClient();

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabledAndVisible(hasSelection);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("=== ReviewCodeAction triggered ===");

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        if (editor == null || project == null) {
            LOG.warn("Editor or project is null — aborting");
            return;
        }

        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.isBlank()) {
            Messages.showWarningDialog(project, "Please select some code to review.", "No Code Selected");
            return;
        }
        LOG.info("Selected text: " + selectedText.length() + " chars");

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        String language = file != null ? detectLanguage(file.getExtension()) : "";
        LOG.info("Language: '" + language + "'");

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            LOG.error("Tool window '" + TOOL_WINDOW_ID + "' not found — check plugin.xml <toolWindow> id");
            Messages.showErrorDialog(project,
                    "AI Code Review panel not found. Please restart the IDE.",
                    "Tool Window Missing");
            return;
        }

        // show() triggers createToolWindowContent() synchronously on EDT (first call only).
        // We then immediately read the registered ReviewToolWindow from ReviewService.
        toolWindow.show();

        ReviewService service = ReviewService.getInstance(project);
        if (service == null) {
            LOG.error("ReviewService is null after show() — plugin.xml projectService may be missing");
            return;
        }

        ReviewToolWindow reviewWindow = service.getToolWindow();
        LOG.info("ReviewToolWindow after show(): " + (reviewWindow != null ? "found" : "NULL"));

        if (reviewWindow == null) {
            LOG.error("ReviewToolWindow is null — createToolWindowContent() probably threw an exception");
            Messages.showErrorDialog(project,
                    "AI Code Review panel failed to initialize. Check IDE logs (Help → Show Log).",
                    "Initialization Error");
            return;
        }

        reviewWindow.showLoading();

        // Capture locals for the background thread — avoids a second service lookup
        // that could theoretically see a different state.
        final String code = selectedText;
        final String lang = language;
        final ReviewToolWindow finalWindow = reviewWindow;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Reviewing Code with AI", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                LOG.info("Background task started");
                indicator.setIndeterminate(true);
                indicator.setText("Calling Groq API...");

                ReviewResult result = apiClient.reviewCode(code, lang);

                LOG.info("API call finished — success=" + result.isSuccess());
                if (!result.isSuccess()) {
                    LOG.warn("API error: " + result.getErrorMessage());
                }

                if (result.isSuccess()) {
                    finalWindow.showResult(result.getContent());
                } else {
                    finalWindow.showError(result.getErrorMessage());
                }
                LOG.info("=== ReviewCodeAction complete ===");
            }
        });
    }

    private String detectLanguage(String extension) {
        if (extension == null) return "";
        return switch (extension.toLowerCase()) {
            case "java"        -> "Java";
            case "kt", "kts"   -> "Kotlin";
            case "py"          -> "Python";
            case "js"          -> "JavaScript";
            case "ts"          -> "TypeScript";
            case "tsx", "jsx"  -> "React";
            case "cpp", "cc"   -> "C++";
            case "c"           -> "C";
            case "cs"          -> "C#";
            case "go"          -> "Go";
            case "rs"          -> "Rust";
            case "rb"          -> "Ruby";
            case "php"         -> "PHP";
            case "swift"       -> "Swift";
            case "sql"         -> "SQL";
            case "sh"          -> "Shell";
            case "html"        -> "HTML";
            case "css"         -> "CSS";
            case "xml"         -> "XML";
            case "json"        -> "JSON";
            case "yaml", "yml" -> "YAML";
            default            -> extension;
        };
    }
}
