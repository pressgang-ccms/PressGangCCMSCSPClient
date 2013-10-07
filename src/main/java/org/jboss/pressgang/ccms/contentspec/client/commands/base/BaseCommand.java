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
     * Gets a message from the properties and formats the message with any additional args.
     *
     * @param key The key for the message.
     * @param args The arguments to format the message with.
     * @return The formatted message.
     */
    String getMessage(String key, Object... args);

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
