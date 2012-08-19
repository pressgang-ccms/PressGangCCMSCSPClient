package com.redhat.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgangccms.contentspec.rest.RESTManager;
import org.jboss.pressgangccms.contentspec.rest.RESTReader;
import org.jboss.pressgangccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgangccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgangccms.rest.v1.entities.RESTUserV1;
import org.jboss.pressgangccms.utils.common.DocBookUtilities;
import org.jboss.pressgangccms.utils.common.FileUtilities;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.converter.FileConverter;
import com.redhat.contentspec.processor.ContentSpecParser;
import com.redhat.contentspec.processor.ContentSpecProcessor;
import com.redhat.contentspec.processor.structures.ProcessingOptions;

@Parameters(commandDescription = "Validate a Content Specification")
public class ValidateCommand extends BaseCommandImpl
{
	@Parameter(converter = FileConverter.class, metaVar = "[FILE]")
	private List<File> files = new ArrayList<File>();
	
	@Parameter(names = {Constants.PERMISSIVE_LONG_PARAM, Constants.PERMISSIVE_SHORT_PARAM}, description = "Turn on permissive processing.")
	private Boolean permissive = false;
	
	private ContentSpecProcessor csp = null;
	
	public ValidateCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
	{
		super(parser, cspConfig, clientConfig);
	}

	public List<File> getFiles()
	{
		return files;
	}

	public void setFiles(final List<File> files)
	{
		this.files = files;
	}

	public Boolean getPermissive()
	{
		return permissive;
	}

	public void setPermissive(final Boolean permissive)
	{
		this.permissive = permissive;
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp)
	{
		printError(errorMsg, displayHelp, Constants.VALIDATE_COMMAND_NAME);
	}

	@Override
	public void printHelp()
	{
		printHelp(Constants.VALIDATE_COMMAND_NAME);
	}
	
	@Override
	public RESTUserV1 authenticate(final RESTReader reader)
	{
		return authenticate(getUsername(), reader);
	}
	
	public boolean isValid()
	{
		// We should have only one file
		if (files.size() != 1)
			return false;
		
		// Check that the file exists
		final File file = files.get(0);
		if (file.isDirectory() || !file.exists() || !file.isFile())
			return false;
		
		return true;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		// If files is empty then we must be using a csprocessor.cfg file
		if (loadFromCSProcessorCfg())
		{
			// Check that the config details are valid
			if (cspConfig != null && cspConfig.getContentSpecId() != null)
			{
				final RESTTopicV1 contentSpec = restManager.getReader().getContentSpecById(cspConfig.getContentSpecId(), null);
				final String fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
				File file = new File(fileName);
				if (!file.exists())
				{
					// Backwards compatibility check for files ending with .txt
					file = new File(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post.txt");
					if (!file.exists())
					{
						printError(String.format(Constants.NO_FILE_FOUND_FOR_CONFIG, fileName), false);
						shutdown(Constants.EXIT_FAILURE);
					}
				}
				files.add(file);
			}
		}
		
		// Check that the parameters are valid
		if (!isValid())
		{
			printError(Constants.ERROR_NO_FILE_MSG, true);
			shutdown(Constants.EXIT_FAILURE);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		boolean success = false;
		
		// Read in the file contents
		final String contentSpec = FileUtilities.readFileContents(files.get(0));
		
		if (contentSpec == null  || contentSpec.equals(""))
		{
			printError(Constants.ERROR_EMPTY_FILE_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Setup the processing options
		final ProcessingOptions processingOptions = new ProcessingOptions();
		processingOptions.setPermissiveMode(permissive);
		processingOptions.setValidating(true);
		processingOptions.setAllowEmptyLevels(true);
		
		// Process the content spec to see if its valid
		csp = new ContentSpecProcessor(restManager, elm, processingOptions);
		try
		{
			success = csp.processContentSpec(contentSpec, user, ContentSpecParser.ParsingMode.EITHER);
		}
		catch (Exception e)
		{
			printError(Constants.ERROR_INTERNAL_ERROR, false);
			shutdown(Constants.EXIT_INTERNAL_SERVER_ERROR);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Print the logs
		JCommander.getConsole().println(elm.generateLogs());
		if (success)
		{
			JCommander.getConsole().println("VALID");
		}
		else
		{
			JCommander.getConsole().println("INVALID");
			JCommander.getConsole().println("");
			shutdown(Constants.EXIT_TOPIC_INVALID);
		}
	}
	
	@Override
	public void shutdown()
	{
		super.shutdown();
		if (csp != null) {
			csp.shutdown();
		}
	}

	@Override
	public boolean loadFromCSProcessorCfg()
	{
		return files.size() == 0;
	}
}
