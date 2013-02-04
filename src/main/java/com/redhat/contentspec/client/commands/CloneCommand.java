package com.redhat.contentspec.client.commands;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;

@Parameters(commandDescription = "Clone a Content Specification to create a new Content Specification")
public class CloneCommand extends BaseCommandImpl {
    @Parameter(metaVar = "[ID]")
    private List<Integer> ids = new ArrayList<Integer>();

    public CloneCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public void printHelp() {
        printHelp(Constants.CLONE_COMMAND_NAME);
    }

    @Override
    public void printError(String errorMsg, boolean displayHelp) {
        printError(errorMsg, displayHelp, Constants.CLONE_COMMAND_NAME);
    }

    @Override
    public UserWrapper authenticate(DataProviderFactory providerFactory) {
        return authenticate(getUsername(), providerFactory);
    }

    @Override
    public void process(DataProviderFactory providerFactory, UserWrapper user) {
        // TODO

    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        return ids.size() == 0;
    }
}
