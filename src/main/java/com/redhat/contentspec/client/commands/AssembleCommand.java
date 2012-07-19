package com.redhat.contentspec.client.commands;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.client.utils.ZipUtilities;
import com.redhat.contentspec.processor.ContentSpecParser;
import com.redhat.contentspec.rest.RESTManager;
import com.redhat.contentspec.rest.RESTReader;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.ecs.commonutils.DocBookUtilities;
import com.redhat.topicindex.rest.entities.interfaces.RESTUserV1;
import com.redhat.topicindex.rest.entities.interfaces.RESTTopicV1;

@Parameters(commandDescription = "Builds and Assembles a Content Specification so that it is ready to be previewed")
public class AssembleCommand extends BuildCommand {
	
	@Parameter(names = Constants.NO_BUILD_LONG_PARAM, description = "Don't build the Content Specification.")
	private Boolean noBuild = false;

	@Parameter(names = Constants.HIDE_OUTPUT_LONG_PARAM, description = "Hide the output from assembling the Content Specification.")
	private Boolean hideOutput = false;
		
	public AssembleCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
	{
		super(parser, cspConfig, clientConfig);
	}
	
	public Boolean getNoBuild()
	{
		return noBuild;
	}

	public void setNoBuild(final Boolean noBuild)
	{
		this.noBuild = noBuild;
	}

	public Boolean getHideOutput()
	{
		return hideOutput;
	}

	public void setHideOutput(Boolean hideOutput)
	{
		this.hideOutput = hideOutput;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		final RESTReader reader = restManager.getReader();
		final boolean assembleFromConfig = loadFromCSProcessorCfg();
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		if (!noBuild)
		{
			super.process(restManager, elm, user);
			if (isShutdown()) return;
		}
		
		JCommander.getConsole().println(Constants.STARTING_ASSEMBLE_MSG);
		
		String fileDirectory = "";
		String outputDirectory = "";
		String fileName = null;
		if (assembleFromConfig)
		{
			final RESTTopicV1 contentSpec = restManager.getReader().getContentSpecById(cspConfig.getContentSpecId(), null);
			
			// Check that that content specification was found
			if (contentSpec == null || contentSpec.getXml() == null) 
			{
				printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
				shutdown(Constants.EXIT_FAILURE);
			}
			
			final String rootDir = (cspConfig.getRootOutputDirectory() == null || cspConfig.getRootOutputDirectory().equals("") ? "" : (cspConfig.getRootOutputDirectory() + DocBookUtilities.escapeTitle(contentSpec.getTitle()) + File.separator));
			
			fileDirectory = rootDir + Constants.DEFAULT_CONFIG_ZIP_LOCATION;
			outputDirectory = rootDir + Constants.DEFAULT_CONFIG_PUBLICAN_LOCATION;
			fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-publican.zip";
		}
		else if (getIds() != null && getIds().size() == 1)
		{
			final String contentSpec = getContentSpecString(reader, getIds().get(0));
			
			/* parse the spec to get the main details */
			final ContentSpecParser csp = new ContentSpecParser(elm, restManager);
			try
			{
				csp.parse(contentSpec);
			}
			catch (Exception e)
			{
				printError(Constants.ERROR_INTERNAL_ERROR, false);
				shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
			}
			
			// Create the fully qualified output path
			if (getOutputPath() != null && getOutputPath().endsWith("/"))
			{
				outputDirectory = this.getOutputPath();
				fileDirectory = this.getOutputPath();
				fileName = DocBookUtilities.escapeTitle(csp.getContentSpec().getTitle()) + ".zip";
			}
			else if (getOutputPath() == null)
			{
				outputDirectory = DocBookUtilities.escapeTitle(csp.getContentSpec().getTitle());
				fileName = DocBookUtilities.escapeTitle(csp.getContentSpec().getTitle()) + ".zip";
			}
			else
			{
				fileName = this.getOutputPath();
			}
		}
		else if (getIds().size() == 0)
		{
			printError(Constants.ERROR_NO_ID_MSG, false);
			shutdown(Constants.EXIT_ARGUMENT_ERROR);
		}
		else
		{
			printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
			shutdown(Constants.EXIT_ARGUMENT_ERROR);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		File file = new File(fileDirectory + fileName);
		if (!file.exists())
		{
			printError(String.format(Constants.ERROR_UNABLE_TO_FIND_ZIP_MSG, fileName), false);
			shutdown(Constants.EXIT_FAILURE);
		}
		
		// Make sure the output directories exist
		File outputDir = new File(outputDirectory);
		outputDir.mkdirs();
		
		// Ensure that the directory is empty
		ClientUtilities.deleteDirContents(outputDir);
		
		// Unzip the file
		if (!ZipUtilities.unzipFileIntoDirectory(file, outputDirectory))
		{
			printError(Constants.ERROR_FAILED_TO_ASSEMBLE_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
		else
		{
			JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_UNZIP_MSG, outputDir.getAbsolutePath()));
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		String publicanOptions = clientConfig.getPublicanBuildOptions();
		
		// Replace the locale in the build options if the locale has been set
		if (getLocale() != null)
			publicanOptions = publicanOptions.replaceAll("--lang(s)?=[A-Za-z\\-,]+", "--lang=" + getLocale());
		
		try
		{
			JCommander.getConsole().println(Constants.STARTING_PUBLICAN_BUILD_MSG);
			Integer exitValue = ClientUtilities.runCommand("publican build " + publicanOptions, Constants.PUBLICAN_ENV_VARS, outputDir, JCommander.getConsole(), !hideOutput);
			if (exitValue == null || exitValue != 0)
			{
				shutdown(Constants.EXIT_FAILURE);
			}
		}
		catch (IOException e)
		{
			printError(Constants.ERROR_RUNNING_PUBLICAN_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
		JCommander.getConsole().println(String.format(Constants.SUCCESSFUL_ASSEMBLE_MSG, outputDir.getAbsolutePath()));
	}
	
	@Override
	public void printError(final String errorMsg, final boolean displayHelp)
	{
		printError(errorMsg, displayHelp, Constants.ASSEMBLE_COMMAND_NAME);
	}
	
	@Override
	public void printHelp()
	{
		printHelp(Constants.ASSEMBLE_COMMAND_NAME);
	}
	
	@Override
	public RESTUserV1 authenticate(final RESTReader reader)
	{
		return noBuild ? null : authenticate(getUsername(), reader);
	}
}