package com.redhat.contentspec.client.config;

import com.redhat.topicindex.zanata.ZanataDetails;

public class ContentSpecConfiguration
{
	private String serverUrl;
	private Integer contentSpecId;
	private String rootOutputDir;
	private ZanataDetails zanataDetails = new ZanataDetails();
	
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
}
