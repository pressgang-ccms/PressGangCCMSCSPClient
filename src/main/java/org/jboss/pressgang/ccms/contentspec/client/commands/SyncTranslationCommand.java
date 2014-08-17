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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ZanataServerConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.RESTTopicProvider;
import org.jboss.pressgang.ccms.services.zanatasync.ZanataSyncService;
import org.jboss.pressgang.ccms.zanata.ETagCache;
import org.jboss.pressgang.ccms.zanata.ETagInterceptor;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.jboss.pressgang.ccms.zanata.ZanataInterface;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.zanata.common.LocaleId;
import org.zanata.rest.client.ITranslatedDocResource;
import org.zanata.rest.service.TranslatedDocResource;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SYNC_TRANSLATION")
public class SyncTranslationCommand extends BaseCommandImpl {
    private static final List<Class<?>> ALLOWED_RESOURCES = Arrays.<Class<?>>asList(ITranslatedDocResource.class,
            TranslatedDocResource.class);

    @Parameter(metaVar = "[IDs]")
    private Set<String> ids = new HashSet<String>();

    @Parameter(names = Constants.LOCALES_LONG_PARAM, metaVar = "[LOCALES]", descriptionKey = "SYNC_TRANSLATION_LOCALES")
    private String locales = "";

    @Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM, descriptionKey = "ZANATA_SERVER", metaVar = "<URL>")
    private String zanataUrl = null;

    @Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM, descriptionKey = "ZANATA_PROJECT", metaVar = "<PROJECT>")
    private String zanataProject = null;

    @Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM, description = "ZANATA_PROJECT_VERSION", metaVar = "<VERSION>")
    private String zanataVersion = null;

    @Parameter(names = Constants.DISABLE_SSL_CERT_CHECK, descriptionKey = "DISABLE_SSL_CERT_CHECK")
    private Boolean disableSSLCert = false;

    private final ETagCache eTagCache = new ETagCache();

    public SyncTranslationCommand(JCommander parser, ContentSpecConfiguration cspConfig, ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public String getCommandName() {
        return Constants.SYNC_TRANSLATION_COMMAND_NAME;
    }

    public Set<String> getIds() {
        return ids;
    }

    public void setIds(Set<String> ids) {
        this.ids = ids;
    }

    public String getLocales() {
        return locales;
    }

    public void setLocales(String locales) {
        this.locales = locales;
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

    public Boolean getDisableSSLCert() {
        return disableSSLCert;
    }

    public void setDisableSSLCert(Boolean disableSSLCert) {
        this.disableSSLCert = disableSSLCert;
    }

    @Override
    public void process() {
        // Set topics to expand their translations by default
        getProviderFactory().getProvider(RESTTopicProvider.class).setExpandTranslations(true);

        // Load the data from the config data if no ids were specified
        ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds());

        // Check that at least one locale has been specified
        if (getLocales().trim().length() == 0) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_NO_LOCALES_MSG"), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Check that the zanata details are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_PUSH_NO_ZANATA_DETAILS_MSG"), false);
        }

        final ZanataInterface zanataInterface = initialiseZanataInterface();

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process the ids
        JCommander.getConsole().println(ClientUtilities.getMessage("DOWNLOADING_TOPICS_MSG"));
        final ZanataSyncService syncService = new ZanataSyncService(getProviderFactory(), getServerSettings(), );
        syncService.sync(ids, null, null);

        // Save the etag cache
        try {
            eTagCache.save(new File(Constants.ZANATA_CACHE_LOCATION));
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    ClientUtilities.getMessage("ERROR_FAILED_TO_SAVE_ZANATA_CACHE_MSG", Constants.ZANATA_CACHE_LOCATION), false);
        }
    }

    protected boolean isValid() {
        final ZanataDetails zanataDetails = getCspConfig().getZanataDetails();

        // Check that we even have some zanata details.
        if (zanataDetails == null) return false;

        // Check that none of the fields are invalid.
        if (zanataDetails.getServer() == null || zanataDetails.getServer().isEmpty() || zanataDetails.getProject() == null ||
                zanataDetails.getProject().isEmpty() || zanataDetails.getVersion() == null || zanataDetails.getVersion().isEmpty() ||
                zanataDetails.getToken() == null || zanataDetails.getToken().isEmpty() || zanataDetails.getUsername() == null ||
                zanataDetails.getUsername().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Sets the zanata options applied by the command line to the options that were set via configuration files.
     */
    protected void setupZanataOptions() {
        // Set the zanata url
        if (getZanataUrl() != null) {
            ZanataServerConfiguration zanataConfig = null;
            // Find the zanata server if the url is a reference to the zanata server name
            for (final String serverName : getClientConfig().getZanataServers().keySet()) {
                if (serverName.equals(getZanataUrl())) {
                    zanataConfig = getClientConfig().getZanataServers().get(serverName);
                    setZanataUrl(zanataConfig.getUrl());
                    break;
                }
            }

            getCspConfig().getZanataDetails().setServer(ClientUtilities.fixHostURL(getZanataUrl()));
            if (zanataConfig != null) {
                getCspConfig().getZanataDetails().setToken(zanataConfig.getToken());
                getCspConfig().getZanataDetails().setUsername(zanataConfig.getUsername());
            }
        }

        // Set the zanata project
        if (getZanataProject() != null) {
            getCspConfig().getZanataDetails().setProject(getZanataProject());
        }

        // Set the zanata version
        if (getZanataVersion() != null) {
            getCspConfig().getZanataDetails().setVersion(getZanataVersion());
        }
    }

    /**
     * Initialise the Zanata Interface and setup it's locales.
     *
     * @return The initialised Zanata Interface.
     */
    protected ZanataInterface initialiseZanataInterface() {
        final ZanataDetails zanataDetails = getCspConfig().getZanataDetails();

        // Configure the cache
        ZanataServerConfiguration configuration = null;
        for (final Map.Entry<String, ZanataServerConfiguration> entry : getClientConfig().getZanataServers().entrySet()) {
            if (entry.getValue().getUrl().equals(zanataDetails.getServer())) {
                configuration = entry.getValue();
                break;
            }
        }
        if (configuration != null && configuration.useCache()) {
            try {
                eTagCache.load(new File(Constants.ZANATA_CACHE_LOCATION));
            } catch (IOException e) {
                // TODO
            }
            final ETagInterceptor interceptor = new ETagInterceptor(eTagCache, ALLOWED_RESOURCES);
            ResteasyProviderFactory.getInstance().getClientExecutionInterceptorRegistry().register(interceptor);
        }

        ZanataInterface zanataInterface;
        try {
            zanataInterface = new ZanataInterface(0.2, zanataDetails, getDisableSSLCert());
        } catch (UnauthorizedException e) {
            printErrorAndShutdown(Constants.EXIT_UNAUTHORISED, ClientUtilities.getMessage("ERROR_ZANATA_UNAUTHORISED_MSG"), false);
            return null;
        }

        final List<LocaleId> localeIds = new ArrayList<LocaleId>();
        final String[] splitLocales = getLocales().split(",");

        // Check to make sure the locales are valid
        if (!ClientUtilities.validateLanguages(this, getServerSettings(), splitLocales)) {
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        for (final String locale : splitLocales) {
            // Covert the language into a LocaleId
            localeIds.add(LocaleId.fromJavaName(locale));
        }

        zanataInterface.getLocaleManager().setLocales(localeIds);

        return zanataInterface;
    }

    @Override
    public boolean validateServerUrl() {
        if (!super.validateServerUrl()) return false;

        setupZanataOptions();
        final ZanataDetails zanataDetails = getCspConfig().getZanataDetails();

        // Print the zanata server url
        JCommander.getConsole().println(ClientUtilities.getMessage("ZANATA_WEBSERVICE_MSG", zanataDetails.getServer()));

        // Test that the server address is valid
        if (!ClientUtilities.validateServerExists(zanataDetails.getServer(), getDisableSSLCert())) {
            // Print a line to separate content
            JCommander.getConsole().println("");

            printErrorAndShutdown(Constants.EXIT_NO_SERVER, ClientUtilities.getMessage("ERROR_UNABLE_TO_FIND_SERVER_MSG"), false);
        }

        return true;
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
