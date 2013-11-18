package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({RESTProviderFactory.class, DocBookUtilities.class, FileUtils.class, ClientUtilities.class})
public class StatusCommandTest extends BaseCommandTest {
    private static final String SYSTEM_EXIT_ERROR = "Program did not call System.exit()";

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary String randomString;
    @Arbitrary Integer randomNumber;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;

    StatusCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        command = new StatusCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldShutdownWhenContentSpecNotFound() {
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // but the content spec doesn't exist
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(null);

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then shutdown should be called and an error printed
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID"));
    }

    @Test
    public void shouldShutdownWhenNoCspConfig() {
        // Given no csprocessor.cfg was found
        given(cspConfig.getContentSpecId()).willReturn(null);

        // When processing the command
        try {
            command.process();
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then make sure an error message is printed
        assertThat(getStdOutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldShutdownWhenFileDoesntExist() {
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a valid id
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("");
        // and some file that won't exist
        given(contentSpecWrapper.getTitle()).willReturn(randomString);

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then make sure an error message is printed and the command shutdown
        assertThat(getStdOutLogs(), containsString("The \"" + DocBookUtilities.escapeTitle(randomString) + "-post.contentspec\" file " +
                "couldn't be found. This could mean the title has changed on the server or the ID is wrong."));
    }

    @Test
    public void shouldShutdownWhenEmptyFileFound() throws URISyntaxException, IOException {
        URL specFileUrl = ClassLoader.getSystemResource("StatusCommand-post.contentspec");
        File specFile = new File(specFileUrl.toURI());
        String specFilePath = specFile.getParentFile().getAbsolutePath() + File.separator;
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a valid id
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("");
        // and the file it should find is inn the resources folder
        given(contentSpecWrapper.getTitle()).willReturn("");
        // Note: this is a bypass since the command works on the current directory, however the actual file might be in a different
        // location for tests.
        PowerMockito.mockStatic(DocBookUtilities.class);
        when(DocBookUtilities.escapeTitle(anyString())).thenReturn(specFilePath + "StatusCommand");
        // and that file should read as empty
        PowerMockito.mockStatic(FileUtils.class);
        when(FileUtils.readFileToString(any(File.class))).thenReturn("");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then make sure an error message is printed and the command shutdown
        assertThat(getStdOutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldShutdownWhenServerAndLocalHasBeenModified() throws URISyntaxException {
        URL specFileUrl = ClassLoader.getSystemResource("StatusCommand-post.contentspec");
        File specFile = new File(specFileUrl.toURI());
        String specFilePath = specFile.getParentFile().getAbsolutePath() + File.separator;
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a valid id
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("CHECKSUM=" + randomNumber + "\n");
        // and the file it should find is inn the resources folder
        given(contentSpecWrapper.getTitle()).willReturn("");
        // Note: this is a bypass since the command works on the current directory, however the actual file might be in a different
        // location for tests.
        PowerMockito.mockStatic(DocBookUtilities.class);
        when(DocBookUtilities.escapeTitle(anyString())).thenReturn(specFilePath + "StatusCommand");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        assertThat(getStdOutLogs(), containsString(
                "The local copy and server copy of the Content Specification has been updated. Please use \"csprocessor pull\" to update " +
                        "your local copy. Your unsaved local changes will be saved as " + specFile.getAbsolutePath() + ".backup."));
    }

    @Test
    public void shouldShutdownWhenLocalHasBeenModified() throws URISyntaxException, IOException {
        URL specFileUrl = ClassLoader.getSystemResource("StatusCommand-post.contentspec");
        File specFile = new File(specFileUrl.toURI());
        String specFilePath = specFile.getParentFile().getAbsolutePath() + File.separator;
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a valid id
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("CHECKSUM=" + randomNumber + "\n");
        // and the file it should find is inn the resources folder
        given(contentSpecWrapper.getTitle()).willReturn("");
        // Note: this is a bypass since the command works on the current directory, however the actual file might be in a different
        // location for tests.
        PowerMockito.mockStatic(DocBookUtilities.class);
        when(DocBookUtilities.escapeTitle(anyString())).thenReturn(specFilePath + "StatusCommand");
        // and that file should contain the server checksum
        PowerMockito.mockStatic(FileUtils.class);
        when(FileUtils.readFileToString(any(File.class))).thenReturn("CHECKSUM=" + randomNumber + "\nTitle = Test Content " +
                "Specification\n");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        assertThat(getStdOutLogs(), containsString(
                "The local copy of the Content Specification has been updated and is out of sync with the server. Please use " +
                        "\"csprocessor push\" to update the server copy."));
    }

    @Test
    public void shouldShutdownWhenServerHasBeenModified() throws URISyntaxException, IOException {
        URL specFileUrl = ClassLoader.getSystemResource("StatusCommand-post.contentspec");
        File specFile = new File(specFileUrl.toURI());
        String specFilePath = specFile.getParentFile().getAbsolutePath() + File.separator;
        // Given a valid csprocessor.cfg
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a valid id
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("CHECKSUM=" + randomNumber + "\n");
        // and the file it should find is inn the resources folder
        given(contentSpecWrapper.getTitle()).willReturn("");
        // Note: this is a bypass since the command works on the current directory, however the actual file might be in a different
        // location for tests.
        PowerMockito.mockStatic(DocBookUtilities.class);
        when(DocBookUtilities.escapeTitle(anyString())).thenReturn(specFilePath + "StatusCommand");
        // and that file should contain the server checksum
        PowerMockito.mockStatic(FileUtils.class);
        when(FileUtils.readFileToString(any(File.class))).thenReturn(
                "CHECKSUM=" + HashUtilities.generateMD5("Title = Test Content Specification\n") + "\nTitle = Test Content " +
                        "Specification\n");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        assertThat(getStdOutLogs(), containsString(
                "The local copy of the Content Specification is out of date. Please use \"csprocessor pull\" to download the latest copy"
                        + "."));
    }

    @Test
    public void shouldRequireAnExternalConnection() {
        // Given an already initialised command

        // When invoking the method
        boolean result = command.requiresExternalConnection();

        // Then the answer should be true
        assertTrue(result);
    }

    @Test
    public void shouldAlwaysLoadFromCsprocessorCfg() {
        // Given a command with no input

        // When invoking the load method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be true
        assertTrue(result);
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "status"
        assertThat(commandName, is("status"));
    }
}
