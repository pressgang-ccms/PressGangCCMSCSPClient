package com.redhat.contentspec.client.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImplWithIds;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;

@Parameters(commandDescription = "Get some basic information and metrics about a project.")
public class InfoCommand extends BaseCommandImplWithIds {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public InfoCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.INFO_COMMAND_NAME;
    }

    @Override
    public List<Integer> getIds() {
        return ids;
    }

    @Override
    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return null;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);

        // Initialise the basic data and perform basic checks
        prepare();

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the Content Specification from the server.
        final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(ids.get(0), null);
        if (contentSpecEntity == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Print the initial CSP ID & Title message
        JCommander.getConsole().println(String.format(Constants.CSP_ID_MSG, ids.get(0)));
        JCommander.getConsole().println(String.format(Constants.CSP_REVISION_MSG, contentSpecEntity.getRevision()));
        JCommander.getConsole().println(String.format(Constants.CSP_TITLE_MSG, contentSpecEntity.getTitle()));
        JCommander.getConsole().println("");

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        JCommander.getConsole().println("Starting to calculate the statistics...");

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Create the list of referenced topics
        final List<Integer> referencedTopicIds = new ArrayList<Integer>();
        final List<SpecTopic> specTopics = contentSpec.getSpecTopics();
        for (final SpecTopic specTopic : specTopics) {
            if (specTopic.getDBId() > 0) {
                referencedTopicIds.add(specTopic.getDBId());
            }
        }

        // Calculate the percentage complete
        final int numTopics = referencedTopicIds.size();
        int numTopicsComplete = 0;
        final CollectionWrapper<TopicWrapper> topics = topicProvider.getTopics(referencedTopicIds);
        if (topics != null && topics.getItems() != null) {
            final List<TopicWrapper> topicItems = topics.getItems();
            for (final TopicWrapper topic : topicItems) {
                if (topic.getXml() != null && !topic.getXml().isEmpty()) {
                    numTopicsComplete++;
                }

                // Good point to check for a shutdown
                if (isAppShuttingDown()) {
                    shutdown.set(true);
                    return;
                }
            }
        }

        // Print the completion status
        JCommander.getConsole().println(String.format(Constants.CSP_COMPLETION_MSG, numTopics, numTopicsComplete,
                ((float) numTopicsComplete / (float) numTopics * 100.0f)));
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

}
