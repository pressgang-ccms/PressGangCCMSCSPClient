package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "PULL_SNAPSHOT")
public class PullSnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM}, metaVar = "<REVISION>")
    private Integer revision = null;

    @Parameter(names = Constants.MAX_TOPIC_REVISION_LONG_PARAM, descriptionKey = "SNAPSHOT_MAX_REV", metaVar = "<REVISION>")
    private Integer maxRevision = null;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM}, descriptionKey = "OUTPUT", metaVar = "<FILE>")
    private String outputPath;

    @Parameter(names = {Constants.UPDATE_LONG_PARAM}, descriptionKey = "SNAPSHOT_UPDATE")
    private Boolean update = false;

    public PullSnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
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
        final Integer contentSpecId = getIds().get(0);
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, contentSpecId,
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

        // Process the content spec to set the snapshot revisions
        final String snapshot = getContentSpecSnapshot(getProviderFactory(), contentSpecId);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (pullForConfig) {
            setOutputPath(ClientUtilities.getOutputRootDirectory(getProviderFactory(), getCspConfig(),
                    contentSpecEntity) + Constants.DEFAULT_SNAPSHOT_LOCATION + File.separator);
        }

        // Save or print the data
        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        final String fileName = ClientUtilities.getEscapedContentSpecTitle(getProviderFactory(),
                contentSpecEntity) + "-snapshot-" + dateFormatter.format(
                contentSpecEntity.getLastModified()) + "." + Constants.FILENAME_EXTENSION;
        if (getOutputPath() == null) {
            JCommander.getConsole().println("");
            JCommander.getConsole().println(snapshot);
        } else {
            ClientUtilities.saveOutputFile(this, fileName, getOutputPath(), snapshot);
        }
    }

    protected String getContentSpecSnapshot(final RESTProviderFactory providerFactory, final Integer contentSpecId) {
        final RESTInterfaceV1 restInterface = providerFactory.getRESTManager().getRESTClient();

        // Create the task to get the response from the server
        final FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (getRevision() == null) {
                    return restInterface.previewTEXTContentSpecFreeze(contentSpecId, getUpdate(), getMaxRevision(), false);
                } else {
                    return restInterface.previewTEXTContentSpecRevisionFreeze(contentSpecId, getRevision(), getUpdate(),
                            getMaxRevision(), false);
                }
            }
        });

        JCommander.getConsole().println(ClientUtilities.getMessage("GENERATING_SNAPSHOT_MSG"));
        return ClientUtilities.runLongRunningRequest(this, task);
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
