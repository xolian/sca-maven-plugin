/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.*;
import com.fortify.sca.plugins.maven.util.*;

public abstract class AbstractFortifyMojo extends AbstractMojo {
    //////////////////////////////////////////////////////////////////////
    // MAVEN PROJECT
    //////////////////////////////////////////////////////////////////////
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
    MojoExecution execution;

    /**
     * If set to true, aggregate mode is enabled.
     * @since 16.10
     */
    @Parameter(property = "fortify.sca.aggregate", defaultValue = "true")
    protected boolean aggregate;

    /**
     * If set to true, generates SCA commands to run, but does not actually run them.
     */
    @Parameter(property = "fortify.sca.dontRunSCA", defaultValue = "false")
    protected boolean dontRunSCA;

    /**
     * Specifies the Fortify working directory.
     * Associated with com.fortify.WorkingDirectory property
     * @since 18.20
     */
    @Parameter(property = "fortify.WorkingDirectory")
    protected String fortifyWorkingDirectory;

    //////////////////////////////////////////////////////////////////////
    // INTERNAL OPTIONS
    //////////////////////////////////////////////////////////////////////
    protected Log log;

    protected File workingDirectory;

    protected File outputDirectory;

    protected final String BUILD_ID_KEY = "fortify.sca.maven.buildId";

    protected final String FORTIFY_DIR = "fortify";

    protected abstract void doExecute() throws MojoExecutionException;

    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        printBasicInfo();
        doExecute();
    }

    protected void printBasicInfo() {
        log.info("Aggregate: " + this.aggregate);
        int index = session.getProjects().indexOf(project) + 1;
        log.info("Index of Project: " + index + "/" + session.getProjects().size());
        log.info("Packaging Type: " + project.getPackaging());
        log.info("Base Dir: " + project.getBasedir());
        log.info("POM: " + project.getFile());
    }

    protected int executeCommand(Commandline commandline) throws CommandLineException {
        final StreamConsumer stdout = new DefaultConsumer();
        final CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        return executeCommand(commandline, stdout, stderr);
    }

    protected int executeCommand(Commandline commandline, StreamConsumer stdout, StreamConsumer stderr) throws CommandLineException {
        log.info("Executing Command: " + commandline.toString());
        int exitCode = CommandLineUtils.executeCommandLine(commandline, stdout, stderr);

        if (exitCode != 0) {
            StringBuffer errMsg = new StringBuffer();
            errMsg.append("Command exited with code " + exitCode + ".");
            if (stdout instanceof CommandLineUtils.StringStreamConsumer) {
                String stdoutOutput = ((CommandLineUtils.StringStreamConsumer) stdout).getOutput();
                if (!StringUtil.isEmpty(stdoutOutput)) {
                    if (!stdoutOutput.startsWith(System.lineSeparator())) {
                        errMsg.append(System.lineSeparator());
                    }
                    errMsg.append(stdoutOutput);
                }
            }
            if (stderr instanceof CommandLineUtils.StringStreamConsumer) {
                String stderrOutput = ((CommandLineUtils.StringStreamConsumer) stderr).getOutput();
                if (!StringUtil.isEmpty(stderrOutput)) {
                    if (!stderrOutput.startsWith(System.lineSeparator())) {
                        errMsg.append(System.lineSeparator());
                    }
                    errMsg.append(stderrOutput);
                }
            }
            throw new CommandLineException(errMsg.toString());
        }
        return exitCode;
    }

    protected int executeSCACommand(Commandline commandline, String argFilePath) throws CommandLineException {
        final StreamConsumer stdout = new DefaultConsumer();
        final CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        return executeSCACommand(commandline, argFilePath, stdout, stderr);
    }

    protected int executeSCACommand(Commandline commandline, String argFilePath, StreamConsumer stdout, StreamConsumer stderr)
            throws CommandLineException {
        if (argFilePath != null) {
            try {
                writeArgFile(commandline, argFilePath);
            } catch (IOException e) {
                throw new CommandLineException(e.getMessage(), e);
            }
        }

        if (dontRunSCA) {
            log.info("Skipping to execute command: 'fortify.sca.dontRunSCA' is true");
            return 0;
        } else {
            return executeCommand(commandline, stdout, stderr);
        }
    }

    private void writeArgFile(Commandline commandline, String argFilePath) throws IOException {

        StringBuffer args = new StringBuffer();
        List<String> jvmOptions = new ArrayList<String>();
        for (String arg : commandline.getArguments()) {
            if (arg.startsWith("-X")) {
                jvmOptions.add(arg);
            } else {
                if (args.length() > 0) {
                    args.append(" ");
                }
                args.append(StringUtil.encloseQuotes(arg));
            }
        }

        commandline.clearArgs();
        for (String jvmOption : jvmOptions) {
            commandline.createArg().setLine(jvmOption);
        }

        File argFile = new File(argFilePath);
        FileUtil.writeTextFile(argFile, args.toString());

        commandline.createArg().setLine("@" + FileUtil.normalizeFilePath(argFilePath));
    }

    protected boolean isFirstProject(MavenProject project) {
        return session.getProjects().indexOf(project) == 0;
    }

    protected boolean isLastProject(MavenProject project) {
        MavenProject lastProject = session.getProjects().get(session.getProjects().size() - 1);
        return lastProject.equals(project);
    }
}
