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
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(commandDescription = "Check the status of a local copy of a Content Specification compared to the server")
public class StatusCommand extends BaseCommandImpl {
    private static final Pattern ID_PATTERN = Pattern.compile("ID[ ]*=[ ]*(?<ID>[0-9]+)");

    @Parameter(metaVar = "[ID] or [FILE]")
    private List<String> ids = new ArrayList<String>();

    public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public String getCommandName() {
        return Constants.STATUS_COMMAND_NAME;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(final List<String> ids) {
        this.ids = ids;
    }

    @Override
    public void process() {
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Initialise the basic data and perform basic checks
        ClientUtilities.prepareAndValidateStringIds(this, getCspConfig(), getIds());

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the content specification from the server
        final String id = ids.get(0);
        final String fileName;
        ContentSpecWrapper contentSpec = null;
        String contentSpecString = null;
        if (id.matches("^\\d+$")) {
            final Integer intId = Integer.parseInt(id);
            contentSpec = contentSpecProvider.getContentSpec(intId, null);
            // Get the string version of the content spec from the server
            contentSpecString = contentSpecProvider.getContentSpecAsString(intId, null);
            if (contentSpec == null || contentSpecString == null) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
            }

            // Create the local file
            String tempFileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
            File tempFile = new File(tempFileName);

            // Check that the file exists
            if (!tempFile.exists()) {
                // Backwards compatibility check for files ending with .txt
                tempFile = new File(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post.txt");
                if (!tempFile.exists()) {
                    printErrorAndShutdown(Constants.EXIT_FAILURE,
                            String.format(Constants.ERROR_NO_FILE_OUT_OF_DATE_MSG, tempFileName), false);
                }
            }

            fileName = tempFileName;
        } else {
            fileName = id;
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Read in the file contents
        String contentSpecData = "";
        try {
            contentSpecData = FileUtils.readFileToString(new File(fileName));
        } catch (IOException e) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        if (contentSpecData.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        // If the content spec is null, than look up the ID from the file
        if (contentSpec == null) {
            final Integer intId = getContentSpecId(contentSpecData);
            contentSpec = contentSpecProvider.getContentSpec(intId, null);
            contentSpecString = contentSpecProvider.getContentSpecAsString(intId, null);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // At this point we should have a content spec topic, if not then shut down
        if (contentSpec == null || contentSpecString == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
        }

        // Calculate the server checksum valuess
        final String serverChecksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecString);

        // Read the local checksum value
        final String localStringChecksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecData);

        // Calculate the local checksum value
        contentSpecData = contentSpecData.replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", "");
        final String localChecksum = HashUtilities.generateMD5(contentSpecData);

        // Check that the checksums match
        if (!localStringChecksum.equals(localChecksum) && !localStringChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, String.format(Constants.ERROR_LOCAL_COPY_AND_SERVER_UPDATED_MSG, fileName),
                    false);
        } else if (!localStringChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, Constants.ERROR_OUT_OF_DATE_MSG, false);
        } else if (!localChecksum.equals(serverChecksum)) {
            printErrorAndShutdown(Constants.EXIT_OUT_OF_DATE, Constants.ERROR_LOCAL_COPY_UPDATED_MSG, false);
        } else {
            JCommander.getConsole().println(Constants.UP_TO_DATE_MSG);
        }
    }

    protected Integer getContentSpecId(String contentSpecFile) {
        final Matcher matcher = ID_PATTERN.matcher(contentSpecFile);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group("ID"));
        } else {
            return null;
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return true;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
