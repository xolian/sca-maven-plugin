/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import com.fortify.sca.plugins.maven.handler.AbstractPackagingHandler;
import com.fortify.sca.plugins.maven.util.*;

/**
 * Translate files in the specified project to NST files.
 */
@Mojo(name = "translate", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.TEST)
public class TranslateMojo extends AbstractSCAMojo {
    //////////////////////////////////////////////////////////////////////
    // SCA
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies an additional class path used as the argument of the SCA -cp option. This is in addition to
     * dependencies worked out from the pom. This should be a correctly formed and separated list of paths for the
     * underlying system. No processing is done on this string other than concatenation.
     * This is potentially useful for JSP compilation.
     */
    @Parameter(property = "fortify.sca.cp", alias = "fortify.sca.classpath")
    private String extraClassPathString;

    /**
     * Specifies the source file encoding type. This is same as the javac encoding option.
     */
    @Parameter(property = "fortify.sca.encoding")
    private String encoding;

    /**
     * Specifies the JDK version that for which the Java code is written. This is same as the javac source option.
     * Valid values for the version are 1.7, 1.8, 1.9, 10, 11, 12, 13 and 14.
     */
    @Parameter(property = "fortify.sca.source.version")
    private String source;

    /**
     * Specifies the SQL type: PLSQL or TSQL.
     */
    @Parameter(property = "fortify.sca.sqlType")
    private String sqlType;

    /**
     * Specifies the directory that contains the source files to use for name resolution. These files are not analyzed.
     */
    @Parameter(property = "fortify.sca.translate.sourcePath")
    private String sourcePath;

    /**
     * Specifies file paths to exclude from translation.
     */
    @Parameter(property = "fortify.sca.exclude")
    private String[] excludes;

    /**
     * Specifies log file produced by sourceanalzer.
     */
    @Parameter(property = "fortify.sca.translateLogfile", defaultValue = "sca-translate.log")
    private String logfile;
    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, tests are not included in the translation.
     */
    @Parameter(property = "fortify.sca.tests.exclude", defaultValue = "true")
    private boolean skipTests;

    /**
     * If set to true, the build fails if errors occur during the SCA translation.
     */
    @Parameter(property = "fortify.sca.translate.failOnError", defaultValue = "false")
    private boolean scaTranslateFailOnError;

    private final String translateArgFilePrefix = "sca-translate-";

    // The following packaging types are not supported.
    // nar: maven-nar-plugin (http://maven-nar.github.io/)
    // dll, exe, lib, a, o, so, sl, dylib, jnilib, uexe: maven-native-plugin (http://www.mojohaus.org/maven-native/native-maven-plugin)
    private static final String[] unsupportedPackageTypes = { "nar", "dll", "exe", "lib", "a", "o", "so", "sl", "dylib", "jnilib", "uexe" };

    private static Properties handlerMap = new Properties();
    static {
        try (InputStream properties = TranslateMojo.class.getClassLoader().getResourceAsStream("sca-maven-plugin.properties")) {
            handlerMap.load(properties);
        } catch (IOException ex) {
            // FATAL ERROR
        }
    }

    public void doExecute() throws MojoExecutionException {
        String projectFortifyDir = project.getBuild().getDirectory() + File.separator + FORTIFY_DIR;
        argFilePath = projectFortifyDir + File.separator + translateArgFilePrefix + project.getArtifactId();

        if (aggregate) {
            MavenProject topLevelProject = session.getTopLevelProject();
            outputDirectory = new File(topLevelProject.getBuild().getDirectory(), FORTIFY_DIR);
            if (isFirstProject(project)) {
                translate();
                session.getUserProperties().setProperty(BUILD_ID_KEY, buildId);
            }else {
                buildId = session.getUserProperties().getProperty(BUILD_ID_KEY);
                translate();
            }
        } else {
            String defaultBuildId = project.getArtifactId() + "-" + project.getVersion();
            if (!defaultBuildId.equals(buildId)) {
                log.warn("Specified build Id :" + buildId + " cannot by applied on individual mode");
                buildId = defaultBuildId;
            }

            outputDirectory = new File(project.getBuild().getDirectory(), FORTIFY_DIR);
            translate();
        }
    }

