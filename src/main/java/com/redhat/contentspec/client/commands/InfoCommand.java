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
import com.redhat.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;

@Parameters(commandDescription = "Get some basic information and metrics about a project.")
public class InfoCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public InfoCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public void printHelp() {
        printHelp(Constants.INFO_COMMAND_NAME);
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.INFO_COMMAND_NAME);
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return null;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);
        // Add the details for the csprocessor.cfg if no ids are specified
        if (loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (cspConfig != null && cspConfig.getContentSpecId() != null) {
                setIds(CollectionUtilities.toArrayList(cspConfig.getContentSpecId()));
            }
        }

        // Check that an id was entered
        if (ids.size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else if (ids.size() > 1) {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Get the Content Specification from the server.
        final TopicWrapper contentSpec = topicProvider.getTopic(ids.get(0), null);
        if (contentSpec == null || contentSpec.getXml() == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Print the initial CSP ID & Title message
        JCommander.getConsole().println(String.format(Constants.CSP_ID_MSG, ids.get(0)));
        JCommander.getConsole().println(String.format(Constants.CSP_REVISION_MSG, contentSpec.getRevision()));
        JCommander.getConsole().println(String.format(Constants.CSP_TITLE_MSG, contentSpec.getTitle()));
        JCommander.getConsole().println("");

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        JCommander.getConsole().println("Starting to calculate the statistics...");

        // Parse the spec to get the ids
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        final ContentSpecParser csp = new ContentSpecParser(providerFactory, loggerManager);
        try {
            csp.parse(contentSpec.getXml());
        } catch (Exception e) {
            JCommander.getConsole().println(loggerManager.generateLogs());
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Calculate the percentage complete
        final int numTopics = csp.getReferencedTopicIds().size();
        int numTopicsComplete = 0;
        final CollectionWrapper<TopicWrapper> topics = topicProvider.getTopics(csp.getReferencedTopicIds());
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
