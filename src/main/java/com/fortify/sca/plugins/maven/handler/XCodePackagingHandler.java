/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import java.io.*;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import com.fortify.sca.plugins.maven.util.*;

public class XCodePackagingHandler extends AbstractPackagingHandler {

    XCodePackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    public Commandline buildMainCommand(String sourceanalyzer, List<String> options) throws CommandLineException {
        if (!SystemUtil.isMacOSX) {
            log.error("xcodebuild integration is supported on Mac OS X only.");
            return null;
        }

        MavenProject currentProject = session.getCurrentProject();
        // Copy xcodebuild into Fortify Working Dir
        File pluginWorkingDir = new File(System.getProperty("user.home") + "/.fortify/maven/");
        if (!pluginWorkingDir.exists()) {
            pluginWorkingDir.mkdirs();
        }
        File xcodebuild = new File(pluginWorkingDir, "xcodebuild");

        try {
            copyXcodebuild(xcodebuild);
        } catch (IOException e) {
            FileUtil.delete(pluginWorkingDir);
            throw new CommandLineException(e.getMessage(), e);
        }
        pluginWorkingDir.deleteOnExit();

        // Create mvn command
        Commandline cli = new Commandline();
        cli.setExecutable(SystemUtil.MVN);
        cli.setWorkingDirectory(currentProject.getBasedir());
        cli.createArg().setLine("compile");
        cli.createArg().setLine("-Dxcodebuild.path=" + xcodebuild.getAbsolutePath());  // For ca.mestevens.ios.xcode-maven-plugin
        cli.createArg().setLine("-Dxcodebuild=" + xcodebuild.getAbsolutePath());       // For de.felixschulze.maven.plugins.xcode.xcode-maven-plugin

        try {
            cli.addSystemEnvironment();

            // SET SCA OPTIONS into Env
            StringBuffer sb = new StringBuffer();
            for (String option : options) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(option);
            }
            cli.addEnvironment("FORTIFY_SCA_OPTIONS", sb.toString());

            String orgXcodebuildPath = SystemUtil.findExecutablePath("xcodebuild");
            if (orgXcodebuildPath == null) {
                throw new FileNotFoundException("Couldn't find xcodebuild. Verify your XCode installation.");
            }
            cli.addEnvironment("ORIGINAL_XCODEBUILD_PATH", orgXcodebuildPath);
            cli.addEnvironment("FORTIFY_SCA_EXECUTABLE_PATH", sourceanalyzer);

            // Modify PATH env
            String pathValue = pluginWorkingDir.getAbsolutePath() + File.pathSeparator + cli.getSystemEnvVars().getProperty("PATH");
            cli.addEnvironment("PATH", pathValue);
        } catch (Exception e) {
            throw new CommandLineException(e.getMessage(), e);
        }
        return cli;
    }

    public Commandline buildTestCommand(String sourceanalyzer, List<String> options) throws CommandLineException {
        return null;
    }

    private void copyXcodebuild(File xcodebuild) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream("bin/xcodebuild");
            outputStream = new FileOutputStream(xcodebuild);
            FileUtil.copy(inputStream, outputStream);
        } finally {
            FileUtil.closeQuietly(inputStream);
            FileUtil.closeQuietly(outputStream);
        }
        xcodebuild.setExecutable(true, true);
    }
}
