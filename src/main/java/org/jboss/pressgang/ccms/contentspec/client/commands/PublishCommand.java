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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "PUBLISH")
public class PublishCommand extends AssembleCommand {
    @Parameter(names = Constants.NO_ASSEMBLE_LONG_PARAM, descriptionKey = "PREVIEW_NO_ASSEMBLE")
    private Boolean noAssemble = false;

    @Parameter(names = Constants.PUBLISH_MESSAGE_LONG_PARAM, descriptionKey = "PUBLISH_MESSAGE", metaVar = "<MESSAGE>")
    private String message = null;

    public PublishCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.PUBLISH_COMMAND_NAME;
    }

    public Boolean getNoAssemble() {
        return noAssemble;
    }

    public void setNoAssemble(Boolean noAssemble) {
        this.noAssemble = noAssemble;
    }

    public String getPublishMessage() {
        return message;
    }

    public void setPublishMessage(final String message) {
        this.message = message;
    }

    protected boolean isValid() {
        return !(getCspConfig().getPublishCommand() == null || getCspConfig().getPublishCommand().isEmpty());
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final boolean publishFromConfig = loadFromCSProcessorCfg();

        // Validate that the configs passed are okay.
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_NO_PUBLISH_COMMAND_MSG"), false);
        }

        if (!getNoAssemble()) {
            super.process();
        } else {
            final ContentSpec contentSpec = getContentSpec(getIds().get(0), true);
            // We need the output directory still
            findBuildDirectoryAndFiles(contentSpec, publishFromConfig);
        }

        String publishCommand = getCspConfig().getPublishCommand();

        // Replace the locale in the build options if the locale has been set
        if (getTargetLocale() != null) {
            publishCommand = publishCommand.replaceAll("--lang(s)?(=| )[A-Za-z\\-,]+", "--lang=" + getTargetLocale());
        } else if (getLocale() != null) {
            publishCommand = publishCommand.replaceAll("--lang(s)?(=| )[A-Za-z\\-,]+", "--lang=" + getLocale());
        }

        // Add the message to the script
        if (message != null) {
            publishCommand += " -m \"" + message + "\"";
        }

        try {
            JCommander.getConsole().println(ClientUtilities.getMessage("PUBLISH_BUILD_MSG"));
            Integer exitValue = ClientUtilities.runCommand(publishCommand, new File(getOutputDirectory()), JCommander.getConsole(),
                    !getHideOutput(), true);
            if (exitValue == null || exitValue != 0) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_RUNNING_PUBLISH_MSG"), false);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_RUNNING_PUBLISH_MSG"), false);
        }
        JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_PUBLISH_MSG"));
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}