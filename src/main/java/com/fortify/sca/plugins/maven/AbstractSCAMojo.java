/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;
import com.fortify.sca.plugins.maven.util.*;

public abstract class AbstractSCAMojo extends AbstractFortifyMojo {
    //////////////////////////////////////////////////////////////////////
    // JVM OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the maximum heap size of JVM which runs SCA.
     */
    @Parameter(property = "fortify.sca.Xmx", alias = "fortify.sca.jvm.Xmx")
    protected String maxHeap;

    /**
     * Specifies the thread stack size of JVM which runs SCA.
     */
    @Parameter(property = "fortify.sca.Xss", alias = "fortify.sca.jvm.Xss")
    protected String stackSize;

    //////////////////////////////////////////////////////////////////////
    // SCA OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * Location of the sourceanalyzer executable. Defaults to sourceanalyzer
     * which runs the version on the PATH or fails if none exists.
     */
    @Parameter(property = "fortify.sca.sourceanalyzer.executable", defaultValue = "sourceanalyzer", required = true)
    protected String sourceanalyzer;

    /**
     * Specifies the SCA build ID. The default is project artifact ID and version. In aggregate mode, the top level project
     * artifact ID and version is used for all modules.
     */
    @Parameter(property = "fortify.sca.buildId", defaultValue = "${project.artifactId}-${project.version}")
    protected String buildId;

    /**
     * If set to true, SCA runs in debug mode. This can be useful for troubleshooting.
     */
    @Parameter(property = "fortify.sca.debug", defaultValue = "false")
    protected boolean debug;

    /**
     * If set to true, SCA sends verbose status messages to the console.
     */
    @Parameter(property = "fortify.sca.verbose", defaultValue = "false")
    protected boolean verbose;

    /**
     * If set to true, SCA writes minimal messages to the console.
     */
    @Parameter(property = "fortify.sca.quiet", defaultValue = "false")
    protected boolean quiet;

    /**
     * If set to true, SCA prints the version to the console.
     */
    @Parameter(property = "fortify.sca.version", defaultValue = "true")
    protected boolean version;

    /**
     * Specifies the sourceanalyzer project root directory.
     * Associated with com.fortify.sca.ProjectRoot SCA property
     * @since 18.20
     */
    @Parameter(property = "fortify.sca.ProjectRoot")
    protected String scaProjectRoot;

    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN CONTROL OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during any SCA processing.
     */
    @Parameter(property = "fortify.sca.failOnError", defaultValue = "false")
    protected boolean scaFailOnError;

    protected String argFilePath;

    protected List<String> buildOptions() {
        List<String> options = new ArrayList<String>();

        OptionUtil.setJvmOption(options, "-Xmx", maxHeap);
        OptionUtil.setJvmOption(options, "-Xss", stackSize);

        OptionUtil.setSwitchOption(options, "-debug", debug);
        OptionUtil.setSwitchOption(options, "-verbose", verbose);
        OptionUtil.setSwitchOption(options, "-quiet", quiet);
        OptionUtil.setSwitchOption(options, "-version", version);
        OptionUtil.setProperty(options, "com.fortify.WorkingDirectory", FileUtil.normalizeFilePath(fortifyWorkingDirectory));
        OptionUtil.setProperty(options, "com.fortify.sca.ProjectRoot", FileUtil.normalizeFilePath(scaProjectRoot));

        return options;
    }
}
