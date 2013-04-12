package com.redhat.contentspec.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.redhat.contentspec.client.commands.AssembleCommand;
import com.redhat.contentspec.client.commands.BuildCommand;
import com.redhat.contentspec.client.commands.CheckoutCommand;
import com.redhat.contentspec.client.commands.ChecksumCommand;
import com.redhat.contentspec.client.commands.CreateCommand;
import com.redhat.contentspec.client.commands.InfoCommand;
import com.redhat.contentspec.client.commands.ListCommand;
import com.redhat.contentspec.client.commands.PreviewCommand;
import com.redhat.contentspec.client.commands.PublishCommand;
import com.redhat.contentspec.client.commands.PullCommand;
import com.redhat.contentspec.client.commands.PullSnapshotCommand;
import com.redhat.contentspec.client.commands.PushCommand;
import com.redhat.contentspec.client.commands.PushTranslationCommand;
import com.redhat.contentspec.client.commands.RevisionsCommand;
import com.redhat.contentspec.client.commands.SearchCommand;
import com.redhat.contentspec.client.commands.SetupCommand;
import com.redhat.contentspec.client.commands.SnapshotCommand;
import com.redhat.contentspec.client.commands.StatusCommand;
import com.redhat.contentspec.client.commands.TemplateCommand;
import com.redhat.contentspec.client.commands.ValidateCommand;
import com.redhat.contentspec.client.commands.base.BaseCommand;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.config.ServerConfiguration;
import com.redhat.contentspec.client.config.ZanataServerConfiguration;
import com.redhat.contentspec.client.constants.ConfigConstants;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.entities.RESTVersionDecorator;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.client.utils.LoggingUtilities;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTUserV1;
import org.jboss.pressgang.ccms.utils.common.VersionUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

public class Client implements BaseCommand, ShutdownAbleApp {
    private final JCommander parser = new JCommander(this);

    /**
     * A mapping of the sub commands the client uses
     */
    private HashMap<String, BaseCommand> commands = new HashMap<String, BaseCommand>();

    private final ErrorLoggerManager elm = new ErrorLoggerManager();
    private RESTManager restManager = null;

    private BaseCommand command;

    private File csprocessorcfg = new File("csprocessor.cfg");
    private ContentSpecConfiguration cspConfig = new ContentSpecConfiguration();

    private ClientConfiguration clientConfig = new ClientConfiguration();
    private boolean firstRun = false;

    @Parameter(names = {Constants.SERVER_LONG_PARAM, Constants.SERVER_SHORT_PARAM}, metaVar = "<URL>")
    private String serverUrl;

    @Parameter(names = {Constants.USERNAME_LONG_PARAM, Constants.USERANME_SHORT_PARAM}, metaVar = "<USERNAME>")
    private String username;

    @Parameter(names = Constants.HELP_LONG_PARAM)
    private Boolean showHelp = false;

    @Parameter(names = Constants.VERSION_LONG_PARAM)
    private Boolean showVersion = false;

    @Parameter(names = Constants.CONFIG_LONG_PARAM, metaVar = "<FILE>")
    private String configLocation = Constants.DEFAULT_CONFIG_LOCATION;

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    protected final AtomicBoolean shutdown = new AtomicBoolean(false);
    protected final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public static void main(String[] args) {
        Client client = new Client();
        try {
            Runtime.getRuntime().addShutdownHook(new ShutdownInterceptor(client));
            client.setup();
            client.processArgs(args);
        } catch (Throwable ex) {
            JCommander.getConsole().println(ex.getMessage());
            client.shutdown(Constants.EXIT_FAILURE);
        }
    }

    public Client() {
    }

    public void setup() {
        /* Set stderr to log to log4j */
        LoggingUtilities.tieSystemErrToLog(Logger.getLogger(Client.class));

        // Set the column width
        try {
            parser.setColumnSize(Integer.parseInt(System.getenv("COLUMNS")));
        } catch (Exception e) {
            parser.setColumnSize(160);
        }

        // Set the program name
        parser.setProgramName(Constants.PROGRAM_NAME);

        // Setup the commands that are to be used
        setupCommands(parser, cspConfig, clientConfig);
    }

