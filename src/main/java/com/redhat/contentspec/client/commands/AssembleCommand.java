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
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;

@Parameters(commandDescription = "Builds and Assembles a Content Specification so that it is ready to be previewed")
public class AssembleCommand extends BuildCommand {

    @Parameter(names = Constants.NO_BUILD_LONG_PARAM, description = "Don't build the Content Specification.")
    private Boolean noBuild = false;

    @Parameter(names = Constants.HIDE_OUTPUT_LONG_PARAM, description = "Hide the output from assembling the Content Specification.")
    private Boolean hideOutput = false;

    public AssembleCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.ASSEMBLE_COMMAND_NAME;
    }

    public Boolean getNoBuild() {
        return noBuild;
    }

    public void setNoBuild(final Boolean noBuild) {
        this.noBuild = noBuild;
    }

    public Boolean getHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(Boolean hideOutput) {
        this.hideOutput = hideOutput;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final boolean assembleFromConfig = loadFromCSProcessorCfg();

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (!noBuild) {
            super.process(providerFactory, user);
        }

        JCommander.getConsole().println(Constants.STARTING_ASSEMBLE_MSG);

        String fileDirectory = "";
        String outputDirectory = "";
        String fileName = null;
        if (assembleFromConfig) {
            final ContentSpecWrapper contentSpec = contentSpecProvider.getContentSpec(getCspConfig().getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null) {
                printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }

            /*
             * If we are assembling from a CSP Project directory then we need to get the location of the ZIP and the assembly directory
             * relative to the current working directory, or the CSP root project directory.
             */
            final String rootDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec);

            fileDirectory = rootDir + Constants.DEFAULT_CONFIG_ZIP_LOCATION;
            outputDirectory = rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION;
            fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-publican.zip";
        } else if (getIds() != null && getIds().size() == 1) {
            final String contentSpecString = getContentSpecFromFile(getIds().get(0));

            // Parse the spec to get the main details
            final ContentSpec contentSpec = parseContentSpec(providerFactory, contentSpecString, user, false);

            // Create the fully qualified output path
            if (getOutputPath() != null && getOutputPath().endsWith("/")) {
                fileDirectory = getOutputPath();
                fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + ".zip";
            } else if (getOutputPath() == null) {
                fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + ".zip";
            } else {
                fileName = getOutputPath();
            }

            // Add the full file path to the output path
            final File file = new File(ClientUtilities.validateFilePath(fileDirectory + fileName));
            if (file.getParent() != null) {
                outputDirectory = file.getParent() + File.separator + DocBookUtilities.escapeTitle(contentSpec.getTitle());
            } else {
                outputDirectory = DocBookUtilities.escapeTitle(contentSpec.getTitle());
            }
        } else if (getIds().size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        final File file = new File(ClientUtilities.validateFilePath(fileDirectory + fileName));
        if (!file.exists()) {
            printError(String.format(Constants.ERROR_UNABLE_TO_FIND_ZIP_MSG, fileName), false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Make sure the output directories exist
        final File outputDir = new File(ClientUtilities.validateDirLocation(outputDirectory));
        outputDir.mkdirs();

        // Ensure that the directory is empty
        FileUtilities.deleteDirContents(outputDir);

        // Unzip the file
        if (!ZipUtilities.unzipFileIntoDirectory(file, outputDirectory)) {
            printError(Constants.ERROR_FAILED_TO_ASSEMBLE_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        } else {
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_UNZIP_MSG, outputDir.getAbsolutePath()));
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        String publicanOptions = getClientConfig().getPublicanBuildOptions();

        // Replace the locale in the build options if the locale has been set
        if (getTargetLocale() != null)
            publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getTargetLocale());
        else if (getLocale() != null) publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getLocale());

        try {
            JCommander.getConsole().println(Constants.STARTING_PUBLICAN_BUILD_MSG);
            final Integer exitValue = ClientUtilities.runCommand("publican build " + publicanOptions, null, outputDir,
                    JCommander.getConsole(), !hideOutput, false);
            if (exitValue == null || exitValue != 0) {
                printError(String.format(Constants.ERROR_RUNNING_PUBLICAN_EXIT_CODE_MSG, (exitValue == null ? 0 : exitValue)), false);
                shutdown(Constants.EXIT_FAILURE);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_RUNNING_PUBLICAN_MSG, false);
        }
        JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_ASSEMBLE_MSG, outputDir.getAbsolutePath()));
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return noBuild ? null : authenticate(getUsername(), providerFactory);
    }
}
