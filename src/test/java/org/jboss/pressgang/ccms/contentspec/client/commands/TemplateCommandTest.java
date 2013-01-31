package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
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
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest(FileUtilities.class)
public class TemplateCommandTest extends BaseUnitTest {
    private static final String OUTPUT_FILE_NAME = "Test.txt";

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;

    TemplateCommand command;
    File rootTestDirectory;
    private final String systemExitError = "Program did not call System.exit()";

    @Before
    public void setUp() {
        bindStdout();
        command = spy(new TemplateCommand(parser, cspConfig, clientConfig));

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
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("An error occurred while trying to save the file."));
    }
}
