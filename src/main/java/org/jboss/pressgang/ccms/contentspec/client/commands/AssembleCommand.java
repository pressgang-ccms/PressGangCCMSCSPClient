package org.jboss.pressgang.ccms.contentspec.client.commands;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.builder.BuildType;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "ASSEMBLE")
public class AssembleCommand extends BuildCommand {

    @Parameter(names = Constants.NO_BUILD_LONG_PARAM, descriptionKey = "ASSEMBLE_NO_BUILD")
    private Boolean noBuild = false;

    @Parameter(names = Constants.HIDE_OUTPUT_LONG_PARAM, descriptionKey = "ASSEMBLE_HIDE_OUTPUT")
    private Boolean hideOutput = false;

    @Parameter(names = Constants.NO_PUBLICAN_BUILD_LONG_PARAM, descriptionKey = "ASSEMBLE_NO_PUBLICAN_BUILD", hidden = true)
    private Boolean noPublicanBuild = false;

    @Parameter(names = Constants.PUBLICAN_CONFIG_LONG_PARAM, descriptionKey = "ASSEMBLE_PUBLICAN_CONFIG")
    private String publicanCfg = null;

    private String buildFileDirectory = "";
    String buildFileName = null;
    String outputDirectory = "";

    public AssembleCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.ASSEMBLE_COMMAND_NAME;
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

    public Boolean isNoPublicanBuild() {
        return noPublicanBuild;
    }

    public void setNoPublicanBuild(Boolean noPublicanBuild) {
        this.noPublicanBuild = noPublicanBuild;
    }

    public String getPublicanCfg() {
        return publicanCfg;
    }

    public void setPublicanCfg(String publicanCfg) {
        this.publicanCfg = publicanCfg;
    }

    @Override
    public void process() {
        boolean assembleFromConfig = loadFromCSProcessorCfg();

        if (!getNoBuild()) {
            super.process();
        } else {
            // Validate that only one id or file was entered
            assembleFromConfig = ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds());
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        JCommander.getConsole().println(ClientUtilities.getMessage("STARTING_ASSEMBLE_MSG"));

        // Find the build directory and required files
        final ContentSpec contentSpec = getContentSpec(getIds().get(0), !getNoBuild());
        findBuildDirectoryAndFiles(contentSpec, assembleFromConfig);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        final File buildFile = new File(ClientUtilities.fixDirectoryPath(getBuildFileDirectory()) + getBuildFileName());
        if (!buildFile.exists()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_UNABLE_TO_FIND_ZIP_MSG", getBuildFileName()), false);
        }

        // Make sure the output directories exist
        final File buildOutputDirectory = new File(ClientUtilities.fixDirectoryPath(getOutputDirectory()));
        buildOutputDirectory.mkdirs();

        // Ensure that the directory is empty
        if (!FileUtilities.deleteDirContents(buildOutputDirectory)) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_FAILED_TO_CLEAN_ASSEMBLY_MSG"), false);
        }

