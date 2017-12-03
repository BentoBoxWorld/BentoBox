package us.tastybento.bskyblock.api.localization;

public class Variable {

    private String placeholder;
    private String replacement;

    public Variable(String placeholder, String replacement) {
        this.placeholder = placeholder;
        this.replacement = replacement;
    }

    public String apply(String s) {
        return s.replace(this.placeholder, this.replacement);
    }
}
