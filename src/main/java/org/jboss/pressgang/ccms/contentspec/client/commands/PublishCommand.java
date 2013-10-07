package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
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
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, getMessage("ERROR_NO_PUBLISH_COMMAND_MSG"), false);
        }

        if (!getNoAssemble()) {
            super.process();
        } else {
            // We need the output directory still
            findBuildDirectoryAndFiles(contentSpecProvider, publishFromConfig);
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
            JCommander.getConsole().println(getMessage("PUBLISH_BUILD_MSG"));
            Integer exitValue = ClientUtilities.runCommand(publishCommand, new File(getOutputDirectory()), JCommander.getConsole(),
                    !getHideOutput(), true);
            if (exitValue == null || exitValue != 0) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, getMessage("ERROR_RUNNING_PUBLISH_MSG"), false);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, getMessage("ERROR_RUNNING_PUBLISH_MSG"), false);
        }
        JCommander.getConsole().println(getMessage("SUCCESSFUL_PUBLISH_MSG"));
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}