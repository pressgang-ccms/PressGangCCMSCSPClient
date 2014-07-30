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
import org.jboss.pressgang.ccms.rest.v1.entities.base.RESTLogDetailsV1;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.resteasy.client.ClientResponseFailure;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SNAPSHOT")
public class SnapshotCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

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

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Load the ids and validate that one and only one exists
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the topic from the rest interface
        final Integer contentSpecId = getIds().get(0);
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, contentSpecId, null);
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
        }

        // Check that the content spec isn't a failed one
        if (contentSpecEntity.getFailed() != null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_INVALID_CONTENT_SPEC_MSG"), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Create the snapshot
        JCommander.getConsole().println(ClientUtilities.getMessage("CREATING_SNAPSHOT_MSG"));

        // Process and save the updated content spec.
        final RESTTextContentSpecV1 output = processAndSaveContentSpec(getProviderFactory(), contentSpecId, getUsername());
        final boolean success = output != null && output.getFailedContentSpec() == null;

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
     * @param contentSpecId    The content spec id to be processed and saved.
     * @param username         The user who requested the content spec be processed and saved.
     * @return True if the content spec was processed and saved successfully, otherwise false.
     */
    protected RESTTextContentSpecV1 processAndSaveContentSpec(final RESTProviderFactory providerFactory, final Integer contentSpecId,
            final String username) {
        final RESTInterfaceV1 restInterface = providerFactory.getRESTManager().getRESTClient();

        // Create the task to update the content spec on the server
        final FutureTask<RESTTextContentSpecV1> task = new FutureTask<RESTTextContentSpecV1>(new Callable<RESTTextContentSpecV1>() {
            @Override
            public RESTTextContentSpecV1 call() throws Exception {
                int flag = 0;
                if (getRevisionHistoryMessage()) {
                    flag = 0 | RESTLogDetailsV1.MAJOR_CHANGE_FLAG_BIT;
                } else {
                    flag = 0 | RESTLogDetailsV1.MINOR_CHANGE_FLAG_BIT;
                }
                final String logMessage = ClientUtilities.createLogMessage(username, getMessage());
                final String userId = getServerEntities().getUnknownUserId().toString();

                // Create the input object to be saved
                RESTTextContentSpecV1 output = null;
                try {
                    output = restInterface.freezeJSONTextContentSpec(contentSpecId, "", getUpdate(), getMaxRevision(), getCreateNew(),
                            logMessage, flag, userId);
                } catch (ClientResponseFailure e) {
                    output = new RESTTextContentSpecV1();
                    output.setErrors(e.getMessage());
                    output.setFailedContentSpec("");
                }

                return output;
            }
        });

        return ClientUtilities.runLongRunningRequest(this, task);
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
