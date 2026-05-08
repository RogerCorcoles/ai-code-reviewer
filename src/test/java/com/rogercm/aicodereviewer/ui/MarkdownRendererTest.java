package com.rogercm.aicodereviewer.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownRendererTest {

    private MarkdownRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new MarkdownRenderer();
    }

    // ── Inline formatting ─────────────────────────────────────────────────────

    @Test
    void boldMarkdownRendersAsHtmlBold() {
        assertContains(renderer.render("**hello world**"), "<b>hello world</b>");
    }

    @Test
    void italicMarkdownRendersAsHtmlItalic() {
        assertContains(renderer.render("*hello world*"), "<i>hello world</i>");
    }

    @Test
    void inlineCodeRendersAsCodeTag() {
        assertContains(renderer.render("`myMethod()`"), "<code>myMethod()</code>");
    }

    // ── Headings ──────────────────────────────────────────────────────────────

    @Test
    void h1HeadingIsRendered() {
        assertContains(renderer.render("# Title"), "<h1>");
        assertContains(renderer.render("# Title"), "Title");
    }

    @Test
    void h2HeadingIsRendered() {
        assertContains(renderer.render("## Section"), "<h2>");
        assertContains(renderer.render("## Section"), "Section");
    }

    @Test
    void h3HeadingIsRendered() {
        assertContains(renderer.render("### Subsection"), "<h3>");
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    @Test
    void unorderedListItemRendersAsLiInsideUl() {
        String html = renderer.render("- first item");
        assertContains(html, "<ul>");
        assertContains(html, "<li>");
        assertContains(html, "first item");
    }

    @Test
    void numberedListItemRendersAsList() {
        String html = renderer.render("1. first item");
        assertContains(html, "<li>");
        assertContains(html, "first item");
    }

    @Test
    void consecutiveListItemsShareOneUlBlock() {
        String html = renderer.render("- alpha\n- beta\n- gamma");
        int ulOpenCount  = countOccurrences(html, "<ul>");
        int ulCloseCount = countOccurrences(html, "</ul>");
        assertEquals(1, ulOpenCount,  "Expected exactly one <ul> opening tag");
        assertEquals(1, ulCloseCount, "Expected exactly one </ul> closing tag");
    }

    // ── Code blocks ───────────────────────────────────────────────────────────

    @Test
    void fencedCodeBlockRendersAsPreCode() {
        String html = renderer.render("```\nint x = 1;\n```");
        assertContains(html, "<pre><code>");
        assertContains(html, "int x = 1;");
        assertContains(html, "</code></pre>");
    }

    @Test
    void codeInsideBlockIsHtmlEscaped() {
        String html = renderer.render("```\n<script>alert('xss')</script>\n```");
        assertFalse(html.contains("<script>"), "Raw <script> tag must not appear in output");
        assertContains(html, "&lt;script&gt;");
    }

    @Test
    void unclosedCodeBlockDoesNotThrow() {
        assertDoesNotThrow(() -> renderer.render("```\nint x = 1;"));
    }

    // ── HTML escaping ─────────────────────────────────────────────────────────

    @Test
    void angledBracketsInTextAreEscaped() {
        String html = renderer.render("Use List<String> here");
        assertFalse(html.contains("<String>"), "Raw generic bracket must not appear as HTML tag");
        assertContains(html, "List&lt;String&gt;");
    }

    @Test
    void ampersandIsEscaped() {
        assertContains(renderer.esc("foo & bar"), "foo &amp; bar");
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void nullInputReturnsPlaceholderMessage() {
        String html = renderer.render(null);
        assertContains(html, "No content returned");
    }

    @Test
    void blankInputReturnsPlaceholderMessage() {
        String html = renderer.render("   ");
        assertContains(html, "No content returned");
    }

    @Test
    void outputIsAlwaysWrappedInHtmlDocument() {
        String html = renderer.render("hello");
        assertTrue(html.startsWith("<html>"), "Output must start with <html>");
        assertContains(html, "<style>");
        assertContains(html, "</html>");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void assertContains(String html, String expected) {
        assertTrue(html.contains(expected),
                "Expected to find «" + expected + "» in:\n" + html);
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }
}
