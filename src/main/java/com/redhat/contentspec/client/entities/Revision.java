package com.redhat.contentspec.client.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Revision
{
	
	private Integer id = 0;
	private Date date = null;
	private String type = null;
	
	public Revision(final Integer id, final Date date, final String type)
	{
		this.id = id;
		this.date = date;
		this.setType(type);
	}
	
	public Revision()
	{
		
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(final Integer id)
	{
		this.id = id;
	}
	
	public Date getDate()
	{
		return date;
	}

	public void setDate(final Date date)
	{
		this.date = date;
	}
	
	public String getType()
	{
		return type;
	}

	public void setType(final String type)
	{
		this.type = type;
	}

	public String toString()
	{
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm:ss a");
		if (type == null || type.isEmpty())
		{
			return String.format("-> ID: %6d on %s", id, dateFormatter.format(date));
		}
		else
		{
			return String.format("-> ID: %6d [%14s] on %s", id, type, dateFormatter.format(date));
		}
	}
}
