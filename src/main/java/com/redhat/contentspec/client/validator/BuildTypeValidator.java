package com.redhat.contentspec.client.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BuildTypeValidator implements IParameterValidator {
    private static final Pattern VALID_BUILD_TYPE = Pattern.compile("^(?i)(jdocbook|publican)$");

    @Override
    public void validate(String name, String value) throws ParameterException {
        final Matcher matcher = VALID_BUILD_TYPE.matcher(value);
        if (!matcher.matches()) {
            throw new ParameterException("Invalid Build Format! The value must be either \"jDocbook\" or \"publican\".");
        }
    }
}