    /**
     * Parse the set of arguments passed via the command line and then process those arguments
     *
     * @param args The array of arguments from the command line
     */
    public void processArgs(final String[] args) {
        try {
            parser.parse(args);
        } catch (ParameterException e) {
            if (parser.getParsedCommand() != null) {
                commands.get(parser.getParsedCommand()).printError("Invalid Argument! Error Message: " + e.getMessage(), true);
            } else {
                printError("Invalid Argument! Error Message: " + e.getMessage(), true);
            }
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Get the command used
        final String commandName = parser.getParsedCommand();
        if (commandName == null) {
            command = this;
        } else {
            command = commands.get(commandName);
        }

        // Process the command
        if (command.isShowHelp() || isShowHelp() || args.length == 0) {
            command.printHelp();
        } else if (command.isShowVersion() || isShowVersion()) {
            // Print the version details
            printVersionDetails(Constants.BUILD_MSG,
                    VersionUtilities.getAPIVersion(Constants.VERSION_PROPERTIES_FILENAME, Constants.VERSION_PROPERTY_NAME), false);
        } else if (command instanceof SetupCommand) {
            command.process(restManager, elm, null);
        } else {
            // Print the version details
            printVersionDetails(Constants.BUILD_MSG,
                    VersionUtilities.getAPIVersion(Constants.VERSION_PROPERTIES_FILENAME, Constants.VERSION_PROPERTY_NAME), false);

            // Good point to check for a shutdown
            if (isAppShuttingDown()) {
                shutdown.set(true);
                return;
            }

            // Load the configuration options. If it fails then stop the program
            if (!setConfigOptions(command.getConfigLocation())) {
                shutdown(Constants.EXIT_CONFIG_ERROR);
            }

            // Good point to check for a shutdown
            if (isAppShuttingDown()) {
                shutdown.set(true);
                return;
            }

            // If we are loading from csprocessor.cfg then display a message
            if (command.loadFromCSProcessorCfg()) {
                JCommander.getConsole().println(Constants.CSP_CONFIG_LOADING_MSG);

                // Load the csprocessor.cfg file from the current directory
                try {
                    if (csprocessorcfg.exists() && csprocessorcfg.isFile()) {
                        ClientUtilities.readFromCsprocessorCfg(csprocessorcfg, cspConfig);
                        if (cspConfig.getContentSpecId() == null) {
                            printError(Constants.ERROR_INVALID_CSPROCESSOR_CFG_MSG, false);
                            shutdown(Constants.EXIT_CONFIG_ERROR);
                        }
                    }
                } catch (Exception e) {
                    // Do nothing if the csprocessor.cfg file couldn't be read
                }
            }

            // Apply the settings from the csprocessor.cfg, csprocessor.ini & command line.
            applySettings();

            // Good point to check for a shutdown
            if (isAppShuttingDown()) {
                shutdown.set(true);
                return;
            }

            // Check that the server Urls are valid
            command.validateServerUrl();
            final String uiServerURL = command.getServerUrl().replace("/TopicIndex/", "/pressgang-ccms-ui/");
            System.setProperty(CommonConstants.PRESS_GANG_UI_SYSTEM_PROPERTY, uiServerURL);

            // Print a line to separate content
            JCommander.getConsole().println("");

            // Create the REST Manager
            restManager = new RESTManager(command.getPressGangServerUrl());
            restManager.getProxyFactory().registerProvider(RESTVersionDecorator.class);

            // Good point to check for a shutdown
            if (isAppShuttingDown()) {
                shutdown.set(true);
                return;
            }

            // Process the commands
            isProcessing.set(true);
            final RESTUserV1 user = command.authenticate(restManager.getReader());
            command.process(restManager, elm, user);
            isProcessing.set(false);

            // Check if the program was shutdown
            if (isShutdown()) return;

            // Add a newline just to separate the output
            JCommander.getConsole().println("");
        }

        // The command is finished so set the client as shutdown
        setShutdown(true);
    }

    /**
     * Setup the commands to be used in the client
     *
     * @param parser       The parser used to parse the command line arguments.
     * @param cspConfig    The configuration settings for the csprocessor.cfg if one exists in the current directory.
     * @param clientConfig The configuration settings for the client.
     */
    protected void setupCommands(final JCommander parser, final ContentSpecConfiguration cspConfig,
            final ClientConfiguration clientConfig) {
        final AssembleCommand assemble = new AssembleCommand(parser, cspConfig, clientConfig);
        final BuildCommand build = new BuildCommand(parser, cspConfig, clientConfig);
        final CheckoutCommand checkout = new CheckoutCommand(parser, cspConfig, clientConfig);
        final CreateCommand create = new CreateCommand(parser, cspConfig, clientConfig);
        final ChecksumCommand checksum = new ChecksumCommand(parser, cspConfig, clientConfig);
        final InfoCommand info = new InfoCommand(parser, cspConfig, clientConfig);
        final ListCommand list = new ListCommand(parser, cspConfig, clientConfig);
        final PreviewCommand preview = new PreviewCommand(parser, cspConfig, clientConfig);
        final PublishCommand publish = new PublishCommand(parser, cspConfig, clientConfig);
        final PullCommand pull = new PullCommand(parser, cspConfig, clientConfig);
        final PullSnapshotCommand pullSnapshot = new PullSnapshotCommand(parser, cspConfig, clientConfig);
        final PushCommand push = new PushCommand(parser, cspConfig, clientConfig);
        final PushTranslationCommand pushTranslation = new PushTranslationCommand(parser, cspConfig, clientConfig);
        final RevisionsCommand revisions = new RevisionsCommand(parser, cspConfig, clientConfig);
        final SearchCommand search = new SearchCommand(parser, cspConfig, clientConfig);
        final SetupCommand setup = new SetupCommand(parser, cspConfig, clientConfig);
        final SnapshotCommand snapshot = new SnapshotCommand(parser, cspConfig, clientConfig);
        final StatusCommand status = new StatusCommand(parser, cspConfig, clientConfig);
        final TemplateCommand template = new TemplateCommand(parser, cspConfig, clientConfig);
        final ValidateCommand validate = new ValidateCommand(parser, cspConfig, clientConfig);

        parser.addCommand(Constants.ASSEMBLE_COMMAND_NAME, assemble);
        commands.put(Constants.ASSEMBLE_COMMAND_NAME, assemble);

        parser.addCommand(Constants.BUILD_COMMAND_NAME, build);
        commands.put(Constants.BUILD_COMMAND_NAME, build);

        parser.addCommand(Constants.CHECKOUT_COMMAND_NAME, checkout);
        commands.put(Constants.CHECKOUT_COMMAND_NAME, checkout);

        parser.addCommand(Constants.CREATE_COMMAND_NAME, create);
        commands.put(Constants.CREATE_COMMAND_NAME, create);

        parser.addCommand(Constants.CHECKSUM_COMMAND_NAME, checksum);
        commands.put(Constants.CHECKSUM_COMMAND_NAME, checksum);

        parser.addCommand(Constants.INFO_COMMAND_NAME, info);
        commands.put(Constants.INFO_COMMAND_NAME, info);

        parser.addCommand(Constants.LIST_COMMAND_NAME, list);
        commands.put(Constants.LIST_COMMAND_NAME, list);

        parser.addCommand(Constants.PREVIEW_COMMAND_NAME, preview);
        commands.put(Constants.PREVIEW_COMMAND_NAME, preview);

        parser.addCommand(Constants.PUBLISH_COMMAND_NAME, publish);
        commands.put(Constants.PUBLISH_COMMAND_NAME, publish);

        parser.addCommand(Constants.PULL_COMMAND_NAME, pull);
        commands.put(Constants.PULL_COMMAND_NAME, pull);

        parser.addCommand(Constants.PULL_SNAPSHOT_COMMAND_NAME, pullSnapshot);
        commands.put(Constants.PULL_SNAPSHOT_COMMAND_NAME, pullSnapshot);

        parser.addCommand(Constants.PUSH_COMMAND_NAME, push);
        commands.put(Constants.PUSH_COMMAND_NAME, push);

        parser.addCommand(Constants.PUSH_TRANSLATION_COMMAND_NAME, pushTranslation);
        commands.put(Constants.PUSH_TRANSLATION_COMMAND_NAME, pushTranslation);

        parser.addCommand(Constants.REVISIONS_COMMAND_NAME, revisions);
        commands.put(Constants.REVISIONS_COMMAND_NAME, revisions);

        parser.addCommand(Constants.SEARCH_COMMAND_NAME, search);
        commands.put(Constants.SEARCH_COMMAND_NAME, search);

        parser.addCommand(Constants.SETUP_COMMAND_NAME, setup);
        commands.put(Constants.SETUP_COMMAND_NAME, setup);

        parser.addCommand(Constants.SNAPSHOT_COMMAND_NAME, snapshot);
        commands.put(Constants.SNAPSHOT_COMMAND_NAME, snapshot);

        parser.addCommand(Constants.STATUS_COMMAND_NAME, status);
        commands.put(Constants.STATUS_COMMAND_NAME, status);

        parser.addCommand(Constants.TEMPLATE_COMMAND_NAME, template);
        commands.put(Constants.TEMPLATE_COMMAND_NAME, template);

        parser.addCommand(Constants.VALIDATE_COMMAND_NAME, validate);
        commands.put(Constants.VALIDATE_COMMAND_NAME, validate);
    }

    /**
     * Apply the settings of the from the various configuration files and command line parameters to the used command.
     */
    protected void applySettings() {
        // Move the main parameters into the sub command
        if (getConfigLocation() != null) {
            command.setConfigLocation(getConfigLocation());
        }

        if (getServerUrl() != null) {
            command.setServerUrl(getServerUrl());
        }

        if (getUsername() != null) {
            command.setUsername(getUsername());
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        applyServerSettings();

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Set the root directory in the csprocessor configuration
        cspConfig.setRootOutputDirectory(clientConfig.getRootDirectory());

        applyZanataSettings();

        // Set the publish options
        if ((cspConfig.getKojiHubUrl() == null || cspConfig.getKojiHubUrl().isEmpty()) && clientConfig.getKojiHubUrl() != null) {
            cspConfig.setKojiHubUrl(ClientUtilities.validateHost(clientConfig.getKojiHubUrl()));
        }
        if ((cspConfig.getPublishCommand() == null || cspConfig.getPublishCommand().isEmpty()) && clientConfig.getPublishCommand() !=
                null) {
            cspConfig.setPublishCommand(clientConfig.getPublishCommand());
        }
    }

    /**
     * Apply the server settings from the client configuration
     * file to the command and/or Content Spec Configuration.
     */
    protected void applyServerSettings() {
        final Map<String, ServerConfiguration> servers = clientConfig.getServers();

        // If there is no server specified and no server in the csprocessor.cfg then make sure a default server exists
        if (command.getServerUrl() == null && ((cspConfig == null || cspConfig.getServerUrl() == null) && command.loadFromCSProcessorCfg
                () || !command.loadFromCSProcessorCfg())) {
            // Check that a default exists in the configuration files or via command line arguments
            if (!servers.containsKey(Constants.DEFAULT_SERVER_NAME) && getServerUrl() == null && command.getServerUrl() == null) {
                final File configFile = new File(ClientUtilities.validateConfigLocation(command.getConfigLocation()));
                command.printError(String.format(Constants.NO_DEFAULT_SERVER_FOUND, configFile.getAbsolutePath()), false);
                shutdown(Constants.EXIT_CONFIG_ERROR);
            } else if (servers.containsKey(Constants.DEFAULT_SERVER_NAME) && !servers.get(Constants.DEFAULT_SERVER_NAME).getUrl().matches(
                    "^(http://|https://).*")) {
                if (!servers.containsKey(servers.get(Constants.DEFAULT_SERVER_NAME).getUrl())) {
                    command.printError(Constants.NO_SERVER_FOUND_FOR_DEFAULT_SERVER, false);
                    shutdown(Constants.EXIT_CONFIG_ERROR);
                } else {
                    final ServerConfiguration defaultConfig = servers.get(Constants.DEFAULT_SERVER_NAME);
                    final ServerConfiguration config = servers.get(defaultConfig.getUrl());
                    defaultConfig.setUrl(config.getUrl());
                    if (config.getUsername() != null && !config.getUsername().equals("")) defaultConfig.setUsername(config.getUsername());
                }
            }
        }

        // Set the URL
        String url = null;
        if (command.getServerUrl() != null) {
            // Check if the server url is a name defined in csprocessor.ini
            for (final Entry<String, ServerConfiguration> serversEntry : servers.entrySet()) {
                final String serverName = serversEntry.getKey();

                // Ignore the default server for csprocessor.cfg configuration files
                if (serverName.equals(Constants.DEFAULT_SERVER_NAME)) {
                    continue;
                } else if (serverName.equals(command.getServerUrl())) {
                    command.setServerUrl(serversEntry.getValue().getUrl());
                    break;
                }
            }

            url = ClientUtilities.validateHost(command.getServerUrl());
        } else if (cspConfig != null && cspConfig.getServerUrl() != null && command.loadFromCSProcessorCfg()) {
            for (final Entry<String, ServerConfiguration> serversEntry : servers.entrySet()) {
                final String serverName = serversEntry.getKey();

                // Ignore the default server for csprocessor.cfg configuration files
                if (serverName.equals(Constants.DEFAULT_SERVER_NAME)) continue;

                // Compare the urls
                try {
                    final ServerConfiguration serverConfig = serversEntry.getValue();

                    URI serverUrl = new URI(ClientUtilities.validateHost(serverConfig.getUrl()));
                    if (serverUrl.equals(new URI(cspConfig.getServerUrl()))) {
                        url = serverConfig.getUrl();
                        break;
                    }
                } catch (URISyntaxException e) {
                    break;
                }
            }

            // If no URL matched between the csprocessor.ini and csprocessor.cfg then print an error
            if (url == null && !firstRun) {
                JCommander.getConsole().println("");
                printError(String.format(Constants.ERROR_NO_SERVER_FOUND_MSG, cspConfig.getServerUrl()), false);
                shutdown(Constants.EXIT_CONFIG_ERROR);
            } else if (url == null) {
                JCommander.getConsole().println("");
                printError(Constants.SETUP_CONFIG_MSG, false);
                shutdown(Constants.EXIT_CONFIG_ERROR);
            }
        } else {
            url = servers.get(Constants.DEFAULT_SERVER_NAME).getUrl();
        }
        command.setServerUrl(url);

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Set the username
        if (command.getUsername() == null) {
            for (final Entry<String, ServerConfiguration> serversEntry : servers.entrySet()) {
                final String serverName = serversEntry.getKey();
                final ServerConfiguration serverConfig = serversEntry.getValue();

                if (serverName.equals(Constants.DEFAULT_SERVER_NAME) || servers.get(serverName).getUrl().isEmpty()) continue;

                try {
                    URL serverUrl = new URL(serverConfig.getUrl());
                    if (serverUrl.equals(new URL(url))) {
                        command.setUsername(serverConfig.getUsername());
                    }
                } catch (MalformedURLException e) {
                    if (servers.get(Constants.DEFAULT_SERVER_NAME) != null) {
                        command.setUsername(servers.get(Constants.DEFAULT_SERVER_NAME).getUsername());
                    }
                }
            }

            // If none were found for the server then use the default
            if ((command.getUsername() == null || command.getUsername().equals("")) && servers.get(Constants.DEFAULT_SERVER_NAME) != null) {
                command.setUsername(servers.get(Constants.DEFAULT_SERVER_NAME).getUsername());
            }
        }

        if (cspConfig.getServerUrl() == null) {
            cspConfig.setServerUrl(url);
        }
    }

    /**
     * Apply the zanata settings from the client configuration
     * file to the command and/or Content Spec Configuration.
     */
    protected void applyZanataSettings() {
        if (cspConfig == null) return;

        // Setup the zanata details
        final Map<String, ZanataServerConfiguration> zanataServers = clientConfig.getZanataServers();

        // Set the zanata details
        if (cspConfig.getZanataDetails() != null && cspConfig.getZanataDetails().getServer() != null && !cspConfig.getZanataDetails()
                .getServer().isEmpty() && command.loadFromCSProcessorCfg()) {
            ZanataServerConfiguration zanataServerConfig = null;
            for (final Entry<String, ZanataServerConfiguration> serverEntry : zanataServers.entrySet()) {
                final String serverName = serverEntry.getKey();
                final ZanataServerConfiguration serverConfig = serverEntry.getValue();

                // Ignore the default server for csprocessor.cfg configuration files
                if (serverName.equals(Constants.DEFAULT_SERVER_NAME)) continue;

                // Compare the urls
                try {
                    URI serverUrl = new URI(ClientUtilities.validateHost(serverConfig.getUrl()));
                    if (serverUrl.equals(new URI(cspConfig.getZanataDetails().getServer()))) {
                        zanataServerConfig = serverConfig;
                        break;
                    }
                } catch (URISyntaxException e) {
                    break;
                }
            }

            // If no URL matched between the csprocessor.ini and csprocessor.cfg then print an error
            if (zanataServerConfig == null) {
                JCommander.getConsole().println("");
                printError(String.format(Constants.ERROR_NO_ZANATA_SERVER_SETUP_MSG, cspConfig.getZanataDetails().getServer()), false);
                shutdown(Constants.EXIT_CONFIG_ERROR);
            } else {
                cspConfig.getZanataDetails().setUsername(zanataServerConfig.getUsername());
                cspConfig.getZanataDetails().setToken(zanataServerConfig.getToken());
            }
        } else if (clientConfig.getZanataServers().containsKey(Constants.DEFAULT_SERVER_NAME)) {
            final ZanataServerConfiguration zanataServerConfig = clientConfig.getZanataServers().get(Constants.DEFAULT_SERVER_NAME);
            cspConfig.getZanataDetails().setServer(ClientUtilities.validateHost(zanataServerConfig.getUrl()));
            cspConfig.getZanataDetails().setUsername(zanataServerConfig.getUsername());
            cspConfig.getZanataDetails().setToken(zanataServerConfig.getToken());
        }

        // Setup the default zanata project and version
        if (cspConfig.getZanataDetails().getProject() == null || cspConfig.getZanataDetails().getProject().isEmpty()) {
            cspConfig.getZanataDetails().setProject(clientConfig.getDefaultZanataProject());
        }
        if (cspConfig.getZanataDetails().getVersion() == null || cspConfig.getZanataDetails().getVersion().isEmpty()) {
            cspConfig.getZanataDetails().setVersion(clientConfig.getDefaultZanataVersion());
        }
    }

    /**
     * Sets the configuration options from the csprocessor.ini configuration file
     *
     * @param location The location of the csprocessor.ini file (eg. /home/{@code<USERNAME>}/.config/)
     * @return Returns false if an error occurs otherwise true
     */
    protected boolean setConfigOptions(final String location) {
        final String fixedLocation = ClientUtilities.validateConfigLocation(location);
        final HierarchicalINIConfiguration configReader;

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return false;
        }

        // Checks if the file exists in the specified location
        final File file = new File(location);
        if (file.exists() && !file.isDirectory()) {
            JCommander.getConsole().println(String.format(Constants.CONFIG_LOADING_MSG, location));
            // Initialise the configuration reader with the skynet.ini content
            try {
                configReader = new HierarchicalINIConfiguration(fixedLocation);
            } catch (ConfigurationException e) {
                command.printError(Constants.INI_NOT_FOUND_MSG, false);
                return false;
            }
        } else if (location.equals(Constants.DEFAULT_CONFIG_LOCATION)) {
            JCommander.getConsole().println(String.format(Constants.CONFIG_CREATING_MSG, location));
            firstRun = true;

            // Save the configuration file
            try {
                // Make sure the directory exists
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }

                // Save the config
                final FileOutputStream fos = new FileOutputStream(file);
                fos.write(ConfigConstants.DEFAULT_CONFIG_FILE.getBytes("UTF-8"));
                fos.flush();
                fos.close();
            } catch (IOException e) {
                printError(Constants.ERROR_FAILED_CREATING_CONFIG_MSG, false);
                return false;
            }
            return setConfigOptions(location);
        } else {
            command.printError(Constants.INI_NOT_FOUND_MSG, false);
            return false;
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return false;
        }
		
		/* Read in the servers from the config file */
        if (!readServersFromConfig(configReader)) {
            return false;
        }

        // Read in the root directory
        if (!configReader.getRootNode().getChildren("directory").isEmpty()) {
            // Load the root content specs directory
            if (configReader.getProperty("directory.root") != null && !configReader.getProperty("directory.root").equals("")) {
                clientConfig.setRootDirectory(ClientUtilities.validateDirLocation(configReader.getProperty("directory.root").toString()));
            }

            // Load the install directory
            if (configReader.getProperty("directory.install") != null && !configReader.getProperty("directory.install").equals("")) {
                clientConfig.setInstallPath(ClientUtilities.validateDirLocation(configReader.getProperty("directory.install").toString()));
            }
        }

        // Read in the publican build options
        if (!configReader.getRootNode().getChildren("publican").isEmpty()) {
            // Load the publican setup values
            if (configReader.getProperty("publican.build..parameters") != null && !configReader.getProperty(
                    "publican.build..parameters").equals("")) {
                clientConfig.setPublicanBuildOptions(configReader.getProperty("publican.build..parameters").toString());
            }
            if (configReader.getProperty("publican.preview..format") != null && !configReader.getProperty(
                    "publican.preview..format").equals("")) {
                clientConfig.setPublicanPreviewFormat(configReader.getProperty("publican.preview..format").toString());
            }
            if (configReader.getProperty("publican.common_content") != null && !configReader.getProperty("publican.common_content").equals(
                    "")) {
                clientConfig.setPublicanCommonContentDirectory(
                        ClientUtilities.validateDirLocation(configReader.getProperty("publican.common_content").toString()));
            } else {
                clientConfig.setPublicanCommonContentDirectory(Constants.LINUX_PUBLICAN_COMMON_CONTENT);
            }
        } else {
            clientConfig.setPublicanBuildOptions(Constants.DEFAULT_PUBLICAN_OPTIONS);
            clientConfig.setPublicanPreviewFormat(Constants.DEFAULT_PUBLICAN_FORMAT);
        }
		
		/* Read in the zanata details from the config file */
        if (!readZanataDetailsFromConfig(configReader)) {
            return false;
        }

        // Read in the publishing information
        if (!configReader.getRootNode().getChildren("publish").isEmpty()) {
            // Load the koji hub url
            if (configReader.getProperty("publish.koji..huburl") != null && !configReader.getProperty("publish.koji..huburl").equals("")) {
                clientConfig.setKojiHubUrl(configReader.getProperty("publish.koji..huburl").toString());
            }

            // Load the publish command name
            if (configReader.getProperty("publish.command") != null && !configReader.getProperty("publish.command").equals("")) {
                clientConfig.setPublishCommand(configReader.getProperty("publish.command").toString());
            }
        }

        return true;
    }

