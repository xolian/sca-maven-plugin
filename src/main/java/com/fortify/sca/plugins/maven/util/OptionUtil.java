/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.util;

import java.util.List;
import org.apache.maven.plugin.logging.Log;

public class OptionUtil {
    public static void setSwitchOption(List<String> options, String option, boolean isRequired) {
        if (isRequired) {
            options.add(option);
        }
    }

    public static void setJvmOption(List<String> options, String option, String value) {
        if (!StringUtil.isEmpty(value)) {
            options.add(option + value);
        }
    }

    public static void setOption(List<String> options, String option, String value) {
        if (!StringUtil.isEmpty(value)) {
            options.add(option);
            options.add(value);
        }
    }

    public static void setOption(List<String> options, String option, Integer value) {
        if (value != null && value > 0) {
            options.add(option);
            options.add(String.valueOf(value));
        }
    }

    public static void setOption(List<String> options, String option, String[] values) {
        if (values != null) {
            for (String value : values) {
                options.add(option);
                options.add(value);
            }
        }
    }

    public static void setProperty(List<String> options, String property, String value) {
        if (!StringUtil.isEmpty(value)) {
            options.add("-D" + property + "=" + value);
        }
    }

    public static void printOption(Log log, List<String> options, String option, String displayName)
    {
        if (!StringUtil.isEmpty(option)) {
            int index = options.indexOf(option);
            if (index != -1 && index < options.size() - 1) {
                log.info(displayName + ": " + options.get(index + 1));
            }
        }
    }
}
