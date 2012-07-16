package com.redhat.contentspec.client.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.rest.RESTManager;
import com.redhat.contentspec.rest.RESTReader;
import com.redhat.contentspec.utils.logging.ErrorLoggerManager;
import com.redhat.ecs.commonutils.DocBookUtilities;
import com.redhat.topicindex.rest.entities.interfaces.RESTUserV1;
import com.redhat.topicindex.rest.entities.interfaces.RESTTopicV1;
import com.redhat.topicindex.zanata.ZanataDetails;

@Parameters(commandDescription = "Checkout an existing Content Specification from the server")
public class CheckoutCommand extends BaseCommandImpl
{
	@Parameter(metaVar = "[ID]")
	private List<Integer> ids = new ArrayList<Integer>();
	
	@Parameter(names = {Constants.FORCE_LONG_PARAM, Constants.FORCE_SHORT_PARAM}, description = "Force the Content Specification directories to be created.")
	private Boolean force = false;
	
	@Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM, description = "The zanata server to be associated with the Content Specification.")
	private String zanataUrl = null;
	
	@Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM, description = "The zanata project name to be associated with the Content Specification.")
	private String zanataProject = null;
	
	@Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM, description = "The zanata project version to be associated with the Content Specification.")
	private String zanataVersion = null;

	public CheckoutCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
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
	
	public String getZanataUrl()
	{
		return zanataUrl;
	}

	public void setZanataUrl(final String zanataUrl)
	{
		this.zanataUrl = zanataUrl;
	}

	public String getZanataProject()
	{
		return zanataProject;
	}

	public void setZanataProject(final String zanataProject)
	{
		this.zanataProject = zanataProject;
	}

	public String getZanataVersion()
	{
		return zanataVersion;
	}

	public void setZanataVersion(final String zanataVersion)
	{
		this.zanataVersion = zanataVersion;
	}

	public Boolean getForce()
	{
		return force;
	}

	public void setForce(final Boolean force)
	{
		this.force = force;
	}

	@Override
	public void printHelp()
	{
		printHelp(Constants.CHECKOUT_COMMAND_NAME);
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp)
	{
		printError(errorMsg, displayHelp, Constants.CHECKOUT_COMMAND_NAME);
	}

	@Override
	public RESTUserV1 authenticate(final RESTReader reader)
	{
		return null;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		// Check that an ID was entered
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
		
		// Get the content spec from the server
		final RESTTopicV1 contentSpec = restManager.getReader().getPostContentSpecById(ids.get(0), null);
		if (contentSpec == null || contentSpec.getXml() == null)
		{
			printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
			
		// Check that the output directory doesn't already exist
		final File directory = new File(cspConfig.getRootOutputDirectory() + DocBookUtilities.escapeTitle(contentSpec.getTitle()));
		if (directory.exists() && !force)
		{
			printError(String.format(Constants.ERROR_CONTENT_SPEC_EXISTS_MSG, directory.getAbsolutePath()), false);
			shutdown(Constants.EXIT_FAILURE);
		// If it exists and force is enabled delete the directory contents
		}
		else if (directory.exists())
		{
			ClientUtilities.deleteDir(directory);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Find the zanata server if the url is a reference to the zanata server name
		for (final String serverName: clientConfig.getZanataServers().keySet())
		{
			if (serverName.equals(zanataUrl))
			{
				zanataUrl = clientConfig.getZanataServers().get(serverName).getUrl();
				break;
			}
		}
		
		// Create the zanata details from the command line options
		final ZanataDetails zanataDetails = new ZanataDetails();
		zanataDetails.setServer(ClientUtilities.validateHost(zanataUrl));
		zanataDetails.setProject(zanataProject);
		zanataDetails.setVersion(zanataVersion);
		
		// Save the csprocessor.cfg and post spec to file if the create was successful
		final String escapedTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
		final File outputSpec = new File(cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + escapedTitle + "-post." + Constants.FILENAME_EXTENSION);
		final File outputConfig = new File(cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + "csprocessor.cfg");
		final String config = ClientUtilities.generateCsprocessorCfg(contentSpec, getServerUrl(), clientConfig, zanataDetails);
		
		// Create the directory
		if (outputConfig.getParentFile() != null)
			outputConfig.getParentFile().mkdirs();
		
		boolean error = false;
		
		// Save the csprocessor.cfg
		try
		{
			final FileOutputStream fos = new FileOutputStream(outputConfig);
			fos.write(config.getBytes());
			fos.flush();
			fos.close();
			JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputConfig.getAbsolutePath()));
		} 
		catch (IOException e)
		{
			printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputConfig.getAbsolutePath()), false);
			error = true;
		}
		
		// Save the Post Processed spec
		try
		{
			final FileOutputStream fos = new FileOutputStream(outputSpec);
			fos.write(contentSpec.getXml().getBytes());
			fos.flush();
			fos.close();
			JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputSpec.getAbsolutePath()));
		}
		catch (IOException e)
		{
			printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()), false);
			error = false;
		}
		
		if (error) 
		{
			shutdown(Constants.EXIT_FAILURE);
		}
	}

	@Override
	public boolean loadFromCSProcessorCfg()
	{
		/* Never load from a cspconfig when checking out */
		return false;
	}
}
