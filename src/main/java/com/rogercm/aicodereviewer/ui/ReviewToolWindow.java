package com.rogercm.aicodereviewer.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class ReviewToolWindow {

    private static final Logger LOG = Logger.getInstance(ReviewToolWindow.class);

    // ── Dark-theme palette ────────────────────────────────────────────────────
    private static final Color C_BG       = Color.decode("#1e1e1e"); // editor background
    private static final Color C_BG_BAR   = Color.decode("#252526"); // status bar
    private static final Color C_FG_MUTED = Color.decode("#9e9e9e"); // status text

    // CSS equivalents (kept in sync with the Color constants above)
    private static final String CSS =
            "body {"
            + "  background-color:#1e1e1e;"
            + "  color:#e0e0e0;"
            + "  font-family:'Segoe UI',Arial,sans-serif;"
            + "  font-size:14px;"
            + "  margin:14px 18px;"
            + "  line-height:1.7;"
            + "}"
            + "h1{"
            + "  color:#ffd700;"          // gold  – top-level title
            + "  font-size:18px;"
            + "  margin:0 0 10px 0;"
            + "}"
            + "h2{"
            + "  color:#4fc3f7;"          // light blue – section headings
            + "  font-size:16px;"
            + "  margin:18px 0 6px 0;"
            + "  border-bottom:1px solid #333;"
            + "  padding-bottom:5px;"
            + "}"
            + "h3{"
            + "  color:#81c784;"          // soft green – sub-headings
            + "  font-size:14px;"
            + "  margin:14px 0 4px 0;"
            + "}"
            + "b,strong{"
            + "  color:#ffd700;"          // gold – bold emphasis
            + "  font-weight:bold;"
            + "}"
            + "i,em{"
            + "  color:#ce9178;"          // soft orange – italics
            + "}"
            + "pre{"
            + "  background:#2d2d2d;"
            + "  color:#a8ff78;"          // bright green – code block text
            + "  font-family:'JetBrains Mono','Consolas','Courier New',monospace;"
            + "  font-size:13px;"
            + "  padding:10px 14px;"
            + "  margin:10px 0;"
            + "  border-left:3px solid #4fc3f7;"
            + "  line-height:1.5;"
            + "}"
            + "code{"
            + "  background:#2d2d2d;"
            + "  color:#50fa7b;"          // cyan-green – inline code
            + "  font-family:'JetBrains Mono','Consolas','Courier New',monospace;"
            + "  font-size:13px;"
            + "  padding:2px 6px;"
            + "  border-radius:3px;"
            + "}"
            + "ul,ol{"
            + "  margin:6px 0 12px 0;"
            + "  padding-left:22px;"
            + "}"
            + "li{"
            + "  margin-bottom:6px;"
            + "}"
            + "p{"
            + "  margin:6px 0 10px 0;"
            + "}"
            + "hr{"
            + "  border:none;"
            + "  border-top:1px solid #3a3a3a;"
            + "  margin:12px 0;"
            + "}";

    // ── Component fields ──────────────────────────────────────────────────────

    private final JPanel contentPanel;
    private final JEditorPane resultArea;
    private final JLabel statusLabel;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ReviewToolWindow() {
        // Status bar
        statusLabel = new JLabel("Select code → right-click → Review Code with AI");
        statusLabel.setBorder(JBUI.Borders.empty(5, 10));
        statusLabel.setForeground(C_FG_MUTED);
        statusLabel.setBackground(C_BG_BAR);
        statusLabel.setOpaque(true);

        // HTML result pane
        resultArea = new JEditorPane();
        resultArea.setEditable(false);
        resultArea.setContentType("text/html");
        resultArea.setBackground(C_BG);
        resultArea.setForeground(Color.decode("#e0e0e0"));
        // Do NOT touch HTMLEditorKit.getStyleSheet() — it is IDE-wide shared state.
        // All styling lives in the <style> block inside each HTML document.
        resultArea.setEditorKit(new HTMLEditorKit());
        resultArea.setText(welcomeHtml());

        JBScrollPane scrollPane = new JBScrollPane(resultArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(C_BG);
        scrollPane.getViewport().setBackground(C_BG);

        // Card panel: switches between results and the loading spinner
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(C_BG);
        cardPanel.add(scrollPane, "content");
        cardPanel.add(buildLoadingPanel(), "loading");

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(C_BG);
        contentPanel.add(statusLabel, BorderLayout.NORTH);
        contentPanel.add(cardPanel, BorderLayout.CENTER);

        LOG.info("ReviewToolWindow created");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public JComponent getContent() {
        return contentPanel;
    }

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
            resultArea.setText(renderMarkdown(markdown));
            resultArea.setCaretPosition(0);
            cardLayout.show(cardPanel, "content");
        });
    }

    public void showError(String message) {
        LOG.warn("showError(): " + message);
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Error");
            resultArea.setText(errorHtml(message));
            resultArea.setCaretPosition(0);
            cardLayout.show(cardPanel, "content");
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private JPanel buildLoadingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(C_BG);

        JLabel label = new JLabel("Analyzing code with AI...");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.decode("#9e9e9e"));
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

    /**
     * Converts the AI's markdown response to HTML, wrapping it in the dark-theme document.
     * Handles: # headings, **bold**, *italic*, `inline code`, ``` code blocks, - lists.
     */
    private String renderMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return wrap("<p><i>No content returned from the API.</i></p>");
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        boolean inList = false;

        for (String line : lines) {
            // ── Fenced code blocks ──────────────────────────────────────────
            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    closeList(sb, inList); inList = false;
                    sb.append("<pre><code>");
                    inCodeBlock = true;
                } else {
                    sb.append("</code></pre>");
                    inCodeBlock = false;
                }
                continue;
            }
            if (inCodeBlock) {
                sb.append(esc(line)).append("\n");
                continue;
            }

            // ── Block-level elements ────────────────────────────────────────
            if (line.startsWith("### ")) {
                closeList(sb, inList); inList = false;
                sb.append("<h3>").append(inline(line.substring(4))).append("</h3>");
            } else if (line.startsWith("## ")) {
                closeList(sb, inList); inList = false;
                sb.append("<h2>").append(inline(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                closeList(sb, inList); inList = false;
                sb.append("<h1>").append(inline(line.substring(2))).append("</h1>");
            } else if (line.startsWith("---")) {
                closeList(sb, inList); inList = false;
                sb.append("<hr>");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList) { sb.append("<ul>"); inList = true; }
                sb.append("<li>").append(inline(line.substring(2))).append("</li>");
            } else if (line.matches("^\\d+\\.\\s.*")) {
                if (!inList) { sb.append("<ul>"); inList = true; }
                sb.append("<li>").append(inline(line.replaceFirst("^\\d+\\.\\s", ""))).append("</li>");
            } else if (line.isBlank()) {
                closeList(sb, inList); inList = false;
                sb.append("<br>");
            } else {
                closeList(sb, inList); inList = false;
                sb.append("<p>").append(inline(line)).append("</p>");
            }
        }
        closeList(sb, inList);
        if (inCodeBlock) sb.append("</code></pre>"); // handle unclosed block

        return wrap(sb.toString());
    }

    /** Applies inline markdown: **bold**, *italic*, `code`. */
    private String inline(String text) {
        String s = esc(text);
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        s = s.replaceAll("\\*(.+?)\\*",       "<i>$1</i>");
        s = s.replaceAll("`([^`]+)`",          "<code>$1</code>");
        return s;
    }

    /** HTML-escapes user content so it can be embedded safely. */
    private String esc(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /** Wraps a body fragment in the full dark-theme HTML document. */
    private String wrap(String body) {
        return "<html><head><style>" + CSS + "</style></head><body>" + body + "</body></html>";
    }

    private static void closeList(StringBuilder sb, boolean inList) {
        if (inList) sb.append("</ul>");
    }

    private String welcomeHtml() {
        return wrap(
            "<h1>AI Code Reviewer</h1>"
            + "<p>Select code in the editor, right-click, and choose "
            + "<b>Review Code with AI</b>.</p>"
            + "<p>Configure your Groq API key at:<br>"
            + "<b>Settings &rarr; Tools &rarr; AI Code Reviewer</b></p>"
        );
    }

    private String errorHtml(String message) {
        return wrap(
            "<h2 style='color:#f44336'>Error</h2>"
            + "<p style='color:#ef9a9a'>" + esc(message) + "</p>"
        );
    }
}
