package com.rogercm.aicodereviewer.model;

/**
 * Maps file extensions to canonical language names used throughout the plugin —
 * both in the Groq API prompt (GroqApiClient.LANGUAGE_GUIDANCE keys) and in the
 * code-fence label sent to the model.
 *
 * Single source of truth: adding a new language here automatically makes it
 * available to every consumer without needing to update multiple switch statements.
 */
public final class Language {

    private Language() {}

    /** Representative extensions used by tests to verify Language ↔ GroqApiClient sync. */
    public static final java.util.List<String> KNOWN_EXTENSIONS = java.util.List.of(
            "java", "kt", "py", "js", "ts", "go", "rs", "sql", "rb", "cs", "cpp", "swift", "php"
    );

    public static String fromExtension(String extension) {
        if (extension == null || extension.isBlank()) return "";
        return switch (extension.toLowerCase()) {
            case "java"          -> "Java";
            case "kt", "kts"     -> "Kotlin";
            case "py"            -> "Python";
            case "js"            -> "JavaScript";
            case "ts"            -> "TypeScript";
            case "tsx", "jsx"    -> "React";
            case "cpp", "cc"     -> "C++";
            case "c"             -> "C";
            case "cs"            -> "C#";
            case "go"            -> "Go";
            case "rs"            -> "Rust";
            case "rb"            -> "Ruby";
            case "php"           -> "PHP";
            case "swift"         -> "Swift";
            case "sql"           -> "SQL";
            case "sh"            -> "Shell";
            case "html"          -> "HTML";
            case "css"           -> "CSS";
            case "xml"           -> "XML";
            case "json"          -> "JSON";
            case "yaml", "yml"   -> "YAML";
            default              -> extension;
        };
    }
}
