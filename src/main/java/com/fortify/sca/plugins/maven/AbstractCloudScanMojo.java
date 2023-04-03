/*
 * Copyright 2020 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven;

import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractCloudScanMojo extends AbstractFortifyClientMojo {
    //////////////////////////////////////////////////////////////////////
    // SCANCENTRAL COMPONENT
    //////////////////////////////////////////////////////////////////////
    /**
     * Location of the ScanCentral client executable. Defaults to ScanCentral
     * which runs the version on the PATH or fails if none exists.
     */
    @Parameter(property = "fortify.ScanCentral.executable", defaultValue = "scancentral")
    protected String scancentral;

    //////////////////////////////////////////////////////////////////////
    // SCANCENTRAL GLOBAL OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * Specifies the URL of the ScanCentral controller.
     */
    @Parameter(property = "fortify.ScanCentral.ctrl.url")
    protected String scancentralCtrlUrl;

    /**
     * Specifies the SSC URL to interact with ScanCentral controller. To enable SSC Integrated Mode,
     * uploadToSSC must be true. If sscUrl is specified, sscScanCentralCtrlToken must also be specified.
     */
    @Parameter(property = "fortify.ScanCentral.ssc.url")
    protected String sscUrl;

    /**
     * Specifies the SSC ScanCentralCtrlToken to use when attempting to interact with the ScanCentral controller.
     * To enable SSC Integrated Mode, uploadToSSC must be true.
     */
    @Parameter(property = "fortify.ScanCentral.ssc.ScanCentralCtrlToken")
    protected String sscScanCentralCtrlToken;

    //////////////////////////////////////////////////////////////////////
    // MAVEN PLUGIN OPTIONS
    //////////////////////////////////////////////////////////////////////
    /**
     * If set to true, the build fails if errors occur during any ScanCentral processing.
     */
    @Parameter(property = "fortify.ScanCentral.failOnError", defaultValue = "false")
    protected boolean scanCentralFailOnError;
}
