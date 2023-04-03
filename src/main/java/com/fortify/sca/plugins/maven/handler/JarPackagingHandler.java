/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.handler;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;

public class JarPackagingHandler extends AbstractJavaPackagingHandler {

    JarPackagingHandler(MavenSession session, Log log) {
        super(session, log);
    }
}
