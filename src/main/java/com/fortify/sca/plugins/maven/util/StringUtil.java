/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.util;

public class StringUtil {

    public static boolean isEmpty(String str) {
        return str == null || str.length() <= 0;
    }

    public static String encloseQuotes(String str) {
        if (!StringUtil.isEmpty(str) && !str.startsWith("\"")) {
            return "\"" + str + "\"";
        } else {
            return str;
        }
    }

    public static String createRegexFromFilePattern(String pattern) {
        String regex = "^";
        for(int i = 0; i < pattern.length(); ++i)
        {
            final char c = pattern.charAt(i);
            switch(c)
            {
                case '*':
                    regex += ".*";
                    break;
                case '?':
                    regex += '.';
                    break;
                case '.':
                    regex += "\\.";
                    break;
                case '\\':
                    regex += "\\\\";
                    break;
                default:
                    regex += c;
                    break;
            }
        }
        regex += '$';
        return regex;
    }
}
