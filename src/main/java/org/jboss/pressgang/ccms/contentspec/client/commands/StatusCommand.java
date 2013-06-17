package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;

@Parameters(commandDescription = "Check the status of a local copy of a Content Specification compared to the server")
public class StatusCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID] or [FILE]")
    private List<String> ids = new ArrayList<String>();

    public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(final List<String> ids) {
        this.ids = ids;
    }

    @Override
    public void printHelp() {
        printHelp(Constants.STATUS_COMMAND_NAME);
    }

    @Override
    public void printError(final String errorMsg, final boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.STATUS_COMMAND_NAME);
    }

    @Override
    public void process(final RESTManager restManager, final ErrorLoggerManager elm) {
        final RESTReader reader = restManager.getReader();

        // Load the data from the config data if no ids were specified
        if (loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (cspConfig != null && cspConfig.getContentSpecId() != null) {
                setIds(CollectionUtilities.toStringArrayList(cspConfig.getContentSpecId()));
            }
        }

        // Check that only one ID exists
        if (ids.size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else if (ids.size() > 1) {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Get the content spec from the server based on the ID
        final String id = ids.get(0);
        final String fileName;
        RESTTopicV1 contentSpec = null;
        if (id.matches("^\\d+$")) {
            contentSpec = reader.getPostContentSpecById(Integer.parseInt(id), null);

            if (contentSpec == null || contentSpec.getXml() == null) {
                printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
                shutdown(Constants.EXIT_FAILURE);
            }

            // Create the local file
            String tempFileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
            File tempFile = new File(tempFileName);

            // Check that the file exists
            if (!tempFile.exists()) {
                // Backwards compatibility check for files ending with .txt
                tempFileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post.txt";
                tempFile = new File(tempFileName);
                if (!tempFile.exists()) {
                    printError(String.format(Constants.ERROR_NO_FILE_OUT_OF_DATE_MSG, tempFile.getName()), false);
                    shutdown(Constants.EXIT_FAILURE);
                }
            }

            fileName = tempFileName;
        } else {
            fileName = id;
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // Get the content spec from the file
        String contentSpecData = null;
        try {
            contentSpecData = FileUtils.readFileToString(new File(fileName));
        } catch (IOException e) {
            // Do nothing as this is handled below
        }

        if (contentSpecData == null || contentSpecData.equals("")) {
            printError(Constants.ERROR_EMPTY_FILE_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // If the content spec is null, than look up the ID from the file
        if (contentSpec == null) {
            contentSpec = reader.getPostContentSpecById(getContentSpecId(contentSpecData), null);
        }

        // Good point to check for a shutdown
        if (isAppShuttingDown()) {
            shutdown.set(true);
            return;
        }

        // At this point we should have a content spec topic, if not then shut down
        if (contentSpec == null || contentSpec.getXml() == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Calculate the server checksum value
        final String serverContentSpecData = contentSpec.getXml().replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", "");
        final String serverChecksum = HashUtilities.generateMD5(serverContentSpecData);

        // Get the local checksum value
        final Pattern pattern = Pattern.compile("CHECKSUM[ ]*=[ ]*(?<Checksum>[A-Za-z0-9]+)");
        final Matcher matcher = pattern.matcher(contentSpecData);
        String localStringChecksum = "";
        while (matcher.find()) {
            final String temp = matcher.group();
            localStringChecksum = temp.replaceAll("^CHECKSUM[ ]*=[ ]*", "");
        }

        // Calculate the local checksum value
        final String localChecksum = HashUtilities.generateMD5(contentSpecData.replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", ""));

        // Check that the checksums match
        if (!localStringChecksum.equals(localChecksum) && !localStringChecksum.equals(serverChecksum)) {
            printError(String.format(Constants.ERROR_LOCAL_COPY_AND_SERVER_UPDATED_MSG, fileName), false);
            shutdown(Constants.EXIT_OUT_OF_DATE);
        } else if (!localStringChecksum.equals(serverChecksum)) {
            printError(Constants.ERROR_OUT_OF_DATE_MSG, false);
            shutdown(Constants.EXIT_OUT_OF_DATE);
        } else if (!localChecksum.equals(serverChecksum)) {
            printError(Constants.ERROR_LOCAL_COPY_UPDATED_MSG, false);
            shutdown(Constants.EXIT_OUT_OF_DATE);
        } else {
            JCommander.getConsole().println(Constants.UP_TO_DATE_MSG);
        }
    }

    protected Integer getContentSpecId(String contentSpecFile) {
        final Pattern pattern = Pattern.compile("ID[ ]*=[ ]*(?<ID>[0-9]+)");
        final Matcher matcher = pattern.matcher(contentSpecFile);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group("ID"));
        } else {
            return null;
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

}
