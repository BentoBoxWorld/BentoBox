package us.tastybento.bskyblock.api.localization;

import java.util.HashMap;
import java.util.Map;

public class LocaleHandler {

    private String identifier;
    private Map<String, BSBLocale> locales;

    public LocaleHandler(String identifier) {
        this.identifier = identifier;
        this.locales = new HashMap<>();
    }

    public void setupLocales() {

    }

    public void loadLocales() {

    }

    public BSBLocale getLocale(String languageTag) {
        return locales.getOrDefault(languageTag, null);
    }

    public Map<String, BSBLocale> getLocales() {
        return this.locales;
    }

    public String getIdentifier() {
        return this.identifier;
    }
}
