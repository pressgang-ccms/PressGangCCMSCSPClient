package com.redhat.contentspec.client.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
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
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return authenticate(getUsername(), providerFactory);
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final boolean pullForConfig = loadFromCSProcessorCfg();

        // If files is empty then we must be using a csprocessor.cfg file
        if (pullForConfig) {
            // Check that the config details are valid
            if (getCspConfig() != null && getCspConfig().getContentSpecId() != null) {
                ids.add(getCspConfig().getContentSpecId());
            }
        }

        // Check that only one ID exists
        if (ids.size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else if (ids.size() > 1) {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        boolean success = false;

        // Get the topic from the rest interface
        final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(ids.get(0), revision);
        if (contentSpecEntity == null) {
            printError(revision == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);

        // If we want to create it as a new spec then remove the checksum and id
        if (createNew) {
            contentSpec.setId(0);
        }

        // Setup the processing options
        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setPermissiveMode(true);
        processingOptions.setValidating(false);
        processingOptions.setAllowEmptyLevels(true);
        processingOptions.setAddRevisions(true);
        processingOptions.setUpdateRevisions(update);
        processingOptions.setIgnoreChecksum(true);
        processingOptions.setRevision(revision);

        // Process the content spec to make sure the spec is valid,
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        csp = new ContentSpecProcessor(providerFactory, loggerManager, processingOptions);
        Integer revision = null;
        try {
            if (createNew) {
                success = csp.processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.NEW);
            } else {
                success = csp.processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EDITED);
            }
            if (success) {
                revision = contentSpecProvider.getContentSpec(contentSpec.getId()).getRevision();
            }
        } catch (Exception e) {
            e.printStackTrace();
            printError(Constants.ERROR_INTERNAL_ERROR, false);
            shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
        }

        if (!success) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            JCommander.getConsole().println(Constants.ERROR_PULL_SNAPSHOT_INVALID);
            JCommander.getConsole().println("");
            shutdown(Constants.EXIT_TOPIC_INVALID);
        } else {
            JCommander.getConsole().println(Constants.SUCCESSFUL_PUSH_SNAPSHOT_MSG);
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_PUSH_MSG, contentSpec.getId(), revision));
        }

    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }
}
