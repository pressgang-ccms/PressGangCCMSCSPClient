package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.Lists;
import com.redhat.j2koji.exceptions.KojiException;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.builder.ContentSpecBuilder;
import org.jboss.pressgang.ccms.contentspec.builder.structures.CSDocbookBuildingOptions;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.client.validator.OverrideValidator;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

@Parameters(commandDescription = "Build a Content Specification from the server")
public class BuildCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID] or [FILE]")
    private List<String> ids = new ArrayList<String>();

    @Parameter(names = Constants.HIDE_ERRORS_LONG_PARAM, description = "Hide the errors in the output.")
    private Boolean hideErrors = false;

    @Parameter(names = Constants.SHOW_CONTENT_SPEC_LONG_PARAM, description = "Show the content spec page in the output.")
    private Boolean hideContentSpec = true;

    @Parameter(names = Constants.INLINE_INJECTION_LONG_PARAM, description = "Stop injections from being processed when building.")
    private Boolean inlineInjection = true;

    @Parameter(names = Constants.INJECTION_TYPES_LONG_PARAM, splitter = CommaParameterSplitter.class, metaVar = "[arg1[,arg2,...]]",
            description = "Specify certain topic types that injection should be processed on.")
    private List<String> injectionTypes;

    @Parameter(names = Constants.EXEC_TIME_LONG_PARAM, description = "Show the execution time of the command.", hidden = true)
    private Boolean executionTime = false;

    @Parameter(names = {Constants.PERMISSIVE_LONG_PARAM, Constants.PERMISSIVE_SHORT_PARAM}, description = "Turn on permissive processing.")
    private Boolean permissive = null;

    @DynamicParameter(names = Constants.OVERRIDE_LONG_PARAM, metaVar = "<variable>=<value>", validateWith = OverrideValidator.class)
    private Map<String, String> overrides = Maps.newHashMap();

    @Parameter(names = Constants.BUG_REPORTING_LONG_PARM, description = "Hide the error reporting links in the output.")
    private Boolean hideBugLinks = false;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM},
            description = "Save the output to the specified file/directory.", metaVar = "<FILE>")
    private String outputPath;

    @Parameter(names = Constants.EMPTY_LEVELS_LONG_PARAM, description = "Allow building with empty levels.", hidden = true)
    private Boolean allowEmptyLevels = false;

    @Parameter(names = Constants.EDITOR_LINKS_LONG_PARAM, description = "Insert Editor links for each topic.")
    private Boolean insertEditorLinks = false;

    @Parameter(names = Constants.LOCALE_LONG_PARAM, description = "What locale to build the content spec for.", metaVar = "<LOCALE>")
    private String locale = null;

    @Parameter(names = Constants.FETCH_PUBSNUM_LONG_PARAM, description = "Fetch the pubsnumber directly from " + Constants.KOJI_NAME + ".")
    protected Boolean fetchPubsnum = false;

    @Parameter(names = Constants.SHOW_REPORT_LONG_PARAM, description = "Show the Report chapter in the output.")
    protected Boolean showReport = false;

    @Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM,
            description = "The zanata server to be associated with the Content Specification.")
    private String zanataUrl = null;

    @Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM,
            description = "The zanata project name to be associated with the Content Specification.")
    private String zanataProject = null;

    @Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM,
            description = "The zanata project version to be associated with the Content Specification.")
    private String zanataVersion = null;

    @Parameter(names = Constants.COMMON_CONTENT_LONG_PARAM, hidden = true)
    private String commonContentLocale = null;

    @Parameter(names = Constants.TARGET_LANG_LONG_PARAM,
            description = "The output target locale if it is different from the --lang " + "option.", metaVar = "<LOCALE>")
    private String targetLocale = null;

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM})
    private Integer revision = null;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, description = "Update all current revisions, to the latest version when building.",
            hidden = true)
    private Boolean update = false;

    @Parameter(names = {Constants.DRAFT_LONG_PARAM, Constants.DRAFT_SHORT_PARAM}, description = "Build the book as a draft.")
    private Boolean draft = false;

    @Parameter(names = Constants.SHOW_REMARKS_LONG_PARAM, description = "Build the book with remarks visible.")
    private Boolean showRemarks = false;

    @Parameter(names = {Constants.REV_MESSAGE_LONG_PARAM, Constants.REV_MESSAGE_SHORT_PARAM},
            description = "Add a message for the revision history.")
    private List<String> messages = Lists.newArrayList();

    private ContentSpecProcessor csp = null;
    private ContentSpecBuilder builder = null;

    public BuildCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.BUILD_COMMAND_NAME;
    }

    public List<String> getInjectionTypes() {
        return injectionTypes;
    }

    public void setInjectionTypes(final List<String> injectionTypes) {
        this.injectionTypes = injectionTypes;
    }

    public Boolean getInlineInjection() {
        return inlineInjection;
    }

    public void setInlineInjection(final Boolean inlineInjection) {
        this.inlineInjection = inlineInjection;
    }

    public Boolean getHideErrors() {
        return hideErrors;
    }

    public void setHideErrors(final Boolean hideErrors) {
        this.hideErrors = hideErrors;
    }

    public Boolean getHideContentSpecPage() {
        return hideErrors;
    }

    public void setHideContentSpecPage(final Boolean hideContentSpecPage) {
        this.hideContentSpec = hideContentSpecPage;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(final List<String> ids) {
        this.ids = ids;
    }

    public Boolean getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final Boolean executionTime) {
        this.executionTime = executionTime;
    }

    public Map<String, String> getOverrides() {
        return overrides;
    }

    public void setOverrides(final Map<String, String> overrides) {
        this.overrides = overrides;
    }

    public Boolean getPermissive() {
        return permissive;
    }

    public void setPermissive(final Boolean permissive) {
        this.permissive = permissive;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(final String outputPath) {
        this.outputPath = outputPath;
    }

    public Boolean getAllowEmptyLevels() {
        return allowEmptyLevels;
    }

    public void setAllowEmptyLevels(final Boolean allowEmptyLevels) {
        this.allowEmptyLevels = allowEmptyLevels;
    }

    public Boolean getInsertEditorLinks() {
        return insertEditorLinks;
    }

    public void setInsertEditorLinks(final Boolean insertEditorLinks) {
        this.insertEditorLinks = insertEditorLinks;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public Boolean getFetchPubsnum() {
        return fetchPubsnum;
    }

    public void setFetchPubsnum(final Boolean fetchPubsnum) {
        this.fetchPubsnum = fetchPubsnum;
    }

    public Boolean getShowReport() {
        return showReport;
    }

    public void setShowReport(final Boolean showReport) {
        this.showReport = showReport;
    }

    public String getZanataUrl() {
        return zanataUrl;
    }

    public void setZanataUrl(final String zanataUrl) {
        this.zanataUrl = zanataUrl;
    }

    public String getZanataProject() {
        return zanataProject;
    }

    public void setZanataProject(final String zanataProject) {
        this.zanataProject = zanataProject;
    }

    public String getZanataVersion() {
        return zanataVersion;
    }

    public void setZanataVersion(final String zanataVersion) {
        this.zanataVersion = zanataVersion;
    }

    public String getCommonContentLocale() {
        return commonContentLocale;
    }

    public void setCommonContentLocale(final String commonContentLocale) {
        this.commonContentLocale = commonContentLocale;
    }

    public String getTargetLocale() {
        return targetLocale;
    }

    public void setTargetLocale(final String targetLocale) {
        this.targetLocale = targetLocale;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(final Integer revision) {
        this.revision = revision;
    }

    public Boolean getUpdate() {
        return update;
    }

    public void setUpdate(final Boolean update) {
        this.update = update;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(final Boolean draft) {
        this.draft = draft;
    }

    public Boolean getShowRemarks() {
        return showRemarks;
    }

    public void setShowRemarks(final Boolean showRemarks) {
        this.showRemarks = showRemarks;
    }

    public List<String> getMessage() {
        return messages;
    }

    public void setMessage(final List<String> messages) {
        this.messages = messages;
    }

    public CSDocbookBuildingOptions getBuildOptions() {
        // Fix up the values for overrides so file names are expanded
        fixOverrides();

        final CSDocbookBuildingOptions buildOptions = new CSDocbookBuildingOptions();
        buildOptions.setInjection(inlineInjection);
        buildOptions.setInjectionTypes(injectionTypes);
        buildOptions.setIgnoreMissingCustomInjections(hideErrors);
        buildOptions.setSuppressErrorsPage(hideErrors);
        buildOptions.setInsertSurveyLink(true);
        buildOptions.setInsertBugzillaLinks(!hideBugLinks);
        buildOptions.setOverrides(overrides);
        buildOptions.setSuppressContentSpecPage(hideContentSpec);
        buildOptions.setInsertEditorLinks(insertEditorLinks);
        buildOptions.setShowReportPage(showReport);
        buildOptions.setLocale(locale);
        buildOptions.setCommonContentLocale(commonContentLocale);
        buildOptions.setCommonContentDirectory(getClientConfig().getPublicanCommonContentDirectory());
        buildOptions.setOutputLocale(targetLocale);
        buildOptions.setDraft(draft);
        buildOptions.setPublicanShowRemarks(showRemarks);
        buildOptions.setRevisionMessages(messages);
        buildOptions.setUseLatestVersions(update);

        return buildOptions;
    }

    protected String getContentSpecFromFile(final String file) {
        // Get the content spec from the file
        final String contentSpec = FileUtilities.readFileContents(new File(ClientUtilities.validateFilePath(file)));

        if (contentSpec == null || contentSpec.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        if (permissive == null) {
            permissive = false;
        }

        return contentSpec;
    }

    /**
     * Sets the zanata options applied by the command line
     * to the options that were set via configuration files.
     */
    protected void setupZanataOptions() {
        // Set the zanata url
        if (this.zanataUrl != null) {
            // Find the zanata server if the url is a reference to the zanata server name
            for (final String serverName : getClientConfig().getZanataServers().keySet()) {
                if (serverName.equals(zanataUrl)) {
                    zanataUrl = getClientConfig().getZanataServers().get(serverName).getUrl();
                    break;
                }
            }

            getCspConfig().getZanataDetails().setServer(ClientUtilities.validateHost(zanataUrl));
        }

        // Set the zanata project
        if (this.zanataProject != null) {
            getCspConfig().getZanataDetails().setProject(zanataProject);
        }

        // Set the zanata version
        if (this.zanataVersion != null) {
            getCspConfig().getZanataDetails().setVersion(zanataVersion);
        }
    }

    @Override
    public void process() {
        // Authenticate the user
        final UserWrapper user = authenticate(getUsername(), getProviderFactory());

        final long startTime = System.currentTimeMillis();
        boolean buildingFromConfig = false;

        // Add the details for the csprocessor.cfg if no ids are specified and then validate input
        if (ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds())) {
            // Set the output path to the value store in the client config
            if (getCspConfig().getRootOutputDirectory() != null && !getCspConfig().getRootOutputDirectory().equals("")) {
                setOutputPath(getCspConfig().getRootOutputDirectory());
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the content spec and make sure it exists
        final ContentSpec contentSpec = getContentSpec(ids.get(0));

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Validate that the content spec is valid
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        boolean success = validateContentSpec(getProviderFactory(), loggerManager, user, contentSpec);

        // Print the error/warning messages
        JCommander.getConsole().println(loggerManager.generateLogs());

        // Check that everything validated fine
        if (!success) {
            shutdown(Constants.EXIT_TOPIC_INVALID);
        }

        // Pull in the pubsnumber from koji if the option is set
        if (getFetchPubsnum()) {
            final Integer pubsnumber = getContentSpecPubsNumberFromKoji(contentSpec, getCspConfig());
            if (pubsnumber != null) {
                contentSpec.setPubsNumber(pubsnumber);
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        JCommander.getConsole().println(Constants.STARTING_BUILD_MSG);

        // Setup the zanata details incase some were overridden via the command line
        setupZanataOptions();

        // Build the Content Specification
        byte[] builderOutput = buildContentSpec(contentSpec, user);

        // Print the success messages
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(String.format(Constants.ZIP_SAVED_ERRORS_MSG, builder.getNumErrors(),
                builder.getNumWarnings()) + (builder.getNumErrors() == 0 && builder.getNumWarnings() == 0 ? " - Flawless Victory!" : ""));
        if (executionTime) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }

        // Get the filename for the spec, using it's title.
        String fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle());

        // Create the output file
        String outputDir = "";
        if (buildingFromConfig) {
            outputDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec) + Constants.DEFAULT_CONFIG_ZIP_LOCATION;
            fileName += "-publican.zip";
        } else {
            fileName += ".zip";
        }

        // Create the output file based on the command line params and content spec
        final File outputFile = getOutputFile(outputDir, fileName);

        // Make sure the directories exist
        if (outputFile.isDirectory() && !outputFile.exists()) {
            // TODO ensure that the directory is created
            outputFile.mkdirs();
        } else if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            // TODO ensure that the directory is created
            outputFile.getParentFile().mkdirs();
        }

        // Save the build output to the output file
        saveBuildToFile(builderOutput, outputFile, buildingFromConfig);
    }

    /**
     * TODO
     *
     * @param contentSpec
     * @param cspConfig
     * @return
     */
    protected Integer getContentSpecPubsNumberFromKoji(final ContentSpec contentSpec, final ContentSpecConfiguration cspConfig) {
        JCommander.getConsole().println(Constants.FETCHING_PUBSNUMBER_MSG);

        try {
            return ClientUtilities.getPubsnumberFromKoji(contentSpec, cspConfig.getKojiHubUrl());
        } catch (MalformedURLException e) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, Constants.ERROR_INVALID_KOJIHUB_URL, false);
        } catch (KojiException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_FAILED_FETCH_PUBSNUM, false);
        }

        return null;
    }

    /**
     * Builds a ContentSpec object into a ZIP archive that can be later built by Publican.
     *
     * @param contentSpec The content spec to build from.
     * @param user        The user who requested the build.
     * @return A ZIP archive as a byte array that is ready to be saved as a file.
     */
    protected byte[] buildContentSpec(final ContentSpec contentSpec, final UserWrapper user) {
        byte[] builderOutput = null;
        try {
            builder = new ContentSpecBuilder(getProviderFactory());
            if (locale == null) {
                builderOutput = builder.buildBook(contentSpec, user, getBuildOptions());
            } else {
                builderOutput = builder.buildTranslatedBook(contentSpec, user, getBuildOptions(), getCspConfig().getZanataDetails());
            }
        } catch (Exception e) {
            printErrorAndShutdown(Constants.EXIT_INTERNAL_SERVER_ERROR, Constants.ERROR_INTERNAL_ERROR, false);
        }

        return builderOutput;
    }

    /**
     * TODO
     *
     * @param providerFactory
     * @param loggerManager
     * @param user
     * @param contentSpec
     * @return
     */
    protected boolean validateContentSpec(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final UserWrapper user, final ContentSpec contentSpec) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);
        processingOptions.setValidating(true);
        processingOptions.setIgnoreChecksum(true);
        processingOptions.setAllowNewTopics(false);
        processingOptions.setRevision(revision);
        processingOptions.setUpdateRevisions(update);
        if (revision != null) {
            processingOptions.setAddRevisions(true);
        }
        if (allowEmptyLevels) {
            processingOptions.setAllowEmptyLevels(true);
        }

        // Validate the Content Specification
        csp = new ContentSpecProcessor(providerFactory, loggerManager, processingOptions);
        boolean success = false;
        try {
            success = csp.processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EITHER, locale);
        } catch (Exception e) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        return success;
    }

    /**
     * Gets a ContentSpec from either an ID or File path.
     *
     * @param fileOrId The File or Id string to use to get the content spec.
     * @return The content spec object if it could be found, otherwise null.
     */
    protected ContentSpec getContentSpec(final String fileOrId) {
        // Get the Content Spec either from file or from the REST API
        final ContentSpec contentSpec;
        if (fileOrId.matches("^\\d+$")) {
            final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
            final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(Integer.parseInt(fileOrId), revision);
            contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);
        } else {
            // Get the content spec from the file
            final String contentSpecString = getContentSpecFromFile(fileOrId);

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            // Parse the content spec
            JCommander.getConsole().println("Starting to parse...");
            contentSpec = parseContentSpec(getProviderFactory(), contentSpecString, true);
        }

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            printErrorAndShutdown(Constants.EXIT_TOPIC_INVALID, ProcessorConstants.ERROR_INVALID_CS_MSG, false);
        }

        return contentSpec;
    }

    /**
     * Generates the output file object using the command
     * line parameters and the calculated output directory
     * and filename from the content specification.
     *
     * @param outputDir The output directory calculated from the content spec.
     * @param fileName  The file name calculated from the content spec.
     * @return The file that
     */
    protected File getOutputFile(final String outputDir, final String fileName) {
        final String output;
        // Create the fully qualified output path
        if (getOutputPath() != null && getOutputPath().endsWith("/")) {
            output = getOutputPath() + outputDir + fileName;
        } else if (getOutputPath() == null) {
            output = outputDir + fileName;
        } else {
            output = getOutputPath();
        }

        return new File(ClientUtilities.validateFilePath(output));
    }

    /**
     * Fixes the Override variables that point to File locations to change relative paths to absolute paths.
     */
    protected void fixOverrides() {
        final Map<String, String> overrides = this.getOverrides();
        for (final Entry<String, String> entry : overrides.entrySet()) {
            final String key = entry.getKey();
            if (key.equals(CSConstants.AUTHOR_GROUP_OVERRIDE) || key.equals(CSConstants.REVISION_HISTORY_OVERRIDE)) {
                overrides.put(key, ClientUtilities.validateFilePath(entry.getValue()));
            }
        }
    }

    /**
     * Saves the build output to a file location.
     *
     * @param buildZip           The File Contents to be saved.
     * @param outputFile         The location/name of the output file.
     * @param buildingFromConfig If the build was built from a csprocessor.cfg config file or by an id.
     */
    protected void saveBuildToFile(final byte[] buildZip, final File outputFile, final boolean buildingFromConfig) {
        String answer = "y";
        /*
         * Check if the file exists, if we aren't building from Project directory with a csprocessor.cfg file. If it does then check if the
         * file should be overwritten.
         */
        if (!buildingFromConfig && outputFile.exists()) {
            JCommander.getConsole().println(String.format(Constants.FILE_EXISTS_OVERWRITE_MSG, outputFile.getName()));
            answer = JCommander.getConsole().readLine();
            while (!(answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase(
                    "yes") || answer.equalsIgnoreCase("no"))) {
                JCommander.getConsole().print(String.format(Constants.FILE_EXISTS_OVERWRITE_MSG, outputFile.getName()));
                answer = JCommander.getConsole().readLine();

                // Check if the app is shutting down and if so let it.
                allowShutdownToContinueIfRequested();
            }
        }

        // Save the book to file
        try {
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                FileUtilities.saveFile(outputFile, buildZip);
                JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputFile.getAbsolutePath()));
            } else {
                shutdown(Constants.EXIT_FAILURE);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_FAILED_SAVING, false);
        }
    }

    @Override
    public void shutdown() {
        // No need to wait as the ShutdownInterceptor is waiting
        // on the whole program.
        if (csp != null) {
            csp.shutdown();
        }

        if (builder != null) {
            builder.shutdown();
        }

        super.shutdown();
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

    @Override
    public boolean validateServerUrl() {
        if (!super.validateServerUrl()) return false;

        /*
         * Check the KojiHub server url to ensure that it exists
         * if the user wants to fetch the pubsnumber from koji.
         */
        if (fetchPubsnum) {
            // Print the kojihub server url
            JCommander.getConsole().println(String.format(Constants.KOJI_WEBSERVICE_MSG, getCspConfig().getKojiHubUrl()));

            // Test that the server address is valid
            if (!ClientUtilities.validateServerExists(getCspConfig().getKojiHubUrl())) {
                // Print a line to separate content
                JCommander.getConsole().println("");

                printErrorAndShutdown(Constants.EXIT_NO_SERVER, Constants.UNABLE_TO_FIND_SERVER_MSG, false);
            }
        }

        /*
         * Check the Zanata server url and Project/Version to ensure that it
         * exists if the user wants to insert editor links for translations.
         */
        if (insertEditorLinks && locale != null) {
            setupZanataOptions();

            final ZanataDetails zanataDetails = getCspConfig().getZanataDetails();
            if (!ClientUtilities.validateServerExists(zanataDetails.returnUrl())) {
                // Print a line to separate content
                JCommander.getConsole().println("");

                printErrorAndShutdown(Constants.EXIT_NO_SERVER,
                        String.format(Constants.ERROR_INVALID_ZANATA_CONFIG_MSG, zanataDetails.getProject(), zanataDetails.getVersion(),
                                zanataDetails.getServer()), false);
            }
        }

        return true;
    }

    /**
     * Parse the content specification and display errors or shutdown the application.
     *
     * @param providerFactory   The Entity Provider Factory to create Providers to get Entities from a Datasource.
     * @param contentSpecString The Content Spec String representation to be parsed.
     * @param processProcesses  If processes should be processed to setup their relationships (makes external calls)
     * @return The parsed content spec if no errors occurred otherwise null.
     */
    protected ContentSpec parseContentSpec(final DataProviderFactory providerFactory, final String contentSpecString,
            boolean processProcesses) {
        // Parse the spec to get the title
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        ContentSpec contentSpec = null;
        try {
            contentSpec = ClientUtilities.parseContentSpecString(providerFactory, loggerManager, contentSpecString,
                    ContentSpecParser.ParsingMode.EITHER, processProcesses);
        } catch (Exception e) {
            printErrorAndShutdown(Constants.EXIT_INTERNAL_SERVER_ERROR, Constants.ERROR_INTERNAL_ERROR, false);
        }

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            JCommander.getConsole().println(ProcessorConstants.ERROR_INVALID_CS_MSG);
            shutdown(Constants.EXIT_TOPIC_INVALID);
        }

        return contentSpec;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
