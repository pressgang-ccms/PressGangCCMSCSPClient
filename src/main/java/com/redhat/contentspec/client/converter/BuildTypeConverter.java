package com.redhat.contentspec.client.converter;

import com.beust.jcommander.IStringConverter;
import com.redhat.contentspec.builder.BuildType;

public class BuildTypeConverter implements IStringConverter<BuildType> {
    @Override
    public BuildType convert(String value) {
        if (value.equalsIgnoreCase("jdocbook")) {
            return BuildType.JDOCBOOK;
        } else if (value.equalsIgnoreCase("publican")) {
            return BuildType.PUBLICAN;
        } else {
            return null;
        }
    }
}
