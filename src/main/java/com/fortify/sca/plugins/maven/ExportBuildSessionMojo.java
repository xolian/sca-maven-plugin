/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.*;
import com.fortify.sca.plugins.maven.util.*;

/**
 * Exports a mobile build session for the specified build ID.
 */
@Mojo(name = "exportBuildSession", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class ExportBuildSessionMojo extends AbstractSCAMojo {
    //////////////////////////////////////////////////////////////////////
    // SCA
    //////////////////////////////////////////////////////////////////////
    /**
     * Location and name of the generated build session file.
     */
    @Parameter(property = "fortify.sca.buildSessionFile")
    private String buildSessionFile;

    /**
     * Specifies the log file that is produced by Fortify SCA.
     */
    @Parameter(property = "fortify.sca.mbsLogfile", defaultValue = "sca-export-build-session.log" )
    private String logfile;
    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during SCA mobile build session export.
     */
    @Parameter(property = "fortify.sca.exportBuildSession.failOnError", defaultValue = "false")
    private boolean scaExportFailOnError;

    private final String exportArgFilePrefix = "sca-export-";

    public void doExecute() throws MojoExecutionException {
        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();

            workingDirectory = topLevelProject.getBasedir();
            outputDirectory = new File(topLevelProject.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = exportArgFilePrefix + topLevelProject.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            if (StringUtil.isEmpty(buildSessionFile)) {
                buildSessionFile = topLevelProject.getArtifactId() + "-" + topLevelProject.getVersion() + ".mbs";
            }

            if (isFirstProject(project) && isLastProject(project)) {
                exportMBS();
            } else if (isFirstProject(project) && !isLastProject(project)) {
                session.getUserProperties().setProperty(BUILD_ID_KEY, buildId);
                log.info("Skipping to export in aggregate mode");
            } else if (!isFirstProject(project) && isLastProject(project)) {
                buildId = session.getUserProperties().getProperty(BUILD_ID_KEY);
                exportMBS();
            } else {
                log.info("Skipping to export in aggregate mode");
            }
        } else {
            workingDirectory = project.getBasedir();
            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = exportArgFilePrefix + project.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            String defaultBuildId = project.getArtifactId() + "-" + project.getVersion();
            if (!defaultBuildId.equals(buildId)) {
                log.warn("Specified build Id :" + buildId + " cannot by applied in individual mode");
                buildId = defaultBuildId;
            }

            if (StringUtil.isEmpty(buildSessionFile)) {
                buildSessionFile = project.getArtifactId() + "-" + project.getVersion() + ".mbs";
            }
            exportMBS();
        }
    }

    private void exportMBS() throws MojoExecutionException {
        Commandline commandline = new Commandline();

        commandline.setExecutable(sourceanalyzer);
        commandline.setWorkingDirectory(workingDirectory);
        try {
            commandline.addSystemEnvironment();

            for (String option : buildOptions()) {
                commandline.createArg().setLine(option);
            }

            log.info("Exporting Mobile Build Session...");
            log.info("Build ID: " + buildId);

            executeSCACommand(commandline, argFilePath);
        } catch (CommandLineException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (scaFailOnError || scaExportFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }

    protected List<String> buildOptions() {
        List<String> options = super.buildOptions();

        OptionUtil.setOption(options, "-b", buildId);
        String logfilePath;
        if (FileUtil.isAbsolute(logfile)) {
            logfilePath = FileUtil.normalizeFilePath(logfile);
        } else {
            logfilePath = FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + logfile);
        }
        OptionUtil.setOption(options, "-logfile", logfilePath);
        OptionUtil.setOption(options, "-export-build-session", FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + buildSessionFile));

        return options;
    }
}
