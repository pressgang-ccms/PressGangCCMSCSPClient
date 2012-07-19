package com.redhat.contentspec.client.commands;

import com.redhat.contentspec.interfaces.ShutdownAbleApp;
import com.redhat.contentspec.rest.RESTManager;
import com.redhat.contentspec.rest.RESTReader;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.topicindex.rest.entities.interfaces.RESTUserV1;

public interface BaseCommand extends ShutdownAbleApp
{
	String getUsername();
	void setUsername(String username);
	String getServerUrl();
	String getSkynetServerUrl();
	void setServerUrl(String serverUrl);
	Boolean isShowHelp();
	void setShowHelp(Boolean showHelp);
	String getConfigLocation();
	void setConfigLocation(String configLocation);
	Boolean isShowVersion();
	void setShowVersion(Boolean showVersion);
	boolean isAppShuttingDown();
	void setAppShuttingDown(boolean shuttingDown);
	void setShutdown(boolean shutdown);
	
	/**
	 * Print the available options to the console
	 * for the command.
	 */
	void printHelp();
	
	/**
	 * Print an error message to the console. If the
	 * display help parameter is set then the commands
	 * options are printed as well.
	 * 
	 * @param errorMsg The error message to display.
	 * @param displayHelp If the commands options should be
	 * displayed.
	 */
	void printError(String errorMsg, boolean displayHelp);
	RESTUserV1 authenticate(RESTReader reader);
	void process(RESTManager restManager, ErrorLoggerManager elm, RESTUserV1 user);
	
	/**
	 * Check to see if the command should load data from
	 * a local csprocessor.cfg configuration file.
	 * 
	 * @return True if the data from the local csprocessor.cfg
	 * should be loaded, otherwise false.
	 */
	boolean loadFromCSProcessorCfg();
	void validateServerUrl();
}