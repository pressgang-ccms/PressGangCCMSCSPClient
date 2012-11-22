package com.redhat.contentspec.client.config;

import java.util.HashMap;
import java.util.Map;

public class ClientConfiguration
{
	private String rootDirectory = "";
	private String publicanBuildOptions = null;
	private String publicanPreviewFormat = null;
	private String publicanCommonContentDirectory = null;
	private String kojiHubUrl = null;
	private String publishCommand = null;
	
	private String defaultZanataProject = null;
	private String defaultZanataVersion = null;
	
	private Map<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();
	private Map<String, ZanataServerConfiguration> zanataServers = new HashMap<String, ZanataServerConfiguration>();
	
	private String installPath = null;

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

	public String getDefaultZanataVersion()
	{
		return defaultZanataVersion;
	}

	public void setDefaultZanataVersion(final String defaultZanataVersion)
	{
		this.defaultZanataVersion = defaultZanataVersion;
	}

	public String getDefaultZanataProject()
	{
		return defaultZanataProject;
	}

	public void setDefaultZanataProject(final String defaultZanataProject)
	{
		this.defaultZanataProject = defaultZanataProject;
	}

	public String getPublicanCommonContentDirectory()
	{
		return publicanCommonContentDirectory;
	}

	public void setPublicanCommonContentDirectory(final String publicanCommonContentDirectory)
	{
		this.publicanCommonContentDirectory = publicanCommonContentDirectory;
	}

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
