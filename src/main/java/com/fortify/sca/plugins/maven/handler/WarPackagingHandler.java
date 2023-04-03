/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import java.io.File;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.Commandline;
import com.fortify.sca.plugins.maven.util.*;

public class WarPackagingHandler extends AbstractJavaPackagingHandler {

    private String DEFAULT_WAR_SRC_DIR = "src" + File.separator + "main" + File.separator + "webapp";

    WarPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    protected boolean addMainFiles(Commandline commandline) {

        boolean added = super.addMainFiles(commandline);

        MavenProject currentProject = session.getCurrentProject();
        Plugin warPlugin = currentProject.getPlugin("org.apache.maven.plugins:maven-war-plugin");
        String warSourceDirPath = PluginUtil.getExecutionConfig(warPlugin, "default-war", "warSourceDirectory", DEFAULT_WAR_SRC_DIR);

        if (FileUtil.containsFile(warSourceDirPath)) {
            commandline.createArg().setLine(FileUtil.normalizeFilePath(warSourceDirPath));
            added = true;
            log.info("War Source Dir: " + warSourceDirPath);
        } else {
            File warSourceDir = new File(currentProject.getBasedir(), warSourceDirPath);
            if (FileUtil.containsFile(warSourceDir)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(warSourceDir.getAbsolutePath()));
                added = true;
                log.info("War Source Dir: " + warSourceDir.getAbsolutePath());
            }
        }

        return added;
    }
}
