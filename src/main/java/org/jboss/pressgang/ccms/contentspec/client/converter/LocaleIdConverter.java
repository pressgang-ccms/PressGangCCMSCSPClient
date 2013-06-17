package org.jboss.pressgang.ccms.contentspec.client.converter;

import com.beust.jcommander.IStringConverter;
import org.zanata.common.LocaleId;

public class LocaleIdConverter implements IStringConverter<LocaleId> {
    @Override
    public LocaleId convert(String value) {
        return LocaleId.fromJavaName(value);
    }
}
