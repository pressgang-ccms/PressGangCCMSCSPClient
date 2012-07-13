package com.redhat.contentspec.client.config;

import com.redhat.topicindex.zanata.ZanataDetails;

public class ContentSpecConfiguration
{
	private String serverUrl = null;
	private Integer contentSpecId = null;
	private String rootOutputDir = null;
	private ZanataDetails zanataDetails = new ZanataDetails();
	private String kojiHubUrl = null;
	
	public Integer getContentSpecId()
	{
		return contentSpecId;
	}
	
	public void setContentSpecId(final Integer contentSpecId)
	{
		this.contentSpecId = contentSpecId;
	}
	
	public String getServerUrl()
	{
		return serverUrl;
	}
	
	public void setServerUrl(final String serverUrl)
	{
		this.serverUrl = serverUrl;
	}

	public String getRootOutputDirectory()
	{
		return rootOutputDir;
	}

	public void setRootOutputDirectory(final String rootOutputDir)
	{
		this.rootOutputDir = rootOutputDir;
	}

	public ZanataDetails getZanataDetails()
	{
		return zanataDetails;
	}

	public void setZanataDetails(final ZanataDetails zanataDetails)
	{
		this.zanataDetails = zanataDetails;
	}

	public String getKojiHubUrl()
	{
		return kojiHubUrl;
	}

	public void setKojiHubUrl(final String kojiHubUrl)
	{
		this.kojiHubUrl = kojiHubUrl;
	}
}
