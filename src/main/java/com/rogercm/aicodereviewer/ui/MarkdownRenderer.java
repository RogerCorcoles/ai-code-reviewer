package com.rogercm.aicodereviewer.ui;

/**
 * Converts a markdown string (as returned by the Groq API) into a self-contained
 * HTML document styled for the plugin's dark theme.
 *
 * Extracted from ReviewToolWindow so it can be unit-tested without any
 * IntelliJ Platform or Swing infrastructure.
 *
 * Supported syntax: # headings, **bold**, *italic*, `inline code`,
 * ``` fenced code blocks, - / * / 1. lists, --- horizontal rules.
 */
public class MarkdownRenderer {

    static final String CSS =
            "body{"
            + "background-color:#1e1e1e;color:#e0e0e0;"
            + "font-family:'Segoe UI',Arial,sans-serif;font-size:14px;"
            + "margin:14px 18px;line-height:1.7}"
            + "h1{color:#ffd700;font-size:18px;margin:0 0 10px 0}"
            + "h2{color:#4fc3f7;font-size:16px;margin:18px 0 6px 0;"
            + "border-bottom:1px solid #333;padding-bottom:5px}"
            + "h3{color:#81c784;font-size:14px;margin:14px 0 4px 0}"
            + "b,strong{color:#ffd700;font-weight:bold}"
            + "i,em{color:#ce9178}"
            + "pre{background:#2d2d2d;color:#a8ff78;"
            + "font-family:'JetBrains Mono','Consolas','Courier New',monospace;"
            + "font-size:13px;padding:10px 14px;margin:10px 0;"
            + "border-left:3px solid #4fc3f7;line-height:1.5}"
            + "code{background:#2d2d2d;color:#50fa7b;"
            + "font-family:'JetBrains Mono','Consolas','Courier New',monospace;"
            + "font-size:13px;padding:2px 6px;border-radius:3px}"
            + "ul,ol{margin:6px 0 12px 0;padding-left:22px}"
            + "li{margin-bottom:6px}"
            + "p{margin:6px 0 10px 0}"
            + "hr{border:none;border-top:1px solid #3a3a3a;margin:12px 0}";

    /** Entry point — converts markdown to a complete dark-theme HTML document. */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return wrap("<p><i>No content returned from the API.</i></p>");
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        boolean inList = false;

        for (String line : lines) {
            // Fenced code block delimiter
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

            // Block-level elements
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
        if (inCodeBlock) sb.append("</code></pre>"); // handle model-returned unclosed block

        return wrap(sb.toString());
    }

    /** Wraps a body fragment in the full dark-theme HTML document. */
    String wrap(String body) {
        return "<html><head><style>" + CSS + "</style></head><body>" + body + "</body></html>";
    }

    /** Applies inline markdown: **bold**, *italic*, `code`. Escapes HTML first. */
    String inline(String text) {
        String s = esc(text);
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        s = s.replaceAll("\\*(.+?)\\*",       "<i>$1</i>");
        s = s.replaceAll("`([^`]+)`",          "<code>$1</code>");
        return s;
    }

    /** Escapes characters that are meaningful in HTML. */
    String esc(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private static void closeList(StringBuilder sb, boolean inList) {
        if (inList) sb.append("</ul>");
    }
}
