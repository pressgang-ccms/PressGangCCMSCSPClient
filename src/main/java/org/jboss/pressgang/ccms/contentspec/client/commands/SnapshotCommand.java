package org.jboss.pressgang.ccms.contentspec.client.commands;

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
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;

@Parameters(
        commandDescription = "Pull a revision of a content specification that represents a snapshot in time and push it back to the " +
                "server.")
public class SnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM})
    private Integer revision = null;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, description = "Update all current revisions when pulling down the snapshot.",
            hidden = true)
    private Boolean update = false;

    @Parameter(names = {Constants.NEW_LONG_PARAM}, description = "Create the snapshot as a new content specification")
    private Boolean createNew = false;

    private ContentSpecProcessor csp = null;

    public SnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
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

    @Override
    public void process() {
        // Authenticate the user
        final UserWrapper user = authenticate(getUsername(), getProviderFactory());

        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Load the ids and validate that one and only one exists
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        boolean success = false;

        // Get the topic from the rest interface
        final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(getIds().get(0), getRevision());
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    getRevision() == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Transform the content spec
        final ContentSpec contentSpec = CSTransformer.transform(contentSpecEntity, getProviderFactory());

        // If we want to create it as a new spec then remove the checksum and id
        if (getCreateNew()) {
            contentSpec.setId(null);
            contentSpec.setChecksum(null);
        }

        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(true);
        processingOptions.setValidating(false);
        processingOptions.setAllowEmptyLevels(true);
        processingOptions.setAddRevisions(true);
        processingOptions.setUpdateRevisions(getUpdate());
        processingOptions.setIgnoreChecksum(true);
        processingOptions.setRevision(getRevision());

        // Process the content spec to make sure the spec is valid,
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        setProcessor(new ContentSpecProcessor(getProviderFactory(), loggerManager, processingOptions));
        Integer revision = null;
        if (getCreateNew()) {
            success = getProcessor().processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.NEW);
        } else {
            success = getProcessor().processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EDITED);
        }
        if (success) {
            revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
        }

        if (!success) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            JCommander.getConsole().println(Constants.ERROR_CREATE_SNAPSHOT_INVALID);
            JCommander.getConsole().println("");
            shutdown(Constants.EXIT_TOPIC_INVALID);
        } else {
            JCommander.getConsole().println(Constants.SUCCESSFUL_PUSH_SNAPSHOT_MSG);
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        }

    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return getIds().size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