    /**
     * Read the Server settings from a INI Configuration file.
     *
     * @param configReader The initialized configuration reader to read
     *                     the server configuration from file.
     * @return True if everything was read in correctly otherwise false.
     */
    @SuppressWarnings("unchecked")
    protected boolean readServersFromConfig(final HierarchicalINIConfiguration configReader) {
        final Map<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();

        // Read in and process the servers
        if (!configReader.getRootNode().getChildren("servers").isEmpty()) {
            final SubnodeConfiguration serversNode = configReader.getSection("servers");
            for (final Iterator<String> it = serversNode.getKeys(); it.hasNext(); ) {
                String prefix = "";
                final String key = it.next();

                // Find the prefix (aka server name) on urls
                if (key.endsWith(".url")) {
                    prefix = key.substring(0, key.length() - ".url".length());

                    final String name = prefix.substring(0, prefix.length() - 1);
                    final String url = serversNode.getString(prefix + ".url");
                    final String username = serversNode.getString(prefix + ".username");

                    // Check that a url was specified
                    if (url == null) {
                        command.printError(String.format(Constants.NO_SERVER_URL_MSG, name), false);
                        return false;
                    }

                    // Create the Server Configuration
                    final ServerConfiguration serverConfig = new ServerConfiguration();
                    serverConfig.setName(name);
                    serverConfig.setUrl(url);
                    serverConfig.setUsername(username);

                    servers.put(name, serverConfig);
                }
                // Just the default server name
                else if (key.equals(Constants.DEFAULT_SERVER_NAME)) {
                    // Create the Server Configuration
                    final ServerConfiguration serverConfig = new ServerConfiguration();
                    serverConfig.setName(key);
                    serverConfig.setUrl(serversNode.getString(key));
                    serverConfig.setUsername(serversNode.getString(key + "..username"));

                    servers.put(Constants.DEFAULT_SERVER_NAME, serverConfig);
                }
            }
        } else {
            // Add the default server config to the list of server configurations
            final ServerConfiguration config = new ServerConfiguration();
            config.setName(Constants.DEFAULT_SERVER_NAME);
            config.setUrl("http://localhost:8080/TopicIndex/");
            servers.put(Constants.DEFAULT_SERVER_NAME, config);
        }

        // Add the servers to the client configuration
        clientConfig.setServers(servers);
        return true;
    }