    private void translate() throws MojoExecutionException {
        if (Arrays.asList(unsupportedPackageTypes).contains(project.getPackaging())) {
            log.warn(project.getPackaging() + " is not supported package type.");
            return;
        }

        String handlerName = handlerMap.getProperty(project.getPackaging());
        if (handlerName == null) {
            // Use "jar"
            handlerName = handlerMap.getProperty("jar");
            log.warn("Unknown packaging type '" + project.getPackaging() + "' is regarded as jar.");
        }

        log.info("Fail on Error: " + (scaFailOnError || scaTranslateFailOnError));

        try {
            AbstractPackagingHandler handler = createHandler(handlerName);
            List<String> options = buildOptions();

            Commandline commandline = handler.buildPomCommand(sourceanalyzer, options);
            log.info("Translating pom.xml...");
            log.info("Build ID: " + buildId);
            for (String exclude : excludes) {
                log.info("Excluded File Paths: " + exclude);
            }

            String pomArgFilePath = argFilePath + "-pom.txt";
            executeSCACommand(commandline, pomArgFilePath);

            commandline = handler.buildMainCommand(sourceanalyzer, options);
            if (commandline != null) {
                log.info("Translating main...");
                log.info("Build ID: " + buildId);
                OptionUtil.printOption(log, options, "-source", "Source");
                for (String exclude : excludes) {
                    log.info("Excluded File Paths: " + exclude);
                }
                String executable = commandline.getExecutable();
                if (executable.endsWith("sourceanalyzer") || executable.endsWith("sourceanalyzer.exe")) {
                    String mainArgFilePath = argFilePath + "-main.txt";
                    executeSCACommand(commandline, mainArgFilePath);
                } else {
                    executeCommand(commandline);
                }
            } else {
                log.info("No files to translate in main.");
            }

            if (!skipTests) {
                commandline = handler.buildTestCommand(sourceanalyzer, options);
                if (commandline != null) {
                    log.info("Translating test...");
                    log.info("Build ID: " + buildId);
                    OptionUtil.printOption(log, options, "-source", "Source");
                    for (String exclude : excludes) {
                        log.info("Excluded File Paths: " + exclude);
                    }
                    String executable = commandline.getExecutable();
                    if (executable.endsWith("sourceanalyzer") || executable.endsWith("sourceanalyzer.exe")) {
                        String testArgFilePath = argFilePath + "-test.txt";
                        executeSCACommand(commandline, testArgFilePath);
                    } else {
                        executeCommand(commandline);
                    }
                } else {
                    log.info("No files to translate in test.");
                }
            }
        } catch (ReflectiveOperationException e) {
            handleException(e);
        } catch (CommandLineException e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws MojoExecutionException {
        if (scaFailOnError || scaTranslateFailOnError) {
            throw new MojoExecutionException(e.getMessage(), e);
        } else {
            log.error(e.getMessage());
        }
    }

    private AbstractPackagingHandler createHandler(String name) throws ReflectiveOperationException {
        Class<? extends AbstractPackagingHandler> cls = Class.forName(name).asSubclass(AbstractPackagingHandler.class);
        Constructor<? extends AbstractPackagingHandler> ctr = cls.getDeclaredConstructor(MavenSession.class, Log.class);
        ctr.setAccessible(true);
        return ctr.newInstance(session, log);
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
        OptionUtil.setOption(options, "-cp", FileUtil.normalizeFilePath(extraClassPathString));
        OptionUtil.setOption(options, "-encoding", encoding);
        OptionUtil.setOption(options, "-source", source);
        OptionUtil.setOption(options, "-sourcepath", FileUtil.normalizeFilePath(sourcePath));
        OptionUtil.setProperty(options, "com.fortify.sca.SqlLanguage", sqlType);
        for (String exclude : excludes) {
            OptionUtil.setOption(options, "-exclude", FileUtil.normalizeFilePath(exclude));
        }

        OptionUtil.setOption(options, "-java-build-dir", FileUtil.normalizeFilePath(project.getBuild().getOutputDirectory()));
        if (!skipTests) {
            OptionUtil.setOption(options, "-java-build-dir", FileUtil.normalizeFilePath(project.getBuild().getTestOutputDirectory()));
        }

        return options;
    }
}
