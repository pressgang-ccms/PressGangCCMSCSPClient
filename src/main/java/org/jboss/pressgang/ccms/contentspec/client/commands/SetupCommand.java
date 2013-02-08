package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ServerConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ZanataServerConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;

@Parameters(commandDescription = "Setup the Content Specification Processor configuration files")
public class SetupCommand extends BaseCommandImpl {

    public SetupCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.SETUP_COMMAND_NAME;
    }

    @Override
    public void process() {
        final StringBuilder configFile = new StringBuilder();

        // Setup the servers that the client can connect to
        setupServers(configFile);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Setup the Root csprocessor project directory
        setupRootDirectory(configFile);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        configFile.append("\n");

        // Setup the default publican options
        setupPublican(configFile);

        // Get the publishing options
        JCommander.getConsole().println("Setup the publishing options? (Yes/No)");
        String answer = JCommander.getConsole().readLine();

        // Setup the publishing settings if required
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupPublish(configFile);
        }

        JCommander.getConsole().println("Setup zanata configuration? (Yes/No)");
        answer = JCommander.getConsole().readLine();

        // Setup the Zanata Settings if required
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupZanata(configFile);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Save the configuration file
        final File file = new File(Constants.DEFAULT_CONFIG_LOCATION);
        try {
            // Make sure the directory exists
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // Make a backup of any existing csprocessor.ini
            if (file.exists()) {
                // TODO check that the rename worked
                file.renameTo(new File(file.getAbsolutePath() + ".backup"));
            }

            // Save the config
            FileUtilities.saveFile(file, configFile.toString(), Constants.FILE_ENCODING);
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, Constants.ERROR_FAILED_CREATING_CONFIG_MSG, false);
        }

        JCommander.getConsole().println(Constants.SUCCESSFUL_SETUP_MSG);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        /*
         * No need to load from csprocessor.cfg as this
         * command configures the client before use.
         */
        return false;
    }

    /**
     * Setup the servers that are to be used by the program
     * by asking the user for specific details.
     *
     * @param configFile The output configuration file to add
     *                   the servers to.
     */
    protected void setupServers(final StringBuilder configFile) {
        String username = "";
        String defaultServerName = "";

        final HashMap<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();

        JCommander.getConsole().println("Use the default server configuration? (Yes/No)");
        String answer = JCommander.getConsole().readLine();

        /* We are using the default setup so we only need to get the default server and a username */
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Get which server they want to connect to by default
            while (!defaultServerName.equalsIgnoreCase("test") && !defaultServerName.equalsIgnoreCase("production")) {
                JCommander.getConsole().println("Which server do you want to connect to by default? (test/production)");
                defaultServerName = JCommander.getConsole().readLine().toLowerCase();

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }

            // Get the users username
            JCommander.getConsole().println("Please enter a username to connect to the servers: ");
            username = JCommander.getConsole().readLine();

            // Create the default settings
            servers.put(Constants.DEFAULT_SERVER_NAME, new ServerConfiguration(Constants.DEFAULT_SERVER_NAME, defaultServerName, username));

            // Setup each servers settings
            servers.put("test", new ServerConfiguration("test", Constants.DEFAULT_TEST_SERVER));
            servers.put("production", new ServerConfiguration("production", Constants.DEFAULT_PROD_SERVER));

        }        /* We need to read in a list of servers and then get the default server */ else if (answer.equalsIgnoreCase(
                "no") || answer.equalsIgnoreCase("n")) {
            while (!answer.matches("^[0-9]+$")) {
                JCommander.getConsole().print("How many servers are to be configured? ");
                answer = JCommander.getConsole().readLine();

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }

            // Get the server setup details from the user
            final Integer numServers = Integer.parseInt(answer);
            final StringBuilder serverNames = new StringBuilder("");
            for (int i = 1; i <= numServers; i++) {
                final ServerConfiguration config = new ServerConfiguration();

                // Get the name of the server
                JCommander.getConsole().println("Please enter the name of server no. " + i + ": ");
                config.setName(JCommander.getConsole().readLine().toLowerCase());

                // Get the url of the server
                JCommander.getConsole().println("Please enter the URL of server no. " + i + ": ");
                config.setUrl(ClientUtilities.fixHostURL(JCommander.getConsole().readLine()));

                // Get the username for the server
                JCommander.getConsole().println("Please enter the username of server no. " + i + ": ");
                config.setUsername(JCommander.getConsole().readLine());

                // Add the server configuration and add the name to the list of displayable strings
                servers.put(config.getName(), config);
                serverNames.append(config.getName() + "/");

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }

            /* Only ask for the default when there are multiple servers */
            if (servers.size() > 1) {
                // Get which server they want to connect to
                while (!servers.containsKey(defaultServerName)) {
                    JCommander.getConsole().println("Which server do you want to connect to by default? (" + serverNames.substring(0,
                            serverNames.length() - 1) + ")");
                    defaultServerName = JCommander.getConsole().readLine().toLowerCase();

                    // Good point to check for a shutdown
                    allowShutdownToContinueIfRequested();
                }
            } else {
                defaultServerName = serverNames.substring(0, serverNames.length() - 1);
            }

            // Create the default settings
            servers.put(Constants.DEFAULT_SERVER_NAME, new ServerConfiguration(Constants.DEFAULT_SERVER_NAME, defaultServerName));
        } else {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.INVALID_ARG_MSG, false);
        }

        // Add the information to the configuration file
        configFile.append("[servers]\n");
        for (final Entry<String, ServerConfiguration> serverEntry : servers.entrySet()) {
            final String serverName = serverEntry.getKey();
            final ServerConfiguration config = serverEntry.getValue();

            // Setup the url for the server
            if (serverName.equals(Constants.DEFAULT_SERVER_NAME)) {
                configFile.append(serverName + "=" + config.getUrl() + "\n");
            } else {
                configFile.append(serverName + ".url=" + config.getUrl() + "\n");
            }

            // Setup the username for the server
            if (config.getUsername() != null && !config.getUsername().equals(""))
                configFile.append(serverName + ".username=" + config.getUsername() + "\n");
            else configFile.append(serverName + ".username=\n");

            // Add a blank line to separate servers
            configFile.append("\n");

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();
        }
    }

    /**
     * Setup the root directory configuration options by asking
     * the user for specific details.
     *
     * @param configFile
     */
    protected void setupRootDirectory(final StringBuilder configFile) {
        String rootDir = "";

        // Get the root directory to store content specifications
        JCommander.getConsole().println("Enter a root directory to store Content Specifications. (Press enter for no root directory)");
        rootDir = JCommander.getConsole().readLine();

        // Create the Root Directory
        configFile.append("[directory]\n");
        configFile.append("root=" + ClientUtilities.fixDirectoryPath(rootDir) + "\n");
    }

    /**
     * Setup the publican configuration options by asking
     * the user for specific details.
     *
     * @param configFile
     */
    protected void setupPublican(final StringBuilder configFile) {
        String publicanParams = "";
        String publicanFormat = "";

        // Get the publican options
        JCommander.getConsole().println(
                "Please enter the publican build command line options. [" + Constants.DEFAULT_PUBLICAN_OPTIONS + "]");
        publicanParams = JCommander.getConsole().readLine();
        JCommander.getConsole().println("Please enter the preferred publican preview format. [" + Constants.DEFAULT_PUBLICAN_FORMAT + "]");
        publicanFormat = JCommander.getConsole().readLine();

        // Create the publican options
        configFile.append("[publican]\n");
        configFile.append("build.parameters=" + (publicanParams.isEmpty() ? Constants.DEFAULT_PUBLICAN_OPTIONS : publicanParams) + "\n");
        configFile.append("preview.format=" + (publicanFormat.isEmpty() ? Constants.DEFAULT_PUBLICAN_FORMAT : publicanFormat) + "\n");
    }

    /**
     * Setup the publishing configuration options by asking
     * the user for specific details.
     *
     * @param configFile
     */
    protected void setupPublish(final StringBuilder configFile) {
        String kojiHubUrl = "";
        String publishCommand = "";

        // Get the publish options
        JCommander.getConsole().println(
                "Please enter the URL of the " + Constants.KOJI_NAME + "hub. [" + Constants.DEFAULT_KOJIHUB_URL + "]");
        kojiHubUrl = JCommander.getConsole().readLine();
        JCommander.getConsole().println(
                "Please enter the default command to publish Content Specifications." + (Constants.DEFAULT_PUBLISH_COMMAND.isEmpty() ? ""
                        : ("[" + Constants.DEFAULT_PUBLISH_COMMAND + "]")));
        publishCommand = JCommander.getConsole().readLine();

        // Create the Publish Settings
        configFile.append("[publish]\n");
        configFile.append("koji.huburl=" + (kojiHubUrl.isEmpty() ? Constants.DEFAULT_KOJIHUB_URL : kojiHubUrl) + "\n");
        configFile.append("command=" + (publishCommand.isEmpty() ? Constants.DEFAULT_PUBLISH_COMMAND : publishCommand) + "\n\n");
    }

    /**
     * Setup the zanata servers configuration options by asking
     * the user for specific details. This setups up the servers
     * in the same form as zanata for convenience.
     *
     * @param configFile
     */
    protected void setupZanata(final StringBuilder configFile) {
        String defaultZanataProject = "";
        String defaultZanataVersion = "";

        JCommander.getConsole().println("Please enter a default zanata project name:");
        defaultZanataProject = JCommander.getConsole().readLine();

        JCommander.getConsole().println("Please enter a default zanata project version:");
        defaultZanataVersion = JCommander.getConsole().readLine();

        final HashMap<String, ZanataServerConfiguration> servers = new HashMap<String, ZanataServerConfiguration>();
        String answer = "";

        while (!answer.matches("^[0-9]+$")) {
            JCommander.getConsole().print("How many zanata servers are to be configured? ");
            answer = JCommander.getConsole().readLine();

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();
        }

        // Get the server setup details from the user
        final Integer numProjects = Integer.parseInt(answer);
        final StringBuilder serverNames = new StringBuilder("");
        for (int i = 1; i <= numProjects; i++) {
            final ZanataServerConfiguration config = new ZanataServerConfiguration();

            // Get the name of the server
            JCommander.getConsole().println("Please enter the name of server no " + i + ": ");
            config.setName(JCommander.getConsole().readLine().toLowerCase());

            // Get the url of the server
            JCommander.getConsole().println("Please enter the URL of server no. " + i + ": ");
            config.setUrl(ClientUtilities.fixHostURL(JCommander.getConsole().readLine()));

            // Get the username for the server
            JCommander.getConsole().println("Please enter the username of server no. " + i + ": ");
            config.setUsername(JCommander.getConsole().readLine());

            // Get the token for the server
            JCommander.getConsole().println("Please enter the API Key of server no. " + i + ": ");
            config.setToken(JCommander.getConsole().readLine());

            // Add the server configuration and add the name to the list of displayable strings
            servers.put(config.getName(), config);
            serverNames.append(config.getName() + "/");

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();
        }

        /* Only ask for the default when there are multiple servers */
        String defaultZanataServerName = "";
        if (servers.size() > 1) {
            // Get which server they want to connect to
            while (!servers.containsKey(defaultZanataServerName)) {
                JCommander.getConsole().println("Which zanata server do you want to connect to by default? (" + serverNames.substring(0,
                        serverNames.length() - 1) + ")");
                defaultZanataServerName = JCommander.getConsole().readLine().toLowerCase();

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }
        } else {
            defaultZanataServerName = serverNames.substring(0, serverNames.length() - 1);
        }

        // Create the default settings
        servers.put(Constants.DEFAULT_SERVER_NAME, new ZanataServerConfiguration(Constants.DEFAULT_SERVER_NAME, defaultZanataServerName));

        // Add the information to the configuration file
        configFile.append("[zanata]\n");
        for (final Entry<String, ZanataServerConfiguration> serverEntry : servers.entrySet()) {
            final String serverName = serverEntry.getKey();
            final ZanataServerConfiguration config = serverEntry.getValue();

            // Setup the url for the server
            if (serverName.equals(Constants.DEFAULT_SERVER_NAME)) {
                configFile.append(serverName + "=" + config.getUrl() + "\n");
                configFile.append(serverName + ".project=" + defaultZanataProject + "\n");
                configFile.append(serverName + ".project-version=" + defaultZanataVersion + "\n");
            } else {
                configFile.append(serverName + ".url=" + config.getUrl() + "\n");

                // Setup the username for the server
                if (config.getUsername() != null && !config.getUsername().equals(""))
                    configFile.append(serverName + ".username=" + config.getUsername() + "\n");
                else configFile.append(serverName + ".username=\n");

                // Setup the token for the server
                if (config.getToken() != null && !config.getToken().equals(""))
                    configFile.append(serverName + ".key=" + config.getToken() + "\n");
                else configFile.append(serverName + ".key=\n");
            }

            // Add a blank line to separate servers
            configFile.append("\n");

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();
        }
    }

    @Override
    public boolean requiresExternalConnection() {
        return false;
    }
}