    /**
     * Read the Zanata Server settings from a INI Configuration file.
     *
     * @param configReader The initialized configuration reader to read
     *                     the server configuration from file.
     * @return True if everything was read in correctly otherwise false.
     */
    @SuppressWarnings("unchecked")
    protected boolean readZanataDetailsFromConfig(final HierarchicalINIConfiguration configReader) {
        // Read in the zanata server information
        final Map<String, ZanataServerConfiguration> zanataServers = new HashMap<String, ZanataServerConfiguration>();

        // Read in and process the servers
        if (!configReader.getRootNode().getChildren("zanata").isEmpty()) {
            final SubnodeConfiguration serversNode = configReader.getSection("zanata");
            for (final Iterator<String> it = serversNode.getKeys(); it.hasNext(); ) {
                String prefix = "";
                final String key = it.next();

                // Find the prefix (aka server name) on urls
                if (key.endsWith(".url")) {
                    prefix = key.substring(0, key.length() - ".url".length());

                    final String name = prefix.substring(0, prefix.length() - 1);
                    final String url = serversNode.getString(prefix + ".url");
                    final String username = serversNode.getString(prefix + ".username");
                    final String token = serversNode.getString(prefix + ".key");

                    // Check that a url was specified
                    if (url == null) {
                        command.printError(String.format(Constants.NO_ZANATA_SERVER_URL_MSG, name), false);
                        return false;
                    }

                    // Create the Server Configuration
                    final ZanataServerConfiguration serverConfig = new ZanataServerConfiguration();
                    serverConfig.setName(name);
                    serverConfig.setUrl(url);
                    serverConfig.setUsername(username);
                    serverConfig.setToken(token);

                    zanataServers.put(name, serverConfig);
                } else if (key.equals(Constants.DEFAULT_SERVER_NAME)) {
                    final String url = serversNode.getString(key);

                    // Only load the default server if one is specified
                    if (url != null && !url.isEmpty()) {
                        // Create the Server Configuration
                        final ZanataServerConfiguration serverConfig = new ZanataServerConfiguration();
                        serverConfig.setName(key);
                        serverConfig.setUrl(url);

                        zanataServers.put(Constants.DEFAULT_SERVER_NAME, serverConfig);
                    }

                    // Find the default project and version values
                    final String project = serversNode.getString(key + "..project");
                    final String version = serversNode.getString(key + "..project-version");

                    if (project != null && !project.isEmpty()) clientConfig.setDefaultZanataProject(project);
                    if (version != null && !version.isEmpty()) clientConfig.setDefaultZanataVersion(version);
                }
            }
        }

        // Setup the default zanata server
        if (zanataServers.containsKey(Constants.DEFAULT_SERVER_NAME) && !zanataServers.get(Constants.DEFAULT_SERVER_NAME).getUrl().matches(
                "^(http://|https://).*")) {
            if (!zanataServers.containsKey(zanataServers.get(Constants.DEFAULT_SERVER_NAME).getUrl())) {
                command.printError(Constants.NO_ZANATA_SERVER_FOUND_FOR_DEFAULT_SERVER, false);
                return false;
            } else {
                final ZanataServerConfiguration defaultConfig = zanataServers.get(Constants.DEFAULT_SERVER_NAME);
                final ZanataServerConfiguration config = zanataServers.get(defaultConfig.getUrl());
                defaultConfig.setUrl(config.getUrl());
                defaultConfig.setUsername(config.getUsername());
                defaultConfig.setToken(config.getToken());
                defaultConfig.setUsername(config.getUsername());
            }
        }

        clientConfig.setZanataServers(zanataServers);
        return true;
    }

