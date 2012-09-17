package com.redhat.contentspec.client.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jboss.pressgangccms.contentspec.constants.TemplateConstants;
import org.jboss.pressgangccms.contentspec.rest.RESTManager;
import org.jboss.pressgangccms.contentspec.rest.RESTReader;
import org.jboss.pressgangccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgangccms.rest.v1.entities.RESTUserV1;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.converter.FileConverter;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;

@Parameters(commandDescription = "Get a basic Content Specification template.")
public class TemplateCommand extends BaseCommandImpl
{
	@Parameter(names = Constants.COMMENTED_LONG_PARAM, description = "Get the fully commented template")
	private Boolean commented = false;
	
	@Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM}, description = "Save the output to the specified file/directory.", metaVar = "<FILE>",
			converter = FileConverter.class)
	private File output;
	
	public TemplateCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
	{
		super(parser, cspConfig, clientConfig);
	}

	public Boolean getCommented()
	{
		return commented;
	}

	public void setCommented(final Boolean commented)
	{
		this.commented = commented;
	}

	public File getOutput()
	{
		return output;
	}

	public void setOutput(File output)
	{
		this.output = output;
	}

	@Override
	public void printHelp()
	{
		printHelp(Constants.TEMPLATE_COMMAND_NAME);
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp) 
	{
		printError(errorMsg, displayHelp, Constants.TEMPLATE_COMMAND_NAME);
	}

	@Override
	public RESTUserV1 authenticate(final RESTReader reader)
	{
		return null;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		final String template = commented ? TemplateConstants.FULLY_COMMENTED_TEMPLATE : TemplateConstants.EMPTY_TEMPLATE;
		
		// Save or print the data
		if (output == null)
		{
			JCommander.getConsole().println(template);
		}
		else
		{
			// Make sure the directories exist
			if (output.isDirectory())
			{
				output.mkdirs();
				output = new File(output.getAbsolutePath() + File.separator + "template." + Constants.FILENAME_EXTENSION);
			}
			else
			{
				if (output.getParentFile() != null)
					output.getParentFile().mkdirs();
			}
			
			// Good point to check for a shutdown
			if (isAppShuttingDown())
			{
				shutdown.set(true);
				return;
			}
			
			// Create and write to the file
			try
			{
				final FileOutputStream fos = new FileOutputStream(output);
				fos.write(template.getBytes("UTF-8"));
				fos.flush();
				fos.close();
				JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, output.getAbsolutePath()));
			}
			catch (IOException e)
			{
				printError(Constants.ERROR_FAILED_SAVING, false);
				shutdown(Constants.EXIT_FAILURE);
			}
		}
	}

	@Override
	public boolean loadFromCSProcessorCfg()
	{
		/* Doesn't need an ID so no point in loading from csprocessor.cfg */
		return false;
	}

}
