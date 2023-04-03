/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import com.fortify.sca.plugins.maven.util.*;

public abstract class AbstractPackagingHandler {

    public MavenSession session;
    public Log log;

    AbstractPackagingHandler(MavenSession session, Log log) {
        this.session = session;
        this.log = log;
    }

    public Commandline buildPomCommand(String sourceanalyzer, List<String> options) throws CommandLineException {
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
        } catch (Exception e) {
            throw new CommandLineException(e.getMessage(), e);
        }
        // pom.xml
        commandline.createArg().setLine(FileUtil.normalizeFilePath(currentProject.getFile().getAbsolutePath()));

        return commandline;
    }

    public abstract Commandline buildMainCommand(String sourceanalyzer, List<String> options) throws CommandLineException;

    public abstract Commandline buildTestCommand(String sourceanalyzer, List<String> options) throws CommandLineException;

    /**
     *
     * @param resources
     * @return
     *
     */
    protected List<String> getResourcesAsStringList(List<Resource> resources) {

        // resources:  is a list of resource elements that each describe what and where to include
        //             files associated with this project.
        // filtering:  is true or false, denoting if filtering is to be enabled for this resource.
        // directory:  This element's value defines where the resources are to be found.
        //             The default directory for a build is ${basedir}/src/main/resources.
        // includes:   A set of files patterns which specify the files to include as resources
        //             under that specified directory, using * as a wildcard.
        // excludes:   The same structure as includes, but specifies which files to ignore.
        //             In conflicts between include and exclude, exclude wins.

        List<String> result = new ArrayList<String>();

        for(Resource resource : resources) {
            String dir = resource.getDirectory();

            if (FileUtil.isDirectory(dir)) {
                List<String> includes = resource.getIncludes();
                List<String> excludes = resource.getExcludes();
                if (includes.size() == 0 && excludes.size() == 0) {
                    result.add(dir);
                } else {
                    List<String> filePaths = FileUtil.getAllFilePathsInDirectory(dir);
                    if (includes.size() == 0) {
                        result.addAll(filePaths);
                    } else {
                        for (String filePath : filePaths) {
                            for ( String include : includes) {
                                File file = new File(filePath);
                                File includePattern = new File(dir + File.separator + include);
                                if (FileUtil.matches(file, includePattern)) {
                                    result.add(filePath);
                                }
                            }
                        }
                    }

                    for (String filePath : filePaths) {
                        for (String exclude : excludes) {
                            File file = new File(filePath);
                            File excludePattern = new File(dir + File.separator + exclude);
                            if (FileUtil.matches(file, excludePattern)) {
                                result.remove(filePath);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
