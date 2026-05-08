package com.rogercm.aicodereviewer.model;

import com.rogercm.aicodereviewer.api.GroqApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageTest {

    @Test
    void commonExtensionsMappedCorrectly() {
        assertEquals("Java",       Language.fromExtension("java"));
        assertEquals("Kotlin",     Language.fromExtension("kt"));
        assertEquals("Python",     Language.fromExtension("py"));
        assertEquals("JavaScript", Language.fromExtension("js"));
        assertEquals("TypeScript", Language.fromExtension("ts"));
        assertEquals("Go",         Language.fromExtension("go"));
        assertEquals("Rust",       Language.fromExtension("rs"));
        assertEquals("SQL",        Language.fromExtension("sql"));
    }

    @Test
    void extensionLookupIsCaseInsensitive() {
        assertEquals("Java",   Language.fromExtension("JAVA"));
        assertEquals("Python", Language.fromExtension("PY"));
    }

    @Test
    void unknownExtensionIsReturnedAsIs() {
        assertEquals("scala", Language.fromExtension("scala"));
        assertEquals("lua",   Language.fromExtension("lua"));
    }

    @Test
    void nullExtensionReturnsEmpty() {
        assertEquals("", Language.fromExtension(null));
    }

    @Test
    void blankExtensionReturnsEmpty() {
        assertEquals("", Language.fromExtension("  "));
    }

    /**
     * Contract test: every language that has an entry in GroqApiClient.LANGUAGE_GUIDANCE
     * must be reachable via Language.fromExtension(), otherwise the language-specific
     * prompt guidance would never be triggered for any file.
     *
     * If this test fails after adding a new language: add its extension to
     * Language.KNOWN_EXTENSIONS and Language.fromExtension().
     */
    @Test
    void everyGuidanceKeyIsReachableViaFromExtension() {
        for (String guidanceLanguage : GroqApiClient.LANGUAGE_GUIDANCE.keySet()) {
            boolean reachable = Language.KNOWN_EXTENSIONS.stream()
                    .anyMatch(ext -> Language.fromExtension(ext).equals(guidanceLanguage));
            assertTrue(reachable,
                    "'" + guidanceLanguage + "' has guidance in GroqApiClient but no extension "
                    + "in Language.fromExtension() maps to it. Add the extension to both classes.");
        }
    }
}
