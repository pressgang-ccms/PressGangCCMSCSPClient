package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.converter.FileConverter;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.processor.ContentSpecParser;
import com.redhat.contentspec.processor.ContentSpecProcessor;
import com.redhat.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.rest.v1.entities.base.RESTLogDetailsV1;
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
    private Boolean noCsprocessorCfg = false;

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
        return noCsprocessorCfg;
    }

    public void setCreateCsprocessorCfg(final Boolean createCsprocessorCfg) {
        this.noCsprocessorCfg = createCsprocessorCfg;
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
        if (files.size() != 1) return false;

        // Check that the file exists
        final File file = files.get(0);
        if (file.isDirectory()) return false;
        if (!file.exists()) return false;
        if (!file.isFile()) return false;

        return true;
    }

    @Override
    public void process(final RESTManager restManager, final ErrorLoggerManager elm) {
        if (!isValid()) {
            printError(Constants.ERROR_NO_FILE_MSG, true);
            shutdown(Constants.EXIT_FAILURE);
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;

        // Read in the file contents
        final String contentSpec = FileUtilities.readFileContents(files.get(0));

        if (contentSpec == null || contentSpec.equals("")) {
            printError(Constants.ERROR_EMPTY_FILE_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Parse the spec to get the title
        final ContentSpecParser parser = new ContentSpecParser(elm, restManager);
        try {
            parser.parse(contentSpec);
        } catch (Exception e) {
            printError(Constants.ERROR_INTERNAL_ERROR, false);
            shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
        }

        // Check that the output directory doesn't already exist
        final File directory = new File(
                cspConfig.getRootOutputDirectory() + DocBookUtilities.escapeTitle(parser.getContentSpec().getTitle()));
        if (directory.exists() && !force && !noCsprocessorCfg && directory.isDirectory()) {
            printError(String.format(Constants.ERROR_CONTENT_SPEC_EXISTS_MSG, directory.getAbsolutePath()), false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Create the log details
        RESTLogDetailsV1 logDetails = ClientUtilities.createLogDetails(message, revisionHistoryMessage);

        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(permissive);
        processingOptions.setStrictLevelTitles(strictLevelTitles);

        csp = new ContentSpecProcessor(restManager, elm, processingOptions);
        Integer revision = null;
        try {
            success = csp.processContentSpec(contentSpec, getUsername(), logDetails, ContentSpecParser.ParsingMode.NEW);
            if (success) {
                revision = restManager.getReader().getLatestCSRevById(csp.getContentSpec().getId());
            }
        } catch (Exception e) {
            printError(Constants.ERROR_INTERNAL_ERROR, false);
            shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
        }

        // Print the logs
        long elapsedTime = System.currentTimeMillis() - startTime;
        JCommander.getConsole().println(elm.generateLogs());
        if (success) {
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_PUSH_MSG, csp.getContentSpec().getId(), revision));
        }
        if (executionTime) {
            JCommander.getConsole().println(String.format(Constants.EXEC_TIME_MSG, elapsedTime));
        }

        // Good point to check for a shutdown
        // It doesn't matter if the directory and files aren't created just so long as the spec finished saving
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        if (success && !noCsprocessorCfg) {
            // If the output directory exists and force is enabled delete the directory contents
            if (directory.exists() && directory.isDirectory()) {
                FileUtilities.deleteDir(directory);
            }

            // Create the blank zanata details as we shouldn't have a zanata setup at creation time
            final ZanataDetails zanataDetails = new ZanataDetails();
            zanataDetails.setServer(null);
            zanataDetails.setProject(null);
            zanataDetails.setVersion(null);

            boolean error = false;

            // Save the csprocessor.cfg and post spec to file if the create was successful
            final String escapedTitle = DocBookUtilities.escapeTitle(csp.getContentSpec().getTitle());
            final RESTTopicV1 contentSpecTopic = restManager.getReader().getContentSpecById(csp.getContentSpec().getId(), null);
            final File outputSpec = new File(
                    cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + escapedTitle + "-post." + Constants
                            .FILENAME_EXTENSION);
            final File outputConfig = new File(cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + "csprocessor.cfg");
            final String config = ClientUtilities.generateCsprocessorCfg(contentSpecTopic, cspConfig.getServerUrl(), clientConfig,
                    zanataDetails);

            // Create the directory
            if (outputConfig.getParentFile() != null) outputConfig.getParentFile().mkdirs();

            // Save the csprocessor.cfg
            try {
                final FileOutputStream fos = new FileOutputStream(outputConfig);
                fos.write(config.getBytes("UTF-8"));
                fos.flush();
                fos.close();
                JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputConfig.getAbsolutePath()));
            } catch (IOException e) {
                printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputConfig.getAbsolutePath()), false);
                error = true;
            }

            // Save the Post Processed spec
            try {
                final FileOutputStream fos = new FileOutputStream(outputSpec);
                fos.write(contentSpecTopic.getXml().getBytes("UTF-8"));
                fos.flush();
                fos.close();
                JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputSpec.getAbsolutePath()));
            } catch (IOException e) {
                printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()), false);
                error = false;
            }

            if (error) {
                shutdown(Constants.EXIT_FAILURE);
            }
        }
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.CREATE_COMMAND_NAME);
    }

    @Override
    public void printHelp() {
        printHelp(Constants.CREATE_COMMAND_NAME);
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
        /* Never use the csprocessor.cfg for creating files */
        return false;
    }
}
