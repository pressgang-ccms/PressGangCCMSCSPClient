package com.redhat.contentspec.client.commands;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;

@Parameters(commandDescription = "Build, Assemble and then Publish the Content Specification")
public class PublishCommand extends BuildCommand {
    @Parameter(names = Constants.NO_BUILD_LONG_PARAM, description = "Don't build the Content Specification.")
    private Boolean noBuild = false;

    @Parameter(names = Constants.NO_ASSEMBLE_LONG_PARAM, description = "Don't assemble the Content Specification.")
    private Boolean noAssemble = false;

    @Parameter(names = Constants.PUBLICAN_BUILD_LONG_PARAM,
            description = "Build the Content Specification with publican before publishing.")
    private Boolean publicanBuild = false;

    @Parameter(names = Constants.HIDE_OUTPUT_LONG_PARAM,
            description = "Hide the output from assembling & publishing the Content Specification.")
    private Boolean hideOutput = false;

    @Parameter(names = Constants.PUBLISH_MESSAGE_LONG_PARAM, description = "Add a message to be used with the publish command.",
            metaVar = "<MESSAGE>")
    private String message = null;

    public PublishCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.PUBLISH_COMMAND_NAME;
    }

    public Boolean getNoBuild() {
        return noBuild;
    }

    public void setNoBuild(final Boolean noBuild) {
        this.noBuild = noBuild;
    }

    public Boolean getNoAssemble() {
        return noAssemble;
    }

    public void setNoAssemble(final Boolean noAssemble) {
        this.noAssemble = noAssemble;
    }

    public Boolean getPublicanBuild() {
        return publicanBuild;
    }

    public void setPublicanBuild(final Boolean publicanBuild) {
        this.publicanBuild = publicanBuild;
    }

    public Boolean getHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(final Boolean hideOutput) {
        this.hideOutput = hideOutput;
    }

    public String getPublishMessage() {
        return message;
    }

    public void setPublishMessage(final String message) {
        this.message = message;
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return noAssemble || noBuild ? null : authenticate(getUsername(), providerFactory);
    }

    private boolean isValid() {
        if (getCspConfig().getPublishCommand() == null || getCspConfig().getPublishCommand().isEmpty()) return false;

        return true;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final boolean publishFromConfig = loadFromCSProcessorCfg();

        if (!isValid()) {
            printError(Constants.ERROR_NO_PUBLISH_COMMAND, false);
            shutdown(Constants.EXIT_CONFIG_ERROR);
        }

        if (!noAssemble) {
            if (!noBuild) {
                super.process(providerFactory, user);
                if (isShutdown()) return;
            }

            JCommander.getConsole().println(Constants.STARTING_ASSEMBLE_MSG);
        }

        String fileDirectory = "";
        String outputDirectory = "";
        String fileName = null;
        if (publishFromConfig) {
            final ContentSpecWrapper contentSpec = contentSpecProvider.getContentSpec(getCspConfig().getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null) {
                printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }

            final String rootDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec);

            fileDirectory = rootDir + Constants.DEFAULT_CONFIG_ZIP_LOCATION;
            outputDirectory = rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION;
            fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-publican.zip";
        } else if (getIds() != null && getIds().size() == 1) {
            final String contentSpecString = getContentSpecFromFile(getIds().get(0));

            // parse the spec to get the main details
            final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
            ContentSpec contentSpec = null;
            try {
                contentSpec = ClientUtilities.parseContentSpecString(providerFactory, loggerManager, contentSpecString);
            } catch (Exception e) {
                printError(Constants.ERROR_INTERNAL_ERROR, false);
                shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
            }

            // Check that that content specification was parsed successfully
            if (contentSpec == null) {
                JCommander.getConsole().println(loggerManager.generateLogs());
                shutdown(Constants.EXIT_FAILURE);
            }

            outputDirectory = DocBookUtilities.escapeTitle(contentSpec.getTitle());
            fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + ".zip";
        } else if (getIds().size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Make sure the output directories exist
        final File outputDir = new File(outputDirectory);

        if (!noAssemble) {
            final File file = new File(fileDirectory + fileName);
            if (!file.exists()) {
                printError(String.format(Constants.ERROR_UNABLE_TO_FIND_ZIP_MSG, fileName), false);
                shutdown(Constants.EXIT_FAILURE);
            }

            // TODO Check that the directory is actually created/cleared
            if (outputDir.exists()) {
                // Ensure that the directory is empty
                FileUtilities.deleteDirContents(outputDir);
            } else {
                // Create the directory and it's parent directories
                outputDir.mkdirs();
            }

            // Unzip the file
            if (!ZipUtilities.unzipFileIntoDirectory(file, outputDirectory)) {
                printError(Constants.ERROR_FAILED_TO_ASSEMBLE_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            } else {
                JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_UNZIP_MSG, outputDir.getAbsolutePath()));
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (publicanBuild) {
            String publicanOptions = getClientConfig().getPublicanBuildOptions();

            // Replace the locale in the build options if the locale has been set
            if (getTargetLocale() != null)
                publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getTargetLocale());
            else if (getLocale() != null)
                publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getLocale());

            try {
                JCommander.getConsole().println(Constants.STARTING_PUBLICAN_BUILD_MSG);
                final Integer exitValue = ClientUtilities.runCommand("publican build " + publicanOptions, outputDir,
                        JCommander.getConsole(), !hideOutput, false);
                if (exitValue == null || exitValue != 0) {
                    shutdown(Constants.EXIT_FAILURE);
                }
            } catch (IOException e) {
                printError(Constants.ERROR_RUNNING_PUBLICAN_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }
        }

        String publishCommand = getCspConfig().getPublishCommand();

        // Replace the locale in the build options if the locale has been set
        if (getTargetLocale() != null) {
            publishCommand = publishCommand.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getTargetLocale());
        } else if (getLocale() != null) {
            publishCommand = publishCommand.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getLocale());
        }

        // Add the message to the script
        if (message != null) {
            publishCommand += " -m \"" + message + "\"";
        }

        try {
            JCommander.getConsole().println(Constants.PUBLISH_BUILD_MSG);
            Integer exitValue = ClientUtilities.runCommand(publishCommand, outputDir, JCommander.getConsole(), !hideOutput, true);
            if (exitValue == null || exitValue != 0) {
                printError(Constants.ERROR_RUNNING_PUBLISH_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }
        } catch (IOException e) {
            printError(Constants.ERROR_RUNNING_PUBLISH_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }
        JCommander.getConsole().println(Constants.SUCCESSFUL_PUBLISH_MSG);
    }
}