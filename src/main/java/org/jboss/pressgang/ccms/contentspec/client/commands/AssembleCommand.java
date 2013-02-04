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
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;

@Parameters(commandDescription = "Builds and Assembles a Content Specification so that it is ready to be previewed")
public class AssembleCommand extends BuildCommand {

    @Parameter(names = Constants.NO_BUILD_LONG_PARAM, description = "Don't build the Content Specification.")
    private Boolean noBuild = false;

    @Parameter(names = Constants.HIDE_OUTPUT_LONG_PARAM, description = "Hide the output from assembling the Content Specification.")
    private Boolean hideOutput = false;

    @Parameter(names = Constants.NO_PUBLICAN_BUILD_LONG_PARAM,
            description = "Build the Content Specification with publican after unzipping.",
            hidden = true)
    private Boolean noPublicanBuild = true;

    private String buildFileDirectory = "";
    String buildFileName = null;
    String outputDirectory = "";

    public AssembleCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public Boolean isNoPublicanBuild() {
        return noPublicanBuild;
    }

    public void setNoPublicanBuild(Boolean noPublicanBuild) {
        this.noPublicanBuild = noPublicanBuild;
    }

    protected String getBuildFileDirectory() {
        return buildFileDirectory;
    }

    protected void setBuildFileDirectory(String buildFileDirectory) {
        this.buildFileDirectory = buildFileDirectory;
    }

    protected String getBuildFileName() {
        return buildFileName;
    }

    protected void setBuildFileName(String buildFileName) {
        this.buildFileName = buildFileName;
    }

    protected String getOutputDirectory() {
        return outputDirectory;
    }

    protected void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
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
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final boolean assembleFromConfig = loadFromCSProcessorCfg();

        // Validate that only one id or file was entered
        ClientUtilities.validateIdsOrFiles(this, getIds(), true);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (!getNoBuild()) {
            super.process();
        }

        JCommander.getConsole().println(Constants.STARTING_ASSEMBLE_MSG);

        // Find the build directory and required files
        findBuildDirectoryAndFiles(contentSpecProvider, assembleFromConfig);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        final File buildFile = new File(ClientUtilities.validateFilePath(getBuildFileDirectory() + getBuildFileName()));
        if (!buildFile.exists()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.ERROR_UNABLE_TO_FIND_ZIP_MSG, getBuildFileName()), false);
        }

        // Make sure the output directories exist
        final File buildOutputDirectory = new File(ClientUtilities.validateDirLocation(getOutputDirectory()));
        buildOutputDirectory.mkdirs();

        // Ensure that the directory is empty
        FileUtilities.deleteDirContents(buildOutputDirectory);

        // Unzip the file
        if (!ZipUtilities.unzipFileIntoDirectory(buildFile, getOutputDirectory())) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_FAILED_TO_ASSEMBLE_MSG, false);
        } else {
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_UNZIP_MSG, buildOutputDirectory.getAbsolutePath()));
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Run publican to assemble the book into the output format(s)
        if (!isNoPublicanBuild()) {
            runPublican(buildOutputDirectory);
            JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_ASSEMBLE_MSG, buildOutputDirectory.getAbsolutePath()));
        }
    }

    /**
     * Find the Build Directory and output files.
     *
     * @param contentSpecProvider The Content Spec provider that can be used to get information about a content spec.
     * @param assembleFromConfig  Whether or not the command is assembling from a csprocessor.cfg directory or not.
     */
    protected void findBuildDirectoryAndFiles(final ContentSpecProvider contentSpecProvider, boolean assembleFromConfig) {
        if (assembleFromConfig) {
            final ContentSpecWrapper contentSpec = contentSpecProvider.getContentSpec(getCspConfig().getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
            }

            /*
             * If we are assembling from a CSP Project directory then we need to get the location of the ZIP and the assembly directory
             * relative to the current working directory, or the CSP root project directory.
             */
            final String rootDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec);

            setBuildFileDirectory(rootDir + Constants.DEFAULT_CONFIG_ZIP_LOCATION);
            setOutputDirectory(rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION);
            setBuildFileName(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + Constants.DEFAULT_CONFIG_BUILD_POSTFIX + ".zip");
        } else if (getIds() != null && getIds().size() == 1) {
            final String contentSpecString = getContentSpecFromFile(getIds().get(0));

            // Parse the spec to get the main details
            final ContentSpec contentSpec = parseContentSpec(getProviderFactory(), contentSpecString, false);

            // Create the fully qualified output path
            if (getOutputPath() != null && getOutputPath().endsWith("/")) {
                setBuildFileDirectory(getOutputPath());
                setBuildFileName(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + ".zip");
            } else if (getOutputPath() == null) {
                setBuildFileName(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + ".zip");
            } else {
                setBuildFileName(getOutputPath());
            }

            // Add the full file path to the output path
            final File file = new File(ClientUtilities.validateFilePath(getBuildFileDirectory() + getBuildFileName()));
            if (file.getParent() != null) {
                setOutputDirectory(file.getParent() + File.separator + DocBookUtilities.escapeTitle(contentSpec.getTitle()));
            } else {
                setOutputDirectory(DocBookUtilities.escapeTitle(contentSpec.getTitle()));
            }
        }
    }

    /**
     * Run Publican to assemble the book from Docbook XML markup to the required format.
     *
     * @param publicanFilesDirectory The directory location that hosts the publican files.
     */
    protected void runPublican(final File publicanFilesDirectory) {
        String publicanOptions = getClientConfig().getPublicanBuildOptions();

        // Replace the locale in the build options if the locale has been set
        if (getTargetLocale() != null)
            publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getTargetLocale());
        else if (getLocale() != null) publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--langs=" + getLocale());

        try {
            JCommander.getConsole().println(Constants.STARTING_PUBLICAN_BUILD_MSG);
            final Integer exitValue = ClientUtilities.runCommand("publican build " + publicanOptions, null, publicanFilesDirectory,
                    JCommander.getConsole(), !hideOutput, false);
            if (exitValue == null || exitValue != 0) {
                printErrorAndShutdown(Constants.EXIT_FAILURE,
                        String.format(Constants.ERROR_RUNNING_PUBLICAN_EXIT_CODE_MSG, (exitValue == null ? 0 : exitValue)), false);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_RUNNING_PUBLICAN_MSG, false);
        }
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
