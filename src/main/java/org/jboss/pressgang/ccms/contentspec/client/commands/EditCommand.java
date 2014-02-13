package org.jboss.pressgang.ccms.contentspec.client.commands;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang.SystemUtils;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.provider.CSNodeProvider;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.LogMessageProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.TranslatedTopicProvider;
import org.jboss.pressgang.ccms.provider.exception.ProviderException;
import org.jboss.pressgang.ccms.rest.v1.entities.base.RESTLogDetailsV1;
import org.jboss.pressgang.ccms.rest.v1.query.RESTCSNodeQueryBuilderV1;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.LogMessageWrapper;
import org.jboss.pressgang.ccms.wrapper.TextCSProcessingOptionsWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedCSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "EDIT")
public class EditCommand extends BaseCommandImpl {
    private static final long FILE_CHECK_INTERVAL = 500L;
    private static final long MIN_START_INTERVAL = 1000L;

    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    @Parameter(names = {Constants.CONTENT_SPEC_LONG_PARAM, Constants.CONTENT_SPEC_SHORT_PARAM}, descriptionKey = "EDIT_CONTENT_SPEC")
    private Boolean contentSpec = false;

    @Parameter(names = {Constants.TOPIC_LONG_PARAM, Constants.TOPIC_SHORT_PARAM}, descriptionKey = "EDIT_TOPIC")
    private Boolean topic = false;

    @Parameter(names = Constants.REVISION_MESSAGE_FLAG_LONG_PARAMETER, descriptionKey = "EDIT_REV_HISTORY")
    private Boolean revHistory = false;

    @Parameter(names = Constants.LOCALE_LONG_PARAM, metaVar = "<LOCALE>", descriptionKey = "EDIT_LOCALE")
    private String locale;

    final AtomicBoolean saving = new AtomicBoolean(false);

    public EditCommand(JCommander parser, ContentSpecConfiguration cspConfig, ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.EDIT_COMMAND_NAME;
    }

    public Boolean getContentSpec() {
        return contentSpec;
    }

    public void setContentSpec(Boolean contentSpec) {
        this.contentSpec = contentSpec;
    }

    public Boolean getTopic() {
        return topic;
    }

    public void setTopic(Boolean topic) {
        this.topic = topic;
    }

    public Boolean getRevHistory() {
        return revHistory;
    }

