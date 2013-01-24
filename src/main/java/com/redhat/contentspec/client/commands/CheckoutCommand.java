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
import com.redhat.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

@Parameters(commandDescription = "Checkout an existing Content Specification from the server")
public class CheckoutCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM},
            description = "Force the Content Specification directories to be created.")
    private Boolean force = false;

    @Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM,
            description = "The zanata server to be associated with the Content Specification.")
    private String zanataUrl = null;

    @Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM,
            description = "The zanata project name to be associated with the Content Specification.")
    private String zanataProject = null;

    @Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM,
            description = "The zanata project version to be associated with the Content Specification.")
    private String zanataVersion = null;

    public CheckoutCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.CHECKOUT_COMMAND_NAME;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    public String getZanataUrl() {
        return zanataUrl;
    }

    public void setZanataUrl(final String zanataUrl) {
        this.zanataUrl = zanataUrl;
    }

    public String getZanataProject() {
        return zanataProject;
    }

    public void setZanataProject(final String zanataProject) {
        this.zanataProject = zanataProject;
    }

    public String getZanataVersion() {
        return zanataVersion;
    }

    public void setZanataVersion(final String zanataVersion) {
        this.zanataVersion = zanataVersion;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(final Boolean force) {
        this.force = force;
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return null;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        // Check that an ID was entered
        if (ids.size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else if (ids.size() > 1) {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Get the content spec from the server
        final ContentSpecWrapper contentSpecEntity = providerFactory.getProvider(ContentSpecProvider.class).getContentSpec(ids.get(0),
                null);
        if (contentSpecEntity == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);

        // Check that the output directory doesn't already exist
        final File directory = new File(
                getCspConfig().getRootOutputDirectory() + DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()));
        if (directory.exists() && !force) {
            printError(String.format(Constants.ERROR_CONTENT_SPEC_EXISTS_MSG, directory.getAbsolutePath()), false);
            shutdown(Constants.EXIT_FAILURE);
            // If it exists and force is enabled delete the directory contents
        } else if (directory.exists()) {
            // TODO Handle failing to delete the directory contents
            FileUtilities.deleteDir(directory);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Find the zanata server if the url is a reference to the zanata server name
        for (final String serverName : getClientConfig().getZanataServers().keySet()) {
            if (serverName.equals(zanataUrl)) {
                zanataUrl = getClientConfig().getZanataServers().get(serverName).getUrl();
                break;
            }
        }

        // Create the zanata details from the command line options
        final ZanataDetails zanataDetails = new ZanataDetails();
        zanataDetails.setServer(ClientUtilities.validateHost(zanataUrl));
        zanataDetails.setProject(zanataProject);
        zanataDetails.setVersion(zanataVersion);

        // Save the csprocessor.cfg and post spec to file if the create was successful
        final String escapedTitle = DocBookUtilities.escapeTitle(contentSpecEntity.getTitle());
        final File outputSpec = new File(
                getCspConfig().getRootOutputDirectory() + escapedTitle + File.separator + escapedTitle + "-post." + Constants
                        .FILENAME_EXTENSION);
        final File outputConfig = new File(getCspConfig().getRootOutputDirectory() + escapedTitle + File.separator + "csprocessor.cfg");
        final String config = ClientUtilities.generateCsprocessorCfg(contentSpecEntity, getServerUrl(), zanataDetails);

        // Create the directory
        if (outputConfig.getParentFile() != null) {
            // TODO make sure that the directory was created
            outputConfig.getParentFile().mkdirs();
        }

        boolean error = false;

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
            FileUtilities.saveFile(outputSpec, contentSpec.toString(), Constants.FILE_ENCODING);
            JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputSpec.getAbsolutePath()));
        } catch (IOException e) {
            printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()), false);
            error = true;
        }

        if (error) {
            shutdown(Constants.EXIT_FAILURE);
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        // Never load from a cspconfig when checking out
        return false;
    }
}
