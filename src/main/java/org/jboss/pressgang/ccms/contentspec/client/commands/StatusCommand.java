package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;

@Parameters(commandDescription = "Check the status of a local copy of a Content Specification compared to the server")
public class StatusCommand extends BaseCommandImpl {

    public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public String getCommandName() {
        return Constants.STATUS_COMMAND_NAME;
    }

    @Override
    // TODO Port through the fix to allow this command to work on any file
    public void process() {
        final List<Integer> ids = new ArrayList<Integer>();
        final ContentSpecProvider contentSpecProvider = getProviderFactory().getProvider(ContentSpecProvider.class);

        // Initialise the basic data and perform basic checks
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), ids);

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the content specification from the server
        final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(ids.get(0), null);
        // Get the string version of the content spec from the server
        final String contentSpecString = contentSpecProvider.getContentSpecAsString(ids.get(0), null);
        if (contentSpecEntity == null || contentSpecString == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
        }

        // Create the local file
        final String fileName = DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
        File file = new File(fileName);

        // Check that the file exists
        if (!file.exists()) {
            // Backwards compatibility check for files ending with .txt
            final File oldFile = new File(DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post.txt");
            if (!oldFile.exists()) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, String.format(Constants.ERROR_NO_FILE_OUT_OF_DATE_MSG, file.getName()),
                        false);
            } else {
                file = oldFile;
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Read in the file contents
        String contentSpecData = FileUtilities.readFileContents(file);

        if (contentSpecData.equals("")) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_EMPTY_FILE_MSG, false);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

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

    @Override
    public boolean loadFromCSProcessorCfg() {
        return true;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
