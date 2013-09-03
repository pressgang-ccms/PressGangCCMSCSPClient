package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.exception.NotFoundException;
import org.jboss.pressgang.ccms.services.zanatasync.SyncMaster;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedTopicWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataConstants;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.jboss.pressgang.ccms.zanata.ZanataInterface;
import org.zanata.common.LocaleId;

@Parameters(commandDescription = "Sync the translations for a Content Specification with Zanata")
public class SyncTranslationCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[IDs]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = Constants.LOCALES_LONG_PARAM, metaVar = "[LOCALES]",
            description = "The locales to sync for the specified IDs.")
    private String locales = "";

    @Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM,
            description = "The zanata server to be associated with the Content Specification.")
    private String zanataUrl = null;

    @Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM,
            description = "The zanata project name to be associated with the Content Specification.")
    private String zanataProject = null;

    @Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM,
            description = "The zanata project version to be associated with the Content Specification.")
    private String zanataVersion = null;

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

    @Override
    public void process() {
        // Load the data from the config data if no ids were specified
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Check that at least one locale has been specified
        if (getLocales().trim().length() == 0) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_NO_LOCALES_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Check that the zanata details are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, Constants.ERROR_PUSH_NO_ZANATA_DETAILS_MSG, false);
        }

        final ZanataInterface zanataInterface = initialiseZanataInterface();
        final SyncMaster syncMaster = new SyncMaster(getProviderFactory(), zanataInterface);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Process the ids
        final Set<String> zanataIds = getZanataIds(getProviderFactory(), ids);
        JCommander.getConsole().println("Syncing the topics...");
        syncMaster.processZanataResources(zanataIds);
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
        final ZanataInterface zanataInterface = new ZanataInterface(0.2);

        final List<LocaleId> localeIds = new ArrayList<LocaleId>();
        final String[] splitLocales = getLocales().split(",");

        // Check to make sure the locales are valid
        if (!ClientUtilities.validateLanguages(this, getProviderFactory(), splitLocales)) {
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        for (final String locale : splitLocales) {
            // Covert the language into a LocaleId
            localeIds.add(LocaleId.fromJavaName(locale));
        }

        zanataInterface.getLocaleManager().setLocales(localeIds);

        return zanataInterface;
    }

    /**
     * Get the Zanata IDs to be synced from a list of content specifications.
     *
     * @param providerFactory
     * @param contentSpecIds  The list of Content Spec IDs to sync.
     * @return A Set of Zanata IDs that represent the topics to be synced from the list of Content Specs.
     */
    protected Set<String> getZanataIds(final DataProviderFactory providerFactory, final List<Integer> contentSpecIds) {
        JCommander.getConsole().println("Downloading topics...");
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);

        final List<TopicWrapper> topics = new ArrayList<TopicWrapper>();
        final List<TranslatedContentSpecWrapper> translatedContentSpecs = new ArrayList<TranslatedContentSpecWrapper>();
        for (final Integer contentSpecId : contentSpecIds) {
            // Get the latest pushed content spec
            ContentSpecWrapper contentSpecEntity = null;
            try {
                final TranslatedContentSpecWrapper translatedContentSpec = EntityUtilities.getClosestTranslatedContentSpecById
                        (getProviderFactory(), contentSpecId, null);
                if (translatedContentSpec != null) {
                    contentSpecEntity = translatedContentSpec.getContentSpec();
                    translatedContentSpecs.add(translatedContentSpec);
                } else {
                    // If we don't have a translation then print an error
                    printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_NO_TRANSLATION_ID_FOUND_MSG, false);
                }
            } catch (NotFoundException e) {
                // Do nothing as this is handled below
            }

            if (contentSpecEntity == null) {
                printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_NO_ID_FOUND_MSG, false);
            }

            // Iterate over the spec topics and get their topics
            final ContentSpec contentSpec = CSTransformer.transform(contentSpecEntity, providerFactory);
            final List<SpecTopic> specTopics = contentSpec.getSpecTopics();
            if (specTopics != null && !specTopics.isEmpty()) {
                for (final SpecTopic specTopic : specTopics) {
                    final TopicWrapper topic = topicProvider.getTopic(specTopic.getDBId(), specTopic.getRevision());
                    topics.add(topic);
                }
            }
        }

        return getZanataIds(translatedContentSpecs, topics);
    }

    /**
     * Get the Zanata IDs that represent a Collection of Topics.
     *
     * @param topics The topics to get the Zanata IDs for.
     * @return The Set of Zanata IDs that represent the topics.
     */
    protected Set<String> getZanataIds(final List<TranslatedContentSpecWrapper> translatedContentSpecs, final List<TopicWrapper> topics) {
        final Set<String> zanataIds = new HashSet<String>();

        // Get the zanata ids for each content spec
        for (final TranslatedContentSpecWrapper translatedContentSpec : translatedContentSpecs) {
            zanataIds.add(translatedContentSpec.getZanataId());
        }

        // Get the zanata ids for each topics
        for (final TopicWrapper topic : topics) {
            // Find the latest pushed translated topic
            final TranslatedTopicWrapper translatedTopic = EntityUtilities.returnPushedTranslatedTopic(topic);
            if (translatedTopic != null) {
                zanataIds.add(translatedTopic.getZanataId());
            }
        }

        return zanataIds;
    }

    @Override
    public boolean validateServerUrl() {
        if (!super.validateServerUrl()) return false;

        setupZanataOptions();
        final ZanataDetails zanataDetails = getCspConfig().getZanataDetails();

        // Print the zanata server url
        JCommander.getConsole().println(String.format(Constants.ZANATA_WEBSERVICE_MSG, zanataDetails.getServer()));

        // Test that the server address is valid
        if (!ClientUtilities.validateServerExists(zanataDetails.getServer())) {
            // Print a line to separate content
            JCommander.getConsole().println("");

            printErrorAndShutdown(Constants.EXIT_NO_SERVER, Constants.UNABLE_TO_FIND_SERVER_MSG, false);
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
