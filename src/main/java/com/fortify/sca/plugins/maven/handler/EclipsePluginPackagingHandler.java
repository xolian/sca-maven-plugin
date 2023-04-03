/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import com.fortify.sca.plugins.maven.util.*;

public class EclipsePluginPackagingHandler extends AbstractJavaPackagingHandler {

    EclipsePluginPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    protected void applyPluginMainConfig(List<String> options) {
        if (options.indexOf("-source") == -1) {
            MavenProject currentProject = session.getCurrentProject();
            // Respect tycho-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.eclipse.tycho:tycho-compiler-plugin");
            String source = PluginUtil.getExecutionConfig(plugin, "default-compile", "source");
            options.add("-source");
            if (source != null) {
                options.add(source);
            } else {
                options.add(DEFAULT_JAVA_SOURCE_VERSION);
            }
        }

        if (options.indexOf("-encoding") == -1) {
            MavenProject currentProject = session.getCurrentProject();
            // Respect tycho-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.eclipse.tycho:tycho-compiler-plugin");
            String encoding = PluginUtil.getExecutionConfig(plugin, "default-compile", "encoding");
            if (encoding != null) {
                options.add("-encoding");
                options.add(encoding);
            }
        }
    }
}
