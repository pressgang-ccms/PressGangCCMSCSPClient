package com.redhat.contentspec.client.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTUserV1;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;

@Parameters(commandDescription = "Check the status of a local copy of a Content Specification compared to the server")
public class StatusCommand extends BaseCommandImpl
{	
	@Parameter(metaVar = "[ID]")
	private List<Integer> ids = new ArrayList<Integer>();

	public StatusCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
	{
		super(parser, cspConfig, clientConfig);
	}

	public List<Integer> getIds()
	{
		return ids;
	}

	public void setIds(final List<Integer> ids)
	{
		this.ids = ids;
	}

	@Override
	public void printHelp()
	{
		printHelp(Constants.STATUS_COMMAND_NAME);
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp)
	{
		printError(errorMsg, displayHelp, Constants.STATUS_COMMAND_NAME);
	}

	@Override
	public RESTUserV1 authenticate(final RESTReader reader) {
		return null;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		final RESTReader reader = restManager.getReader();
		
		// Load the data from the config data if no ids were specified
		if (loadFromCSProcessorCfg())
		{
			// Check that the config details are valid
			if (cspConfig != null && cspConfig.getContentSpecId() != null)
			{
				setIds(CollectionUtilities.toArrayList(cspConfig.getContentSpecId()));
			}
		}
		
		// Check that only one ID exists
		if (ids.size() == 0)
		{
			printError(Constants.ERROR_NO_ID_MSG, false);
			shutdown(Constants.EXIT_ARGUMENT_ERROR);
		}
		else if (ids.size() > 1)
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
		
		// Get the content specification from the server
		final RESTTopicV1 contentSpec = reader.getPostContentSpecById(ids.get(0), null);
		if (contentSpec == null)
		{
			printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
		
		// Create the local file
		final String fileName = DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post." + Constants.FILENAME_EXTENSION;
		File file = new File(fileName);
		
		// Check that the file exists
		if (!file.exists())
		{
			// Backwards compatibility check for files ending with .txt
			file = new File(DocBookUtilities.escapeTitle(contentSpec.getTitle()) + "-post.txt");
			if (!file.exists())
			{
				printError(String.format(Constants.ERROR_NO_FILE_OUT_OF_DATE_MSG, file.getName()), false);
				shutdown(Constants.EXIT_FAILURE);
			}
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Read in the file contents
		String contentSpecData = FileUtilities.readFileContents(file);
		
		if (contentSpecData == null  || contentSpecData.equals(""))
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
		
		// Calculate the server checksum value
		final String serverContentSpecData = contentSpec.getXml().replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", "");
		final String serverChecksum = HashUtilities.generateMD5(serverContentSpecData);
		
		// Get the local checksum value
		final NamedPattern pattern = NamedPattern.compile("CHECKSUM[ ]*=[ ]*(?<Checksum>[A-Za-z0-9]+)");
		final NamedMatcher matcher = pattern.matcher(contentSpecData);
		String localStringChecksum = "";
		while (matcher.find())
		{
			final String temp = matcher.group();
			localStringChecksum = temp.replaceAll("^CHECKSUM[ ]*=[ ]*", "");
		}
		
		// Calculate the local checksum value
		contentSpecData = contentSpecData.replaceFirst("CHECKSUM[ ]*=.*(\r)?\n", "");
		final String localChecksum = HashUtilities.generateMD5(contentSpecData);
		
		// Check that the checksums match
		if (!localStringChecksum.equals(localChecksum) && !localStringChecksum.equals(serverChecksum))
		{
			printError(String.format(Constants.ERROR_LOCAL_COPY_AND_SERVER_UPDATED_MSG, fileName), false);
			shutdown(Constants.EXIT_OUT_OF_DATE);
		}
		else if (!localStringChecksum.equals(serverChecksum))
		{
			printError(Constants.ERROR_OUT_OF_DATE_MSG, false);
			shutdown(Constants.EXIT_OUT_OF_DATE);
		}
		else if (!localChecksum.equals(serverChecksum))
		{
			printError(Constants.ERROR_LOCAL_COPY_UPDATED_MSG, false);
			shutdown(Constants.EXIT_OUT_OF_DATE);
		}
		else
		{
			JCommander.getConsole().println(Constants.UP_TO_DATE_MSG);
		}
	}

	@Override
	public boolean loadFromCSProcessorCfg()
	{
		return ids.size() == 0;
	}

}
