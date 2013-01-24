package com.redhat.contentspec.client.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
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

@Parameters(commandDescription = "Get the checksum value for a Content Specification")
public class ChecksumCommand extends BaseCommandImplWithIds {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public ChecksumCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.CHECKSUM_COMMAND_NAME;
    }

    @Override
    public List<Integer> getIds() {
        return ids;
    }

    @Override
    public void setIds(List<Integer> ids) {
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

        // Get the content spec from the server
        final ContentSpecWrapper contentSpecEntity = providerFactory.getProvider(ContentSpecProvider.class).getContentSpec(ids.get(0),
                null);

        // Transform the content spec
        final ContentSpec contentSpec = ClientUtilities.transformContentSpec(contentSpecEntity);

        // Check that that content specification was found
        if (contentSpecEntity == null) {
            printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
            shutdown(Constants.EXIT_FAILURE);
        }

        // Calculate and print the checksum value
        final String checksum = ContentSpecUtilities.getContentSpecChecksum(contentSpec);
        JCommander.getConsole().println("CHECKSUM=" + checksum);
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }
}
