package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;

@Parameters(commandDescription = "Pull a revision of a content specification that represents a snapshot in time.")
public class PullSnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM})
    private Integer revision = null;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM},
            description = "Save the output to the specified file/directory.", metaVar = "<FILE>")
    private String outputPath;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, description = "Update all current revisions when pulling down the snapshot.",
            hidden = true)
    private Boolean update = false;

    private ContentSpecProcessor csp = null;

    public PullSnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
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
        return Constants.PULL_SNAPSHOT_COMMAND_NAME;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(final Integer revision) {
        this.revision = revision;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(final String outputPath) {
        this.outputPath = outputPath;
    }

    public Boolean getUpdate() {
        return update;
    }

    public void setUpdate(final Boolean update) {
        this.update = update;
    }

    @Override
    public void process() {
        // Authenticate the user
        final UserWrapper user = authenticate(getUsername(), getProviderFactory());

        // Load the ids and validate that only one exists
        final boolean pullForConfig = ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the topic from the rest interface
        final ContentSpecWrapper contentSpecEntity = getProviderFactory().getProvider(ContentSpecProvider.class).getContentSpec(
                getIds().get(0), revision);
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    getRevision() == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity, getProviderFactory());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process the content spec to set the snapshot revisions
        setRevisionsForContentSpec(contentSpec, user);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (pullForConfig) {
            setOutputPath(ClientUtilities.getOutputRootDirectory(getCspConfig(),
                    contentSpecEntity) + Constants.DEFAULT_SNAPSHOT_LOCATION + File.separator);
        }

        // Save or print the data
        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        final String contentSpecString = contentSpec.toString();
        final String fileName = DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-snapshot-" + dateFormatter.format(
                contentSpecEntity.getLastModified()) + "." + Constants.FILENAME_EXTENSION;
        if (getOutputPath() == null) {
            JCommander.getConsole().println(contentSpecString);
        } else {
            ClientUtilities.saveOutputFile(this, fileName, getOutputPath(), contentSpecString);
        }
    }

    /**
     * Processes a content spec object and adds the revision information for topics to the spec.
     *
     * @param contentSpec The content spec to be processed
     * @param user        The user who requested the processing.
     */
    protected void setRevisionsForContentSpec(final ContentSpec contentSpec, final UserWrapper user) {
        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(true);
        processingOptions.setValidating(true);
        processingOptions.setAllowEmptyLevels(true);
        processingOptions.setAddRevisions(true);
        processingOptions.setUpdateRevisions(getUpdate());
        processingOptions.setIgnoreChecksum(true);
        processingOptions.setRevision(getRevision());

        // Process the content spec to make sure the spec is valid,
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        setProcessor(new ContentSpecProcessor(getProviderFactory(), loggerManager, processingOptions));
        boolean success = getProcessor().processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EITHER);

        if (!success) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            JCommander.getConsole().println(Constants.ERROR_PULL_SNAPSHOT_INVALID);
            JCommander.getConsole().println("");
            shutdown(Constants.EXIT_TOPIC_INVALID);
        }
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
        return ids.size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
