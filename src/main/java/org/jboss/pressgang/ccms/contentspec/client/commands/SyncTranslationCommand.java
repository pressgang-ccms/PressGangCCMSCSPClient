package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.services.zanatasync.ZanataSyncService;
import org.jboss.pressgang.ccms.zanata.ZanataConstants;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.jboss.pressgang.ccms.zanata.ZanataInterface;
import org.zanata.common.LocaleId;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SYNC_TRANSLATION")
public class SyncTranslationCommand extends BaseCommandImpl {
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
        JCommander.getConsole().println("Downloading topics...");
        final ZanataSyncService syncService = new ZanataSyncService(getProviderFactory(), zanataInterface, getServerSettings());
        syncService.sync(ids, null, null);
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

        // At this point the zanata details are valid, so save the details.
        System.setProperty(ZanataConstants.ZANATA_SERVER_PROPERTY, zanataDetails.getServer());
        System.setProperty(ZanataConstants.ZANATA_PROJECT_PROPERTY, zanataDetails.getProject());
        System.setProperty(ZanataConstants.ZANATA_PROJECT_VERSION_PROPERTY, zanataDetails.getVersion());
        System.setProperty(ZanataConstants.ZANATA_USERNAME_PROPERTY, zanataDetails.getUsername());
        System.setProperty(ZanataConstants.ZANATA_TOKEN_PROPERTY, zanataDetails.getToken());

        return true;
    }

    /**
     * Sets the zanata options applied by the command line to the options that were set via configuration files.
     */
    protected void setupZanataOptions() {
        // Set the zanata url
        if (getZanataUrl() != null) {
            // Find the zanata server if the url is a reference to the zanata server name
            for (final String serverName : getClientConfig().getZanataServers().keySet()) {
                if (serverName.equals(getZanataUrl())) {
                    setZanataUrl(getClientConfig().getZanataServers().get(serverName).getUrl());
                    break;
                }
            }

            getCspConfig().getZanataDetails().setServer(ClientUtilities.fixHostURL(getZanataUrl()));
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
        final ZanataInterface zanataInterface = new ZanataInterface(0.2, getDisableSSLCert());

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
        if (!ClientUtilities.validateServerExists(zanataDetails.getServer())) {
            // Print a line to separate content
            JCommander.getConsole().println("");

            printErrorAndShutdown(Constants.EXIT_NO_SERVER, ClientUtilities.getMessage("UNABLE_TO_FIND_SERVER_MSG"), false);
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
