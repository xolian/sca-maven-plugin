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

public class EarPackagingHandler extends AbstractJavaPackagingHandler {

    private String DEFAULT_EAR_SRC_DIR = "src" + File.separator + "main" + File.separator + "application";

    EarPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    protected boolean addMainFiles(Commandline commandline) {

        boolean added = super.addMainFiles(commandline);

        MavenProject currentProject = session.getCurrentProject();
        Plugin earPlugin = currentProject.getPlugin("org.apache.maven.plugins:maven-ear-plugin");
        String earSourceDirPath = PluginUtil.getExecutionConfig(earPlugin, "default-ear", "earSourceDirectory", DEFAULT_EAR_SRC_DIR);

        if (FileUtil.containsFile(earSourceDirPath)) {
            commandline.createArg().setLine(FileUtil.normalizeFilePath(earSourceDirPath));
            added = true;
            log.info("Ear Source Dir: " + earSourceDirPath);
        } else {
            File earSourceDir = new File(currentProject.getBasedir(), earSourceDirPath);
            if (FileUtil.containsFile(earSourceDir)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(earSourceDir.getAbsolutePath()));
                added = true;
                log.info("Ear Source Dir: " + earSourceDir.getAbsolutePath());
            }
        }

        return added;
    }
}
