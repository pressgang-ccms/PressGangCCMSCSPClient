package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.createRealFile;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setUpAuthorisedUser;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setValidContentSpecMocking;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setValidContentSpecWrapperMocking;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setValidFileProperties;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setValidLevelMocking;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.PropertyTagProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.UserProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.UpdateableCollectionWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class, FileUtilities.class, DocBookUtilities.class})
public class PushCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String contentSpecString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphanumString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String filename;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;
    @Mock TopicProvider topicProvider;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock Level level;
    @Mock TopicWrapper topicWrapper;
    @Mock PropertyTagProvider propertyTagProvider;
    @Mock UpdateableCollectionWrapper<PropertyTagInTopicWrapper> restPropertyTagInTopicCollectionV1Wrapper;
    @Mock CollectionWrapper<TopicWrapper> topicWrapperCollection;
    @Mock File file;
    @Mock File file2;

    private File realFile;
    private PushCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        this.command = new PushCommand(parser, cspConfig, clientConfig);
    }

    @After
    public void cleanUp() {
        if (realFile != null && realFile.exists() && (!realFile.delete())) {
            resetStdStreams();
            System.err.println("WARNING: Test file cleanup for " + realFile.getAbsolutePath() + "was unsuccessful");
        }
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
    public void shouldNotLoadFromCsprocessorCfgWhenFileSpecified() {
        // Given a command with a file
        command.setFiles(Arrays.asList(file));

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "push"
        assertThat(commandName, is("push"));
    }

    @Test
    public void shouldFailIfUserUnauthorised() {
        // Given a user who is unauthorised as they have no name
        command.setUsername("");

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(2));
        }

        // Then it should fail and the program should print an error and exit
        assertThat(getStdOutLogs(), containsString("No username was specified for the server."));
    }

    @Test
    public void shouldFailIfNoFileOrIdInCspConfig() {
        // Given no files are specified
        // And there is no id specified in CSP config
        given(cspConfig.getContentSpecId()).willReturn(null);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldFailIfFileInvalid() {
        // Given multiple files to push, which is an invalid case
        command.setFiles(Arrays.asList(file, file2));
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldFailIfFileHasNoContent() {
        // Given a valid file to push that has no content
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn("");
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldPrintErrorLogsAndExitIfSpecStringParsingFails() {
        // Given a valid file with a content spec string that produces a null result when parsed
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then error messages are printed, INVALID is written to the console and the program exits
        assertThat(getStdOutLogs(), containsString("Invalid Content Specification! Incorrect file format."));
    }

    @Test
    public void shouldPrintErrorLogsAndFailWhenInvalidSpec() {
        // Given a valid file with a content spec that is invalid
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.parseContentSpecString(any(DataProviderFactory.class), any(ErrorLoggerManager.class), any(String.class),
                any(ContentSpecParser.ParsingMode.class))).willReturn(contentSpec);
        given(contentSpec.getBaseLevel()).willReturn(new Level(randomAlphanumString, LevelType.BASE));
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then error messages are printed to the console and the program exits
        assertThat(getStdOutLogs(), containsString("Invalid Content Specification! No Title.")); // Just one of the missing fields
        assertThat(getStdOutLogs(), containsString("The Content Specification is not valid."));
    }

    @Test
    public void shouldPrintResultWhenSpecPushed() throws Exception {
        // Given a valid CSP
        setUpValidCspFromFileParameter();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And command is set to require execution time
        command.setExecutionTime(true);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the PushCommand is processed
        command.process();

        // Then the time taken should be returned to the console as well as the ID and revision
        assertThat(getStdOutLogs(), containsString("The Content Specification is valid."));
        assertThat(getStdOutLogs(), containsString("The Content Specification saved successfully."));
        assertThat(getStdOutLogs(), containsString("Content Specification ID: " + id));
        assertThat(getStdOutLogs(), containsString("Revision: " + id));
        assertThat(getStdOutLogs(), containsString("Request processed in"));
    }

    @Test
    public void shouldNotSaveValidSpecWhenPushOnly() throws Exception {
        // Given a valid CSP
        setUpValidCspFromFileParameter();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And command is set to push only
        command.setPushOnly(true);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the command is processed
        command.process();

        // Then the processed spec is not saved
        PowerMockito.verifyStatic(Mockito.times(0));
        FileUtilities.saveFile(any(File.class), anyString(), anyString());

        // And the spec is validated and pushed but not saved post processing
        assertThat(getStdOutLogs(), containsString("The Content Specification is valid."));
        assertThat(getStdOutLogs(), containsString("The Content Specification saved successfully."));
        assertThat(getStdOutLogs(), containsString("Content Specification ID: " + id));
        assertThat(getStdOutLogs(), containsString("Revision: " + id));
    }

    @Test
    public void shouldProcessFileFromConfigAndCreateFileWhenSaving() throws Exception {
        // Given no files are specified as a parameter
        // And there is a valid .contentspec file with its ID specified in the CSP config
        given(cspConfig.getContentSpecId()).willReturn(id);
        PowerMockito.mockStatic(DocBookUtilities.class);
        given(DocBookUtilities.escapeTitle(anyString())).willReturn(filename);
        String testFilename = filename + "-post.contentspec";
        realFile = createRealFile(testFilename, randomAlphanumString);
        // And valid values are available for validation
        setUpValidValues();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);
        // And readFileContents will work but saveFile will not actually save (to keep our environment clean)
        mockSaveFileButNotReadFileContents();

        // When the command is processed
        command.process();

        // Then a file with this name is added and processed
        assertThat(command.getFiles().get(0), is(realFile));
        assertThat(command.getFiles().get(0).getName(), is(testFilename));
        // And a file with the post-processing content spec is created
        PowerMockito.verifyStatic(Mockito.times(1));
        ClientUtilities.getOutputRootDirectory(cspConfig, contentSpec);
        PowerMockito.verifyStatic(Mockito.times(1));
        FileUtilities.saveFile(any(File.class), anyString(), anyString());
    }

    @Test
    public void shouldPrintErrorAndShutdownIfErrorWhileSavingFile() throws Exception {
        // Given a valid CSP
        setUpValidCspFromFileParameter();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);
        // And an IOException will be thrown when saveFile is called
        PowerMockito.doThrow(new IOException()).when(FileUtilities.class);
        FileUtilities.saveFile(any(File.class), anyString(), anyString());

        // When the command is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then both validation confirmation and an error message is printed to the console and the program exits
        assertThat(getStdOutLogs(), containsString("The Content Specification is valid."));
        assertThat(getStdOutLogs(), containsString("The Content Specification saved successfully."));
        assertThat(getStdOutLogs(), containsString("An error occurred while trying to save"));
    }

    @Test
    public void shouldCreateDirectoriesWhenSavingFileIfRequired() throws Exception {
        // Given a valid CSP
        setUpValidCspFromFileParameter();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);
        // And an output spec parent file
        given(file.getParentFile()).willReturn(file2);

        // When the command is processed
        command.process();

        // Then a directory and any required parent directories are created
        verify(file2, times(1)).mkdirs();
        // And the command completes as expected
        assertThat(getStdOutLogs(), containsString("The Content Specification is valid."));
        assertThat(getStdOutLogs(), containsString("The Content Specification saved successfully."));
        assertThat(getStdOutLogs(), containsString("Content Specification ID: " + id));
        assertThat(getStdOutLogs(), containsString("Revision: " + id));
    }

    @Test
    public void shouldProcessContentSpecFileFromCspConfigWithTxtExtension() throws Exception {
        // Given no files are specified as a parameter
        // And there is a valid .txt file with its ID specified in the CSP config but no .contentspec version
        given(cspConfig.getContentSpecId()).willReturn(id);
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        PowerMockito.mockStatic(DocBookUtilities.class);
        given(DocBookUtilities.escapeTitle(anyString())).willReturn(filename);
        String testFilename = filename + "-post.txt";
        realFile = createRealFile(testFilename, randomAlphanumString);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);
        // And valid values for validation
        setUpValidValues();
        // And the wrapper has the basic data
        setValidContentSpecWrapperMocking(contentSpecWrapper, randomAlphanumString, id);
        // And no files will be actually written to disk to keep our environment clean
        mockSaveFileButNotReadFileContents();

        // When the command is processed
        command.process();

        // Then a file with this name is added and processed
        assertThat(command.getFiles().get(0), is(realFile));
        assertThat(command.getFiles().get(0).getName(), is(testFilename));
    }

    @Test
    public void shouldFailIfCspConfigSpecIdDoesNotMapToExistingFile() {
        // Given no files are specified as a parameter
        // And there is an ID specifided in the CSP config but the spec file doesn't exist
        given(cspConfig.getContentSpecId()).willReturn(id);
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        PowerMockito.mockStatic(DocBookUtilities.class);
        given(DocBookUtilities.escapeTitle(anyString())).willReturn(filename);
        // And an authorised user
        setUpAuthorisedUser(command, userProvider, users, user, username);

        // When the command is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString(filename));
        assertThat(getStdOutLogs(), containsString("was not found in the current directory"));
        // And no files were added for processing
        assertThat(command.getFiles().size(), is(0));
    }

    private void setValidTopicProviderAndWrapperMocking() throws Exception {
        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
        given(topicProvider.getTopic(id)).willReturn(topicWrapper);
        given(topicProvider.updateTopic(any(TopicWrapper.class))).willReturn(topicWrapper);
        given(topicProvider.newTopicCollection()).willReturn(topicWrapperCollection);
        given(topicWrapperCollection.isEmpty()).willReturn(true);
        given(topicWrapper.getTitle()).willReturn(randomAlphanumString);
        given(topicWrapper.getProperties()).willReturn(restPropertyTagInTopicCollectionV1Wrapper);
    }

    private void setUpValidCspFromFileParameter() throws Exception {
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        setUpValidValues();
    }

    private void setUpValidValues() throws Exception {
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpec(anyInt())).willReturn(contentSpecWrapper);
        given(providerFactory.getProvider(PropertyTagProvider.class)).willReturn(propertyTagProvider);
        setValidTopicProviderAndWrapperMocking();
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.parseContentSpecString(any(DataProviderFactory.class), any(ErrorLoggerManager.class), any(String.class),
                any(ContentSpecParser.ParsingMode.class))).willReturn(contentSpec);
        setValidLevelMocking(level, randomAlphanumString);
        setValidContentSpecMocking(contentSpec, level, randomAlphanumString, id);
    }

    private void mockSaveFileButNotReadFileContents() throws IOException {
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenCallRealMethod();
        PowerMockito.doNothing().when(FileUtilities.class);
        FileUtilities.saveFile(any(File.class), anyString(), anyString());
    }
}
