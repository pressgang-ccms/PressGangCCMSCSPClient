package org.jboss.pressgang.ccms.contentspec.client.commands;

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
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;

@Parameters(commandDescription = "Get the checksum value for a Content Specification")
public class ChecksumCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public ChecksumCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.CHECKSUM_COMMAND_NAME;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public void process() {
        // Initialise the basic data and perform basic checks
        ClientUtilities.prepareAndValidateIds(this, getCspConfig(), getIds());

        // Get the content spec from the server
        final String contentSpecString = getProviderFactory().getProvider(ContentSpecProvider.class).getContentSpecAsString(ids.get(0),
                null);

        // Check that that content specification was found
        if (contentSpecString == null) {
            printErrorAndShutdown(Constants.EXIT_FAILURE, Constants.ERROR_NO_ID_FOUND_MSG, false);
        }

        // Calculate and print the checksum value
        final String checksum = ContentSpecUtilities.getContentSpecChecksum(contentSpecString);
        JCommander.getConsole().println("CHECKSUM=" + checksum);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }

    @Override
    public boolean requiresExternalConnection() {
        return true;
    }
}
