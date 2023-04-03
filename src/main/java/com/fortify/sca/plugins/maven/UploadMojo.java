/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.*;
import com.fortify.sca.plugins.maven.util.*;

/**
 * Uploads analysis results to Software Security Center (SSC).
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class UploadMojo extends AbstractFortifyClientMojo {
    //////////////////////////////////////////////////////////////////////
    // FORTIFYCLIENT
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the file path to the FPR file to upload. By default, project artifact ID - version are used.
     */
    @Parameter(property = "fortify.sca.resultsFile")
    private String resultsFile;

    /**
     * Specifies the SSC URL to interact with during the upload actions.
     */
    @Parameter(property = "fortify.ssc.url", alias = "fortify.f360.url", required = true)
    private String sscUrl;

    /**
     * Specifies the SSC AnalysisUploadToken to use when attempting to upload the FPR to Software Security Center (SSC).
     */
    @Parameter(property = "fortify.ssc.authToken", alias = "fortify.f360.authToken", required = true)
    private String sscUploadToken;

    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during any SCA processing.
     */
    @Parameter(property = "fortify.sca.failOnError", defaultValue = "false")
    private boolean scaFailOnError;

    /**
     * If set to true, the build fails if errors occur during the upload.
     */
    @Parameter(property = "fortify.sca.upload.failOnError", defaultValue = "false")
    private boolean scaUploadFailOnError;

    private String mavenProjectName;

    private String mavenProjectVersion;

    public void doExecute() throws MojoExecutionException {
        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();

            workingDirectory = topLevelProject.getBasedir();
            outputDirectory = new File(topLevelProject.getBuild().getDirectory(), FORTIFY_DIR);
            mavenProjectName = topLevelProject.getArtifactId();
            mavenProjectVersion = topLevelProject.getVersion();

            if (StringUtil.isEmpty(resultsFile)) {
                resultsFile = topLevelProject.getArtifactId() + "-" + topLevelProject.getVersion() + ".fpr";
            }

            if (isFirstProject(project) && isLastProject(project)) {
                upload();
            } else if (isFirstProject(project) && !isLastProject(project)) {
                log.info("Skipping to upload in aggregate mode");
            } else if (!isFirstProject(project) && isLastProject(project)) {
                upload();
            } else {
                log.info("Skipping to upload in aggregate mode");
            }
        } else {
            workingDirectory = project.getBasedir();
            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);
            mavenProjectName = project.getArtifactId();
            mavenProjectVersion = project.getVersion();

            if (StringUtil.isEmpty(resultsFile)) {
                resultsFile = project.getArtifactId() + "-" + project.getVersion() + ".fpr";
            }
            upload();
        }
    }

    public void upload() throws MojoExecutionException {

        Commandline commandline = new Commandline();
        commandline.setExecutable(fortifyclient);
        commandline.setWorkingDirectory(workingDirectory);
        try {
            validateParameters();
            commandline.addSystemEnvironment();

            for (String option : buildOptions()) {
                commandline.createArg().setLine(option);
            }

            log.info("Uploading analysis result to SSC...");
            log.info("Application Version Id: " + sscApplicationVersionId != null ? sscApplicationVersionId : "N/A");
            log.info("Application Name: " + sscApplicationName != null ? sscApplicationName : "N/A");
            log.info("Application Version: " + sscApplicationVersion != null ? sscApplicationVersion : "N/A");
            log.info("File: " + resultsFile);

            executeCommand(commandline);
        } catch (IllegalArgumentException e) {
            handleException(e);
        } catch (CommandLineException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (scaFailOnError || scaUploadFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }

    protected void validateParameters() throws CommandLineException, IllegalArgumentException {
        if (StringUtil.isEmpty(sscApplicationVersionId) && (StringUtil.isEmpty(sscApplicationName) || StringUtil.isEmpty(sscApplicationVersion))) {
            log.warn("'sscApplicationName', 'sscApplicationVersion' and 'sscApplicationVersionId' are not specified.");
            String message = "Using " + project.getArtifactId() + " - " + project.getVersion() + " to try to get SSC Application Version ID";
            log.warn(message);
            sscApplicationVersionId = getApplicationVersionId(sscUrl, sscUploadToken, mavenProjectName, mavenProjectVersion);
        }
    }

    protected List<String> buildOptions() {
        List<String> options = new ArrayList<>();

        OptionUtil.setSwitchOption(options, "uploadFPR", true);
        OptionUtil.setOption(options, "-f", FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + resultsFile));

        if (!StringUtil.isEmpty(sscApplicationVersionId)) {
            OptionUtil.setOption(options, "-applicationVersionID", sscApplicationVersionId);
        } else {
            OptionUtil.setOption(options, "-application", sscApplicationName);
            OptionUtil.setOption(options, "-applicationVersion", sscApplicationVersion);
        }

        OptionUtil.setOption(options, "-authtoken", sscUploadToken);
        OptionUtil.setOption(options, "-url", sscUrl);

        return options;
    }
}
