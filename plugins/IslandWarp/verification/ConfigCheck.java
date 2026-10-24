import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigCheck {
    public static void main(String[] args) throws Exception {
        var defaults=new YamlConfiguration();
        defaults.loadFromString("comments:\n  enabled: true\n  notify-reply: true\nlimits:\n  per-player: 5\n");
        var config=new YamlConfiguration();
        config.loadFromString("comments:\n  enabled: false\nlimits:\n  per-player: 12\ncustom: keep\n");
        config.setDefaults(defaults);
        config.options().copyDefaults(true);
        var saved=new YamlConfiguration();
        saved.loadFromString(config.saveToString());
        if(saved.getBoolean("comments.enabled")||!saved.getBoolean("comments.notify-reply")||saved.getInt("limits.per-player")!=12||!saved.getString("custom").equals("keep")) throw new AssertionError("Missing defaults or overwritten values");
        try { saved.loadFromString("comments: [broken"); throw new AssertionError("Invalid YAML accepted"); }
        catch(org.bukkit.configuration.InvalidConfigurationException expected) {}
        var base=java.nio.file.Path.of(System.getProperty("project.basedir",System.getProperty("user.dir")));
        var template=new YamlConfiguration();
        template.loadFromString(java.nio.file.Files.readString(base.resolve("src/main/resources/config.yml")));
        if(template.contains("ratings.min-stars")||template.contains("ratings.max-stars")) throw new AssertionError("Unsupported rating bounds in template");
        var reference=new YamlConfiguration(); reference.load(base.resolve("配置参考.yml").toFile());
        if(!template.saveToString().equals(reference.saveToString())) throw new AssertionError("Reference configuration drift");
        System.out.println("CONFIG_CHECK_PASS: missing defaults filled, existing values preserved, invalid YAML rejected");
    }
}
