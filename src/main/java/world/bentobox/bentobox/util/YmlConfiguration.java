package world.bentobox.bentobox.util;

import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * A YamlConfiguration that supports comments
 *
 * Note: This includes a VERY SIMPLISTIC Yaml-parser, which sole purpose is to detect and store comments.
 */
public class YmlConfiguration extends YamlConfiguration {
    private YmlCommentParser commentParser = new YmlCommentParser();

    public String getComment(String key) {
        String comment = commentParser.getComment(key);
        return comment != null ? comment.replaceAll("^# ?", "").replaceAll("\n# ?", "") : null;
    }

    public Map<String,String> getComments() {
        return commentParser.getCommentMap();
    }

    public void addComment(String path, String comment) {
        commentParser.addComment(path, comment);
    }
    public void addComments(Map<String,String> comments) {
        commentParser.addComments(comments);
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
        super.loadFromString(contents);
        commentParser.loadFromString(contents);
    }

    @Override
    public String saveToString() {
        String ymlPure = super.saveToString();
        return commentParser.mergeComments(ymlPure);
    }

}