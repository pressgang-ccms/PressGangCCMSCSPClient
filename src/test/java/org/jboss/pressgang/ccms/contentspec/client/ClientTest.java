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

package org.jboss.pressgang.ccms.contentspec.client;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;

import net.sf.ipsedixit.integration.junit.JUnit4IpsedixitTestRunner;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.constants.TemplateConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.runner.RunWith;

// TODO Integration tests
@RunWith(JUnit4IpsedixitTestRunner.class)
@Ignore
public class ClientTest {

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule public final StandardOutputStreamLog outputLog = new StandardOutputStreamLog();

    @Before
    public void setUp() {
        File file = FileUtils.toFile(ClassLoader.getSystemResource(""));
        System.setProperty("user.home", file.getAbsolutePath());
    }

    @Test
    public void shouldPerformTemplateCommandAsExpectedWithNoArguments() {
        Client.main(new String[]{"template"});

        assertThat(outputLog.getLog(), containsString(TemplateConstants.EMPTY_TEMPLATE));
    }

    @Test
    public void shouldPerformTemplateCommandAsExpectedWithCommentedOutputArgument() {
        Client.main(new String[]{"template", "--commented"});

        assertThat(outputLog.getLog(), containsString(TemplateConstants.FULLY_COMMENTED_TEMPLATE));
    }
}
