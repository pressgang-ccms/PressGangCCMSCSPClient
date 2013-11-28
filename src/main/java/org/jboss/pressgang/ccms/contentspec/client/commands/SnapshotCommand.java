package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

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
import org.jboss.pressgang.ccms.provider.LogMessageProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.exception.ProviderException;
import org.jboss.pressgang.ccms.rest.v1.entities.base.RESTLogDetailsV1;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.LogMessageWrapper;
import org.jboss.pressgang.ccms.wrapper.TextCSProcessingOptionsWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SNAPSHOT")
public class SnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM}, metaVar = "<REVISION>")
    private Integer revision = null;

    @Parameter(names = Constants.MAX_TOPIC_REVISION_LONG_PARAM, descriptionKey = "SNAPSHOT_MAX_REV", metaVar = "<REVISION>")
    private Integer maxRevision = null;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, descriptionKey = "SNAPSHOT_UPDATE")
    private Boolean update = false;

    @Parameter(names = {Constants.NEW_LONG_PARAM}, descriptionKey = "SNAPSHOT_CREATE_NEW")
    private Boolean createNew = false;

    @Parameter(names = {Constants.MESSAGE_LONG_PARAM, Constants.MESSAGE_SHORT_PARAM}, descriptionKey = "COMMIT_MESSAGE",
            metaVar = "<MESSAGE>")
    private String message = null;

    @Parameter(names = Constants.REVISION_MESSAGE_FLAG_LONG_PARAMETER, descriptionKey = "COMMIT_REV_MESSAGE", metaVar = "<MESSAGE>")
    private Boolean revisionHistoryMessage = false;

    private SnapshotProcessor processor = null;

    public SnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
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
        return Constants.SNAPSHOT_COMMAND_NAME;
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

    public Boolean getUpdate() {
        return update;
    }

    public void setUpdate(final Boolean update) {
        this.update = update;
    }

    public Boolean getCreateNew() {
        return createNew;
    }

    public void setCreateNew(Boolean createNew) {
        this.createNew = createNew;
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

    /**
     * Checks to make sure the command line options are valid.
     */
    protected void validate() {
        if (getRevision() != null && !getCreateNew()) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_SNAPSHOT_REVISION_MSG",
                    Constants.NEW_LONG_PARAM), false);
        }
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Load the ids and validate that one and only one exists
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Check the command line options are valid
        validate();

        boolean success = false;

        // Get the topic from the rest interface
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, getIds().get(0),
                getRevision());
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    ClientUtilities.getMessage(getRevision() == null ? "ERROR_NO_ID_FOUND_MSG" : "ERROR_NO_REV_ID_FOUND_MSG"), false);
        }

        // Check that the content spec isn't a failed one
        if (contentSpecEntity.getFailed() != null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_INVALID_CONTENT_SPEC_MSG"), false);
        }

        // Add a warning about the revisions not matching
        if (getRevision() != null && !getRevision().equals(contentSpecEntity.getRevision())) {
            printWarn(ClientUtilities.getMessage("WARN_REVISION_NOT_EXIST_USING_X_MSG", contentSpecEntity.getRevision()));
            // Print a space to highlight the warning
            JCommander.getConsole().println("");
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Transform the content spec
        final ContentSpec contentSpec = CSTransformer.transform(contentSpecEntity, getProviderFactory(), INCLUDE_CHECKSUMS);

        // If we want to create it as a new spec then remove the checksum and id
        if (getCreateNew()) {
            contentSpec.setId(null);
            contentSpec.setChecksum(null);
        }

        // Attempt to download all the topic data in one request
        ClientUtilities.downloadAllTopics(getProviderFactory(), contentSpec, null);

        // Setup the processing options
        final SnapshotOptions snapshotOptions = new SnapshotOptions();
        snapshotOptions.setAddRevisions(true);
        snapshotOptions.setUpdateRevisions(getUpdate());
        snapshotOptions.setRevision(getMaxRevision() == null ? getRevision() : getMaxRevision());

        // Create the snapshot
        JCommander.getConsole().println(ClientUtilities.getMessage("CREATING_SNAPSHOT_MSG"));
        setProcessor(new SnapshotProcessor(getProviderFactory()));
        getProcessor().processContentSpec(contentSpec, snapshotOptions);

        // Process and save the updated content spec.
        final TextContentSpecWrapper output = processAndSaveContentSpec(getProviderFactory(), contentSpec, getUsername());
        success = output != null && output.getFailed() == null;

        if (!success) {
            JCommander.getConsole().println(output.getErrors());
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_PUSH_MSG", output.getId(), output.getRevision()));
            JCommander.getConsole().println("");
            shutdown(Constants.EXIT_TOPIC_INVALID);
        } else {
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_PUSH_SNAPSHOT_MSG"));
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_PUSH_MSG", output.getId(), output.getRevision()));
        }
    }

    /**
     * Process a content specification and save it to the server.
     *
     * @param providerFactory  The provider factory to create providers to lookup entity details.
     * @param contentSpec      The content spec to be processed and saved.
     * @param username         The user who requested the content spec be processed and saved.
     * @return True if the content spec was processed and saved successfully, otherwise false.
     */
    protected TextContentSpecWrapper processAndSaveContentSpec(final RESTProviderFactory providerFactory, final ContentSpec contentSpec,
            final String username) {
        final TextContentSpecProvider textContentSpecProvider = providerFactory.getProvider(TextContentSpecProvider.class);
        final TextCSProcessingOptionsWrapper processingOptions = textContentSpecProvider.newTextProcessingOptions();
        processingOptions.setStrictTitles(false);

        // Create the task to update the content spec on the server
        final FutureTask<TextContentSpecWrapper> task = new FutureTask<TextContentSpecWrapper>(new Callable<TextContentSpecWrapper>() {
            @Override
            public TextContentSpecWrapper call() throws Exception {
                int flag = 0;
                if (getRevisionHistoryMessage()) {
                    flag = 0 | RESTLogDetailsV1.MAJOR_CHANGE_FLAG_BIT;
                } else {
                    flag = 0 | RESTLogDetailsV1.MINOR_CHANGE_FLAG_BIT;
                }
                final LogMessageWrapper logMessage = providerFactory.getProvider(LogMessageProvider.class).createLogMessage();
                logMessage.setFlags(flag);
                logMessage.setMessage(ClientUtilities.createLogMessage(username, getMessage()));
                logMessage.setUser(getServerEntities().getUnknownUserId().toString());

                // Create the input object to be saved
                TextContentSpecWrapper output = null;
                try {
                    final TextContentSpecWrapper input = textContentSpecProvider.newTextContentSpec();
                    if (getCreateNew()) {
                        contentSpec.setId(null);
                        contentSpec.setChecksum(null);
                        input.setText(contentSpec.toString(INCLUDE_CHECKSUMS));
                        output = textContentSpecProvider.createTextContentSpec(input, processingOptions, logMessage);
                    } else {
                        input.setText(contentSpec.toString());
                        input.setId(contentSpec.getId());
                        output = textContentSpecProvider.updateTextContentSpec(input, processingOptions, logMessage);
                    }
                } catch (ProviderException e) {
                    output = textContentSpecProvider.newTextContentSpec();
                    output.setErrors(e.getMessage());
                    output.setFailed(contentSpec.toString(INCLUDE_CHECKSUMS));
                }

                return output;
            }
        });

        return ClientUtilities.saveContentSpec(this, task);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return getIds().size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }

    @Override
    public void shutdown() {
        // No need to wait as the ShutdownInterceptor is waiting
        // on the whole program.
        if (getProcessor() != null) {
            getProcessor().shutdown();
        }

        super.shutdown();
    }
}
