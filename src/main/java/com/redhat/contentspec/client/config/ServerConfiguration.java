package com.redhat.contentspec.client.config;

public class ServerConfiguration
{
	private String name;
	private String url;
	private String username;
	
	public ServerConfiguration()
	{
		
	}
	
	public ServerConfiguration(final String name, final String url)
	{
		this.name = name;
		this.url = url;
	}
	
	public ServerConfiguration(final String name, final String url, final String username)
	{
		this.name = name;
		this.url = url;
		this.username = username;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(final String name)
	{
		this.name = name;
	}
	
	public String getUrl()
	{
		return url;
	}
	public void setUrl(final String url)
	{
		this.url = url;
	}
	
	public String getUsername()
	{
		return username;
	}
	public void setUsername(final String username)
	{
		this.username = username;
	}
}
