package com.redhat.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.code.regexp.NamedMatcher;
import com.redhat.contentspec.client.commands.base.BaseCommandImplWithIds;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;

@Parameters(commandDescription = "Check the status of a local copy of a Content Specification compared to the server")
public class StatusCommand extends BaseCommandImplWithIds {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.STATUS_COMMAND_NAME;
    }

    @Override
    public List<Integer> getIds() {
        return ids;
    }

    @Override
    public void setIds(final List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public UserWrapper authenticate(final DataProviderFactory providerFactory) {
        return null;
    }

    @Override
    public void process(final DataProviderFactory providerFactory, final UserWrapper user) {
        // Initialise the basic data and perform basic checks
        prepare();

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Get the content specification from the server
        final ContentSpecWrapper contentSpecEntity = providerFactory.getProvider(ContentSpecProvider
                .class).getContentSpec(ids.get(0), null);
        if (contentSpecEntity == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Create the local file
        final String fileName = DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
        File file = new File(fileName);

        // Check that the file exists
        if (!file.exists()) {
            // Backwards compatibility check for files ending with .txt
            file = new File(DocBookUtilities.escapeTitle(contentSpecEntity.getTitle()) + "-post.txt");
            if (!file.exists()) {
                printError(String.format(Constants.ERROR_NO_FILE_OUT_OF_DATE_MSG, file.getName()), false);
                shutdown(Constants.EXIT_FAILURE);
            }
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Read in the file contents
        String contentSpecData = FileUtilities.readFileContents(file);

        if (contentSpecData == null || contentSpecData.equals("")) {
            printError(Constants.ERROR_EMPTY_FILE_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Good point to check for a shutdown
        allowShutdownToContinueIfRequested();

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);

        // Calculate the server checksum value
        final String serverChecksum = ContentSpecUtilities.getContentSpecChecksum(contentSpec);

        // Read the local checksum value
        final NamedMatcher matcher = ContentSpecUtilities.CS_CHECKSUM_PATTERN.matcher(contentSpecData);
        String localStringChecksum = "";
        while (matcher.find()) {
            final String temp = matcher.group();
            localStringChecksum = temp.replaceAll("^CHECKSUM[ ]*=[ ]*", "");
        }

        // Calculate the local checksum value
        contentSpecData = contentSpecData.replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", "");
        final String localChecksum = HashUtilities.generateMD5(contentSpecData);

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

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

}