    /**
     * Prints the version of the client to the console
     *
     * @param msg     The version message format to be printed.
     * @param version The version to be displayed.
     * @param printNL Whether a newline should be printed after the message
     */
    private void printVersionDetails(final String msg, final String version, final boolean printNL) {
        if (version.contains("-SNAPSHOT")) {
            final String buildDate = VersionUtilities.getAPIBuildTimestamp(Constants.VERSION_PROPERTIES_FILENAME,
                    Constants.BUILDDATE_PROPERTY_NAME);
            JCommander.getConsole().println(String.format(msg + ", Build %s", version, buildDate));
        } else {
            JCommander.getConsole().println(String.format(msg, version));
        }
        if (printNL) {
            JCommander.getConsole().println("");
        }
    }

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
        return serverUrl == null ? null : ((serverUrl.endsWith("/") ? serverUrl : (serverUrl + "/")) + "seam/resource/rest");
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

    @Override
    public Boolean isShowVersion() {
        return showVersion;
    }

    @Override
    public void setShowVersion(final Boolean showVersion) {
        this.showVersion = showVersion;
    }

    @Override
    public void printHelp() {
        parser.usage(false);
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        JCommander.getConsole().println("ERROR: " + errorMsg);
        if (displayHelp) {
            JCommander.getConsole().println("");
            printHelp();
        } else {
            JCommander.getConsole().println("");
        }
    }

