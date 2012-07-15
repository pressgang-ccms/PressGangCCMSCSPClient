package com.redhat.contentspec.client.converter;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.redhat.contentspec.client.utils.ClientUtilities;

public class FileConverter implements IStringConverter<File>
{
	@Override
	public File convert(final String value)
	{
		return new File(ClientUtilities.validateFilePath(value));
	}

}
