/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "CHECKOUT")
public class CheckoutCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM}, descriptionKey = "CHECKOUT_FORCE")
    private Boolean force = false;

    @Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM, descriptionKey = "ZANATA_SERVER", metaVar = "<URL>")
    private String zanataUrl = null;

    @Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM, descriptionKey = "ZANATA_PROJECT", metaVar = "<PROJECT>")
    private String zanataProject = null;

    @Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM, description = "ZANATA_PROJECT_VERSION", metaVar = "<VERSION>")
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
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Check that an ID was entered
        ClientUtilities.validateIdsOrFiles(this, getIds(), false);

        // Get the content spec from the server
        final String contentSpecString = ClientUtilities.getContentSpecAsString(contentSpecProvider, ids.get(0), null);
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, ids.get(0), null);
        if (contentSpecString == null || contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
        }

        // Check that the output directory doesn't already exist
        final String escapedTitle = ClientUtilities.getEscapedContentSpecTitle(getProviderFactory(), contentSpecEntity);
        final File directory = new File(getCspConfig().getRootOutputDirectory() + escapedTitle);
        if (directory.exists() && !force) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_CONTENT_SPEC_EXISTS_MSG",
                    directory.getAbsolutePath(), Constants.FORCE_LONG_PARAM), false);
            // If it exists and force is enabled delete the directory contents
        } else if (directory.exists()) {
            // TODO Handle failing to delete the directory contents
            FileUtilities.deleteDir(directory);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Find the zanata server if the url is a reference to the zanata server name
        if (zanataUrl != null && getClientConfig().getZanataServers() != null) {
            for (final String serverName : getClientConfig().getZanataServers().keySet()) {
                if (serverName.equals(zanataUrl)) {
                    zanataUrl = getClientConfig().getZanataServers().get(serverName).getUrl();
                    break;
                }
            }
        }

        // Create the zanata details from the command line options
        final ZanataDetails zanataDetails = new ZanataDetails();
        zanataDetails.setServer(ClientUtilities.fixHostURL(zanataUrl));
        zanataDetails.setProject(zanataProject);
        zanataDetails.setVersion(zanataVersion);

        // Create the content spec project directory and files.
        ClientUtilities.createContentSpecProject(this, getCspConfig(), directory, contentSpecString, contentSpecEntity, zanataDetails);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        // Never load from a cspconfig when checking out
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
