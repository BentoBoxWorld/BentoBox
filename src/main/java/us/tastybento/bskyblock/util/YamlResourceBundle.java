/*
 * https://github.com/akihyro/yaml-resource-bundle
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.tastybento.bskyblock.util;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.yaml.snakeyaml.Yaml;

/**
 * {@link ResourceBundle} for YAML format.
 */
public class YamlResourceBundle extends ResourceBundle {

    /**
     * Entries.
     */
    private final Map<String, Object> entries;

    /**
     * Constructor.
     *
     * @param string YAML data.
     */
    public YamlResourceBundle(String string) {
        entries = flattenYamlTree(new Yaml().loadAs(string, Map.class));
    }

    /**
     * Constructor.
     *
     * @param stream YAML data as input stream.
     */
    public YamlResourceBundle(InputStream stream) {
        entries = flattenYamlTree(new Yaml().loadAs(stream, Map.class));
    }

    /**
     * Constructor.
     *
     * @param reader YAML data as input character stream.
     */
    public YamlResourceBundle(Reader reader) {
        entries = flattenYamlTree(new Yaml().loadAs(reader, Map.class));
    }

    /**
     * Flatten yaml tree structure.
     *
     * @param map {@link Map} of yaml tree.
     * @return {@link Map} of entries.
     */
    private static Map<String, Object> flattenYamlTree(Map<?, ?> map) {
        return map.entrySet().stream()
                .flatMap(YamlResourceBundle::flattenYamlTree)
                .collect(toMap(
                        e -> e.getKey(),
                        e -> e.getValue(),
                        (oldValue, newValue) -> newValue
                ));
    }

    /**
     * Flatten yaml tree structure.
     *
     * @param entry {@link Entry} of yaml tree.
     * @return {@link Stream} of entries
     */
    private static Stream<Entry<String, Object>> flattenYamlTree(Entry<?, ?> entry) {
        String key = entry.getKey().toString();
        Object value = entry.getValue();
        if (value instanceof Map) {
            Map<?, ?> valueAsMap = (Map<?, ?>) value;
            return valueAsMap.entrySet().stream()
                    .flatMap(YamlResourceBundle::flattenYamlTree)
                    .map(e -> new SimpleImmutableEntry<>(key + "." + e.getKey(), e.getValue()));
        } else if (value instanceof List) {
            List<?> valueAsList = (List<?>) value;
            value = valueAsList.stream().toArray(String[]::new);
            AtomicInteger index = new AtomicInteger();
            return Stream.concat(
                    Stream.of(new SimpleImmutableEntry<>(key, value)),
                    valueAsList.stream()
                            .map(v -> new SimpleImmutableEntry<>(key + "[" + index.getAndIncrement() + "]", v))
            );
        }
        return Stream.of(new SimpleImmutableEntry<>(key, value));
    }

    /** {@inheritDoc} */
    @Override
    protected Set<String> handleKeySet() {
        return entries.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getKeys() {
        return enumeration(keySet());
    }

    /** {@inheritDoc} */
    @Override
    protected Object handleGetObject(String key) {
        return entries.get(key);
    }

    /**
     * {@link ResourceBundle.Control} for YAML format.
     */
    public static class Control extends ResourceBundle.Control {

        /**
         * Singleton instance.
         */
        public static final Control INSTANCE = new Control();

        /**
         * Constructor.
         */
        protected Control() {
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getFormats(String baseName) {
            return unmodifiableList(asList("yaml", "yml"));
        }

        /** {@inheritDoc} */
        @Override
        public ResourceBundle newBundle(String baseName,
                                        Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            if (!getFormats(baseName).contains(format)) {
                return null;
            }
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, format);
            InputStream stream = loader.getResourceAsStream(resourceName);
            try {
                return new YamlResourceBundle(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }

        }

    }

}