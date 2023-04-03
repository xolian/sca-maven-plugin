/*
 * Copyright 2020 Micro Focus or one of its affiliates.
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
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import com.fortify.sca.plugins.maven.util.*;

/**
 * Performs the ScanCentral for the specified build ID or mobile build session.
 */
@Mojo(name = "startScanCentral", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class StartCloudscanMojo extends AbstractCloudScanMojo {
    //////////////////////////////////////////////////////////////////////
    // SCANCENTRAL START OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the SCA build ID. The default is project artifact ID and version. In aggregate mode, the top level project
     * artifact ID and version is used for all modules.
     */
    @Parameter(property = "fortify.ScanCentral.buildId", defaultValue = "${project.artifactId}-${project.version}")
    private String buildId;

    /**
     * Location and name of the existing mobile build session to be uploaded to the ScanCentral controller.
     */
    @Parameter(property = "fortify.ScanCentral.buildSessionFile")
    private String buildSessionFile;

    /**
     * Specifies project directory for the mobile build session export.
     */
    @Parameter(property = "fortify.ScanCentral.buildSessionProjectRoot")
    private String buildSessionProjectRoot;

    /**
     * Specifies the location and name of the FPR file to retrieve.
     */
    @Parameter(property = "fortify.ScanCentral.resultsFile")
    private String resultsFile;

    /**
     * If set to true, ScanCentral client waits for job to complete and download the results.
     */
    @Parameter(property = "fortify.ScanCentral.enableBlock", defaultValue = "false")
    private boolean enableBlock;

    /**
     * If set to true, the existing FPR or log file might be overwritten with new data.
     */
    @Parameter(property = "fortify.ScanCentral.overwrite", defaultValue = "false")
    private boolean overwrite;

    /**
     * Specifies an email address for job status notification.
     */
    @Parameter(property = "fortify.ScanCentral.email")
    private String emailAddress;

    /**
     * Specifies the location and name of the filter file to use during the scan (repeatable).
     */
    @Parameter(property = "fortify.ScanCentral.filter")
    private String filter;

    /**
     * Specifies the custom rule file/directory to use.
     */
    @Parameter(property = "fortify.ScanCentral.rules")
    private String rules;

    /**
     * Specifies the location and name of the log file produced by the ScanCentral client.
     */
    @Parameter(property = "fortify.ScanCentral.startLogfile", defaultValue = "scancentral-start.log")
    private String logFile;

    //////////////////////////////////////////////////////////////////////
    // SCANCENTRAL SSC OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the ScanCentral controller uploads the analysis results to SSC.
     * Note that this must be supplied in conjunction with sscApplicationVersionId,
     * or sscApplicationName and sscApplicationVersion combinations.
     */
    @Parameter(property = "fortify.ScanCentral.uploadToSSC", defaultValue = "false")
    private boolean uploadToSSC;

    /**
     * AnalysisUploadToken to use when attempting to upload fpr files to SSC.
     */
    @Parameter(property = "fortify.ScanCentral.ssc.uploadToken")
    private String sscUploadToken;

    /**
     * Specifies Issue Template to be applied to the ScanCentral.
     */
    @Parameter(property = "fortify.ScanCentral.issueTemplate", alias = "fortify.ScanCentral.projectTemplate")
    private String issueTemplate;

    //////////////////////////////////////////////////////////////////////
    // SCA JVM OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the maximum heap size of the JVM that runs SCA.
     */
    @Parameter(property = "fortify.ScanCentral.sca.Xmx", alias = "fortify.ScanCentral.sca.jvm.Xmx")
    private String maxHeap;

    /**
     * Specifies the thread stack size of JVM that runs SCA.
     */
    @Parameter(property = "fortify.ScanCentral.sca.Xss", alias = "fortify.ScanCentral.sca.jvm.Xss")
    private String stackSize;

    //////////////////////////////////////////////////////////////////////
    // SCA OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, SCA runs in debug mode. This can be useful for troubleshooting.
     */
    @Parameter(property = "fortify.ScanCentral.sca.debug", defaultValue = "false")
    private boolean debug;

    /**
     * IIf set to true, SCA sends verbose status messages to the console.
     */
    @Parameter(property = "fortify.ScanCentral.sca.verbose", defaultValue = "false")
    private boolean verbose;

    /**
     * If set true, SCA writes minimal message to the console.
     */
    @Parameter(property = "fortify.ScanCentral.sca.quiet", defaultValue = "false")
    private boolean quiet;

    /**
     * If set to true, SCA prints the version to the console.
     */
    @Parameter(property = "fortify.ScanCentral.sca.version", defaultValue = "true")
    private boolean version;

    /**
     * Build Label inserted in to generated FPR.
     */
    @Parameter(property = "fortify.ScanCentral.sca.buildLabel", defaultValue = "${project.artifactId}-${project.version}")
    private String buildLabel;

    /**
     * Build Project Name inserted in to generated FPR.
     */
    @Parameter(property = "fortify.ScanCentral.sca.buildProject", defaultValue = "${project.artifactId}")
    private String buildProject;

    /**
     * Build Version inserted in to generated FPR.
     */
    @Parameter(property = "fortify.ScanCentral.sca.buildVersion", defaultValue = "${project.version}")
    private String buildVersion;

    /**
     * If set to true, source files are included in the FPR file.
     */
    @Parameter(property = "fortify.ScanCentral.sca.renderSources", defaultValue = "true")
    private boolean renderSources;

    /**
     * If set to true, scans the project in Quick Scan Mode (uses the fortifysca-quickscan.properties file).
     * By default, this scan only searches for high-confidence, high-severity issues.
     */
    @Parameter(property = "fortify.ScanCentral.sca.quickScan", defaultValue = "false")
    private boolean quickScan;

    /**
     * Specify the number of worker threads for parallel analysis.
     */
    @Parameter(property = "fortify.ScanCentral.sca.numOfWorkerThreads")
    private Integer numOfWorkerThreads;

    /**
     * If set to true, the build fails if errors occur during the ScanCentral start.
     */
    @Parameter(property = "fortify.ScanCentral.start.failOnError", defaultValue = "false")
    private boolean scanCentralStartFailOnError;

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
                startCloudscan();
            } else if (isFirstProject(project) && !isLastProject(project)) {
                session.getUserProperties().setProperty(BUILD_ID_KEY, buildId);
                log.info("Skipping to ScanCentral in aggregate mode");
            } else if (!isFirstProject(project) && isLastProject(project)) {
                buildId = session.getUserProperties().getProperty(BUILD_ID_KEY);
                startCloudscan();
            } else {
                log.info("Skipping to ScanCentral in aggregate mode");
            }
        } else {
            workingDirectory = project.getBasedir();
            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);

            mavenProjectName = project.getArtifactId();
            mavenProjectVersion = project.getVersion();

            String defaultBuildId = project.getArtifactId() + "-" + project.getVersion();
            if (!defaultBuildId.equals(buildId)) {
                log.warn("Specified build Id :" + buildId + " cannot by applied in individual mode");
                buildId = defaultBuildId;
            }

            if (StringUtil.isEmpty(resultsFile)) {
                resultsFile = project.getArtifactId() + "-" + project.getVersion() + ".fpr";
            }
            startCloudscan();
        }
    }

    public void startCloudscan() throws MojoExecutionException {
        Commandline commandline = new Commandline();

        commandline.setExecutable(scancentral);
        commandline.setWorkingDirectory(workingDirectory);
        try {
            validateParameters();
            commandline.addSystemEnvironment();

            for (String option : buildOptions()) {
                commandline.createArg().setLine(option);
            }

            log.info("Starting ScanCentral...");
            log.info("Build ID: " + buildId);
            log.info("ScanCentral URL: " + scancentralCtrlUrl);
            log.info("Upload to SSC: " + uploadToSSC);
            if (uploadToSSC) {
                log.info("SSC URL: " + sscUrl);
                log.info("Application Version Id: " + sscApplicationVersionId != null ? sscApplicationVersionId : "N/A");
                log.info("Application Name: " + sscApplicationName != null ? sscApplicationName : "N/A");
                log.info("Application Version: " + sscApplicationVersion != null ? sscApplicationVersion : "N/A");
            }

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
        if (scanCentralFailOnError || scanCentralStartFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }

    private void validateParameters() throws CommandLineException, IllegalArgumentException {
        if (uploadToSSC) {
            if (StringUtil.isEmpty(sscUrl)) {
                throw new IllegalArgumentException("'sscUrl' is not specified.");
            }
            if (StringUtil.isEmpty(sscScanCentralCtrlToken)) {
                throw new IllegalArgumentException("'sscScanCentralCtrlToken' is not specified.");
            }
            if (StringUtil.isEmpty(sscUploadToken)) {
                throw new IllegalArgumentException("'sscUploadToken' is not specified.");
            }

            if (StringUtil.isEmpty(sscApplicationVersionId) && (StringUtil.isEmpty(sscApplicationName) || StringUtil.isEmpty(sscApplicationVersion))) {
                log.warn("'sscApplicationName', 'sscApplicationVersion' and 'sscApplicationVersionId' are not specified.");
                String message = "Using " + mavenProjectName + " - " + mavenProjectVersion + " to try to get SSC Application Version Id";
                log.warn(message);
                sscApplicationVersionId = getApplicationVersionId(sscUrl, sscUploadToken, mavenProjectName, mavenProjectVersion);
            }
        } else {
            if (StringUtil.isEmpty(this.scancentralCtrlUrl)) {
                StringBuffer errMsg = new StringBuffer();
                errMsg.append("ScanCentral controller URL is not specified.");
                if (!StringUtil.isEmpty(sscUrl) && !StringUtil.isEmpty(sscScanCentralCtrlToken)) {
                    errMsg.append(" When you want to upload analysis result to SSC, you need to set fortify.ScanCentral.uploadToSSC to true.");
                }
                throw new IllegalArgumentException(errMsg.toString());
            }
        }

        if (StringUtil.isEmpty(buildId) && StringUtil.isEmpty(buildSessionFile)) {
            throw new IllegalArgumentException("Both 'buildId' and 'buildSessionFile' are not specified.");
        }

        if (!StringUtil.isEmpty(buildSessionFile) && StringUtil.isEmpty(buildSessionProjectRoot)) {
            throw new IllegalArgumentException("'buildSessionFile' must be supplied in conjunction with 'buildSessionProjectRoot'.");
        }

        if (StringUtil.isEmpty(buildSessionFile) && !StringUtil.isEmpty(buildSessionProjectRoot)) {
            throw new IllegalArgumentException("'buildSessionProjectRoot' must be supplied in conjunction with 'buildSessionFile'.");
        }
    }

    protected List<String> buildOptions() {
        List<String> options = new ArrayList<String>();

        addCloudscanStartOptions(options);
        addSCAScanOptions(options);

        return options;
    }

    private void addCloudscanStartOptions(List<String> options) {
        if (uploadToSSC) {
            OptionUtil.setOption(options, "-sscurl", sscUrl);
            OptionUtil.setOption(options, "-ssctoken", sscScanCentralCtrlToken);
            OptionUtil.setSwitchOption(options, "start", true);
            OptionUtil.setSwitchOption(options, "-upload", true);
            if (!StringUtil.isEmpty(sscApplicationVersionId)) {
                OptionUtil.setOption(options, "--application-version-id", sscApplicationVersionId);
            } else {
                OptionUtil.setOption(options, "--application", StringUtil.encloseQuotes(sscApplicationName));
                OptionUtil.setOption(options, "--application-version", sscApplicationVersion);
            }
            OptionUtil.setOption(options, "-uptoken", sscUploadToken);
        } else {
            OptionUtil.setOption(options, "-url", scancentralCtrlUrl);
            OptionUtil.setSwitchOption(options, "start", true);
        }

        if (!StringUtil.isEmpty(buildSessionFile)) {
            OptionUtil.setOption(options, "-mbs", FileUtil.normalizeFilePath(buildSessionFile));
            OptionUtil.setOption(options, "-projroot", FileUtil.normalizeFilePath(buildSessionProjectRoot));
        } else {
            OptionUtil.setOption(options, "-b", StringUtil.encloseQuotes(buildId));
        }
        OptionUtil.setSwitchOption(options, "-block", enableBlock);
        OptionUtil.setOption(options, "-email", StringUtil.encloseQuotes(emailAddress));
        OptionUtil.setOption(options, "-f", FileUtil.normalizeFilePath(resultsFile));
        OptionUtil.setOption(options, "-filter", FileUtil.normalizeFilePath(filter));
        OptionUtil.setOption(options, "-log", FileUtil.normalizeFilePath(logFile));
        OptionUtil.setSwitchOption(options, "-o", overwrite);
        OptionUtil.setOption(options, "-projtl", FileUtil.normalizeFilePath(issueTemplate));
        OptionUtil.setOption(options, "-rules", FileUtil.normalizeFilePath(rules));

        // Set -scan : as delimiter
        OptionUtil.setSwitchOption(options, "-scan", true);
    }

    private void addSCAScanOptions(List<String> options) {

        OptionUtil.setJvmOption(options, "-Xmx", maxHeap);
        OptionUtil.setJvmOption(options, "-Xss", stackSize);

        OptionUtil.setSwitchOption(options, "-debug", debug);
        OptionUtil.setSwitchOption(options, "-verbose", verbose);
        OptionUtil.setSwitchOption(options, "-quiet", quiet);
        OptionUtil.setSwitchOption(options, "-version", version);

        OptionUtil.setOption(options, "-build-label", buildLabel);
        OptionUtil.setOption(options, "-build-project", buildProject);
        OptionUtil.setOption(options, "-build-version", buildVersion);

        OptionUtil.setOption(options, "-j", numOfWorkerThreads);

        OptionUtil.setSwitchOption(options, "-disable-source-bundling", !renderSources);
        OptionUtil.setSwitchOption(options, "-quick", quickScan);
    }
}
