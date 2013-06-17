package org.jboss.pressgang.ccms.contentspec.client.converter;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;

public class FileConverter implements IStringConverter<File>
{
	@Override
	public File convert(final String value)
	{
		return new File(ClientUtilities.validateFilePath(value));
	}

}
