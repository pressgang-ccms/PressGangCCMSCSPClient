/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jboss.pressgang.ccms.contentspec.client.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BuildTypeValidator implements IParameterValidator {
    private static final Pattern VALID_BUILD_TYPE = Pattern.compile("^(?i)(jdocbook|publican(-po)?)?$");

    @Override
    public void validate(String name, String value) throws ParameterException {
        final Matcher matcher = VALID_BUILD_TYPE.matcher(value);
        if (!matcher.matches()) {
            throw new ParameterException("Invalid Build Format! The value must be either \"jDocbook\", \"publican\" or \"publican-po\".");
        }
    }
}
