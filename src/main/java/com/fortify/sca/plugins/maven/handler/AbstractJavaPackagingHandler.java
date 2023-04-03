/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import com.fortify.sca.plugins.maven.util.*;

public abstract class AbstractJavaPackagingHandler extends AbstractPackagingHandler {

    protected String DEFAULT_JAVA_SOURCE_VERSION = "1.8";

    AbstractJavaPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }

    public Commandline buildMainCommand(String sourceanalyzer, List<String> options) throws CommandLineException {
        applyPluginMainConfig(options);
        Commandline commandline = buildBaseCommand(sourceanalyzer, options, false);

        boolean added = addMainFiles(commandline);

        if (added) {
            return commandline;
        } else {
            return null;
        }
    }

    public Commandline buildTestCommand(String sourceanalyzer, List<String> options) throws CommandLineException {
        applyPluginTestConfig(options);
        Commandline commandline = buildBaseCommand(sourceanalyzer, options, true);

        boolean added = addTestFiles(commandline);

        if (added) {
            return commandline;
        } else {
            return null;
        }
    }

    protected void applyPluginMainConfig(List<String> options) {
        if (options.indexOf("-source") == -1) {
            MavenProject currentProject = session.getCurrentProject();
            // Respect maven-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
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
            // Respect maven-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
            String encoding = PluginUtil.getExecutionConfig(plugin, "default-compile", "encoding");
            if (encoding != null) {
                options.add("-encoding");
                options.add(encoding);
            }
        }
    }

    protected Commandline buildBaseCommand(String sourceanalyzer, List<String> options, boolean isTest) throws CommandLineException {
        MavenProject currentProject = session.getCurrentProject();
        Commandline commandline = new Commandline();
        commandline.setExecutable(sourceanalyzer);
        commandline.setWorkingDirectory(currentProject.getBasedir());
        try {
            commandline.addSystemEnvironment();
            // Set SCA Translation options
            for (String option : options) {
                commandline.createArg().setLine(option);
            }

            Set<String> classpathElements = new LinkedHashSet<String>();

            for (String classpath : currentProject.getCompileClasspathElements()) {
                if (FileUtil.containsClass(classpath) && !currentProject.getBuild().getOutputDirectory().equals(classpath)) {
                    classpathElements.add(classpath);
                }
            }

            ArtifactRepository localRepository = session.getLocalRepository();
            for (Artifact artifact : currentProject.getArtifacts()){
                if (isTest) {
                    if (artifact.getScope().equals("test")) {
                        File jarFile = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));
                        if (FileUtil.containsClass(jarFile)) {
                            classpathElements.add(jarFile.getAbsolutePath());
                        }
                    }
                } else {
                    if (!artifact.getScope().equals("test")) {
                        File jarFile = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));
                        if (FileUtil.containsClass(jarFile)) {
                            classpathElements.add(jarFile.getAbsolutePath());
                        }
                    }
                }
            }

            for (String cp : classpathElements) {
                commandline.createArg().setLine("-cp");
                commandline.createArg().setLine(FileUtil.normalizeFilePath(cp));
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new CommandLineException(e.getMessage(), e);
        } catch (Exception e) {
            throw new CommandLineException(e.getMessage());
        }
        return commandline;
    }

    protected boolean addMainFiles(Commandline commandline) {
        boolean added = false;
        MavenProject currentProject = session.getCurrentProject();
        // main java source files
        List<String> sourceDirs = new ArrayList<>();
        for (String sourceDirPath : currentProject.getCompileSourceRoots()) {
            if (FileUtil.containsFile(sourceDirPath)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(sourceDirPath));
                added = true;
                sourceDirs.add(sourceDirPath);
            }
        }

        for (String sourceDir : sourceDirs) {
            log.info("Source File Path: " + sourceDir);
        }

        // main script files
        List<String> scriptSourceDirs = new ArrayList<>();
        String scriptDirPath = currentProject.getBuild().getScriptSourceDirectory();
        if (FileUtil.containsFile(scriptDirPath)) {
            commandline.createArg().setLine(FileUtil.normalizeFilePath(scriptDirPath));
            added = true;
            scriptSourceDirs.add(scriptDirPath);
        }

        for (String scriptDir : scriptSourceDirs) {
            log.info("Script File Path: " + scriptDir);
        }

        // main resource files
        List<String> resourceDirs = new ArrayList<>();
        for (String resourceFilePath : getResourcesAsStringList(currentProject.getResources())) {
            if (FileUtil.containsFile(resourceFilePath)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(resourceFilePath));
                added = true;
                resourceDirs.add(resourceFilePath);
            }
        }

        for (String resourceDir : resourceDirs) {
            log.info("Resources: " + resourceDir);
        }

        return added;
    }

    protected void applyPluginTestConfig(List<String> options) {
        if (options.indexOf("-source") == -1) {
            MavenProject currentProject = session.getCurrentProject();
            // Respect maven-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
            String source = PluginUtil.getExecutionConfig(plugin, "default-test-compile", "source");
            options.add("-source");
            if (source != null) {
                options.add(source);
            } else {
                options.add(DEFAULT_JAVA_SOURCE_VERSION);
            }
        }

        if (options.indexOf("-encoding") == -1) {
            MavenProject currentProject = session.getCurrentProject();
            // Respect maven-compiler-plugin configuration
            Plugin plugin = currentProject.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
            String encoding = PluginUtil.getExecutionConfig(plugin, "default-test-compile", "encoding");
            if (encoding != null) {
                options.add("-encoding");
                options.add(encoding);
            }
        }
    }

    protected boolean addTestFiles(Commandline commandline) {
        boolean added = false;
        MavenProject currentProject = session.getCurrentProject();
        // test java source files

        List<String> sourceDirs = new ArrayList<>();
        for (String sourceDirPath : currentProject.getTestCompileSourceRoots()) {
            if (FileUtil.containsFile(sourceDirPath)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(sourceDirPath));
                added = true;
                sourceDirs.add(sourceDirPath);
            }
        }

        for (String sourceDir : sourceDirs) {
            log.info("Test Source File Path: " + sourceDir);
        }

        // test resource files
        List<String> resourceDirs = new ArrayList<>();
        for (String resourceFilePath : getResourcesAsStringList(currentProject.getTestResources())){
            if (FileUtil.containsFile(resourceFilePath)) {
                commandline.createArg().setLine(FileUtil.normalizeFilePath(resourceFilePath));
                added = true;
                resourceDirs.add(resourceFilePath);
            }
        }

        for (String resourceDir : resourceDirs) {
            log.info("Test Resources: " + resourceDir);
        }

        return added;
    }
}