    @Override
    public RESTUserV1 authenticate(final RESTReader reader) {
        return null;
    }

    @Override
    public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user) {
    }

    @Override
    public void shutdown() {
        this.isShuttingDown.set(true);
        if (command != null && command != this) command.shutdown();
    }

    public void shutdown(int exitStatus) {
        shutdown.set(true);
        if (command != null) command.setShutdown(true);
        System.exit(exitStatus);
    }

    @Override
    public boolean isAppShuttingDown() {
        return isShuttingDown.get();
    }

    @Override
    public void setAppShuttingDown(boolean shuttingDown) {
        this.isShuttingDown.set(shuttingDown);
    }

    @Override
    public boolean isShutdown() {
        boolean processingSubCommand = command != null && command != this && isProcessing.get();
        return processingSubCommand ? command.isShutdown() : shutdown.get();
    }

    @Override
    public void setShutdown(boolean shutdown) {
        this.shutdown.set(shutdown);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return cspConfig != null && csprocessorcfg.exists();
    }

    @Override
    public void validateServerUrl() {
        // Print the server url
        JCommander.getConsole().println(String.format(Constants.WEBSERVICE_MSG, getServerUrl()));

        // Test that the server address is valid
        if (!ClientUtilities.validateServerExists(getServerUrl())) {
            // Print a line to separate content
            JCommander.getConsole().println("");

            printError(Constants.UNABLE_TO_FIND_SERVER_MSG, false);
            shutdown(Constants.EXIT_NO_SERVER);
        }
    }
}
