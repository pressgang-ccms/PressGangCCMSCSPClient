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

package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(FileUtilities.class)
public class TemplateCommandTest extends BaseCommandTest {
    private static final String OUTPUT_FILE_NAME = "Test.txt";

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;

    TemplateCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        command = new TemplateCommand(parser, cspConfig, clientConfig);

        // Return the test directory as the root directory
        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
    }

    @Test
    public void shouldShutdownWhenSaveFails() throws IOException {
        File outputFile = new File(rootTestDirectory, OUTPUT_FILE_NAME);
        // Given the saving should fail
        PowerMockito.mockStatic(FileUtilities.class);
        PowerMockito.doThrow(new IOException()).when(FileUtilities.class);
        FileUtilities.saveFile(any(File.class), anyString(), anyString());
        // and the output directory is set
        command.setOutput(outputFile);

        // When the command is processing
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("An error occurred while trying to save the file."));
    }

    @Test
    public void shouldRequireAnExternalConnection() {
        // Given an already initialised command

        // When invoking the method
        boolean result = command.requiresExternalConnection();

        // Then the answer should be false
        assertFalse(result);
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "template"
        assertThat(commandName, is("template"));
    }
}
