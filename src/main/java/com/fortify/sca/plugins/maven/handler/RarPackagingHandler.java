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

public class RarPackagingHandler extends AbstractJavaPackagingHandler {

    private String DEFAULT_RAR_SRC_DIR = "src" + File.separator + "main" + File.separator + "rar";

    RarPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    protected boolean addMainFiles(Commandline commandline) {

        boolean added = super.addMainFiles(commandline);

        MavenProject currentProject = session.getCurrentProject();
        Plugin rarPlugin = currentProject.getPlugin("org.apache.maven.plugins:maven-rar-plugin");
        String rarSourceDirPath = PluginUtil.getExecutionConfig(rarPlugin, "default-rar", "rarSourceDirectory", DEFAULT_RAR_SRC_DIR);

        if (FileUtil.containsFile(rarSourceDirPath)) {
            commandline.createArg().setLine(FileUtil.normalizeFilePath(rarSourceDirPath));
            added = true;
            log.info("Rar Source Dir: " + rarSourceDirPath);
        } else {
            File rarSourceDir = new File(currentProject.getBasedir(), rarSourceDirPath);
            if (FileUtil.containsFile(rarSourceDir)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(rarSourceDir.getAbsolutePath()));
                added = true;
                log.info("Rar Source Dir: " + rarSourceDir.getAbsolutePath());
            }
        }

        return added;
    }
}
