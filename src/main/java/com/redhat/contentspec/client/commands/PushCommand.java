package com.redhat.contentspec.client.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.converter.FileConverter;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.processor.ContentSpecParser;
import com.redhat.contentspec.processor.ContentSpecProcessor;
import com.redhat.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;

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

    private ContentSpecProcessor csp = null;

    public PushCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
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

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return authenticate(getUsername(), providerFactory);
    }

    public boolean isValid() {
        // We should have only one file
        if (files.size() != 1) return false;

        // Check that the file exists
        final File file = files.get(0);
        if (file.isDirectory()) return false;
        if (!file.exists()) return false;
        if (!file.isFile()) return false;

        return true;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
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
                        printError(String.format(Constants.NO_FILE_FOUND_FOR_CONFIG, fileName), false);
                        shutdown(Constants.EXIT_FAILURE);
                    }
                }
                files.add(file);
            }
            pushingFromConfig = true;
        }

        // Check that the parameters are valid
        if (!isValid()) {
            printError(Constants.ERROR_NO_FILE_MSG, true);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown (before starting)
        allowShutdownToContinueIfRequested();

        long startTime = System.currentTimeMillis();
        boolean success = false;

        // Read in the file contents
        String contentSpecString = FileUtilities.readFileContents(files.get(0));

        if (contentSpecString == null || contentSpecString.equals("")) {
            printError(Constants.ERROR_EMPTY_FILE_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Parse the spec
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        ContentSpec contentSpec = null;
        try {
            contentSpec = ClientUtilities.parseContentSpecString(providerFactory, loggerManager, contentSpecString, user,
                    ContentSpecParser.ParsingMode.EDITED);
        } catch (Exception e) {
            printError(Constants.ERROR_INTERNAL_ERROR, false);
            shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
        }

        // Check that that content specification was parsed successfully
        if (contentSpec == null) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);
        processingOptions.setAllowEmptyLevels(true);

        csp = new ContentSpecProcessor(providerFactory, loggerManager, processingOptions);
        Integer revision = null;
        try {
            success = csp.processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EDITED);
            if (success) {
                revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
            }
        } catch (Exception e) {
            printError(Constants.ERROR_INTERNAL_ERROR, false);
            shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
        }

        // Print the logs
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(loggerManager.generateLogs());
        if (success) {
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        }
        if (executionTime) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }

        // if we failed validation then exit
        if (!success) {
            shutdown(Constants.EXIT_TOPIC_INVALID);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (success && !pushOnly) {
            // Save the post spec to file if the push was successful
            final File outputSpec;
            if (pushingFromConfig) {
                final String escapedTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
                outputSpec = new File(ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec) + escapedTitle + "-post." +
                        Constants.FILENAME_EXTENSION);
            } else {
                outputSpec = files.get(0);
            }

            // Create the directory
            if (outputSpec.getParentFile() != null) outputSpec.getParentFile().mkdirs();

            // Save the Post Processed spec
            try {
                final FileOutputStream fos = new FileOutputStream(outputSpec);
                fos.write(contentSpec.toString().getBytes("UTF-8"));
                fos.flush();
                fos.close();
            } catch (IOException e) {
                printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()), false);
                shutdown(Constants.EXIT_FAILURE);
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (csp != null) {
            csp.shutdown();
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return files.size() == 0;
    }
}
