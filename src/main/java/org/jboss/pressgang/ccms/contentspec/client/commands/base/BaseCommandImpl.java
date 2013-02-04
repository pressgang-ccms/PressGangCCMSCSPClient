package org.jboss.pressgang.ccms.contentspec.client.commands.base;

import java.util.concurrent.atomic.AtomicBoolean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.UserProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;

public abstract class BaseCommandImpl implements BaseCommand {
    private final JCommander parser;
    private final ContentSpecConfiguration cspConfig;
    private final ClientConfiguration clientConfig;
    private DataProviderFactory providerFactory = null;

    @Parameter(names = {Constants.SERVER_LONG_PARAM, Constants.SERVER_SHORT_PARAM}, hidden = true)
    private String serverUrl;

    @Parameter(names = {Constants.USERNAME_LONG_PARAM, Constants.USERANME_SHORT_PARAM}, hidden = true)
    private String username;

    @Parameter(names = Constants.HELP_LONG_PARAM, hidden = true)
    private Boolean showHelp = false;

    @Parameter(names = Constants.CONFIG_LONG_PARAM, hidden = true)
    private String configLocation = Constants.DEFAULT_CONFIG_LOCATION;

    @Parameter(names = Constants.DEBUG_LONG_PARAM, hidden = true)
    private Boolean debug = false;

    @Parameter(names = Constants.VERSION_LONG_PARAM, hidden = true)
    private Boolean showVersion = false;

    protected final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    protected final AtomicBoolean shutdown = new AtomicBoolean(false);

    public BaseCommandImpl(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        assert parser != null;
        assert cspConfig != null;
        assert clientConfig != null;

        this.parser = parser;
        this.cspConfig = cspConfig;
        this.clientConfig = clientConfig;
    }

    public DataProviderFactory getProviderFactory() {
        if (providerFactory == null) {
            providerFactory = RESTProviderFactory.create(getPressGangServerUrl());
        }
        return providerFactory;
    }

    protected ContentSpecConfiguration getCspConfig() {
        return cspConfig;
    }

    protected ClientConfiguration getClientConfig() {
        return clientConfig;
    }

    public abstract String getCommandName();

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public String getPressGangServerUrl() {
        return serverUrl == null ? null : ((serverUrl.endsWith("/") ? serverUrl : (serverUrl + "/")) + "seam/resource/rest/");
    }

    @Override
    public void setServerUrl(final String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public Boolean isShowHelp() {
        return showHelp;
    }

    @Override
    public void setShowHelp(final Boolean showHelp) {
        this.showHelp = showHelp;
    }

    @Override
    public String getConfigLocation() {
        return configLocation;
    }

    @Override
    public void setConfigLocation(final String configLocation) {
        this.configLocation = configLocation;
    }

    public Boolean isDebug() {
        return debug;
    }

    public void setDebug(final Boolean debug) {
        this.debug = debug;
    }

    @Override
    public Boolean isShowVersion() {
        return showVersion;
    }

    @Override
    public void setShowVersion(final Boolean showVersion) {
        this.showVersion = showVersion;
    }

    @Override
    public boolean isAppShuttingDown() {
        return isShuttingDown.get();
    }

    @Override
    public void setAppShuttingDown(final boolean shuttingDown) {
        this.isShuttingDown.set(shuttingDown);
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public void setShutdown(final boolean shutdown) {
        this.shutdown.set(shutdown);
    }

    @Override
    public void printErrorAndShutdown(int exitStatus, final String errorMsg, boolean displayHelp) {
        printError(errorMsg, displayHelp);
        shutdown(exitStatus);
    }


    /**
     * Prints an error message and then displays the main help screen
     *
     * @param errorMsg    The error message to be displayed.
     * @param displayHelp Whether help should be displayed for the error.
     */
    public void printError(final String errorMsg, final boolean displayHelp) {
        JCommander.getConsole().println("ERROR: " + errorMsg);
        if (displayHelp) {
            JCommander.getConsole().println("");
            printHelp();
        } else {
            JCommander.getConsole().println("");
        }
    }

    /**
     * Prints the Help output for a specific command
     *
     * @param commandName The name of the command
     */
    protected void printHelp(final String commandName) {
        if (commandName == null) {
            parser.usage(false);
        } else {
            parser.usage(true, new String[]{commandName});
        }
    }

    /**
     * Prints the Help output
     */
    public void printHelp() {
        final String commandName = getCommandName();
        printHelp(commandName);
    }

    /**
     * Authenticate a user to ensure that they exist on the server.
     *
     * @param username        The randomString of the user.
     * @param providerFactory The Factory for the entity providers that provider data from an external datasource.
     * @return The user object if they existed otherwise false.
     */
    public UserWrapper authenticate(final String username, final DataProviderFactory providerFactory) {
        if (username == null || username.equals("")) {
            printErrorAndShutdown(Constants.EXIT_UNAUTHORISED, Constants.ERROR_NO_USERNAME, false);
        }

        // Get the user from the external Datasource
        final CollectionWrapper<UserWrapper> users = providerFactory.getProvider(UserProvider.class).getUsersByName(username);
        final UserWrapper user = users != null && users.size() == 1 ? users.getItems().get(0) : null;

        // Check that a valid user was found
        if (user == null) {
            printErrorAndShutdown(Constants.EXIT_UNAUTHORISED, Constants.ERROR_UNAUTHORISED, false);
        }
        return user;
    }

    /**
     * Set the application to shutdown so that
     * any shutdown hooks know that the application
     * can be shutdown.
     */
    @Override
    public void shutdown() {
        setAppShuttingDown(true);
    }

    /**
     * Shutdown the application with a specific exit status.
     *
     * @param exitStatus The exit status to shut the application down with.
     */
    public void shutdown(int exitStatus) {
        shutdown.set(true);
        System.exit(exitStatus);
    }

    /**
     * Validate that any servers connected to with this command exist. If they don't then print an error
     * message and stop the program.
     */
    @Override
    public boolean validateServerUrl() {
        // Print the server url
        JCommander.getConsole().println(String.format(Constants.WEBSERVICE_MSG, getServerUrl()));

        // Test that the server address is valid
        if (!ClientUtilities.validateServerExists(getServerUrl())) {
            // Print a line to separate content
            JCommander.getConsole().println("");

            printErrorAndShutdown(Constants.EXIT_NO_SERVER, Constants.UNABLE_TO_FIND_SERVER_MSG, false);
        }

        return true;
    }

    /**
     * Allows a shutdown to continue if requested. If the application is shutting down then this method will create a loop to stop
     * further execution of code.
     */
    protected void allowShutdownToContinueIfRequested() {
        if (isAppShuttingDown()) {
            shutdown.set(true);
            while (true) {
                // Just loop until the application shuts down
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Do nothing as this should only get interrupted when the app fully shuts down.
                }
            }
        }
    }
}