package world.bentobox.bentobox.lists;

public enum GameModePlaceholders {

    WORLD_FRIENDLY_NAME("world-friendlyname"),
    ISLAND_DISTANCE("island-distance"),
    ISLAND_PROTECTION_RANGE("island-protection-range"),
    ISLAND_OWNER("island-owner"),
    ISLAND_CREATION_DATE("island-creation-date"),
    ISLAND_SPAWNPOINT("island-spawnpoint"),
    ISLAND_NAME("island-name");

    private String placeholder;

    GameModePlaceholders(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
