package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.builder.BuildType;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;

@Parameters(commandDescription = "Build, Assemble and then open the preview of the Content Specification")
public class PreviewCommand extends AssembleCommand {
    @Parameter(names = Constants.NO_ASSEMBLE_LONG_PARAM, description = "Don't assemble the Content Specification.")
    private Boolean noAssemble = false;

    public PreviewCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public Boolean getNoAssemble() {
        return noAssemble;
    }

    public void setNoAssemble(final Boolean noAssemble) {
        this.noAssemble = noAssemble;
    }

    @Override
    public void process(final RESTManager restManager, final ErrorLoggerManager elm) {
        final boolean previewFromConfig = loadFromCSProcessorCfg();
        final String previewFormat = getPreviewFormat();

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        if (!noAssemble) {
            // Assemble the content specification
            super.process(restManager, elm);
            if (isShutdown()) return;
        }

        // Create the file object that will be opened
        String previewFileName = getPreviewFileName(restManager, elm, previewFromConfig, previewFormat);

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        final File previewFile = new File(previewFileName);

        // Check that the file exists
        if (!previewFile.exists()) {
            printError(String.format(Constants.ERROR_UNABLE_TO_FIND_HTML_SINGLE_MSG, previewFile.getAbsolutePath()), false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Open the file
        try {
            ClientUtilities.openFile(previewFile);
        } catch (Exception e) {
            printError(String.format(Constants.ERROR_UNABLE_TO_OPEN_FILE_MSG, previewFile.getAbsolutePath()), false);
            shutdown(Constants.EXIT_FAILURE);
        }
    }

    protected String getPreviewFormat() {
        final String previewFormat;

        if (getBuildType() == BuildType.JDOCBOOK) {
            previewFormat= clientConfig.getjDocbookPreviewFormat();
        } else {
            previewFormat= clientConfig.getPublicanPreviewFormat();
        }

        // Check that the format can be previewed
        if (!validateFormat(previewFormat)) {
            printError(String.format(Constants.ERROR_UNSUPPORTED_FORMAT, previewFormat), false);
            shutdown(Constants.EXIT_FAILURE);
        }

        return previewFormat;
    }

    protected boolean validateFormat(final String previewFormat) {
        if (previewFormat == null) return false;

        if (getBuildType() == BuildType.JDOCBOOK) {
            return previewFormat.equals("html") || previewFormat.equals("html_single") || previewFormat.equals("pdf");
        } else {
            return previewFormat.equals("html") || previewFormat.equals("html-single") || previewFormat.equals("pdf");
        }
    }

    protected String getPreviewFileName(final RESTManager restManager, final ErrorLoggerManager elm, final boolean previewFromConfig,
            final String previewFormat) {
        final RESTReader reader = restManager.getReader();
        String previewFileName = null;

        if (previewFromConfig) {
            final RESTTopicV1 contentSpec = restManager.getReader().getContentSpecById(cspConfig.getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null || contentSpec.getXml() == null) {
                printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }

            // Parse the content specification to get the product and versions
            final ContentSpecParser csp = new ContentSpecParser(elm, restManager);
            try {
                csp.parse(contentSpec.getXml());
            } catch (Exception e) {
                printError(Constants.ERROR_INTERNAL_ERROR, false);
                shutdown(Constants.EXIT_ARGUMENT_ERROR);
            }

            final String rootDir = (cspConfig.getRootOutputDirectory() == null || cspConfig.getRootOutputDirectory().equals(
                    "") ? "" : (cspConfig.getRootOutputDirectory() + DocBookUtilities.escapeTitle(
                    contentSpec.getTitle()) + File.separator));

            if (getBuildType() == BuildType.JDOCBOOK) {
                previewFileName = getJDocbookPreviewName(rootDir + Constants.DEFAULT_JDOCBOOK_LOCATION, csp.getContentSpec(), previewFormat);
            } else {
                previewFileName = getPublicanPreviewName(rootDir + Constants.DEFAULT_PUBLICAN_LOCATION, csp.getContentSpec(),
                        previewFormat);
            }
        } else if (getIds() != null && getIds().size() == 1) {
            // Create the file based on an ID passed from the command line
            final String contentSpec = this.getContentSpecString(reader, getIds().get(0));

            final ContentSpecParser csp = new ContentSpecParser(elm, restManager);
            try {
                csp.parse(contentSpec);
            } catch (Exception e) {
                printError(Constants.ERROR_INTERNAL_ERROR, false);
                shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
            }

            // Create the fully qualified output path
            String fileDirectory = "";
            if (getOutputPath() != null && getOutputPath().endsWith(File.separator)) {
                fileDirectory = ClientUtilities.validateDirLocation(this.getOutputPath());
            } else if (this.getOutputPath() != null) {
                final File file = new File(ClientUtilities.validateFilePath(this.getOutputPath()));
                if (file.getParent() != null) fileDirectory = ClientUtilities.validateDirLocation(file.getParent());
            }

            final String rootDir = fileDirectory + DocBookUtilities.escapeTitle(csp.getContentSpec().getTitle()) + File.separator;
            if (getBuildType() == BuildType.JDOCBOOK) {
                previewFileName = getJDocbookPreviewName(rootDir, csp.getContentSpec(), previewFormat);
            } else {
                previewFileName = getPublicanPreviewName(rootDir, csp.getContentSpec(), previewFormat);
            }
        } else if (getIds().size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        return previewFileName;
    }

    protected String getJDocbookPreviewName(final String rootDir, final ContentSpec contentSpec, final String previewFormat) {
        final String locale = generateOutputLocale(contentSpec);

        // Fix the root book directory to point to the root build directory
        final String fixedRootDir = rootDir + "target" + File.separator + "docbook" + File.separator + "publish" + File.separator;

        if (previewFormat.equals("pdf")) {
            return  fixedRootDir + File.separator + locale + File.separator + previewFormat + File.separator +
                    DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-" + locale + ".pdf";
        } else {
            return fixedRootDir + File.separator + locale + File.separator + previewFormat + File.separator + "index" +
                    ".html";
        }
    }

    protected String getPublicanPreviewName(final String rootDir, final ContentSpec contentSpec, final String previewFormat) {
        final String locale = generateOutputLocale(contentSpec);

        if (previewFormat.equals("pdf")) {
            return rootDir + "tmp" + File.separator + locale + File.separator + previewFormat + File.separator +
                    DocBookUtilities.escapeTitle(
                            contentSpec.getProduct()) + "-" + contentSpec.getVersion() + "-" + DocBookUtilities.escapeTitle(
                    contentSpec.getTitle()) + "-" + locale + ".pdf";
        } else {
            return rootDir + "tmp" + File.separator + locale + File.separator + previewFormat + File.separator + "index" +
                    ".html";
        }
    }

    protected String generateOutputLocale(final ContentSpec contentSpec) {
        return getOutputLocale() == null ? (getLocale() == null ? (contentSpec.getLocale() == null ? CommonConstants.DEFAULT_LOCALE :
                contentSpec.getLocale()) : getLocale()) : getOutputLocale();
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.PREVIEW_COMMAND_NAME);
    }

    @Override
    public void printHelp() {
        printHelp(Constants.PREVIEW_COMMAND_NAME);
    }
}