    public void setRevHistory(Boolean revHistory) {
        this.revHistory = revHistory;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public void process() {
        validateOptions();

        // Check that an editor command exists
        if (isNullOrEmpty(getClientConfig().getEditorCommand())) {
            printErrorAndShutdown(Constants.EXIT_CONFIG_ERROR, ClientUtilities.getMessage("ERROR_NO_EDITOR_COMMAND_MSG"), false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        if (getContentSpec()) {
            processContentSpec(getIds().get(0), getRevHistory(), getLocale());
        } else {
            processTopic(getIds().get(0));
        }
    }

    protected void processContentSpec(final Integer id, final boolean revHistory, final String locale) {
        if (revHistory) {
            final CSNodeProvider csNodeProvider = getProviderFactory().getProvider(CSNodeProvider.class);

            final RESTCSNodeQueryBuilderV1 queryBuilder = new RESTCSNodeQueryBuilderV1();
            queryBuilder.setCSNodeTypes(Arrays.asList(CommonConstants.CS_NODE_META_DATA_TOPIC));
            queryBuilder.setContentSpecIds(Arrays.asList(id));
            queryBuilder.setCSNodeTitle(CommonConstants.CS_REV_HISTORY_TITLE);

            // Get any revision history nodes for the content spec
            final CollectionWrapper<CSNodeWrapper> nodes = csNodeProvider.getCSNodesWithQuery(queryBuilder.getQuery());

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            if (nodes.isEmpty()) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_NO_REV_HISTORY_MSG"), false);
            } else {
                final CSNodeWrapper revisionHistoryNode = nodes.getItems().get(0);
                if (!isNullOrEmpty(locale)) {
                    processTranslatedRevisionHistory(revisionHistoryNode, locale);
                } else {
                    processTopic(revisionHistoryNode.getEntityId());
                }
            }
        } else {
            final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);
            final String contentSpec = ClientUtilities.getContentSpecAsString(contentSpecProvider, id, null);

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            // Create the file name
            final String fileName;
            if (!isNullOrEmpty(locale)) {
                fileName = "CS" + id + "-" + locale + "." + Constants.FILENAME_EXTENSION;
            } else {
                fileName = "CS" + id + "." + Constants.FILENAME_EXTENSION;
            }

            // Create the temp file
            final File file = createTempFile(fileName, contentSpec);

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            // Edit the temp file with the users editor
            editFile(file, id);
        }
    }

    protected TranslatedCSNodeWrapper getMatchingTranslatedCSNode(final CSNodeWrapper node,
            final CollectionWrapper<TranslatedCSNodeWrapper> translatedNodes) {
        TranslatedCSNodeWrapper matchingNode = null;
        for (final TranslatedCSNodeWrapper translatedNode : translatedNodes.getItems()) {
            if (translatedNode.getNodeRevision().equals(node.getRevision())) {
                return translatedNode;
            } else if ((matchingNode == null || matchingNode.getNodeRevision() > translatedNode.getNodeRevision()) && translatedNode
                    .getNodeRevision() <= node.getRevision()) {
                matchingNode = translatedNode;
            }
        }

        return matchingNode;
    }

    protected void processTopic(final Integer id) {
        final TopicProvider topicProvider = getProviderFactory().getProvider(TopicProvider.class);
        final TopicWrapper topicWrapper = topicProvider.getTopic(id);
        final String contents = topicWrapper.getXml();

        final String fileName = id + ".xml";

        // Create the temp file
        final File file = createTempFile(fileName, contents);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Edit the temp file with the users editor
        editFile(file, id);
    }

    protected void processTranslatedRevisionHistory(final CSNodeWrapper revisionHistoryNode, final String locale) {
        final TopicProvider topicProvider = getProviderFactory().getProvider(TopicProvider.class);

        // Get the matching translated csnode
        final CollectionWrapper<TranslatedCSNodeWrapper> translatedCSNodes = revisionHistoryNode.getTranslatedNodes();
        final TranslatedCSNodeWrapper matchingTranslatedCSNode = getMatchingTranslatedCSNode(revisionHistoryNode, translatedCSNodes);

        // Get the actual topic as it might be different to the latest content spec
        final CSNodeWrapper csNode = matchingTranslatedCSNode.getCSNode();
        final TopicWrapper revisionHistory = ClientUtilities.getTopicEntity(topicProvider, csNode.getEntityId(),
                csNode.getEntityRevision());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Find the translated topic for the node
        TranslatedTopicWrapper translatedTopic = EntityUtilities.returnClosestTranslatedTopic(revisionHistory, matchingTranslatedCSNode,
                locale);
        if (translatedTopic == null) {
            translatedTopic = EntityUtilities.returnClosestTranslatedTopic(revisionHistory, locale);
        }

        final String contents = translatedTopic.getTranslatedAdditionalXML();
        final String fileName = translatedTopic.getZanataId() + "-" + locale + ".xml";

        // Create the temp file
        final File file = createTempFile(fileName, contents);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Edit the temp file with the users editor
        editFile(file, translatedTopic.getId());

    }

    protected String getCommand(final String file) {
        if (getClientConfig().getEditorRequiresTerminal()) {
            if (SystemUtils.IS_OS_WINDOWS) {
                return "start \"\" \"" + getClientConfig().getEditorCommand() + "\" \"" + file + "\"";
            } else if (SystemUtils.IS_OS_LINUX) {
                return ClientUtilities.getLinuxTerminalCommand().replace("<COMMAND>", getClientConfig().getEditorCommand() + " " + file);
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                return "osascript -e 'tell app \"Terminal\" to do script \"" + getClientConfig().getEditorCommand() + " " + file + "; exit\"'";
            } else {
                printErrorAndShutdown(Constants.EXIT_FAILURE, "ERROR_UNABLE_TO_OPEN_EDITOR_IN_TERMINAL_MSG", false);
                return null;
            }
        } else {
            return getClientConfig().getEditorCommand() + " \"" + file + "\"";
        }
    }

    /**
     * Check that the options given to the edit command are valid
     */
    protected void validateOptions() {
        // Validate that an id was specified
        ClientUtilities.validateIdsOrFiles(this, getIds(), loadFromCSProcessorCfg());

        if (getContentSpec() && getTopic()) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_CANNOT_EDIT_TOPIC_AND_CONTENT_SPEC_MSG"),
                    true);
        } else if (!getContentSpec() && !getTopic() && !getRevHistory()) {
            // Edit topics by default
            setTopic(true);
        }

