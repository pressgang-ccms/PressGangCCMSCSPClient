package org.jboss.pressgang.ccms.contentspec.client.commands;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.builder.BuildType;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "PREVIEW")
public class PreviewCommand extends AssembleCommand {
    @Parameter(names = Constants.NO_ASSEMBLE_LONG_PARAM, descriptionKey = "PREVIEW_NO_ASSEMBLE")
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

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        boolean previewFromConfig = loadFromCSProcessorCfg();
        final String previewFormat = getPreviewFormat();

        if (!getNoAssemble()) {
            // Assemble the content specification
            super.process();
        } else {
            // Validate that only one id or file was entered
            previewFromConfig = ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds());
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Find the preview file to be opened.
        String previewFileName = findFileToPreview(contentSpecProvider, previewFromConfig, previewFormat);
        final File previewFile = new File(previewFileName);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Check that the file exists
        if (!previewFile.exists() || previewFile.isDirectory()) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    ClientUtilities.getMessage("ERROR_UNABLE_TO_FIND_PREVIEW_FILE_MSG", previewFile.getAbsolutePath()), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Open the file
        try {
            FileUtilities.openFile(previewFile);
        } catch (Exception e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE,
                    ClientUtilities.getMessage("ERROR_UNABLE_TO_OPEN_FILE_MSG", previewFile.getAbsolutePath()), false);
        }
    }

    /**
     * Get the format to be previewed and validate it.
     *
     * @return The format to be previewed.
     */
    protected String getPreviewFormat() {
        final String previewFormat;

        if (getBuildType() == BuildType.JDOCBOOK) {
            previewFormat = getClientConfig().getjDocbookPreviewFormat();
        } else {
            previewFormat = getClientConfig().getPublicanPreviewFormat();
        }

        // Check that the format can be previewed
        if (!validateFormat(previewFormat)) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_UNSUPPORTED_FORMAT_MSG", previewFormat), false);
        }

        return previewFormat;
    }

    /**
     * Check that the preview format is a valid format.
     *
     * @param previewFormat The format to be previewed.
     * @return True if the format is valid, otherwise false.
     */
    protected boolean validateFormat(final String previewFormat) {
        if (previewFormat == null) return false;

        if (getBuildType() == BuildType.JDOCBOOK) {
            return previewFormat.equals("html") || previewFormat.equals("html_single") || previewFormat.equals("pdf");
        } else {
            return previewFormat.equals("html") || previewFormat.equals("html-single") || previewFormat.equals("pdf");
        }
    }

    /**
     * Find the name/location of the file to be opened for previewing.
     *
     * @param contentSpecProvider The Content Spec provider that can be used to get information about a content spec.
     * @param previewFromConfig   Whether or not the command is executing from a csprocessor.cfg directory or not.
     * @param previewFormat       The format of the file that should be previewed.
     * @return The filename and location of the file to be opened to be previewed, or null if it can't be found.
     */
    protected String findFileToPreview(final ContentSpecProvider contentSpecProvider, boolean previewFromConfig,
            final String previewFormat) {
        boolean previewFromId = true;
        Integer id = null;
        // Find if we are assembling from a file or ID
        if (!previewFromConfig) {
            assert getIds() != null && getIds().size() == 1;

            if (!getIds().get(0).matches("^\\d+")) {
                previewFromId = false;
            } else {
                id = Integer.parseInt(getIds().get(0));
            }
        } else {
            id = getCspConfig().getContentSpecId();
        }

        if (previewFromId) {
            final ContentSpecWrapper contentSpec = ClientUtilities.getContentSpecEntity(contentSpecProvider, id, getRevision());

            // Check that that content specification was found
            if (contentSpec == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_ID_FOUND_MSG"), false);
            }

            // Check that the content spec has a valid version
            if (contentSpec.getChildren() == null || contentSpec.getChildren().isEmpty()) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_VALID_CONTENT_SPEC_MSG"), false);
            }

            // If using a content spec project directory the file names/locations are static based on the root directory
            final String escapedTitle = ClientUtilities.getEscapedContentSpecTitle(getProviderFactory(), contentSpec);
            if (previewFromConfig) {
                final String rootDir = ClientUtilities.getOutputRootDirectory(getProviderFactory(), getCspConfig(), contentSpec);

                if (getBuildType() == BuildType.JDOCBOOK) {
                    return getJDocBookPreviewName(rootDir + Constants.DEFAULT_CONFIG_JDOCBOOK_LOCATION, escapedTitle, previewFormat,
                            contentSpec.getLocale());
                } else {
                    return getPublicanPreviewName(rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION, escapedTitle,
                            contentSpec.getVersion(), contentSpec.getProduct(), previewFormat, contentSpec.getLocale());
                }
            } else {
                return findFileToPreview(escapedTitle, contentSpec.getVersion(), contentSpec.getProduct(), previewFormat,
                        contentSpec.getLocale());
            }
        } else {
            // Parse the spec from a file to get the main details
            final ContentSpec contentSpec = getContentSpecFromFile(getIds().get(0), false);

            return findFileToPreview(DocBookUtilities.escapeTitle(contentSpec.getTitle()), contentSpec.getVersion(),
                    contentSpec.getProduct(), previewFormat, contentSpec.getLocale());
        }
    }

    /**
     * Find the file to preview when previewing from a file or id passed via the command line, for a content specification.
     *
     *
     * @param escapedContentSpecTitle   The title of the content spec.
     * @param contentSpecVersion The version of the content specs product.
     * @param contentSpecProduct The product that content spec is for.
     * @param previewFormat      The file format to be previewed (html, html-single, pdf, etc...).
     * @param contentSpecLocale
     * @return The filename and location of the file to be opened to be previewed, or null if it can't be found.
     */
    protected String findFileToPreview(final String escapedContentSpecTitle, final String contentSpecVersion, final String contentSpecProduct,
            final String previewFormat, String contentSpecLocale) {
        // Create the fully qualified output path
        String fileDirectory = "";
        if (getOutputPath() != null && (getOutputPath().endsWith(File.separator) || new File(getOutputPath()).isDirectory())) {
            fileDirectory = ClientUtilities.fixDirectoryPath(getOutputPath());
        } else if (getOutputPath() != null) {
            final File file = new File(ClientUtilities.fixFilePath(getOutputPath()));
            if (file.getParent() != null) {
                fileDirectory = ClientUtilities.fixDirectoryPath(file.getParent());
            }
        }

        // Add the book name to the path
        fileDirectory += escapedContentSpecTitle + File.separator;

        // Get the preview file name based on the build type
        if (getBuildType() == BuildType.JDOCBOOK) {
            return getJDocBookPreviewName(fileDirectory, escapedContentSpecTitle, previewFormat, contentSpecLocale);
        } else {
            return getPublicanPreviewName(fileDirectory, escapedContentSpecTitle, contentSpecVersion, contentSpecProduct, previewFormat,
                    contentSpecLocale);
        }
    }

    /**
     * Get the filename to open for a jDocbook build.
     *
     *
     * @param rootDir                 The root directory of the build.
     * @param escapedContentSpecTitle The title of the content specification used for the build.
     * @param previewFormat           The format to be previewed.
     * @param contentSpecLocale
     * @return The file name and location of the file to be previewed.
     */
    protected String getJDocBookPreviewName(final String rootDir, final String escapedContentSpecTitle, final String previewFormat,
            final String contentSpecLocale) {
        final String FS = File.separator;
        final String locale = generateOutputLocale(contentSpecLocale);

        // Fix the root book directory to point to the root build directory
        final String fixedRootDir = rootDir + "target" + FS + "docbook" + FS + "publish" + FS;

        if (previewFormat.equals("pdf")) {
            return fixedRootDir + FS + locale + FS + previewFormat + FS + escapedContentSpecTitle + "-" + locale + ".pdf";
        } else {
            return fixedRootDir + FS + locale + FS + previewFormat + FS + "index.html";
        }
    }

    /**
     * Get the filename to open for a publican build.
     *
     *
     * @param rootDir                 The root directory of the build.
     * @param escapedContentSpecTitle The title of the content specification used for the build.
     * @param contentSpecVersion      The product version of the content specification used for the build.
     * @param contentSpecProduct      The product of the content specification used for the build.
     * @param previewFormat           The format to be previewed.
     * @param contentSpecLocale
     * @return The file name and location of the file to be previewed.
     */
    protected String getPublicanPreviewName(final String rootDir, final String escapedContentSpecTitle, final String contentSpecVersion,
            final String contentSpecProduct, final String previewFormat, final String contentSpecLocale) {
        final String FS = File.separator;
        final String locale = generateOutputLocale(contentSpecLocale);

        if (previewFormat.equals("pdf")) {
            return rootDir + "tmp" + FS + locale + FS + previewFormat + File.separator +
                    DocBookUtilities.escapeTitle(contentSpecProduct) + "-" + contentSpecVersion + "-" + escapedContentSpecTitle + "-" +
                    locale + ".pdf";
        } else {
            return rootDir + "tmp" + FS + locale + FS + previewFormat + FS + "index.html";
        }
    }

    /**
     * Gets the Locale for the publican build, which can be used to find the location of files to preview.
     *
     * @return The locale that the publican files were created as.
     * @param contentSpecLocale
     */
    protected String generateOutputLocale(final String contentSpecLocale) {
        if (getTargetLocale() == null) {
            if (isNullOrEmpty(contentSpecLocale)) {
                return getLocale() == null ? getServerSettings().getDefaultLocale() : getLocale();
            } else {
                return contentSpecLocale;
            }
        } else {
            return getTargetLocale();
        }
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
