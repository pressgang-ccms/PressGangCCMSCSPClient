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
import com.redhat.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;

@Parameters(commandDescription = "List the Content Specifications on the server")
public class ListCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.CONTENT_SPEC_LONG_PARAM, Constants.CONTENT_SPEC_SHORT_PARAM})
    private Boolean contentSpec = false;

    @Parameter(names = {Constants.SNAPSHOT_LONG_PARAM, Constants.SNAPSHOT_SHORT_PARAM}, hidden = true)
    private Boolean snapshot = false;

    @Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM}, hidden = true)
    private Boolean force = false;

    @Parameter(names = Constants.LIMIT_LONG_PARAM, metaVar = "<NUM>", description = "Limit the results to only show up to <NUM> results.")
    private Integer limit = null;

    public ListCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public Boolean useContentSpec() {
        return contentSpec;
    }

    public void setContentSpec(final Boolean contentSpec) {
        this.contentSpec = contentSpec;
    }

    public Boolean useSnapshot() {
        return snapshot;
    }

    public void setSnapshot(final Boolean snapshot) {
        this.snapshot = snapshot;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    public Boolean isForce() {
        return force;
    }

    public void setForce(final Boolean force) {
        this.force = force;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(final Integer limit) {
        this.limit = limit;
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.LIST_COMMAND_NAME);
    }

    @Override
    public void printHelp() {
        printHelp(Constants.LIST_COMMAND_NAME);
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return null;
    }

    public boolean isValid() {
        if (contentSpec && snapshot) {
            return false;
        }
        return true;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        if (!isValid()) {
            printError(Constants.INVALID_ARG_MSG, true);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);

        // Get the number of results to display
        Integer numResults = 0;
        if (limit != null && !force) {
            numResults = limit;
        } else if (!force) {
            numResults = Constants.MAX_LIST_RESULT;
        }

        if (snapshot) {
            // TODO implement snapshot listing
        } else {
            final CollectionWrapper<TopicWrapper> contentSpecs = topicProvider.getTopicsWithQuery(
                    "query;tag" + CSConstants.CONTENT_SPEC_TAG_ID + "=1");
            // Get the number of content specs in the database
            long noSpecs = contentSpecs.size();

            // Good point to check for a shutdown
            if (isAppShuttingDown()) {
                shutdown.set(true);
                return;
            }

            // If there are too many content specs & force isn't set then send back an error message
            if (noSpecs > Constants.MAX_LIST_RESULT && limit == null && !force) {
                printError(String.format(Constants.LIST_ERROR_MSG, noSpecs), false);
                shutdown(Constants.EXIT_FAILURE);
            } else {
                final List<TopicWrapper> csList = contentSpecs.getItems().subList(0, numResults);

                // Good point to check for a shutdown
                if (isAppShuttingDown()) {
                    shutdown.set(true);
                    return;
                }

                if (csList.isEmpty()) {
                    JCommander.getConsole().println(Constants.NO_CS_FOUND_MSG);
                } else {
                    try {
                        JCommander.getConsole().println(
                                ClientUtilities.generateContentSpecListResponse(ClientUtilities.buildSpecList(csList, providerFactory)));
                    } catch (Exception e) {
                        printError(Constants.ERROR_INTERNAL_ERROR, false);
                        shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {        /*
         * We shouldn't use a csprocessor.cfg file for the
		 * list URL as it should be specified or read from the
		 * csprocessor.ini
		 */
        return false;
    }
}
