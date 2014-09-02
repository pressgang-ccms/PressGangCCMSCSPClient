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

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.rest.v1.elements.RESTProcessInformationV1;
import org.jboss.pressgang.ccms.rest.v1.elements.enums.RESTProcessStatusV1;
import org.jboss.pressgang.ccms.rest.v1.expansion.ExpandDataDetails;
import org.jboss.pressgang.ccms.rest.v1.expansion.ExpandDataTrunk;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.utils.common.ExceptionUtilities;
import org.jboss.pressgang.ccms.wrapper.CSTranslationDetailWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SYNC_TRANSLATION")
public class SyncTranslationCommand extends BaseCommandImpl {

    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = Constants.LOCALES_LONG_PARAM, metaVar = "[LOCALES]", descriptionKey = "SYNC_TRANSLATION_LOCALES")
    private String locales = "";

    @Parameter(names = Constants.NO_WAIT_LONG_PARAM, descriptionKey = "PUSH_NO_WAIT")
    private Boolean noWait = false;

    public SyncTranslationCommand(JCommander parser, ContentSpecConfiguration cspConfig, ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public String getCommandName() {
        return Constants.SYNC_TRANSLATION_COMMAND_NAME;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public String getLocales() {
        return locales;
    }

    public void setLocales(String locales) {
        this.locales = locales;
    }

    public Boolean getNoWait() {
        return noWait;
    }

    public void setNoWait(Boolean noWait) {
        this.noWait = noWait;
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Load the ids and validate that one and only one exists
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Check that at least one locale has been specified
        if (getLocales().trim().length() == 0) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_NO_LOCALES_MSG"), false);
        }

        // Check to make sure the locales are valid
        final String[] splitLocales = getLocales().split(",");
        if (!ClientUtilities.validateLanguages(this, getServerSettings(), splitLocales)) {
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Load the spec and make sure it exists
        final ContentSpecWrapper contentSpecEntity = ClientUtilities.getContentSpecEntity(contentSpecProvider, ids.get(0), null);
        if (contentSpecEntity == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the translation details for the content spec
        final CSTranslationDetailWrapper translationDetails = contentSpecEntity.getTranslationDetails();
        if (translationDetails == null || translationDetails.getTranslationServer() == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_TRANSLATION_DETAILS_NOT_CONFIGURED_MSG"), false);
        }

        // Get the Zanata Details and check that they are valid
        final ZanataDetails zanataDetails = ClientUtilities.generateZanataDetails(translationDetails, getClientConfig());
        if (!isValid(zanataDetails)) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_NO_ZANATA_SERVER_SETUP_MSG",
                    zanataDetails.getServer()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process the ids
        syncTranslations(getProviderFactory(), contentSpecEntity, zanataDetails.getUsername(), zanataDetails.getToken());
    }

    protected boolean isValid(final ZanataDetails zanataDetails) {
        // Check that we even have some zanata details.
        if (zanataDetails == null) return false;

        // Check that we have a username and token
        if (zanataDetails.getToken() == null || zanataDetails.getToken().isEmpty() || zanataDetails.getUsername() == null ||
                zanataDetails.getUsername().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     *
     *
     * @param providerFactory
     * @param contentSpecEntity
     */
    protected void syncTranslations(final RESTProviderFactory providerFactory, final ContentSpecWrapper contentSpecEntity,
            final String username, final String apikey) {
        final RESTInterfaceV1 restClient = providerFactory.getRESTManager().getRESTClient();

        // Start the process on the server
        RESTProcessInformationV1 processInformation = restClient.syncContentSpecTranslations(contentSpecEntity.getId(), "", "",
                getLocales(), username, apikey);
        JCommander.getConsole().println(ClientUtilities.getMessage("STARTING_TO_SYNC_TRANSLATIONS_MSG"));
        JCommander.getConsole().println(ClientUtilities.getMessage("PROCESS_UUID_MSG", processInformation.getId()));

        // If the user doesn't want to wait just return
        if (noWait) {
            return;
        }

        // Get the expansion for the logs
        String expand = "";
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ExpandDataTrunk expandDataTrunk = new ExpandDataTrunk();
            expandDataTrunk.setBranches(Arrays.asList(new ExpandDataTrunk(new ExpandDataDetails(RESTProcessInformationV1.LOGS_NAME))));
            expand = mapper.writeValueAsString(expandDataTrunk);
        } catch (IOException e) {
            JCommander.getConsole().println(ExceptionUtilities.getStackTrace(e));
            shutdown(Constants.EXIT_FAILURE);
        }

        // Wait until the process has finished executing on the server
        while (!(processInformation.getStatus() == RESTProcessStatusV1.COMPLETED || processInformation.getStatus() == RESTProcessStatusV1
                .FAILED || processInformation.getStatus() == RESTProcessStatusV1.CANCELLED)) {
            JCommander.getConsole().println(ClientUtilities.getMessage("WAITING_FOR_TRANSLATION_SYNC_TO_COMPLETE"));
            try {
                // Wait for 10 secs before checking the status again
                Thread.sleep(Constants.ASYNC_STATUS_INTERVAL);
            } catch (Exception e) {
                JCommander.getConsole().println(ExceptionUtilities.getStackTrace(e));
                shutdown(Constants.EXIT_FAILURE);
            }

            // Get the latest process information
            processInformation = restClient.getJSONProcess(processInformation.getId(), expand);
        }

        if (!isNullOrEmpty(processInformation.getLogs())) {
            // Print the log information
            JCommander.getConsole().println("");
            JCommander.getConsole().println(ClientUtilities.cleanAsyncProcessLogs(processInformation.getLogs()));
        }

        // Print the success/failue messages
        if (processInformation.getStatus() == RESTProcessStatusV1.FAILED) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_ZANATA_SYNC_FAILED_MSG"), false);
            shutdown(Constants.EXIT_FAILURE);
        } else if (processInformation.getStatus() == RESTProcessStatusV1.CANCELLED) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_ZANATA_SYNC_CANCELLED_MSG"), false);
            shutdown(Constants.EXIT_FAILURE);
        } else {
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_ZANATA_SYNC_MSG"));
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
