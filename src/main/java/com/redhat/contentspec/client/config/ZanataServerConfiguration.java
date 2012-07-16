package com.redhat.contentspec.client.config;

public class ZanataServerConfiguration
{
	private String name;
	private String url;
	private String username;
	private String token;
	
	public ZanataServerConfiguration()
	{
		
	}
	
	public ZanataServerConfiguration(final String name, final String url)
	{
		this.name = name;
		this.url = url;
	}
	
	public ZanataServerConfiguration(final String name, final String url, final String username, final String token)
	{
		this.name = name;
		this.url = url;
		this.username = username;
		this.token = token;
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

	public String getToken()
	{
		return token;
	}

	public void setToken(final String token)
	{
		this.token = token;
	}
}
