package com.redhat.contentspec.client.commands.base;

import org.jboss.pressgangccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgangccms.contentspec.rest.RESTManager;
import org.jboss.pressgangccms.contentspec.rest.RESTReader;
import org.jboss.pressgangccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgangccms.rest.v1.entities.RESTUserV1;

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
	
	/**
	 * Do the main process working involved in running the
	 * command by using the command line arguments and 
	 * configuration files to do the command actions.
	 * 
	 * @param restManager
	 * 				The REST manager containing the client
	 * 				and actions to be used to access the 
	 * 				REST Interface.
	 * @param elm
	 * 				The Error Logging Manager used to store 
	 * 				logs and log messages.
	 * @param user
	 * 				The user to perform the actions for.
	 */
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
