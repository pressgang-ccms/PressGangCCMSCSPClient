package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.junit.After;
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
public class CheckoutCommandTest extends BaseUnitTest {
    private static final String BOOK_TITLE = "Test";
    private static final String EMPTY_FILE_NAME = "empty.txt";

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

    CheckoutCommand command;
    File rootTestDirectory;
    File bookDir;
    File emptyFile;
    private final String systemExitError = "Program did not call System.exit()";

    @Before
    public void setUp() throws IOException {
        bindStdout();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = spy(new CheckoutCommand(parser, cspConfig, clientConfig));

        // Return the test directory as the root directory
        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);

        // Make the book title directory
        bookDir = new File(rootTestDirectory, BOOK_TITLE);
        bookDir.mkdir();

        // Make a empty file in that directory
        emptyFile = new File(bookDir, EMPTY_FILE_NAME);
        emptyFile.createNewFile();
    }

    @Test
    public void shouldFailWithNoIds() {
        // Given a command with no ids
        command.setIds(Collections.EMPTY_LIST);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No ID was specified by the command line."));
    }

    @Test
    public void shouldShutdownWhenContentSpecNotFound() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id));
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(null);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldShutdownAndPrintErrorWhenFileAlreadyExists() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id));
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("");
        // and the title of the book is empty so that the root directory is used
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("A directory already exists for the Content Specification. Please check the " +
                "\"" + bookDir.getAbsolutePath() + "\" directory first and if it's correct, then use the --force option."));
    }

    @Test
    public void shouldDeleteOldDirectoryWhenFileAlreadyExistsAndForce() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id));
        // and a new folder should be created
        command.setForce(true);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("");
        // and the title of the book is empty so that the root directory is used
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        // and we want to shutdown the command after the directory has been deleted
        when(clientConfig.getZanataServers()).thenThrow(new CheckExitCalled(-2));
        // and we are calling the real delete method
        PowerMockito.mockStatic(FileUtilities.class, CALLS_REAL_METHODS);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-2));
        }

        // Then the command should be shutdown and an error message printed
        PowerMockito.verifyStatic(times(1));
        FileUtilities.deleteDir(any(File.class));
        assertFalse(emptyFile.exists());
    }

    @Test
    public void shouldSaveSpecAndConfig() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id));
        // and a new folder should be created
        command.setForce(true);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn(randomString);
        // and the title of the book is empty so that the root directory is used
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        // and the ClientUtilities create method is mocked
        PowerMockito.mockStatic(ClientUtilities.class);

        // When the command is processing
        command.process();

        // Then check that the create method was called
        PowerMockito.verifyStatic();
        ClientUtilities.createContentSpecProject(eq(command), eq(cspConfig), any(File.class), anyString(), eq(contentSpecWrapper),
                any(ZanataDetails.class));
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
    public void shouldNotLoadFromCsprocessorCfg() {
        // Given a command with no arguments

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }

    @After
    public void cleanUp() {
        bookDir.delete();
    }
}
