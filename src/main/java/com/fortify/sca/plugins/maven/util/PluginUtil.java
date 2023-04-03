/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.util;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class PluginUtil {

    public static String getConfig(Plugin plugin, String name) {
        if (plugin != null && !StringUtil.isEmpty(name)) {
            Object config = plugin.getConfiguration();
            if (config instanceof Xpp3Dom) {
                Xpp3Dom xml = (Xpp3Dom)config;
                Xpp3Dom child = xml.getChild(name);
                if (child != null) {
                    return child.getValue();
                }
            }
        }

        return null;
    }

    public static String getExecutionConfig(Plugin plugin, String executionId, String name) {
        if (plugin != null && !StringUtil.isEmpty(name)) {
            for (PluginExecution execution : plugin.getExecutions()) {
                if (execution.getId().equals(executionId)) {
                    return getConfig(plugin, name);
                }
            }
        }
        return null;
    }

    public static String getExecutionConfig(Plugin plugin, String executionId, String name, String defaultValue) {
        String config = getExecutionConfig(plugin, executionId, name);
        if (StringUtil.isEmpty(config)) {
            return defaultValue;
        } else {
            return config;
        }
    }
}
