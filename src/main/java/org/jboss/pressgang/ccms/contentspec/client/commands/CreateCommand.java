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
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
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

    public boolean isValid() {
        // We should have only one file
        if (getFiles().size() != 1) return false;

        // Check that the file exists
        final File file = getFiles().get(0);
        if (file.isDirectory()) return false;
        if (!file.exists()) return false;
        if (!file.isFile()) return false;

        return true;
    }

    @Override
    public void process() {
        // Authenticate the user
        final UserWrapper user = authenticate(getUsername(), getProviderFactory());

        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Check that the options set are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_FILE_MSG, true);
        }

        long startTime = System.currentTimeMillis();

        // Read in the file contents
        final String contentSpecString = FileUtilities.readFileContents(files.get(0));
        if (contentSpecString.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Parse the spec to get the title
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        ContentSpec contentSpec = null;
        contentSpec = ClientUtilities.parseContentSpecString(getProviderFactory(), loggerManager, contentSpecString,
                ContentSpecParser.ParsingMode.NEW, true);

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        // Check that the output directory doesn't already exist
        final File directory = new File(getCspConfig().getRootOutputDirectory() + DocBookUtilities.escapeTitle(contentSpec.getTitle()));
        if (directory.exists() && !getForce() && getCreateCsprocessorCfg() && directory.isDirectory()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    String.format(Constants.ERROR_CONTENT_SPEC_EXISTS_MSG, directory.getAbsolutePath()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process/Save the content spec
        boolean success = processContentSpec(contentSpec, loggerManager, user);

        // Print the logs
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(loggerManager.generateLogs());
        if (success) {
            Integer revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        } else {
            shutdown(Constants.EXIT_FAILURE);
        }

        // Print the command execution time as saving files shouldn't be included
        if (executionTime) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (success && createCsprocessorCfg) {
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
     * Process and save a content specification to the server.
     *
     * @param contentSpec   The content spec to be saved.
     * @param loggerManager The logging manager to handle logs from processing.
     * @param user          The user who requested the content spec be saved.
     * @return True if the content spec was processed and saved successfully, otherwise false.
     */
    protected boolean processContentSpec(final ContentSpec contentSpec, final ErrorLoggerManager loggerManager, final UserWrapper user) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);

        setContentSpecProcessor(new ContentSpecProcessor(getProviderFactory(), loggerManager, processingOptions));
        try {
            return getContentSpecProcessor().processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.NEW);
        } catch (Exception e) {
            printErrorAndShutdown(Constants.EXIT_INTERNAL_SERVER_ERROR, Constants.ERROR_INTERNAL_ERROR, false);
        }

        return false;
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
