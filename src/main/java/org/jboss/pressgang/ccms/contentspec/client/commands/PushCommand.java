package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
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
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.LogMessageWrapper;

@Parameters(commandDescription = "Push an updated Content Specification to the server")
public class PushCommand extends BaseCommandImpl {
    @Parameter(converter = FileConverter.class, metaVar = "[FILE]")
    private List<File> files = new ArrayList<File>();

    @Parameter(names = {Constants.PERMISSIVE_LONG_PARAM, Constants.PERMISSIVE_SHORT_PARAM}, description = "Turn on permissive processing.")
    private Boolean permissive = false;

    @Parameter(names = Constants.EXEC_TIME_LONG_PARAM, description = "Show the execution time of the command.", hidden = true)
    private Boolean executionTime = false;

    @Parameter(names = Constants.PUSH_ONLY_LONG_PARAM,
            description = "Only push the Content Specification and don't save the Post Processed Content Specification.")
    private Boolean pushOnly = false;

    @Parameter(names = {Constants.MESSAGE_LONG_PARAM, Constants.MESSAGE_SHORT_PARAM}, description = "A commit message about what was " +
            "changed.")
    private String message = null;

    @Parameter(names = Constants.REVISION_MESSAGE_FLAG_LONG_PARAMETER, description = "The commit message should be set to be included in " +
            "the Revision History.")
    private Boolean revisionHistoryMessage = false;

    @Parameter(names = Constants.STRICT_LEVEL_TITLES_LONG_PARAM, description = "Enforce that the level titles match their topic titles.")
    protected Boolean strictLevelTitles = false;

    private ContentSpecProcessor csp = null;

    public PushCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    protected ContentSpecProcessor getProcessor() {
        return csp;
    }

    protected void setProcessor(ContentSpecProcessor processor) {
        csp = processor;
    }

    @Override
    public String getCommandName() {
        return Constants.PUSH_COMMAND_NAME;
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

    public Boolean getPushOnly() {
        return pushOnly;
    }

    public void setPushOnly(final boolean pushOnly) {
        this.pushOnly = pushOnly;
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

    /**
     * Checks that the input from the command line is valid arguments.
     *
     * @return True if the set arguments are valid, otherwise false.
     */
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
        boolean pushingFromConfig = false;
        // If files is empty then we must be using a csprocessor.cfg file
        if (loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (getCspConfig() != null && getCspConfig().getContentSpecId() != null) {
                final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(getCspConfig().getContentSpecId(), null);
                final String fileName = DocBookUtilities.escapeTitle(
                        contentSpecEntity.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
                File file = new File(fileName);
                if (!file.exists()) {
                    // Backwards compatibility check for files ending with .txt
                    file = new File(DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post.txt");
                    if (!file.exists()) {
                        printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.NO_FILE_FOUND_FOR_CONFIG, fileName), false);
                    }
                }
                getFiles().add(file);
            }
            pushingFromConfig = true;
        }

        // Check that the parameters are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_FILE_MSG, true);
        }

        // Good point to check for a shutdown (before starting)
        allowShutdownToContinueIfRequested();

        long startTime = System.currentTimeMillis();
        boolean success = false;

        // Load the content spec from the file and parse it into a ContentSpec object
        final ContentSpec contentSpec = getContentSpecFromFile(getFiles().get(0));

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process and save the content spec
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        success = processAndSaveContentSpec(getProviderFactory(), loggerManager, contentSpec, getUsername());

        // Print the logs
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(loggerManager.generateLogs());
        if (getExecutionTime()) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }

        // if we failed validation then exit
        if (!success) {
            shutdown(Constants.EXIT_TOPIC_INVALID);
        } else {
            final Integer revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
            JCommander.getConsole().println(String.format(ProcessorConstants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (success && !pushOnly) {
            savePostProcessedContentSpec(pushingFromConfig, contentSpec);
        }
    }

    /**
     * Get a content specification from a file and parse it into a ContentSpec object, so that ti can be used for processing.
     *
     * @param file The file to load the content spec from.
     * @return The parsed content specification object.
     */
    protected ContentSpec getContentSpecFromFile(File file) {
        // Read in the file contents
        String contentSpecString = FileUtilities.readFileContents(file);

        if (contentSpecString.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        // Parse the spec
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        JCommander.getConsole().println("Starting to parse...");
        ContentSpec contentSpec = ClientUtilities.parseContentSpecString(getProviderFactory(), loggerManager, contentSpecString,
                ContentSpecParser.ParsingMode.EDITED);

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        return contentSpec;
    }

    /**
     * Process a content specification and save it to the server.
     *
     * @param providerFactory The provider factory to create providers to lookup entity details.
     * @param loggerManager   The manager object that handles logging.
     * @param contentSpec     The content spec to be processed and saved.
     * @param user            The user who requested the content spec be processed and saved.
     * @return True if the content spec was processed and saved successfully, otherwise false.
     */
    protected boolean processAndSaveContentSpec(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final ContentSpec contentSpec, final String username) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);
        processingOptions.setStrictLevelTitles(strictLevelTitles);

        // Setup the log message
        final LogMessageWrapper logMessage = ClientUtilities.createLogDetails(getProviderFactory(), username, message, revisionHistoryMessage);

        setProcessor(new ContentSpecProcessor(providerFactory, loggerManager, processingOptions));
        return getProcessor().processContentSpec(contentSpec, username, ContentSpecParser.ParsingMode.EDITED, logMessage);
    }

    /**
     * Saves a post processed content specification to the project directory if pushing using a csprocessor.cfg,
     * otherwise save it in the current working directory.
     *
     * @param pushingFromConfig If the command is pushing from a CSP Project directory.
     * @param contentSpec       The post processed content spec object.
     */
    protected void savePostProcessedContentSpec(boolean pushingFromConfig, final ContentSpec contentSpec) {
        // Save the post spec to file if the push was successful
        final File outputSpec;
        if (pushingFromConfig) {
            final String escapedTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
            outputSpec = new File(ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec) + escapedTitle + "-post." +
                    Constants.FILENAME_EXTENSION);
        } else {
            outputSpec = getFiles().get(0);
        }

        // Create the directory
        if (outputSpec.getParentFile() != null) {
            outputSpec.getParentFile().mkdirs();
        }

        // Save the Post Processed spec
        try {
            FileUtilities.saveFile(outputSpec, contentSpec.toString(), Constants.FILE_ENCODING);
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()),
                    false);
        }
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
        return getFiles().size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
