package com.redhat.contentspec.client.config;

import java.util.HashMap;
import java.util.Map;

import com.redhat.contentspec.client.constants.Constants;

public class ClientConfiguration
{
	private String rootDirectory = "";
	private String publicanBuildOptions = Constants.DEFAULT_PUBLICAN_OPTIONS;
	private String publicanPreviewFormat = Constants.DEFAULT_PUBLICAN_FORMAT;
	private String kojiHubUrl = Constants.DEFAULT_KOJIHUB_URL;
	private String publishCommand = Constants.DEFAULT_PUBLISH_COMMAND;
	
	private Map<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();
	private Map<String, ZanataServerConfiguration> zanataServers = new HashMap<String, ZanataServerConfiguration>();

	public Map<String, ServerConfiguration> getServers()
	{
		return servers;
	}

	public void setServers(final Map<String, ServerConfiguration> servers)
	{
		this.servers = servers;
	}
	
	public String getRootDirectory()
	{
		return rootDirectory;
	}

	public void setRootDirectory(final String rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	public String getPublicanBuildOptions()
	{
		return publicanBuildOptions;
	}

	public void setPublicanBuildOptions(final String publicanBuildOptions)
	{
		this.publicanBuildOptions = publicanBuildOptions;
	}

	public String getPublicanPreviewFormat()
	{
		return publicanPreviewFormat;
	}

	public void setPublicanPreviewFormat(final String publicanPreviewFormat)
	{
		this.publicanPreviewFormat = publicanPreviewFormat;
	}

	public Map<String, ZanataServerConfiguration> getZanataServers()
	{
		return zanataServers;
	}

	public void setZanataServers(Map<String, ZanataServerConfiguration> zanataServers)
	{
		this.zanataServers = zanataServers;
	}

	public String getKojiHubUrl()
	{
		return kojiHubUrl;
	}

	public void setKojiHubUrl(final String kojiHubUrl)
	{
		this.kojiHubUrl = kojiHubUrl;
	}

	public String getPublishCommand()
	{
		return publishCommand;
	}

	public void setPublishCommand(final String publishCommand)
	{
		this.publishCommand = publishCommand;
	}
}
