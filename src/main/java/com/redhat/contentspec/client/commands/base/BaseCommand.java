package com.redhat.contentspec.client.commands.base;

import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;

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

    boolean isAppShuttingDown();

    void setAppShuttingDown(boolean shuttingDown);

    void setShutdown(boolean shutdown);

    /**
     * Print the available options to the console
     * for the command.
     */
    void printHelp();

    /**
     * Print an error message to the console. If the display help parameter is set then the commands
     * options are printed as well.
     *
     * @param errorMsg    The error message to display.
     * @param displayHelp If the commands options should be displayed.
     */
    void printError(String errorMsg, boolean displayHelp);

    /**
     * Do the main process working involved in running the
     * command by using the command line arguments and
     * configuration files to do the command actions.
     *
     * @param restManager The REST manager containing the client
     *                    and actions to be used to access the
     *                    REST Interface.
     * @param elm         The Error Logging Manager used to store
     *                    logs and log messages.
     */
    void process(RESTManager restManager, ErrorLoggerManager elm);

    /**
     * Check to see if the command should load data from a local csprocessor.cfg configuration file.
     *
     * @return True if the data from the local csprocessor.cfg should be loaded, otherwise false.
     */
    boolean loadFromCSProcessorCfg();

    void validateServerUrl();

    /**
     * Shutdown the application with a specific exit status.
     *
     * @param exitStatus The exit status to shut the
     *                   application down with.
     */
    void shutdown(int exitStatus);
}
