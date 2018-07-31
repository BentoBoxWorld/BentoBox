package world.bentobox.bentobox.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

/**
 * A very simplistic yml parser, that only do the following:
 * <ol>
 *     <li>Keep track of indentation-levels and sections.</li>
 *     <li>Handle comments.</li>
 * </ol>
 */
public class YmlCommentParser {
    private static final Logger log = Bukkit.getLogger();
    private static final Pattern SECTION_PATTERN = Pattern.compile("^(?<indent>\\s*)(?<name>[^ \\-][^:]*):(?<value>[^#]*)?(?<comment>#.*)?");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^(?<indent>\\s*)(?<comment>#.*)");
    private Map<String, String> commentMap = new HashMap<>();

    public Map<String, String> getCommentMap() {
        return Collections.unmodifiableMap(commentMap);
    }


    public void addComment(String path, String comment) {
        commentMap.put(path, comment);
    }

    public void addComments(Map<String,String> comments) {
        commentMap.putAll(comments);
    }

    private void readLines(BufferedReader rdr) throws IOException {
        int indentLevel = 0;
        Deque<Section> sections = new ArrayDeque<>();
        StringBuilder comments = new StringBuilder();
        String baseKey = null;
        sections.add(new Section(null, 0));
        String line;
        int lineNum = 1;
        boolean isFirstAfterSection = true;
        while ((line = rdr.readLine()) != null) {
            Matcher commentM = COMMENT_PATTERN.matcher(line);
            Matcher sectionM = SECTION_PATTERN.matcher(line);
            if (commentM.matches()) {
                comments.append(commentM.group("comment")).append("\n");
            } else if (sectionM.matches()) {
                String comment = sectionM.group("comment");
                if (comment != null && !comment.trim().isEmpty()) {
                    comments.append(comment).append("\n");
                }
                String name = sectionM.group("name").trim();
                String value = sectionM.group("value");
                String indent = sectionM.group("indent");
                if (isFirstAfterSection && indent.length() > indentLevel) {
                    indentLevel = indent.length();
                    sections.peek().setIndentation(indentLevel);
                } else if (indent.length() < indentLevel) {
                    while (indent.length() < indentLevel && !sections.isEmpty()) {
                        sections.pop();
                        baseKey = sections.peek().getPath();
                        indentLevel = sections.peek().getIndentation();
                        isFirstAfterSection = false;
                    }
                }
                String path = getPath(baseKey, name);
                if (value != null && !value.trim().isEmpty()) {
                    // Scalar with value
                    addComments(path, comments);
                    if (!isFirstAfterSection && indent.length() > indentLevel) {
                        log.warning("line " + lineNum + ": mixed indentation, expected " + indentLevel + " but got " + indent.length());
                    }
                    isFirstAfterSection = false;
                } else if (indent.length() >= indentLevel) {
                    indentLevel = indent.length();
                    sections.push(createSection(path, indentLevel, comments));
                    baseKey = path;
                    isFirstAfterSection = true;
                }
            } else if (line.trim().isEmpty()) {
                // Currently gathered comments are reset - they are "floating", decoupled from sections.
                comments.setLength(0);
                comments.trimToSize();
            }
            lineNum++;
        }
    }

    private String getPath(String baseKey, String name) {
        return baseKey != null ? baseKey + "." + name : name;
    }

    private Section createSection(String path, int indentLevel, StringBuilder comments) {
        Section section = new Section(path, indentLevel);
        addComments(path, comments);
        return section;
    }

    private void addComments(String path, StringBuilder comments) {
        if (comments.length() > 0) {
            commentMap.put(path, comments.toString());
            comments.setLength(0);
            comments.trimToSize();
        }
    }

    public String getComment(String path) {
        return commentMap.get(path);
    }

    /**
     * Merges the comments into the "pure" yml.
     * @param ymlPure A YML data-tree, without comments.
     * @return A YML data-tree including comments.
     */
    public String mergeComments(String ymlPure) {
        StringBuilder sb = new StringBuilder();
        boolean isFirstAfterSection = true;
        Deque<Section> sections = new ArrayDeque<>();
        sections.push(new Section(null, 0));
        int indentLevel = 0;
        String baseKey = null;
        int lineNum = 1;
        // First section shares comments with the header - so ignore that one
        boolean isHeader = true;
        for (String line : ymlPure.split("\n")) {
            // Skip header
            Matcher commentM = COMMENT_PATTERN.matcher(line);
            if (isHeader && (commentM.matches() || line.trim().isEmpty())) {
                continue; // Skip header
            }
            isHeader = false;
            Matcher sectionM = SECTION_PATTERN.matcher(line);
            if (sectionM.matches()) {
                String name = sectionM.group("name").trim();
                String value = sectionM.group("value");
                String indent = sectionM.group("indent");
                if (isFirstAfterSection && indent.length() > indentLevel) {
                    indentLevel = indent.length();
                    sections.peek().setIndentation(indentLevel);
                } else if (indent.length() < indentLevel) {
                    while (indent.length() < indentLevel && !sections.isEmpty()) {
                        sections.pop();
                        baseKey = sections.peek().getPath();
                        indentLevel = sections.peek().getIndentation();
                        isFirstAfterSection = false;
                    }
                }
                String path = getPath(baseKey, name);
                String comment = getComment(path);
                if (comment != null) {
                    sb.append(lineNum > 1 ? "\n" : "").append(comment
                            .replaceAll("^#", Matcher.quoteReplacement(indent + "#"))
                            .replaceAll("\n#", Matcher.quoteReplacement("\n" + indent + "#")));
                }
                if (value != null && !value.trim().isEmpty()) {
                    // Scalar with value
                    isFirstAfterSection = false;
                } else if (indent.length() >= indentLevel) {
                    indentLevel = indent.length();
                    sections.push(new Section(path, indentLevel));
                    baseKey = path;
                    isFirstAfterSection = true;
                }
            }
            lineNum++;
            sb.append(line).append("\n");
        }
        return sb.toString().replaceAll("\r\n", "\n").replaceAll("\n\r", "\n").replaceAll("\n", "\r\n");
    }

    public void load(Reader reader) throws IOException {
        readLines(new BufferedReader(reader));
    }

    public void loadFromString(String contents) {
        try {
            readLines(new BufferedReader(new StringReader(contents)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read from string", e);
        }
    }


    private static class Section {
        private int indentation;
        private final String path;

        private Section(String name, int indentation) {
            this.indentation = indentation;
            path = name;
        }

        public int getIndentation() {
            return indentation;
        }

        public String getPath() {
            return path;
        }

        public void setIndentation(int indentLevel) {
            indentation = indentLevel;
        }
    }
}