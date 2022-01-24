package com.github.gpaddons.gpclaimexpiration.config;

import com.github.jikoo.planarwrappers.config.ParsedComplexSetting;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

public abstract class TreeMapSetting<K extends Comparable<K>, V> extends ParsedComplexSetting<NavigableMap<K, V>>
{

    protected TreeMapSetting(@NotNull ConfigurationSection section,
                             @NotNull String path, @NotNull TreeMap<K, V> defaultValue)
    {
        super(section, path, Collections.unmodifiableNavigableMap(defaultValue));
    }

    @Override
    protected @Nullable NavigableMap<K, V> convert(
            @Nullable ConfigurationSection value)
    {
        TreeMap<K, V> map = new TreeMap<>();

        if (value == null) {
            return map;
        }

        for (String section1Key : value.getKeys(true)) {
            K convertedKey = convertKey(section1Key);
            if (convertedKey == null || !value.isList(section1Key)) {
                continue;
            }

            for (String rawValue : value.getStringList(section1Key)) {
                V convertedValue = convertValue(rawValue);
                if (convertedValue != null) {
                    map.put(convertedKey, convertedValue);
                }
            }
        }

        return Collections.unmodifiableNavigableMap(map);
    }

    /**
     * Convert a {@link String} into a usable key.
     *
     * @param key the key in {@code String} form
     * @return the key or {@code null} if the key cannot be parsed
     */
    protected abstract @Nullable K convertKey(@NotNull String key);

    /**
     * Convert a {@link String} into a usable value.
     *
     * @param value the value in {@code String} form
     * @return the value or {@code null} if the value cannot be parsed
     */
    protected abstract @Nullable V convertValue(@NotNull String value);

}
