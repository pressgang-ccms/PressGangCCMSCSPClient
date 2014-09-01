/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jboss.pressgang.ccms.contentspec.client.commands.base;

import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;

public interface BaseCommand extends ShutdownAbleApp {
    String getUsername();

    void setUsername(String username);

    String getServerUrl();

    String getPressGangServerUrl();

    void setServerUrl(String serverUrl);

    Boolean isShowHelp();

    void setShowHelp(Boolean showHelp);

    String getConfigLocation();

    void setConfigLocation(String configLocation);

    Boolean isShowVersion();

    void setShowVersion(Boolean showVersion);

    Boolean getDisableSSLCert();

    void setDisableSSLCert(Boolean disableSSLCert);

    boolean isAppShuttingDown();

    void setAppShuttingDown(boolean shuttingDown);

    void setShutdown(boolean shutdown);

    /**
     * Shutdown the application with a specific exit status.
     *
     * @param exitStatus The exit status to shut the
     *                   application down with.
     */
    void shutdown(int exitStatus);

    /**
     * Print the available options to the console
     * for the command.
     */
    void printHelp();

    /**
     * Print an error message to the console. If the
     * display help parameter is set then the commands
     * options are printed as well.
     *
     * @param errorMsg    The error message to display.
     * @param displayHelp If the commands options should be
     *                    displayed.
     */
    void printError(String errorMsg, boolean displayHelp);

    /**
     * Print a warning message to the console.
     *
     * @param warnMsg     The warning message to display.
     */
    void printWarn(String warnMsg);

    /**
     * Print an error and then shutdown the application.
     *
     * @param exitStatus  The exit status to shut the application down with.
     * @param errorMsg    The error message to be displayed.
     * @param displayHelp Whether help should be displayed for the error.
     */
    void printErrorAndShutdown(int exitStatus, final String errorMsg, boolean displayHelp);

    /**
     * Do the main process working involved in running the
     * command by using the command line arguments and
     * configuration files to do the command actions.
     *
     */
    void process();

    /**
     * Check to see if the command should load data from
     * a local csprocessor.cfg configuration file.
     *
     * @return True if the data from the local csprocessor.cfg
     *         should be loaded, otherwise false.
     */
    boolean loadFromCSProcessorCfg();

    /**
     * Whether or not the command needs a connection to an external service. (ie Zanata, PressGang, etc...)
     */
    boolean requiresExternalConnection();

    /**
     * Validate that the server url is valid.
     *
     * @return True if the url is valid otherwise false.
     */
    boolean validateServerUrl();
}
