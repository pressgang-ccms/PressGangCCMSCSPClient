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
import org.jboss.pressgang.ccms.contentspec.processor.SnapshotProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.structures.SnapshotOptions;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(commandDescription = "Pull a revision of a content specification that represents a snapshot in time.")
public class PullSnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM})
    private Integer revision = null;

    @Parameter(names = Constants.MAX_TOPIC_REVISION_LONG_PARAM, description = "The maximum revision to update all topics to")
    private Integer maxRevision = null;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM},
            description = "Save the output to the specified file/directory.", metaVar = "<FILE>")
    private String outputPath;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, description = "Update all current revisions when pulling down the snapshot.")
    private Boolean update = false;

    private SnapshotProcessor processor = null;

    public PullSnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    protected SnapshotProcessor getProcessor() {
        return processor;
    }

    protected void setProcessor(final SnapshotProcessor processor) {
        this.processor = processor;
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

    public Integer getMaxRevision() {
        return maxRevision;
    }

    public void setMaxRevision(Integer maxRevision) {
        this.maxRevision = maxRevision;
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
        // Load the ids and validate that only one exists
        final boolean pullForConfig = ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the topic from the rest interface
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, getIds().get(0),
                getRevision());
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    getRevision() == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
        }

        // Check that the content spec isn't a failed one
        if (contentSpecEntity.getFailed() != null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_INVALID_CONTENT_SPEC, false);
        }

        // Add a warning about the revisions not matching
        if (getRevision() != null && !getRevision().equals(contentSpecEntity.getRevision())) {
            printWarn(String.format(Constants.WARN_REVISION_NOT_EXIST_USING_X_MSG, contentSpecEntity.getRevision()));
            // Print a space to highlight the warning
            JCommander.getConsole().println("");
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Transform the content spec
        final ContentSpec contentSpec = CSTransformer.transform(contentSpecEntity, getProviderFactory(), INCLUDE_CHECKSUMS);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process the content spec to set the snapshot revisions
        setRevisionsForContentSpec(contentSpec);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (pullForConfig) {
            setOutputPath(ClientUtilities.getOutputRootDirectory(getProviderFactory(), getCspConfig(),
                    contentSpecEntity) + Constants.DEFAULT_SNAPSHOT_LOCATION + File.separator);
        }

        // Save or print the data
        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        final String contentSpecString = contentSpec.toString(INCLUDE_CHECKSUMS);
        final String fileName = ClientUtilities.getEscapedContentSpecTitle(getProviderFactory(),
                contentSpecEntity) + "-snapshot-" + dateFormatter.format(
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
     */
    protected void setRevisionsForContentSpec(final ContentSpec contentSpec) {
        // Setup the processing options
        final SnapshotOptions snapshotOptions = new SnapshotOptions();
        snapshotOptions.setAddRevisions(true);
        snapshotOptions.setUpdateRevisions(getUpdate());
        snapshotOptions.setRevision(getMaxRevision() == null ? getRevision() : getMaxRevision());

        // Process the content spec to make sure the spec is valid,
        JCommander.getConsole().println("Creating the snapshot...");
        setProcessor(new SnapshotProcessor(getProviderFactory()));
        getProcessor().processContentSpec(contentSpec, snapshotOptions);
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
