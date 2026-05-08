package com.rogercm.aicodereviewer.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class ReviewToolWindow {

    private static final Logger LOG = Logger.getInstance(ReviewToolWindow.class);

    private static final Color C_BG       = Color.decode("#1e1e1e");
    private static final Color C_BG_BAR   = Color.decode("#252526");
    private static final Color C_FG_MUTED = Color.decode("#9e9e9e");

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    private final JPanel     contentPanel;
    private final JEditorPane resultArea;
    private final JLabel     statusLabel;
    private final CardLayout cardLayout;
    private final JPanel     cardPanel;

    public ReviewToolWindow() {
        statusLabel = new JLabel("Select code → right-click → Review Code with AI");
        statusLabel.setBorder(JBUI.Borders.empty(5, 10));
        statusLabel.setForeground(C_FG_MUTED);
        statusLabel.setBackground(C_BG_BAR);
        statusLabel.setOpaque(true);

        resultArea = new JEditorPane();
        resultArea.setEditable(false);
        resultArea.setContentType("text/html");
        resultArea.setBackground(C_BG);
        resultArea.setForeground(Color.decode("#e0e0e0"));
        // Do NOT touch HTMLEditorKit.getStyleSheet() — it is IDE-wide shared state.
        // All styling lives in the <style> block produced by MarkdownRenderer.
        resultArea.setEditorKit(new HTMLEditorKit());
        resultArea.setText(welcomeHtml());

        JBScrollPane scrollPane = new JBScrollPane(resultArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(C_BG);
        scrollPane.getViewport().setBackground(C_BG);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(C_BG);
        cardPanel.add(scrollPane, "content");
        cardPanel.add(buildLoadingPanel(), "loading");

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(C_BG);
        contentPanel.add(statusLabel, BorderLayout.NORTH);
        contentPanel.add(cardPanel,   BorderLayout.CENTER);

        LOG.info("ReviewToolWindow created");
    }

    public JComponent getContent() { return contentPanel; }

    public void showLoading() {
        LOG.info("showLoading()");
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Analyzing code with AI...");
            cardLayout.show(cardPanel, "loading");
        });
    }

    public void showResult(String markdown) {
        LOG.info("showResult(): " + (markdown != null ? markdown.length() : 0) + " chars");
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Review complete");
            resultArea.setText(renderer.render(markdown));
            resultArea.setCaretPosition(0);
            cardLayout.show(cardPanel, "content");
        });
    }

    public void showError(String message) {
        LOG.warn("showError(): " + message);
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Error");
            resultArea.setText(renderer.wrap(
                "<h2 style='color:#f44336'>Error</h2>"
                + "<p style='color:#ef9a9a'>" + renderer.esc(message) + "</p>"
            ));
            resultArea.setCaretPosition(0);
            cardLayout.show(cardPanel, "content");
        });
    }

    private JPanel buildLoadingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(C_BG);

        JLabel label = new JLabel("Analyzing code with AI...");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(C_FG_MUTED);
        label.setFont(label.getFont().deriveFont(Font.ITALIC, 14f));

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setMaximumSize(new Dimension(240, 4));

        inner.add(label);
        inner.add(Box.createVerticalStrut(12));
        inner.add(bar);
        panel.add(inner);
        return panel;
    }

    private String welcomeHtml() {
        return renderer.wrap(
            "<h1>AI Code Reviewer</h1>"
            + "<p>Select code in the editor, right-click, and choose "
            + "<b>Review Code with AI</b>.</p>"
            + "<p>Configure your Groq API key at:<br>"
            + "<b>Settings &rarr; Tools &rarr; AI Code Reviewer</b></p>"
        );
    }
}
