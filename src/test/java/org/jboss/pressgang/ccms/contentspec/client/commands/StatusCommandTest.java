package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;

import com.beust.jcommander.JCommander;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, DocBookUtilities.class, FileUtilities.class, ClientUtilities.class})
public class StatusCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary String randomString;
    @Arbitrary Integer randomNumber;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;

    StatusCommand command;
    private final String systemExitError = "Program did not call System.exit()";

    @Before
    public void setUp() throws UnsupportedEncodingException {
        bindStdout();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = spy(new StatusCommand(parser, cspConfig, clientConfig));
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
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then shutdown should be called and an error printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID"));
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
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
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
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then make sure an error message is printed and the command shutdown
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("The \"" + DocBookUtilities.escapeTitle(randomString) + "-post.contentspec\" file " +
                "couldn't be found. This could mean the title has changed on the server or the ID is wrong."));
    }

    @Test
    public void shouldShutdownWhenEmptyFileFound() throws URISyntaxException {
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
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn("");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then make sure an error message is printed and the command shutdown
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("The specified file was empty!"));
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
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString(
                "The local copy and server copy of the Content Specification has been updated. Please use \"csprocessor pull\" to update " +
                        "your local copy. Your unsaved local changes will be saved as " + specFile.getAbsolutePath() + ".backup."));
    }

    @Test
    public void shouldShutdownWhenLocalHasBeenModified() throws URISyntaxException {
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
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn("CHECKSUM=" + randomNumber + "\nTitle = Test Content " +
                "Specification\n");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString(
                "The local copy of the Content Specification has been updated and is out of sync with the server. Please use " +
                        "\"csprocessor push\" to update the server copy."));
    }

    @Test
    public void shouldShutdownWhenServerHasBeenModified() throws URISyntaxException {
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
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(
                "CHECKSUM=" + HashUtilities.generateMD5("Title = Test Content Specification\n") + "\nTitle = Test Content " +
                        "Specification\n");

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(9));
        }

        // Then make sure an error message is printed and the command shutdown
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString(
                "The local copy of the Content Specification is out of date. Please use \"csprocessor pull\" to download the latest copy."));
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
}
