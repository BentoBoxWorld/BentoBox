package us.tastybento.bskyblock.api.placeholders;

public class PlaceholderBuilder {

    private String identifier;
    private Placeholder.PlaceholderRequest value;

    public PlaceholderBuilder identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * The value this placeholder should take
     * @param value
     * @return
     */
    public PlaceholderBuilder value(Placeholder.PlaceholderRequest value) {
        this.value = value;
        return this;
    }

    public Placeholder build() {
        return new Placeholder(identifier, value);
    }
}
