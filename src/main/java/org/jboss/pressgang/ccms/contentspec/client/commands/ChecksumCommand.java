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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "CHECKSUM")
public class ChecksumCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public ChecksumCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.CHECKSUM_COMMAND_NAME;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public void process() {
        // Initialise the basic data and perform basic checks
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Get the content spec from the server
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final String contentSpecString = ClientUtilities.getContentSpecAsString(contentSpecProvider, ids.get(0),
                null);

        // Check that that content specification was found
        if (contentSpecString == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
        }

        // Calculate and print the checksum value
        final String checksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecString);
        JCommander.getConsole().println("CHECKSUM=" + checksum);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
