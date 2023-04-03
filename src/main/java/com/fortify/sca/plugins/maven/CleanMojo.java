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
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import com.fortify.sca.plugins.maven.util.*;

/**
 * Attempts to clean all NST files associated with the specified build ID.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class CleanMojo extends AbstractSCAMojo {
    //////////////////////////////////////////////////////////////////////
    // SCA
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the log file produced by SCA.
     */
    @Parameter(property = "fortify.sca.cleanLogfile", defaultValue = "sca-clean.log")
    private String logfile;

    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during SCA clean.
     */
    @Parameter(property = "fortify.sca.clean.failOnError", defaultValue = "false")
    private boolean scaCleanFailOnError;

    private final String cleanArgFilePrefix = "sca-clean-";

    protected void doExecute() throws MojoExecutionException {
        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();

            workingDirectory = topLevelProject.getBasedir();
            outputDirectory = new File(topLevelProject.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = cleanArgFilePrefix + topLevelProject.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            if (isFirstProject(project)) {
                clean();
            } else {
                log.info("Skipping to clean in aggregate mode");
            }
        } else {
            workingDirectory = project.getBasedir();
            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = cleanArgFilePrefix + project.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            String defaultBuildId = project.getArtifactId() + "-" + project.getVersion();
            if (!defaultBuildId.equals(buildId)) {
                log.warn("Specified build Id :" + buildId + " cannot by applied in individual mode");
                buildId = defaultBuildId;
            }

            clean();
        }
    }

    private void clean() throws MojoExecutionException {
        Commandline commandline = new Commandline();

        commandline.setExecutable(sourceanalyzer);
        commandline.setWorkingDirectory(workingDirectory);
        try {
            commandline.addSystemEnvironment();

            for (String option : buildOptions()) {
                commandline.createArg().setLine(option);
            }

            log.info("Fail on Error: " + (scaFailOnError || scaCleanFailOnError));
            log.info("Cleaning intermediate files...");
            log.info("Build ID: " + buildId);

            executeSCACommand(commandline, argFilePath);
        } catch (CommandLineException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (scaFailOnError || scaCleanFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage(), e);
        }
    }

    protected List<String> buildOptions() {
        List<String> options = super.buildOptions();

        OptionUtil.setOption(options, "-b", buildId);
        OptionUtil.setSwitchOption(options, "-clean", true);
        String logfilePath;
        if (FileUtil.isAbsolute(logfile)) {
            logfilePath = FileUtil.normalizeFilePath(logfile);
        } else {
            logfilePath = FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + logfile);
        }
        OptionUtil.setOption(options, "-logfile", logfilePath);

        return options;
    }
}
