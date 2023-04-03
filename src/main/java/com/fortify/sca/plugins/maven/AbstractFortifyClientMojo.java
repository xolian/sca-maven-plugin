/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public abstract class AbstractFortifyClientMojo extends AbstractFortifyMojo {
    //////////////////////////////////////////////////////////////////////
    // FORTIFYCLIENT COMPONENT
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the location of the fortifyclient executable. Defaults to fortifyclient
     * that runs the version on the PATH. Upload fails if none exists.
     */
    @Parameter(property = "fortify.fortifyclient.executable", defaultValue = "fortifyclient", required = true)
    protected String fortifyclient;

    /**
     * Specifies the SSC application name.
     * Note that this must be supplied in conjunction with sscApplicationVersion.
     */
    @Parameter(property = "fortify.ssc.applicationName")
    protected String sscApplicationName;

    /**
     * Specifies the SSC application version.
     * Note that this must be supplied in conjunction with sscApplicationName.
     */
    @Parameter(property = "fortify.ssc.applicationVersion")
    protected String sscApplicationVersion;

    /**
     * Specifies the Application Version ID of the SSC application.
     * Note that sscApplicationVersionId overrides sscApplicationName and sscApplicationVersion combinations if supplied.
     */
    @Parameter(property = "fortify.ssc.applicationVersionId")
    protected String sscApplicationVersionId;

    protected String getApplicationVersionId(String sscUrl, String sscUploadToken, String mavenProjectName, String mavenProjectVersion)
            throws CommandLineException, IllegalArgumentException {

        CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        Commandline commandline = new Commandline();
        commandline.setExecutable(fortifyclient);

        String applicationVersionId = null;

        try {
            commandline.addSystemEnvironment();
        } catch (Exception e) {
            throw new CommandLineException(e.getMessage(), e);
        }

        commandline.createArg().setValue("listProjectVersions");
        commandline.createArg().setValue("-url");
        commandline.createArg().setValue(sscUrl);
        commandline.createArg().setValue("-authtoken");
        commandline.createArg().setValue(sscUploadToken);

        executeCommand(commandline, stdout, stderr);

        String strOutput = stdout.getOutput();
        for (String line : strOutput.split(System.lineSeparator())) {
            log.info(line);
        }
        // Parse result from "foritfyclient listProjectVersion" to look for project name
        final String regex = ".*(\\d+)\\s+" + mavenProjectName + "\\s+" + mavenProjectVersion + ".*";
        Pattern p = Pattern.compile(regex, Pattern.MULTILINE  | Pattern.DOTALL);
        Matcher m = p.matcher(strOutput);
        if (m.matches()) {
            applicationVersionId = m.group(1);
            log.info("Found SSC Application Version Id: " + applicationVersionId);
        } else {
            String message = "Application: " + mavenProjectName + " - Version: " + mavenProjectVersion + " does not exist in SSC.";
            throw new IllegalArgumentException(message);
        }

        return applicationVersionId;
    }
}
