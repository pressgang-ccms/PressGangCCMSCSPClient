package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.sort.EntityWrapperIDComparator;
import org.jboss.pressgang.ccms.provider.RESTTextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.rest.v1.query.RESTContentSpecQueryBuilderV1;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "SEARCH")
public class SearchCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[QUERY]")
    private List<String> queries = new ArrayList<String>();

    @Parameter(names = {Constants.CONTENT_SPEC_LONG_PARAM, Constants.CONTENT_SPEC_SHORT_PARAM})
    private Boolean useContentSpec;

    public SearchCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.SEARCH_COMMAND_NAME;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(final List<String> queries) {
        this.queries = queries;
    }

    public Boolean isUseContentSpec() {
        return useContentSpec;
    }

    public void setUseContentSpec(final Boolean useContentSpec) {
        this.useContentSpec = useContentSpec;
    }

    @Override
    public void process() {
        final TextContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(TextContentSpecProvider.class);
        ((RESTTextContentSpecProvider) contentSpecProvider).setExpandProperties(true);
        final String searchText = StringUtilities.buildString(queries.toArray(new String[queries.size()]), " ");

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Create the query
        final RESTContentSpecQueryBuilderV1 queryBuilder = new RESTContentSpecQueryBuilderV1();
        queryBuilder.setQueryLogic(CommonFilterConstants.OR_LOGIC);
        queryBuilder.setContentSpecTitle(searchText);
        queryBuilder.setContentSpecProduct(searchText);
        queryBuilder.setContentSpecVersion(searchText);
        queryBuilder.setPropertyTag(getServerEntities().getAddedByPropertyTagId(), searchText);

        // Search the database for content specs that match the query parameters
        final CollectionWrapper<TextContentSpecWrapper> contentSpecs = contentSpecProvider.getTextContentSpecsWithQuery(queryBuilder.getQuery());
        final List<TextContentSpecWrapper> csList;
        if (contentSpecs != null) {
            csList = contentSpecs.getItems();
        } else {
            csList = new ArrayList<TextContentSpecWrapper>();
        }

        Collections.sort(csList, new EntityWrapperIDComparator());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Display the search results
        if (csList.isEmpty()) {
            JCommander.getConsole().println(ClientUtilities.getMessage("NO_CS_FOUND_MSG"));
        } else {
            JCommander.getConsole().println(
                    ClientUtilities.generateContentSpecList(ClientUtilities.buildSpecList(csList, getProviderFactory(), getServerEntities())));
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        /*
         * Searching involves looking for a String so
         * there's no point in loading from the csprocessor.cfg
         */
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
