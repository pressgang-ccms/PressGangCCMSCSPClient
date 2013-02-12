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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.UserProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
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

@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class, FileUtilities.class})
public class CreateCommandTest extends BaseUnitTest {
    private static final String SYSTEM_EXIT_ERROR = "Program did not call System.exit()";
    private static final String BOOK_TITLE = "Test";

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer randomNumber;
    @Arbitrary String randomString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphanumString;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TopicProvider topicProvider;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock File mockFile;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock TopicWrapper topicWrapper;
    @Mock ContentSpec contentSpec;
    @Mock Level level;

    CreateCommand command;
    File rootTestDirectory;
    File bookDir;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        command = spy(new CreateCommand(parser, cspConfig, clientConfig));

        // Authentication is tested in the base implementation so assume all users are valid
        setUpAuthorisedUser();

        // Return the test directory as the root directory
        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);

        // Make the book title directory
        bookDir = new File(rootTestDirectory, BOOK_TITLE);
        bookDir.mkdir();
    }

    @Test
    public void shouldShutdownWhenNoFileArgument() {
        // Given a command with invalid arguments
        command.setFiles(new ArrayList<File>());

        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then an error message should be printed and the command shutdown
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldShutdownWhenFileArgumentIsDirectory() {
        // Given a command with a directory as the file
        command.setFiles(Arrays.asList(mockFile));
        given(mockFile.isDirectory()).willReturn(true);

        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then an error message should be printed and the command shutdown
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldShutdownWhenFileArgumentDoesntExist() {
        // Given a command with a file that doesn't exist
        command.setFiles(Arrays.asList(mockFile));
        given(mockFile.exists()).willReturn(false);

        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then an error message should be printed and the command shutdown
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldShutdownWhenFileIsEmpty() {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn("");

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("The specified file was empty"));
    }

    @Test
    public void shouldShutdownWhenFileIsNotValidContentSpec() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec won't parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }
    }

    @Test
    public void shouldShutdownWhenOutputDirectoryExistsAndNoForceAndCreateCsprocessorCfg() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // and no force
        command.setForce(false);
        // and should create the csprocessor cfg
        command.setCreateCsprocessorCfg(true);
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec will parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the Content Spec contains a test title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown
        assertThat(getStdOutLogs(), containsString(
                "A directory already exists for the Content Specification. Please check the \"" + bookDir.getAbsolutePath() + "\" " +
                        "directory first and if it's correct, then use the --force option."));
    }

    @Test
    public void shouldNotShutdownWhenOutputDirectoryExistsAndNoCreateCsprocessorCfg() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // and should create the csprocessor cfg
        command.setCreateCsprocessorCfg(false);
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec will parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the Content Spec contains a test title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        // and the processing fails
        doReturn(false).when(command).processContentSpec(any(ContentSpec.class), any(ErrorLoggerManager.class), any(UserWrapper.class));
        ;

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            // Then the command should exit from the given above.
            assertThat(e.getStatus(), is(-1));
        }
    }

    @Test
    public void shouldShutdownWhenProcessingFails() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // and force to bypass the directory exists check
        command.setForce(true);
        // and should create the csprocessor cfg
        command.setCreateCsprocessorCfg(true);
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec will parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the Content Spec contains a test title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        // and the processing fails
        doReturn(false).when(command).processContentSpec(any(ContentSpec.class), any(ErrorLoggerManager.class), any(UserWrapper.class));

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }
    }

    @Test
    public void shouldCreateProjectDirectoryOnSuccess() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // and force to bypass the directory exists check
        command.setForce(true);
        // and should create the csprocessor cfg
        command.setCreateCsprocessorCfg(true);
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec will parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        given(contentSpec.getId()).willReturn(id);
        // and the Content Spec contains a test title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        // and the processing succeeds
        doReturn(true).when(command).processContentSpec(any(ContentSpec.class), any(ErrorLoggerManager.class), any(UserWrapper.class));
        // and the content spec provider returns a content spec
        given(contentSpecProvider.getContentSpec(anyInt())).willReturn(contentSpecWrapper);
        // and the wrapper will return a valid revision
        given(contentSpecWrapper.getRevision()).willReturn(randomNumber);

        // When it is processed
        command.process();

        // Then the command should have called the create method
        PowerMockito.verifyStatic();
        ClientUtilities.createContentSpecProject(eq(command), eq(cspConfig), any(File.class), anyString(), eq(contentSpecWrapper),
                any(ZanataDetails.class));
        assertThat(getStdOutLogs(), containsString("Content Specification ID: " + id + "\nRevision: " + randomNumber));
    }

    @Test
    public void shouldNotCreateProjectDirectoryOnSuccessAndNoCsprocessorCfg() throws Exception {
        // Given a command with a file
        command.setFiles(Arrays.asList(mockFile));
        // and the file is valid
        setUpValidMockFile();
        // and should create the csprocessor cfg
        command.setCreateCsprocessorCfg(false);
        // that is empty
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec will parse
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.parseContentSpecString(any(RESTProviderFactory.class), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        given(contentSpec.getId()).willReturn(id);
        // and the Content Spec contains a test title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        // and the processing succeeds
        doReturn(true).when(command).processContentSpec(any(ContentSpec.class), any(ErrorLoggerManager.class), any(UserWrapper.class));
        // and the content spec provider returns a content spec
        given(contentSpecProvider.getContentSpec(anyInt())).willReturn(contentSpecWrapper);
        // and the wrapper will return a valid revision
        given(contentSpecWrapper.getRevision()).willReturn(randomNumber);

        // When it is processed
        command.process();

        // Then the command should have called the create method
        PowerMockito.verifyStatic(times(0));
        ClientUtilities.createContentSpecProject(eq(command), eq(cspConfig), any(File.class), anyString(), eq(contentSpecWrapper),
                any(ZanataDetails.class));
        assertThat(getStdOutLogs(), containsString("Content Specification ID: " + id + "\nRevision: " + randomNumber));
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
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(bookDir);
    }

    protected void setUpValidMockFile() {
        given(mockFile.isDirectory()).willReturn(false);
        given(mockFile.isFile()).willReturn(true);
        given(mockFile.exists()).willReturn(true);
    }

    protected void setUpAuthorisedUser() {
        command.setUsername(username);
        given(userProvider.getUsersByName(username)).willReturn(users);
        given(users.size()).willReturn(1);
        given(users.getItems()).willReturn(Arrays.asList(user));
        given(user.getUsername()).willReturn(username);
    }
}