        // Make sure the --rev-history option isn't used with topics
        if (getTopic() && getRevHistory()) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_REV_HISTORY_OPTION_IGNORED_MSG"), true);
        }

        // Make sure a lang isn't used for getting content specs
        if (getContentSpec() && !isNullOrEmpty(getLocale()) && !getRevHistory()) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR,
                    ClientUtilities.getMessage("ERROR_CANNOT_EDIT_TRANSLATED_CONTENT_SPEC_MSG"), true);
        }

        // Make sure a lang isn't used for getting topics
        if (getTopic() && !isNullOrEmpty(getLocale())) {
            printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, ClientUtilities.getMessage("ERROR_CANNOT_EDIT_TRANSLATED_TOPIC_MSG"),
                    true);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Check to make sure the lang is valid
        if (getLocale() != null && !ClientUtilities.validateLanguage(this, getServerSettings(), getLocale())) {
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }
    }

    protected File createTempFile(final String fileName, final String contents) {
        final File tmpDir = new File(Constants.TEMP_LOCATION);
        final File file = new File(tmpDir, fileName);
        file.deleteOnExit();

        final String fixedContents = contents == null ? "" : contents;
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(fixedContents.getBytes(Constants.FILE_ENCODING));
            fos.flush();
        } catch (IOException e) {

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }

        return file;
    }

    protected void editFile(final File file, final Integer id) {
        // Add a listener for any changes to the file content
        final FileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter(file.getName()));
        final FileAlterationObserver fileObserver = new FileAlterationObserver(file.getParentFile(), fileFilter);
        final FileAlterationMonitor monitor = new FileAlterationMonitor(FILE_CHECK_INTERVAL);
        monitor.addObserver(fileObserver);

        // Create the listener, where on changes (ie saves), the content is saved to PressGang
        final String[] currentContent = {null};
        final FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(final File file) {
                final String content = FileUtilities.readFileContents(file);
                final String prevContent = getCurrentContent();
                setCurrentContent(content);

                if (prevContent == null || !content.trim().equals(prevContent.trim())) {
                    // If we are already saving something then wait until it's finished
                    while (saving.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                    }

                    // Make sure this content is still the latest (ie another save hasn't been done)
                    final String currentContent = getCurrentContent();
                    if (content.trim().equals(currentContent.trim())) {
                        saveChanges(id, content);
                    }
                }
            }

            protected synchronized void setCurrentContent(final String content) {
                currentContent[0] = content;
            }

            protected synchronized String getCurrentContent() {
                return currentContent[0];
            }
        };

        // Add the listener and start the monitor
        fileObserver.addListener(listener);
        try {
            monitor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Open the file in the editor
        JCommander.getConsole().println(ClientUtilities.getMessage("OPENING_FILE_MSG", file.getAbsoluteFile()));
        try {
            final Process process = ClientUtilities.runCommand(getCommand(file.getAbsolutePath()), null, null);
            final long startTime = System.currentTimeMillis();

            // Add a stream reader to clear anything that might stop the process from finishing
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String errorMsg;
            while ((errorMsg = reader.readLine()) != null) {
                printError(errorMsg, false);
            }

            // Wait for the editor to close
            try {
                process.waitFor();

                // If the time between the start and the end is small (ie 1 second) then it means the program probably forked a child process
                // and the parent has ended. So wait instead for the user to type "exit".
                final long endTime = System.currentTimeMillis();
                if (endTime - startTime < MIN_START_INTERVAL) {
                    final Scanner sc = new Scanner(System.in);
                    printWarn(ClientUtilities.getMessage("WARN_EDITOR_FORKED_MSG"));
                    String answer = sc.nextLine();
                    while (!(answer.equalsIgnoreCase("exit") || answer.equalsIgnoreCase("quit") || answer.equalsIgnoreCase("q"))) {
                        answer = sc.nextLine();
                    }
                }

                // Wait a little to allow for changes to be picked up
                Thread.sleep(FILE_CHECK_INTERVAL);
            } catch (InterruptedException e) {

            }
        } catch (IOException e) {
            printError(e.getMessage(), false);
        }

        // Clean up
        try {
            monitor.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileObserver.removeListener(listener);

        // Wait for any saving to finish
        if (saving.get()) {
            JCommander.getConsole().println(ClientUtilities.getMessage("WAITING_FOR_SAVE_TO_COMPLETE"));
            while (saving.get()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }
    }

    protected synchronized void saveChanges(final Integer id, final String content) {
        // Save the topic/content spec
        saving.set(true);
        if (getContentSpec()) {
            if (getRevHistory() && !isNullOrEmpty(getLocale())) {
                saveTranslatedRevisionHistory(id, content);
            } else if (getRevHistory()) {
                saveTopic(id, content);
            } else {
                saveContentSpec(id, content);
            }
        } else {
            saveTopic(id, content);
        }
        saving.set(false);
    }

    protected void saveContentSpec(final Integer id, final String content) {
        JCommander.getConsole().println(ClientUtilities.getMessage("SAVING_CONTENT_SPEC_MSG"));

        final TextContentSpecProvider textContentSpecProvider = getProviderFactory().getProvider(TextContentSpecProvider.class);
        final TextCSProcessingOptionsWrapper processingOptions = textContentSpecProvider.newTextProcessingOptions();

        // Create the task to update the content spec on the server
        int flag = 0 | RESTLogDetailsV1.MINOR_CHANGE_FLAG_BIT;
        final LogMessageWrapper logMessage = getProviderFactory().getProvider(LogMessageProvider.class).createLogMessage();
        logMessage.setFlags(flag);
        logMessage.setMessage(ClientUtilities.createLogMessage(getUsername(), null));
        logMessage.setUser(getServerEntities().getUnknownUserId().toString());

        TextContentSpecWrapper output = null;
        try {
            final TextContentSpecWrapper contentSpecEntity = textContentSpecProvider.newTextContentSpec();
            contentSpecEntity.setText(content);
            contentSpecEntity.setId(id);
            output = textContentSpecProvider.updateTextContentSpec(contentSpecEntity, processingOptions, logMessage);
        } catch (ProviderException e) {
            output = textContentSpecProvider.newTextContentSpec();
            output.setErrors(e.getMessage());
        }

        JCommander.getConsole().println(output.getErrors());
        JCommander.getConsole().println("");
    }


    protected void saveTopic(final Integer id, final String content) {
        if (getRevHistory()) {
            JCommander.getConsole().println(ClientUtilities.getMessage("SAVING_REV_HISTORY_MSG"));
        } else {
            JCommander.getConsole().println(ClientUtilities.getMessage("SAVING_TOPIC_MSG"));
        }

        final TopicProvider topicProvider = getProviderFactory().getProvider(TopicProvider.class);

        // Create the task to update the content spec on the server
        int flag = 0 | RESTLogDetailsV1.MINOR_CHANGE_FLAG_BIT;
        final LogMessageWrapper logMessage = getProviderFactory().getProvider(LogMessageProvider.class).createLogMessage();
        logMessage.setFlags(flag);
        logMessage.setMessage(ClientUtilities.createLogMessage(getUsername(), null));
        logMessage.setUser(getServerEntities().getUnknownUserId().toString());

        try {
            final TopicWrapper topicEntity = topicProvider.newTopic();
            topicEntity.setXml(content);
            topicEntity.setId(id);
            topicProvider.updateTopic(topicEntity, logMessage);
            if (getRevHistory()) {
                JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFULLY_SAVED_REV_HISTORY_MSG"));
            } else {
                JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFULLY_SAVED_TOPIC_MSG"));
            }
        } catch (ProviderException e) {
            printError(e.getMessage(), false);
        }
    }

    protected void saveTranslatedRevisionHistory(final Integer id, final String content) {
        JCommander.getConsole().println("Saving the revision history to the server...");

        final TranslatedTopicProvider translatedTopicProvider = getProviderFactory().getProvider(TranslatedTopicProvider.class);

        // Create the task to update the content spec on the server
        int flag = 0 | RESTLogDetailsV1.MINOR_CHANGE_FLAG_BIT;
        final LogMessageWrapper logMessage = getProviderFactory().getProvider(LogMessageProvider.class).createLogMessage();
        logMessage.setFlags(flag);
        logMessage.setMessage(ClientUtilities.createLogMessage(getUsername(), null));
        logMessage.setUser(getServerEntities().getUnknownUserId().toString());

        try {
            final TranslatedTopicWrapper translatedTopicEntity = translatedTopicProvider.newTranslatedTopic();
            translatedTopicEntity.setTranslatedAdditionalXML(content);
            translatedTopicEntity.setId(id);
            translatedTopicProvider.updateTranslatedTopic(translatedTopicEntity, logMessage);
            JCommander.getConsole().println(ClientUtilities.getMessage("SUCCESSFULLY_SAVED_REV_HISTORY_MSG"));
        } catch (ProviderException e) {
            printError(e.getMessage(), false);
        }
    }

    @Override
    public void shutdown() {
        // Wait for the save to finish
        while (saving.get()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        super.shutdown();
    }


    @Override
    public boolean loadFromCSProcessorCfg() {
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
