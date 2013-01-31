package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

@Parameters(commandDescription = "Build, Assemble and then open the preview of the Content Specification")
public class PreviewCommand extends AssembleCommand {
    @Parameter(names = Constants.NO_ASSEMBLE_LONG_PARAM, description = "Don't assemble the Content Specification.")
    private Boolean noAssemble = false;

    public PreviewCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.PREVIEW_COMMAND_NAME;
    }

    public Boolean getNoAssemble() {
        return noAssemble;
    }

    public void setNoAssemble(final Boolean noAssemble) {
        this.noAssemble = noAssemble;
    }

    private boolean validateFormat(final String previewFormat) {
        if (previewFormat == null) return false;

        if (previewFormat.equals("html") || previewFormat.equals("html-single") || previewFormat.equals("pdf")) return true;
        else return false;
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        final boolean previewFromConfig = loadFromCSProcessorCfg();
        final String previewFormat = getClientConfig().getPublicanPreviewFormat();

        // Validate that only one id or file was entered
        ClientUtilities.validateIdsOrFiles(this, getIds(), true);

        // Check that the format can be previewed
        if (!validateFormat(previewFormat)) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, String.format(Constants.ERROR_UNSUPPORTED_FORMAT, previewFormat), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (!noAssemble) {
            // Assemble the content specification
            super.process();
        }

        // Create the file object that will be opened
        String previewFileName = null;
        if (previewFromConfig) {
            final ContentSpecWrapper contentSpec = contentSpecProvider.getContentSpec(getCspConfig().getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
            }

            final String rootDir = ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpec);
            final String locale = generateOutputLocale();

            if (previewFormat.equals("pdf")) {
                // Create the file
                previewFileName = rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION + "tmp/" + locale + "/" + previewFormat + "/" +
                        DocBookUtilities.escapeTitle(
                                contentSpec.getProduct()) + "-" + contentSpec.getVersion() + "-" + DocBookUtilities.escapeTitle(
                        contentSpec.getTitle()) + "-en-US.pdf";
            } else {
                previewFileName = rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION + "tmp/" + locale + "/" + previewFormat + "/index" +
                        ".html";
            }
        } else if (getIds() != null && getIds().size() == 1) {
            // Create the file based on an ID passed from the command line
            final String contentSpecString = getContentSpecFromFile(getIds().get(0));

            // Parse the spec to get the main details
            final ContentSpec contentSpec = parseContentSpec(getProviderFactory(), contentSpecString, false);

            // Create the fully qualified output path
            String fileDirectory = "";
            if (getOutputPath() != null && getOutputPath().endsWith("/")) {
                fileDirectory = ClientUtilities.validateDirLocation(getOutputPath());
            } else if (getOutputPath() != null) {
                final File file = new File(ClientUtilities.validateFilePath(getOutputPath()));
                if (file.getParent() != null) {
                    fileDirectory = ClientUtilities.validateDirLocation(file.getParent());
                }
            }

            final String locale = generateOutputLocale();

            if (previewFormat.equals("pdf")) {
                previewFileName = fileDirectory + DocBookUtilities.escapeTitle(
                        contentSpec.getTitle()) + "/tmp/" + locale + "/" + previewFormat + "/" + DocBookUtilities.escapeTitle(
                        contentSpec.getProduct()) + "-" + contentSpec.getVersion() + "-" + DocBookUtilities.escapeTitle(
                        contentSpec.getTitle()) + "-en-US.pdf";
            } else {
                previewFileName = fileDirectory + DocBookUtilities.escapeTitle(
                        contentSpec.getTitle()) + "/tmp/" + locale + "/" + previewFormat + "/index.html";
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        final File previewFile = new File(previewFileName);

        // Check that the file exists
        if (!previewFile.exists()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    String.format(Constants.ERROR_UNABLE_TO_FIND_HTML_SINGLE_MSG, previewFile.getAbsolutePath()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Open the file
        try {
            FileUtilities.openFile(previewFile);
        } catch (Exception e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.ERROR_UNABLE_TO_OPEN_FILE_MSG,
                    previewFile.getAbsolutePath()), false);
        }
    }

    /**
     * Gets the Locale for the publican build, which can be used to find the location of files to preview.
     *
     * @return The locale that the publican files were created as.
     */
    protected String generateOutputLocale() {
        return getTargetLocale() == null ? (getLocale() == null ? CommonConstants.DEFAULT_LOCALE : getLocale()) : getTargetLocale();
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
