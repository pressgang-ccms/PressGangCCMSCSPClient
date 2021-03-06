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

import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.provider.RESTTextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "LIST")
public class ListCommand extends BaseCommandImpl {
    @Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM}, hidden = true)
    private Boolean force = false;

    @Parameter(names = Constants.LIMIT_LONG_PARAM, metaVar = "<NUM>", descriptionKey = "LIST_LIMIT")
    private Integer limit = null;

    public ListCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.LIST_COMMAND_NAME;
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
    public void process() {
        final TextContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(TextContentSpecProvider.class);
        ((RESTTextContentSpecProvider) contentSpecProvider).setExpandProperties(true);

        // Get the content specs from the database
        final CollectionWrapper<TextContentSpecWrapper> contentSpecs = contentSpecProvider.getTextContentSpecsWithQuery("query;");
        int noSpecs = contentSpecs.size();

        // Get the number of results to display
        final Integer limit = getLimit() == null ? Constants.MAX_LIST_RESULT : (getLimit() > 0 ? getLimit() : 0);
        final Integer numResults;
        if (limit > noSpecs) {
            numResults = noSpecs;
        } else if (!isForce()) {
            numResults = limit;
        } else {
            numResults = noSpecs;
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // If there are too many content specs & force isn't set then send back an error message
        if (noSpecs > Constants.MAX_LIST_RESULT && getLimit() == null && !isForce()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_LIST_TOO_MANY_MSG", noSpecs,
                    Constants.LIMIT_LONG_PARAM), false);
        } else if (noSpecs == 0) {
            JCommander.getConsole().println(ClientUtilities.getMessage("NO_CS_FOUND_MSG"));
        } else {
            // Get the sublist of results to display
            final List<TextContentSpecWrapper> csList = contentSpecs.getItems().subList(0, numResults);

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            // Generate and then print the list of content specs
            final SpecList specList = ClientUtilities.buildSpecList(csList, getProviderFactory(), getServerEntities());
            JCommander.getConsole().println(ClientUtilities.generateContentSpecList(specList));
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        /*
         * We shouldn't use a csprocessor.cfg file for the
         * list URL as it should be specified or read from the
         * csprocessor.ini
         */
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
