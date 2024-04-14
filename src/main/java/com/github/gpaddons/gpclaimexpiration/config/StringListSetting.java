package com.github.gpaddons.gpclaimexpiration.config;

import com.github.jikoo.planarwrappers.config.ParsedSetting;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class StringListSetting extends ParsedSetting<List<String>>
{

    protected StringListSetting(
            @NotNull ConfigurationSection section,
            @NotNull String path,
            @NotNull List<String> defaultValue)
    {
        super(section, path, defaultValue);
    }

    @Override
    protected boolean test(@NotNull String path)
    {
        return section.isList(path);
    }

    @Override
    protected @Nullable List<String> convert(@NotNull String path)
    {
        return Collections.unmodifiableList(section.getStringList(path));
    }

}
