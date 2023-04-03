/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.util;

import java.io.File;
import java.io.IOException;
import org.codehaus.plexus.util.cli.CommandLineUtils;

public class SystemUtil {
    public static final boolean isWindows;
    public static final boolean isMacOSX;
    public static final boolean isSolaris;
    public static final boolean isLinux;
    public static final boolean isAIX;
    public static final boolean isHPUX;
    public static final String MVN;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        isWindows = osName.indexOf("win") != -1;
        isMacOSX = osName.indexOf("mac") != -1;
        isSolaris = osName.indexOf("sunos") != -1 ||osName.indexOf("solaris") != -1;
        isLinux = osName.indexOf("linux") != -1;
        isAIX = osName.indexOf("aix") != -1;
        isHPUX = osName.indexOf("hp-ux") != -1;
        MVN = isWindows ? "mvn.bat" : "mvn";
    }

    public static String findExecutablePath(String executable) throws IOException {
        String[] pathList = CommandLineUtils.getSystemEnvVars().getProperty("PATH").split(File.pathSeparator);
        return findExecutablePath(pathList, executable);
    }

    public static String findExecutablePath(String[] pathArray, String executable) {
        if (pathArray == null || executable == null || executable.isEmpty()) {
            return null;
        }

        for (String path : pathArray) {
            File exe = new File(path, executable);
            try {
                exe = exe.getCanonicalFile();
            } catch (IOException e) {
                continue;
            }
            if (exe.isFile()) {
                return exe.getAbsolutePath();
            }
        }
        return null;
    }
}
