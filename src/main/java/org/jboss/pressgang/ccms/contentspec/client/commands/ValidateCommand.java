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
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(commandDescription = "Validate a Content Specification")
public class ValidateCommand extends BaseCommandImpl {
    @Parameter(converter = FileConverter.class, metaVar = "[FILE]")
    private List<File> files = new ArrayList<File>();

    @Parameter(names = Constants.STRICT_TITLES_LONG_PARAM, description = "Enforce that all titles match their matching topic titles.")
    protected Boolean strictTitles = false;

    private ContentSpecProcessor csp = null;

    public ValidateCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    protected ContentSpecProcessor getProcessor() {
        return csp;
    }

    protected void setProcessor(final ContentSpecProcessor processor) {
        csp = processor;
    }

    @Override
    public String getCommandName() {
        return Constants.VALIDATE_COMMAND_NAME;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(final List<File> files) {
        this.files = files;
    }

    public Boolean getStrictTitles() {
        return strictTitles;
    }

    public void setStrictTitles(Boolean strictTitles) {
        this.strictTitles = strictTitles;
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
        // If files is empty then we must be using a csprocessor.cfg file
        if (loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (getCspConfig() != null && getCspConfig().getContentSpecId() != null) {
                final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
                final ContentSpecWrapper contentSpec = ClientUtilities.getContentSpecEntity(contentSpecProvider,
                        getCspConfig().getContentSpecId(), null);
                final String fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
                File file = new File(fileName);
                if (!file.exists()) {
                    // Backwards compatibility check for files ending with .txt
                    file = new File(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post.txt");
                    if (!file.exists()) {
                        printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.NO_FILE_FOUND_FOR_CONFIG, fileName), false);
                    }
                }
                getFiles().add(file);
            }
        }

        // Check that the parameters are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_FILE_MSG, true);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        boolean success = false;

        // Read in the file contents and parse it to a content spec object
        final ContentSpec contentSpec = getContentSpecFromFile(getFiles().get(0));

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Validate the content spec
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        success = validateContentSpec(getProviderFactory(), loggerManager, contentSpec, getUsername());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Print the logs
        JCommander.getConsole().println(loggerManager.generateLogs());
        if (success) {
            JCommander.getConsole().println("VALID");
        } else {
            JCommander.getConsole().println("INVALID");
            JCommander.getConsole().println("");
            shutdown(Constants.EXIT_TOPIC_INVALID);
        }
    }

    /**
     * Get a content specification from a file and parse it into a ContentSpec object, so that it can be used for processing.
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
        ContentSpec contentSpec = ClientUtilities.parseContentSpecString(getProviderFactory(), loggerManager, contentSpecString);

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        return contentSpec;
    }

    /**
     * Process a content specification to see if it is valid
     *
     * @param providerFactory The provider factory to create providers to lookup entity details.
     * @param loggerManager   The manager object that handles logging.
     * @param contentSpec     The content spec to be validated.
     * @param username        The user who requested the content spec be validated.
     * @return True if the content spec is valid, otherwise false.
     */
    protected boolean validateContentSpec(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final ContentSpec contentSpec, final String username) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setValidating(true);
        processingOptions.setStrictTitles(strictTitles);

        // Process the content spec to see if it's valid
        setProcessor(new ContentSpecProcessor(providerFactory, loggerManager, processingOptions));
        return getProcessor().processContentSpec(contentSpec, username, ContentSpecParser.ParsingMode.EITHER);
    }

    @Override
    public void shutdown() {
        if (getProcessor() != null) {
            getProcessor().shutdown();
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
