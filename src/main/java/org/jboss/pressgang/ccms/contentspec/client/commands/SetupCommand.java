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

package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

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

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SETUP")
public class SetupCommand extends BaseCommandImpl {
    private static final String YES_NO = " (Yes/No)";

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

        configFile.append("\n");

        // Setup the Root csprocessor project directory
        setupRootDirectory(configFile);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        configFile.append("\n");

        // Setup the default publican options
        setupPublican(configFile);

        // Get the jdocbook build options
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_JDOCBOOK_MSG") + YES_NO);
        String answer = JCommander.getConsole().readLine();

        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupjDocbook(configFile);
        }

        // Get the publishing options
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_PUBLISHING_MSG") + YES_NO);
        answer = JCommander.getConsole().readLine();

        // Setup the publishing settings if required
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupPublish(configFile);
        }

        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_ZANATA_MSG") + YES_NO);
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

        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_EDITOR_MSG") + YES_NO);
        answer = JCommander.getConsole().readLine();

        // Setup the Editor Settings if required
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupEditor(configFile);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the default options
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_DEFAULTS_MSG") + YES_NO);
        answer = JCommander.getConsole().readLine();

        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            configFile.append("\n");

            setupDefaults(configFile);
        }

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
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_NO_WRITE_INI_MSG",
                    Constants.DEFAULT_CONFIG_LOCATION),
                    false);
        }

        JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_SETUP_MSG"));
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

        final TreeMap<String, ServerConfiguration> servers = new TreeMap<String, ServerConfiguration>(new ServerNameComparator());

        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_USE_DEFAULT_CONFIG_MSG") + YES_NO);
        String answer = JCommander.getConsole().readLine();

        // We are using the default setup so we only need to get the default server and a username
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            // Get which server they want to connect to by default
            while (!defaultServerName.equalsIgnoreCase("test") && !defaultServerName.equalsIgnoreCase("production")) {
                JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_WHICH_SERVER_MSG") + " (test/production)");
                defaultServerName = JCommander.getConsole().readLine().toLowerCase();

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }

            // Get the users username
            JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_ENTER_USERNAME_MSG") + " ");
            username = JCommander.getConsole().readLine();

            // Create the default settings
            servers.put(Constants.DEFAULT_SERVER_NAME, new ServerConfiguration(Constants.DEFAULT_SERVER_NAME, defaultServerName, username));

            // Setup each servers settings
            servers.put("test", new ServerConfiguration("test", Constants.DEFAULT_TEST_SERVER));
            servers.put("production", new ServerConfiguration("production", Constants.DEFAULT_PROD_SERVER));

        } else if (answer.equalsIgnoreCase("no") || answer.equalsIgnoreCase("n")) {
            // We need to read in a list of servers and then get the default server
            while (!answer.matches("^[0-9]+$") || (Integer.parseInt(answer) <= 0)) {
                JCommander.getConsole().print(ClientUtilities.getMessage("SETUP_HOW_MANY_SERVERS_MSG") + " ");
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

            // Only ask for the default when there are multiple servers
            if (servers.size() > 1) {
                // Get which server they want to connect to
                while (!servers.containsKey(defaultServerName)) {
                    JCommander.getConsole().println(
                            ClientUtilities.getMessage("SETUP_WHICH_SERVER_MSG") + " (" + serverNames.substring(0, serverNames.length() - 1) + ")");
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
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("INVALID_ARG_MSG"), false);
        }

        // Add the information to the configuration file
        configFile.append("[servers]\n");
        int count = 0;
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

            if (++count < servers.size()) {
                // Add a blank line to separate servers
                configFile.append("\n");
            }

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
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_ROOT_DIRECTORY_MSG"));
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
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_PUBLICAN_BUILD_PARAMS_MSG", Constants.DEFAULT_PUBLICAN_OPTIONS));
        publicanParams = JCommander.getConsole().readLine();
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_PUBLICAN_PREVIEW_FORMAT_MSG", Constants.DEFAULT_PUBLICAN_FORMAT));
        publicanFormat = JCommander.getConsole().readLine();

        // Create the publican options
        configFile.append("[publican]\n");
        configFile.append("build.parameters=" + (publicanParams.isEmpty() ? Constants.DEFAULT_PUBLICAN_OPTIONS : publicanParams) + "\n");
        configFile.append("preview.format=" + (publicanFormat.isEmpty() ? Constants.DEFAULT_PUBLICAN_FORMAT : publicanFormat) + "\n");
    }

    /**
     * Setup the jDocbook configuration options by asking
     * the user for specific details.
     *
     * @param configFile
     */
    protected void setupjDocbook(final StringBuilder configFile) {
        String jDocbookParams = "";
        String jDocbookFormat = "";

        // Get the jDocbook options
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_JDOCBOOK_BUILD_PARAM_MSG", Constants.DEFAULT_JDOCBOOK_OPTIONS));
        jDocbookParams = JCommander.getConsole().readLine();
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_JDOCBOOK_PREVIEW_FORMAT_MSG", Constants.DEFAULT_JDOCBOOK_FORMAT));
        jDocbookFormat = JCommander.getConsole().readLine();

        // Create the publican options
        configFile.append("[jDocbook]\n");
        configFile.append("build.parameters=" + (jDocbookParams.isEmpty() ? Constants.DEFAULT_JDOCBOOK_OPTIONS : jDocbookParams) + "\n");
        configFile.append("preview.format=" + (jDocbookFormat.isEmpty() ? Constants.DEFAULT_JDOCBOOK_FORMAT : jDocbookFormat) + "\n");
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
        configFile.append("command=" + (publishCommand.isEmpty() ? Constants.DEFAULT_PUBLISH_COMMAND : publishCommand) + "\n");
    }

    /**
     * Setup the zanata servers configuration options by asking
     * the user for specific details. This setups up the servers
     * in the same form as zanata for convenience.
     *
     * @param configFile
     */
    protected void setupZanata(final StringBuilder configFile) {
        final TreeMap<String, ZanataServerConfiguration> servers = new TreeMap<String, ZanataServerConfiguration>(
                new ServerNameComparator());

        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_USE_DEFAULT_ZANATA_CONFIG_MSG") + YES_NO);
        String answer = JCommander.getConsole().readLine();

        // We are using the default setup so we only need to get the username and api key.
        if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
            final ZanataServerConfiguration config = new ZanataServerConfiguration();

            // Set the server defaults
            config.setName(Constants.DEFAULT_ZANATA_SERVER_NAME);
            config.setUrl(Constants.DEFAULT_ZANATA_SERVER);

            // Get the username for the server
            JCommander.getConsole().println("Please enter your username for " + Constants.DEFAULT_ZANATA_SERVER + ": ");
            config.setUsername(JCommander.getConsole().readLine());

            // Get the token for the server
            JCommander.getConsole().println("Please enter your API Key for " + Constants.DEFAULT_ZANATA_SERVER + ": ");
            config.setToken(JCommander.getConsole().readLine());

            // Add the server configuration
            servers.put(config.getName(), config);
        } else if (answer.equalsIgnoreCase("no") || answer.equalsIgnoreCase("n")) {
            while (!answer.matches("^[0-9]+$") || (Integer.parseInt(answer) <= 0)) {
                JCommander.getConsole().print("How many zanata servers are to be configured? ");
                answer = JCommander.getConsole().readLine();

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }

            // Get the number of servers
            final Integer numServers = Integer.parseInt(answer);

            // Get the server setup details from the user
            final StringBuilder serverNames = new StringBuilder("");
            for (int i = 1; i <= numServers; i++) {
                final ZanataServerConfiguration config = new ZanataServerConfiguration();

                // Get the name of the server
                JCommander.getConsole().println("Please enter a name for server no. " + i + ": ");
                config.setName(JCommander.getConsole().readLine().toLowerCase());

                // Get the url of the server
                JCommander.getConsole().println("Please enter the URL of server no. " + i + ": ");
                config.setUrl(ClientUtilities.fixHostURL(JCommander.getConsole().readLine()));

                // Get the username for the server
                JCommander.getConsole().println("Please enter your username of server no. " + i + ": ");
                config.setUsername(JCommander.getConsole().readLine());

                // Get the token for the server
                JCommander.getConsole().println("Please enter your API Key of server no. " + i + ": ");
                config.setToken(JCommander.getConsole().readLine());

                // Add the server configuration and add the name to the list of displayable strings
                servers.put(config.getName(), config);
                serverNames.append(config.getName() + "/");

                // Good point to check for a shutdown
                allowShutdownToContinueIfRequested();
            }
        } else {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("INVALID_ARG_MSG"), false);
        }

        // Add the information to the configuration file
        configFile.append("[zanata]\n");
        int count = 0;
        for (final Entry<String, ZanataServerConfiguration> serverEntry : servers.entrySet()) {
            final String serverName = serverEntry.getKey();
            final ZanataServerConfiguration config = serverEntry.getValue();

            // Setup the url for the server
            configFile.append(serverName + ".url=" + config.getUrl() + "\n");

            // Setup the username for the server
            if (config.getUsername() != null && !config.getUsername().equals(""))
                configFile.append(serverName + ".username=" + config.getUsername() + "\n");
            else configFile.append(serverName + ".username=\n");

            // Setup the token for the server
            if (config.getToken() != null && !config.getToken().equals(""))
                configFile.append(serverName + ".key=" + config.getToken() + "\n");
            else configFile.append(serverName + ".key=\n");

            if (++count < servers.size()) {
                // Add a blank line to separate servers
                configFile.append("\n");
            }

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();
        }
    }

    /**
     * Setup the editor configuration options by asking
     * the user specific details.
     *
     * @param configFile
     */
    protected void setupEditor(final StringBuilder configFile) {
        // Get the command
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_EDITOR_COMMAND_MSG"));
        String command = JCommander.getConsole().readLine();

        // Find out if the editor runs in a terminal
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_EDITOR_REQUIRES_TERMINAL_MSG") + YES_NO);
        String requiresTerminal = JCommander.getConsole().readLine();

        // Create the Root Directory
        configFile.append("[editor]\n");
        configFile.append("command=" + command + "\n");
        if (requiresTerminal.equalsIgnoreCase("yes") || requiresTerminal.equalsIgnoreCase("y")) {
            configFile.append("requiresTerminal=true\n");
        } else {
            configFile.append("requiresTerminal=false\n");
        }
    }

    /**
     * Setup the default value configuration options by asking
     * the user for specific details.
     *
     * @param configFile
     */
    protected void setupDefaults(final StringBuilder configFile) {
        // Get the default firstname
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_DEFAULT_FIRSTNAME_MSG"));
        String firstname = JCommander.getConsole().readLine();

        // Get the default surname
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_DEFAULT_SURNAME_MSG"));
        String surname = JCommander.getConsole().readLine();

        // Get the default email
        JCommander.getConsole().println(ClientUtilities.getMessage("SETUP_DEFAULT_EMAIL_MSG"));
        String email = JCommander.getConsole().readLine();

        // Create the Root Directory
        configFile.append("[defaults]\n");
        configFile.append("firstname=" + firstname + "\n");
        configFile.append("surname=" + surname + "\n");
        configFile.append("email=" + email + "\n");
    }

    @Override
    public boolean requiresExternalConnection() {
        return false;
    }

    /**
     * A comparator that can be used to sort servers into alphabetical order and ensure the default server is always first.
     */
    private static class ServerNameComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 8086132813427986154L;

        @Override
        public int compare(String serverName1, String serverName2) {
            if (serverName1.equals(Constants.DEFAULT_SERVER_NAME)) {
                return -1;
            } else if (serverName2.equals(Constants.DEFAULT_SERVER_NAME)) {
                return 1;
            } else {
                return serverName1.compareTo(serverName2);
            }
        }
    }
}
