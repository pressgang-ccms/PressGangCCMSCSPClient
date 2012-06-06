package com.redhat.contentspec.client.commands;

import com.beust.jcommander.JCommander;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.rest.RESTManager;
import com.redhat.contentspec.rest.RESTReader;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.topicindex.rest.entities.interfaces.RESTUserV1;

public class SnapshotCommand extends BaseCommandImpl {

	public SnapshotCommand(final JCommander parser, final ContentSpecConfiguration cspConfig) {
		super(parser, cspConfig);
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp) {
		printError(errorMsg, displayHelp, Constants.SNAPSHOT_COMMAND_NAME);
	}

	@Override
	public void printHelp() {
		printHelp(Constants.SNAPSHOT_COMMAND_NAME);
	}
	
	@Override
	public RESTUserV1 authenticate(final RESTReader reader) {
		return authenticate(getUsername(), reader);
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		printError("Snapshots aren't currently supported", false);
		// TODO Add the ability to create snapshots at a later stage
	}

	@Override
	public boolean loadFromCSProcessorCfg() {
		// TODO Auto-generated method stub
		return false;
	}
}
