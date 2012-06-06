package com.redhat.contentspec.client.commands;

import com.redhat.contentspec.interfaces.ShutdownAbleApp;
import com.redhat.contentspec.rest.RESTManager;
import com.redhat.contentspec.rest.RESTReader;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.topicindex.rest.entities.interfaces.RESTUserV1;

public interface BaseCommand extends ShutdownAbleApp {

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
	
	void printHelp();
	void printError(String errorMsg, boolean displayHelp);
	RESTUserV1 authenticate(RESTReader reader);
	void process(RESTManager restManager, ErrorLoggerManager elm, RESTUserV1 user);
	boolean loadFromCSProcessorCfg();
}
