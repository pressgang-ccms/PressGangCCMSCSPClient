package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;

@Parameters(commandDescription = "Pull a Content Specification from the server")
public class PullCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.CONTENT_SPEC_LONG_PARAM, Constants.CONTENT_SPEC_SHORT_PARAM})
    private Boolean pullContentSpec = false;

    @Parameter(names = {Constants.TOPIC_LONG_PARAM, Constants.TOPIC_SHORT_PARAM})
    private Boolean pullTopic = false;

    @Parameter(names = {Constants.XML_LONG_PARAM, Constants.XML_SHORT_PARAM})
    private Boolean useXml = false;

    @Parameter(names = {Constants.HTML_LONG_PARAM, Constants.HTML_SHORT_PARAM})
    private Boolean useHtml = false;

    @Parameter(names = {Constants.REVISION_LONG_PARAM, Constants.REVISION_SHORT_PARAM})
    private Integer revision;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM},
            description = "Save the output to the specified file/directory.", metaVar = "<FILE>")
    private String outputPath;

    public PullCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.PULL_COMMAND_NAME;
    }

    public Boolean useContentSpec() {
        return pullContentSpec;
    }

    public void setContentSpec(final Boolean contentSpec) {
        pullContentSpec = contentSpec;
    }

    public Boolean useTopic() {
        return pullTopic;
    }

    public void setTopic(final Boolean topic) {
        pullTopic = topic;
    }

    public Boolean isUseXml() {
        return useXml;
    }

    public void setUseXml(final Boolean useXml) {
        this.useXml = useXml;
    }

    public Boolean isUseHtml() {
        return useHtml;
    }

    public void setUseHtml(final Boolean useHtml) {
        this.useHtml = useHtml;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(final Integer revision) {
        this.revision = revision;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(final String outputPath) {
        this.outputPath = outputPath;
    }

    public boolean isValid() {
        if (useTopic() && !useContentSpec()) {
            if (isUseXml() && isUseHtml()) {
                return false;
            }

        } else if (!useTopic()) {
            if (isUseXml() || isUseHtml()) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
        boolean pullForConfig = false;

        // Initialise the basic data and perform basic input checks
        if (ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds())) {
            pullForConfig = true;
            setRevision(null);
        }

        // Check that the additional options are valid
        if (!isValid()) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.INVALID_ARG_MSG, true);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        String outputString = "";
        String fileName = "";
        // Topic
        if (useTopic()) {
            final TopicWrapper topic = getProviderFactory().getProvider(TopicProvider.class).getTopic(getIds().get(0), getRevision());
            if (topic == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE,
                        getRevision() == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
            } else {
                if (isUseXml()) {
                    outputString = topic.getXml();
                    fileName = DocBookUtilities.escapeTitle(topic.getTitle()) + ".xml";
                } else if (isUseHtml()) {
                    outputString = topic.getHtml();
                    fileName = DocBookUtilities.escapeTitle(topic.getTitle()) + ".html";
                }
            }
            // Content Specification
        } else {
            final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(getIds().get(0), getRevision());
            final String contentSpecString = contentSpecProvider.getContentSpecAsString(getIds().get(0), getRevision());
            if (contentSpecEntity == null || contentSpecString == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE,
                        getRevision() == null ? Constants.ERROR_NO_ID_FOUND_MSG : Constants.ERROR_NO_REV_ID_FOUND_MSG, false);
            } else {
                outputString = contentSpecString;

                // Calculate the filenames and output directory.
                if (getRevision() == null) {
                    fileName = DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
                } else {
                    fileName = DocBookUtilities.escapeTitle(
                            contentSpecEntity.getTitle()) + "-post-r" + getRevision() + "." + Constants.FILENAME_EXTENSION;
                }
                if (pullForConfig) {
                    setOutputPath(ClientUtilities.getOutputRootDirectory(getCspConfig(), contentSpecEntity));
                }
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Save or print the data
        if (getOutputPath() == null) {
            JCommander.getConsole().println(outputString);
        } else {
            saveOutputFile(fileName, getOutputPath(), outputString);
        }
    }

    /**
     * Saves the some content to a file. The method will also determine the correct directory to save to using the filename and output
     * path.
     *
     * @param fileName   The name that the file should be saved as.
     * @param outputPath The location that the file should be saved to.
     * @param content    The content to be saved to the file.
     */
    protected void saveOutputFile(String fileName, String outputPath, String content) {
        // Create the output file
        File output;
        String fixedOutputPath = ClientUtilities.fixFilePath(outputPath);
        if (fixedOutputPath != null && fixedOutputPath.endsWith(File.separator)) {
            output = new File(fixedOutputPath + fileName);
        } else if (fixedOutputPath == null || fixedOutputPath.equals("")) {
            output = new File(fileName);
        } else {
            output = new File(fixedOutputPath);
        }

        // Make sure the directories exist
        if (output.isDirectory()) {
            output.mkdirs();
            output = new File(output.getAbsolutePath() + File.separator + fileName);
        } else if (output.getParentFile() != null) {
            output.getParentFile().mkdirs();
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // If the file exists then create a backup file
        if (output.exists()) {
            output.renameTo(new File(output.getAbsolutePath() + ".backup"));
        }

        // Create and write to the file
        try {
            FileUtilities.saveFile(output, content, Constants.FILE_ENCODING);
            JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, output.getName()));
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_FAILED_SAVING, false);
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0 && !pullTopic;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
