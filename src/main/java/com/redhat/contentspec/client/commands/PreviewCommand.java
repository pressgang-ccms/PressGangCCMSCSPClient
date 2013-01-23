package com.redhat.contentspec.client.commands;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
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

    private boolean validateFormat(final String previewFormat) {
        if (previewFormat == null) return false;

        if (previewFormat.equals("html") || previewFormat.equals("html-single") || previewFormat.equals("pdf")) return true;
        else return false;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        final ContentSpecProvider contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        final boolean previewFromConfig = loadFromCSProcessorCfg();
        final String previewFormat = clientConfig.getPublicanPreviewFormat();

        // Check that the format can be previewed
        if (!validateFormat(previewFormat)) {
            printError(String.format(Constants.ERROR_UNSUPPORTED_FORMAT, previewFormat), false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        if (!noAssemble) {
            // Assemble the content specification
            super.process(providerFactory, user);
            if (isShutdown()) return;
        }

        // Create the file object that will be opened
        String previewFileName = null;
        final ErrorLoggerManager loggerManager = new ErrorLoggerManager();
        if (previewFromConfig) {
            final ContentSpecWrapper contentSpec = contentSpecProvider.getContentSpec(cspConfig.getContentSpecId(), null);

            // Check that that content specification was found
            if (contentSpec == null) {
                printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }

            final String rootDir = (cspConfig.getRootOutputDirectory() == null || cspConfig.getRootOutputDirectory().equals(
                    "") ? "" : (cspConfig.getRootOutputDirectory() + DocBookUtilities.escapeTitle(
                    contentSpec.getTitle()) + File.separator));
            final String locale = getOutputLocale() == null ? (getLocale() == null ? CommonConstants.DEFAULT_LOCALE : getLocale()) :
                    getOutputLocale();

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
            final String contentSpec = getContentSpecFromFile(getIds().get(0));

            final ContentSpecParser csp = new ContentSpecParser(providerFactory, loggerManager);
            try {
                csp.parse(contentSpec);
            } catch (Exception e) {
                printError(Constants.ERROR_INTERNAL_ERROR, false);
                shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
            }

            // Create the fully qualified output path
            String fileDirectory = "";
            if (getOutputPath() != null && getOutputPath().endsWith("/")) {
                fileDirectory = ClientUtilities.validateDirLocation(getOutputPath());
            } else if (getOutputPath() != null) {
                final File file = new File(ClientUtilities.validateFilePath(getOutputPath()));
                if (file.getParent() != null) fileDirectory = ClientUtilities.validateDirLocation(file.getParent());
            }

            final String locale = getOutputLocale() == null ? (getLocale() == null ? (csp.getContentSpec().getLocale() == null ?
                    CommonConstants.DEFAULT_LOCALE : csp.getContentSpec().getLocale()) : getLocale()) : getOutputLocale();

            if (previewFormat.equals("pdf")) {
                previewFileName = fileDirectory + DocBookUtilities.escapeTitle(
                        csp.getContentSpec().getTitle()) + "/tmp/" + locale + "/" + previewFormat + "/" + DocBookUtilities.escapeTitle(
                        csp.getContentSpec().getProduct()) + "-" + csp.getContentSpec().getVersion() + "-" + DocBookUtilities.escapeTitle(
                        csp.getContentSpec().getTitle()) + "-en-US.pdf";
            } else {
                previewFileName = fileDirectory + DocBookUtilities.escapeTitle(
                        csp.getContentSpec().getTitle()) + "/tmp/" + locale + "/" + previewFormat + "/index.html";
            }
        } else if (getIds().size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

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

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.PREVIEW_COMMAND_NAME);
    }

    @Override
    public void printHelp() {
        printHelp(Constants.PREVIEW_COMMAND_NAME);
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return noAssemble || getNoBuild() ? null : authenticate(getUsername(), providerFactory);
    }
}