        // Unzip the file
        if (!ZipUtilities.unzipFileIntoDirectory(buildFile, getOutputDirectory())) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_FAILED_TO_ASSEMBLE_MSG"), false);
        } else {
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_UNZIP_MSG", buildOutputDirectory.getAbsolutePath()));
        }

        // Make sure the specified publican.cfg file exists in the output
        if (!validatePublicanCfg(contentSpec, buildOutputDirectory)) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_PUBLICAN_CFG_DOESNT_EXIST_MSG",
                    getPublicanCfg()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Run publican to assemble the book into the output format(s)
        if (!isNoPublicanBuild()) {
            if (getBuildType() == BuildType.JDOCBOOK) {
                runMaven(buildOutputDirectory);
            } else {
                runPublican(contentSpec, buildOutputDirectory);
            }
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFUL_ASSEMBLE_MSG", buildOutputDirectory.getAbsolutePath()));
        }
    }

    /**
     * Find the Build Directory and output files.
     *
     * @param contentSpec        The Content Spec to find the build directory and file names for.
     * @param assembleFromConfig Whether or not the command is assembling from a csprocessor.cfg directory or not.
     */
    protected void findBuildDirectoryAndFiles(final ContentSpec contentSpec, boolean assembleFromConfig) {
        boolean assembleFromId = true;
        Integer id = null;
        // Find if we are assembling from a file or ID
        if (!assembleFromConfig) {
            assert getIds() != null && getIds().size() == 1;

            if (!getIds().get(0).matches("^\\d+")) {
                assembleFromId = false;
            } else {
                id = Integer.parseInt(getIds().get(0));
            }
        } else {
            id = getCspConfig().getContentSpecId();
        }

        final String escapedTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
        if (assembleFromId) {
            if (assembleFromConfig) {
                /*
                 * If we are assembling from a CSP Project directory then we need to get the location of the ZIP and the assembly directory
                 * relative to the current working directory, or the CSP root project directory.
                 */
                final String rootDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec);

                setBuildFileDirectory(rootDir + Constants.DEFAULT_CONFIG_ZIP_LOCATION);
                if (getBuildType() == BuildType.JDOCBOOK) {
                    setOutputDirectory(rootDir + Constants.DEFAULT_CONFIG_JDOCBOOK_LOCATION);
                    setBuildFileName(escapedTitle + Constants.DEFAULT_CONFIG_JDOCBOOK_BUILD_POSTFIX + ".zip");
                } else {
                    setOutputDirectory(rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION);
                    setBuildFileName(escapedTitle + Constants.DEFAULT_CONFIG_PUBLICAN_BUILD_POSTFIX + ".zip");
                }
            } else {
                // Find the build directories and files from the content spec
                findBuildDirectoryAndFiles(escapedTitle);
            }
        } else {
            // Find the build directories and files from the content spec
            findBuildDirectoryAndFiles(escapedTitle);
        }
    }

    /**
     * Find the Build Directory and output files for a content spec using it's title
     *
     * @param escapedContentSpecTitle The content specs title.
     */
    private void findBuildDirectoryAndFiles(final String escapedContentSpecTitle) {
        // Create the fully qualified output path
        if (getOutputPath() != null && (getOutputPath().endsWith(File.separator) || new File(getOutputPath()).isDirectory())) {
            setBuildFileDirectory(ClientUtilities.fixDirectoryPath(getOutputPath()));
            setBuildFileName(escapedContentSpecTitle + ".zip");
        } else if (getOutputPath() == null) {
            setBuildFileName(escapedContentSpecTitle + ".zip");
        } else {
            setBuildFileName(getOutputPath());
        }

        // Add the full file path to the output path
        final File file = new File(ClientUtilities.fixFilePath(getBuildFileDirectory() + getBuildFileName()));
        if (file.getParent() != null) {
            setOutputDirectory(file.getParent() + File.separator + DocBookUtilities.escapeTitle(escapedContentSpecTitle));
        } else {
            setOutputDirectory(DocBookUtilities.escapeTitle(escapedContentSpecTitle));
        }
    }

    /**
     * Run Publican to assemble the book from Docbook XML markup to the required format.
     *
     * @param contentSpec            The content spec that is being assembled.
     * @param publicanFilesDirectory The directory location that hosts the publican files.
     */
    protected void runPublican(final ContentSpec contentSpec, final File publicanFilesDirectory) {
        String publicanOptions = getClientConfig().getPublicanBuildOptions();

        // Replace the locale in the build options if the locale has been set
        final String locale = generateOutputLocale(contentSpec.getLocale());
        if (!isNullOrEmpty(locale)) {
            publicanOptions = publicanOptions.replaceAll("--lang(s)?(=| )[A-Za-z\\-,]+", "--langs=" + locale);
        }

        // Add the config filename
        final String name = getPublicanCfg() == null ? contentSpec.getDefaultPublicanCfg() : getPublicanCfg();
        if (name != null && !"publican.cfg".equals(name)) {
            final Matcher matcher = CSConstants.CUSTOM_PUBLICAN_CFG_PATTERN.matcher(name);
            final String fixedName = (matcher.find() ? matcher.group(1) : name) + "-" + CommonConstants.CS_PUBLICAN_CFG_TITLE;
            publicanOptions += " --config=" + fixedName;
        }

        try {
            JCommander.getConsole().println(ClientUtilities.getMessage("STARTING_PUBLICAN_BUILD_MSG"));
            final Integer exitValue = ClientUtilities.runCommand("publican build " + publicanOptions, publicanFilesDirectory,
                    JCommander.getConsole(), !hideOutput, false);
            if (exitValue == null || exitValue != 0) {
                printErrorAndShutdown(Constants.EXIT_FAILURE,
                        ClientUtilities.getMessage("ERROR_RUNNING_PUBLICAN_EXIT_CODE_MSG", (exitValue == null ? 0 : exitValue)), false);
            }
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_RUNNING_PUBLICAN_MSG"), false);
        }
    }

    /**
     * Run Maven to assemble the book from Docbook XML markup to the required format.
     *
     * @param jDocbookFilesDirectory The directory location that hosts the jDocbook files.
     */
    protected void runMaven(final File jDocbookFilesDirectory) {
        String jDocbookOptions = getClientConfig().getjDocbookBuildOptions();

        try {
            JCommander.getConsole().println(ClientUtilities.getMessage("STARTING_MAVEN_BUILD_MSG"));
            final Integer exitValue = ClientUtilities.runCommand("mvn " + jDocbookOptions, null, jDocbookFilesDirectory,
                    JCommander.getConsole(), !hideOutput, false);
            if (exitValue == null || exitValue != 0) {
                printError(ClientUtilities.getMessage("ERROR_RUNNING_MAVEN_EXIT_CODE_MSG", (exitValue == null ? 0 : exitValue)), false);
                shutdown(Constants.EXIT_FAILURE);
            }
        } catch (IOException e) {
            printError(ClientUtilities.getMessage("ERROR_RUNNING_MAVEN_MSG"), false);
            shutdown(Constants.EXIT_FAILURE);
        }
    }

    /**
     * Validates that the --publican-config value specified has a matching config in the input directory.
     *
     * @param contentSpec     The content spec that is being assembled.
     * @param outputDirectory The output directory where the files should exist.
     * @return True if the value is valid, otherwise false.
     */
    protected boolean validatePublicanCfg(final ContentSpec contentSpec, final File outputDirectory) {
        // If it's not a publican build then it doesn't matter what value is specified
        if (getBuildType() == null || getBuildType().equals(BuildType.PUBLICAN) || getBuildType().equals(BuildType.PUBLICAN_PO)) {
            final String name = getPublicanCfg() == null ? contentSpec.getDefaultPublicanCfg() : getPublicanCfg();
            if (name != null && !"publican.cfg".equals(name)) {
                final Matcher matcher = CSConstants.CUSTOM_PUBLICAN_CFG_PATTERN.matcher(name);
                final String fixedName = (matcher.find() ? matcher.group(1) : name) + "-" + CommonConstants.CS_PUBLICAN_CFG_TITLE;
                final File config = new File(outputDirectory, fixedName);
                return config.exists() && config.isFile();
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
