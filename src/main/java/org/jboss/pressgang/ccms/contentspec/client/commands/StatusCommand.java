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
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.code.regexp.Pattern;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "STATUS")
public class StatusCommand extends BaseCommandImpl {
    private static final Pattern ID_PATTERN = Pattern.compile("ID[ ]*=[ ]*(?<ID>[0-9]+)");

    @Parameter(metaVar = "[ID] or [FILE]")
    private List<String> ids = new ArrayList<String>();

    public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public String getCommandName() {
        return Constants.STATUS_COMMAND_NAME;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(final List<String> ids) {
        this.ids = ids;
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Initialise the basic data and perform basic checks
        ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the content specification from the server
        final String id = ids.get(0);
        final String fileName;
        ContentSpecWrapper contentSpec = null;
        String contentSpecString = null;
        if (id.matches("^\\d+$")) {
            final Integer intId = Integer.parseInt(id);
            contentSpec = ClientUtilities.getContentSpecEntity(contentSpecProvider, intId, null);
            // Get the string version of the content spec from the server
            contentSpecString = ClientUtilities.getContentSpecAsString(contentSpecProvider, intId, null);
            if (contentSpec == null || contentSpecString == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
            }

            // Create the local file
            final String escapedTitle = ClientUtilities.getEscapedContentSpecTitle(getProviderFactory(), contentSpec);
            String tempFileName = escapedTitle + "-post." + Constants.FILENAME_EXTENSION;
            File tempFile = new File(tempFileName);

            // Check that the file exists
            if (!tempFile.exists()) {
                // Backwards compatibility check for files ending with .txt
                tempFile = new File(escapedTitle + "-post.txt");
                if (!tempFile.exists()) {
                    printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_FILE_OUT_OF_DATE_MSG", tempFileName), false);
                }
            }

            fileName = tempFileName;
        } else {
            fileName = id;
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Read in the file contents
        String contentSpecData = "";
        try {
            contentSpecData = FileUtils.readFileToString(new File(fileName));
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_EMPTY_FILE_MSG"), false);
        }

        if (contentSpecData.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_EMPTY_FILE_MSG"), false);
        }

        // If the content spec is null, than look up the ID from the file
        if (contentSpec == null) {
            final Integer intId = ContentSpecUtilities.getContentSpecID(contentSpecData);
            if (intId == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_UNABLE_TO_DETERMINE_ID_FROM_FILE_MSG"), false);
            } else {
                contentSpec = ClientUtilities.getContentSpecEntity(contentSpecProvider, intId, null);
                contentSpecString = ClientUtilities.getContentSpecAsString(contentSpecProvider, intId, null);
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // At this point we should have a content spec topic, if not then shut down
        if (contentSpec == null || contentSpecString == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
        }

        // Calculate the server checksum values
        final String serverChecksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecString);

        // Read the local checksum value
        final String localStringChecksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecData);

        // Calculate the local checksum value
        final String localChecksum = HashUtilities.generateMD5(ContentSpecUtilities.removeChecksum(contentSpecData));

        // Check that the checksums match
        if (!localStringChecksum.equals(localChecksum) && !localStringChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, ClientUtilities.getMessage("ERROR_LOCAL_COPY_AND_SERVER_UPDATED_MSG",
                    fileName),
                    false);
        } else if (!localStringChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, ClientUtilities.getMessage("ERROR_OUT_OF_DATE_MSG"), false);
        } else if (!localChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, ClientUtilities.getMessage("ERROR_LOCAL_COPY_UPDATED_MSG"), false);
        } else {
            JCommander.getConsole().println(ClientUtilities.getMessage("UP_TO_DATE_MSG"));
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return true;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
