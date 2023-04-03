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
 * Performs analysis for the specified build ID.
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ScanMojo extends AbstractSCAMojo {
    //////////////////////////////////////////////////////////////////////
    // SCA
    //////////////////////////////////////////////////////////////////////
    /**
     * Location and name of the generated FPR file. By default, top level project artifact ID and version is used for aggregate mode.
     * The current project artifact ID and version is used for individual mode.
     */
    @Parameter(property = "fortify.sca.resultsFile")
    private String resultsFile;

    /**
     * Specifies build label included in the generated FPR.
     */
    @Parameter(property = "fortify.sca.buildLabel", defaultValue = "${project.artifactId}-${project.version}")
    private String buildLabel;

    /**
     * Specifies build project name included in the generated FPR.
     */
    @Parameter(property = "fortify.sca.buildProject", defaultValue = "${project.artifactId}")
    private String buildProject;

    /**
     * Specifies the build version included in the generated FPR.
     */
    @Parameter(property = "fortify.sca.buildVersion", defaultValue = "${project.version}")
    private String buildVersion;

    /**
     * If set to true, includes FindBugs analysis results in the final report.
     */
    @Parameter(property = "fortify.sca.findbugs", defaultValue = "false")
    private boolean findbugs;

    /**
     * If set to true, source files are included in the FPR file.
     */
    @Parameter(property = "fortify.sca.renderSources", defaultValue = "true")
    private boolean renderSources;

    /**
     * If set to true, scans the project in Quick Scan Mode (uses the fortifysca-quickscan.properties file).
     * By default, this scan only searches for high-confidence, high-severity issues.
     */
    @Parameter(property = "fortify.sca.quickScan", defaultValue = "false")
    private boolean quickScan;

    /**
     * Speficies rules used during a scan. Use an outer &lt;rules&gt; tag
     * followed by nested &lt;rule&gt; tags to specify one or more rules files
     * to use during the scan.
     */
    @Parameter(property = "fortify.sca.rules")
    private String[] rules;

    /**
     * Specifies filter file to use during the scan.
     */
    @Parameter(property = "fortify.sca.filter")
    private String filter;

    /**
     * Specifies the number of worker threads for parallel analysis.
     */
    @Parameter(property = "fortify.sca.numOfWorkerThreads")
    private Integer numOfWorkerThreads;

    /**
     * Specifies the log file that is produced by SCA.
     */
    @Parameter(property = "fortify.sca.scanLogfile", defaultValue = "sca-scan.log" )
    private String logfile;
    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during the SCA scan.
     */
    @Parameter(property = "fortify.sca.scan.failOnError", defaultValue = "false")
    private boolean scaScanFailOnError;

    private final String scanArgFilePrefix = "sca-scan-";

    public void doExecute() throws MojoExecutionException {
        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();

            workingDirectory = topLevelProject.getBasedir();
            outputDirectory = new File(topLevelProject.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = scanArgFilePrefix + topLevelProject.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            if (StringUtil.isEmpty(resultsFile)) {
                resultsFile = topLevelProject.getArtifactId() + "-" + topLevelProject.getVersion() + ".fpr";
            }

            if (isFirstProject(project) && isLastProject(project)) {
                scan();
            } else if (isFirstProject(project) && !isLastProject(project)) {
                session.getUserProperties().setProperty(BUILD_ID_KEY, buildId);
                log.info("Skipping to scan in aggregate mode");
            } else if (!isFirstProject(project) && isLastProject(project)) {
                buildId = session.getUserProperties().getProperty(BUILD_ID_KEY);
                scan();
            } else {
                log.info("Skipping to scan in aggregate mode");
            }
        } else {
            workingDirectory = project.getBasedir();
            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);
            String argFileName = scanArgFilePrefix + project.getArtifactId() + ".txt";
            argFilePath = outputDirectory.getAbsolutePath() + File.separator + argFileName;

            String defaultBuildId = project.getArtifactId() + "-" + project.getVersion();
            if (!defaultBuildId.equals(buildId)) {
                log.warn("Specified build Id :" + buildId + " cannot by applied in individual mode");
                buildId = defaultBuildId;
            }

            if (StringUtil.isEmpty(resultsFile)) {
                resultsFile = project.getArtifactId() + "-" + project.getVersion() + ".fpr";
            }
            scan();
        }
    }

    private void scan() throws MojoExecutionException {
        Commandline commandline = new Commandline();

        commandline.setExecutable(sourceanalyzer);
        commandline.setWorkingDirectory(workingDirectory);
        try {
            commandline.addSystemEnvironment();

            for (String option : buildOptions()) {
                commandline.createArg().setLine(option);
            }

            log.info("Fail on Error: " + (scaFailOnError || scaScanFailOnError));
            log.info("Scanning...");
            log.info("Build ID: " + buildId);
            log.info("Build Label: " + buildLabel);
            log.info("Build Project: " + buildProject);
            log.info("Build Version: " + buildVersion);
            log.info("Results File: " + resultsFile);
            log.info("Findbugs: " + findbugs);

            executeSCACommand(commandline, argFilePath);
        } catch (CommandLineException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (scaFailOnError || scaScanFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }

    protected List<String> buildOptions() {
        List<String> options = super.buildOptions();

        OptionUtil.setOption(options, "-b", buildId);
        OptionUtil.setSwitchOption(options, "-scan", true);

        String logfilePath;
        if (FileUtil.isAbsolute(logfile)) {
            logfilePath = FileUtil.normalizeFilePath(logfile);
        } else {
            logfilePath = FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + logfile);
        }
        OptionUtil.setOption(options, "-logfile", logfilePath);
        String resultsPath;
        if (FileUtil.isAbsolute(resultsFile)) {
            resultsPath = FileUtil.normalizeFilePath(resultsFile);
        } else {
            resultsPath = FileUtil.normalizeFilePath(outputDirectory.getAbsolutePath() + File.separator + resultsFile);
        }
        OptionUtil.setOption(options, "-f", resultsPath);

        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();
            if (StringUtil.isEmpty(buildLabel)) {
                buildLabel = topLevelProject.getArtifactId() + "-" + topLevelProject.getVersion();
            }
            if (StringUtil.isEmpty(buildProject)) {
                buildProject = topLevelProject.getArtifactId();
            }
            if (StringUtil.isEmpty(buildVersion)) {
                buildVersion = topLevelProject.getVersion();
            }
        }
        OptionUtil.setOption(options, "-build-label", buildLabel);
        OptionUtil.setOption(options, "-build-project", buildProject);
        OptionUtil.setOption(options, "-build-version", buildVersion);
        OptionUtil.setOption(options, "-filter", filter);
        OptionUtil.setOption(options, "-j", numOfWorkerThreads);

        OptionUtil.setOption(options, "-rules", rules);

        OptionUtil.setSwitchOption(options, "-findbugs", findbugs);
        OptionUtil.setSwitchOption(options, "-disable-source-bundling", !renderSources);
        OptionUtil.setSwitchOption(options, "-quick", quickScan);

        return options;
    }
}
