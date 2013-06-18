package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.converter.FileConverter;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.constants.ProcessorConstants;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.LogMessageWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

@Parameters(commandDescription = "Create a new Content Specification on the server")
public class CreateCommand extends BaseCommandImpl {
    @Parameter(converter = FileConverter.class, metaVar = "[FILE]")
    private List<File> files = new ArrayList<File>();

    @Parameter(names = {Constants.PERMISSIVE_LONG_PARAM, Constants.PERMISSIVE_SHORT_PARAM}, description = "Turn on permissive processing.")
    private Boolean permissive = false;

    @Parameter(names = Constants.EXEC_TIME_LONG_PARAM, description = "Show the execution time of the command.", hidden = true)
    private Boolean executionTime = false;

    @Parameter(names = Constants.NO_CREATE_CSPROCESSOR_CFG_LONG_PARAM, description = "Don't create the csprocessor.cfg and other files.")
    private Boolean createCsprocessorCfg = true;

    @Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM},
            description = "Force the Content Specification directories to be created.")
    private Boolean force = false;

    @Parameter(names = {Constants.MESSAGE_LONG_PARAM, Constants.MESSAGE_SHORT_PARAM}, description = "A commit message about what was " +
            "changed.")
    private String message = null;

    @Parameter(names = Constants.REVISION_MESSAGE_FLAG_LONG_PARAMETER, description = "The commit message should be set to be included in " +
            "the Revision History.")
    private Boolean revisionHistoryMessage = false;

    @Parameter(names = Constants.STRICT_LEVEL_TITLES_LONG_PARAM, description = "Enforce that the level titles match their topic titles.")
    protected Boolean strictLevelTitles = false;

    private ContentSpecProcessor csp = null;

    public CreateCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.CREATE_COMMAND_NAME;
    }

    protected ContentSpecProcessor getContentSpecProcessor() {
        return csp;
    }

    protected void setContentSpecProcessor(final ContentSpecProcessor csp) {
        this.csp = csp;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(final List<File> files) {
        this.files = files;
    }

    public Boolean getPermissive() {
        return permissive;
    }

    public void setPermissive(final Boolean permissive) {
        this.permissive = permissive;
    }

    public Boolean getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final Boolean executionTime) {
        this.executionTime = executionTime;
    }

    public Boolean getCreateCsprocessorCfg() {
        return createCsprocessorCfg;
    }

    public void setCreateCsprocessorCfg(final Boolean createCsprocessorCfg) {
        this.createCsprocessorCfg = createCsprocessorCfg;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(final Boolean force) {
        this.force = force;
    }

    public Boolean getRevisionHistoryMessage() {
        return revisionHistoryMessage;
    }

    public void setRevisionHistoryMessage(Boolean revisionHistoryMessage) {
        this.revisionHistoryMessage = revisionHistoryMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getStrictLevelTitles() {
        return strictLevelTitles;
    }

    public void setStrictLevelTitles(Boolean strictLevelTitles) {
        this.strictLevelTitles = strictLevelTitles;
    }

    public boolean isValid() {
        // We should have only one file
        if (getFiles().size() != 1) return false;

        // Check that the file exists
        final File file = getFiles().get(0);
        return !(file.isDirectory() || !file.exists() || !file.isFile());
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Check that the options set are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_FILE_MSG, true);
        }

        long startTime = System.currentTimeMillis();

        // Read in the file contents and parse the file into a ContentSpec object
        final ContentSpec contentSpec = getContentSpecFromFile(getFiles().get(0));

        // Check that the output directory doesn't already exist
        final File directory = new File(getCspConfig().getRootOutputDirectory() + DocBookUtilities.escapeTitle(contentSpec.getTitle()));
        if (directory.exists() && !getForce() && getCreateCsprocessorCfg() && directory.isDirectory()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    String.format(Constants.ERROR_CONTENT_SPEC_EXISTS_MSG, directory.getAbsolutePath()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process/Save the content spec
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        boolean success = processContentSpec(contentSpec, loggerManager, getUsername());

        // Print the logs
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(loggerManager.generateLogs());
        // Print the command execution time as saving files shouldn't be included
        if (executionTime) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }
        if (!success) {
            shutdown(Constants.EXIT_FAILURE);
        } else {
            final Integer revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
            JCommander.getConsole().println(String.format(ProcessorConstants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (success && getCreateCsprocessorCfg()) {
            // Create the blank zanata details as we shouldn't have a zanata setup at creation time
            final ZanataDetails zanataDetails = new ZanataDetails();
            zanataDetails.setServer(null);
            zanataDetails.setProject(null);
            zanataDetails.setVersion(null);

            // Get the content spec entity
            final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(contentSpec.getId());

            // Create the project directory and files
            ClientUtilities.createContentSpecProject(this, getCspConfig(), directory, contentSpec.toString(), contentSpecEntity,
                    zanataDetails);
        }
    }

    /**
     * Get a content specification from a file and parse it into a ContentSpec object, so that ti can be used for processing.
     *
     * @param file The file to load the content spec from.
     * @return The parsed content specification object.
     */
    protected ContentSpec getContentSpecFromFile(final File file) {
        final String contentSpecString = FileUtilities.readFileContents(file);
        if (contentSpecString.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Parse the spec to get the title
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        JCommander.getConsole().println("Starting to parse...");
        ContentSpec contentSpec = ClientUtilities.parseContentSpecString(getProviderFactory(), loggerManager, contentSpecString,
                ContentSpecParser.ParsingMode.NEW, true);

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        return contentSpec;
    }

    /**
     * Process and save a content specification to the server.
     *
     * @param contentSpec   The content spec to be saved.
     * @param loggerManager The logging manager to handle logs from processing.
     * @param user          The user who requested the content spec be saved.
     * @return True if the content spec was processed and saved successfully, otherwise false.
     */
    protected boolean processContentSpec(final ContentSpec contentSpec, final ErrorLoggerManager loggerManager, final String username) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);
        processingOptions.setStrictLevelTitles(strictLevelTitles);

        // Setup the log message
        final LogMessageWrapper logMessage = ClientUtilities.createLogDetails(getProviderFactory(), username, getMessage(), getRevisionHistoryMessage());

        setContentSpecProcessor(new ContentSpecProcessor(getProviderFactory(), loggerManager, processingOptions));
        return getContentSpecProcessor().processContentSpec(contentSpec, username, ContentSpecParser.ParsingMode.NEW, logMessage);
    }

    @Override
    public void shutdown() {
        if (csp != null) {
            csp.shutdown();
        }
        super.shutdown();
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        // Never use the csprocessor.cfg for creating files
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
