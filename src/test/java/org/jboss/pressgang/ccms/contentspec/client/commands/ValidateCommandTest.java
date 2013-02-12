package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
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
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.provider.UserProvider;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
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

@PrepareForTest({RESTProviderFactory.class, FileUtilities.class, ClientUtilities.class})
public class ValidateCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String contentSpecString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphanumString;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;
    @Mock TopicProvider topicProvider;
    @Mock Level level;
    @Mock File file;
    @Mock File file2;

    private ValidateCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        PowerMockito.mockStatic(FileUtilities.class);
        PowerMockito.mockStatic(ClientUtilities.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        this.command = new ValidateCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldFailIfUserUnauthorised() {
        // Given a user who is unauthorised as they have no name
        command.setUsername("");

        // When the ValidateCommand is processed
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
    public void shouldFailIfFileInvalid() {
        // Given multiple files to validate, which is an invalid case
        command.setFiles(Arrays.asList(file, file2));
        // And an authorised user
        setUpAuthorisedUser();

        // When the ValidateCommand is processed
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
        // Given a valid file to validate that has no content
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        given(FileUtilities.readFileContents(file)).willReturn("");
        // And an authorised user
        setUpAuthorisedUser();

        // When the ValidateCommand is processed
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
    public void shouldPrintErrorLogsAndFailWhenInvalidSpec() {
        // Given a valid file with content spec that is invalid
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
        given(ClientUtilities.parseContentSpecString(any(DataProviderFactory.class), any(ErrorLoggerManager.class),
                any(String.class))).willReturn(contentSpec);
        given(contentSpec.getBaseLevel()).willReturn(new Level(randomAlphanumString, LevelType.BASE));
        // And an authorised user
        setUpAuthorisedUser();

        // When the ValidateCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then error messages are printed, INVALID is written to the console and the program exits
        assertThat(getStdOutLogs(), containsString("Invalid Content Specification! No Title.")); // Just one of the missing fields
        assertThat(getStdOutLogs(), containsString("The Content Specification is not valid."));
        assertThat(getStdOutLogs(), containsString("INVALID"));
    }

//    @Test
//    public void shouldFailIfExceptionThrown() {
//        // Given a valid file but a content spec that is null, which will result in an NPE
//        command.setFiles(Arrays.asList(file));
//        setValidFileProperties(file);
//        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
//        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
//        given(ClientUtilities.parseContentSpecString(any(DataProviderFactory.class), any(ErrorLoggerManager.class),
// any(String.class))).willReturn(null);
//        // And an authorised user
//        setUpAuthorisedUser();
//
//        // When the ValidateCommand is processed
//        try {
//            command.process();
//            // If we get here then the test failed
//            fail(SYSTEM_EXIT_ERROR);
//        } catch (CheckExitCalled e) {
//            assertThat(e.getStatus(), is(3));
//        }
//
//        // Then an error message is printed and the system exits
//        assertThat(getStdOutLogs(), containsString("Internal processing error!"));
//    }

    @Test
    public void shouldPrintValidationResult() {
        // Given a valid CSP
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(providerFactory.getProvider(TopicProvider.class)).willReturn(topicProvider);
        given(ClientUtilities.parseContentSpecString(any(DataProviderFactory.class), any(ErrorLoggerManager.class),
                any(String.class))).willReturn(contentSpec);
        given(level.getType()).willReturn(LevelType.BASE);
        given(level.getNumberOfSpecTopics()).willReturn(1);
        given(level.getTitle()).willReturn(randomAlphanumString);
        given(contentSpec.getBaseLevel()).willReturn(level);
        given(contentSpec.getPreProcessedText()).willReturn(Arrays.asList(randomAlphanumString));
        given(contentSpec.getTitle()).willReturn(randomAlphanumString);
        given(contentSpec.getProduct()).willReturn(randomAlphanumString);
        given(contentSpec.getVersion()).willReturn("1-A");
        given(contentSpec.getDtd()).willReturn("Docbook 4.5");
        given(contentSpec.getCopyrightHolder()).willReturn(randomAlphanumString);
        given(contentSpec.getId()).willReturn(id);
        given(contentSpec.getChecksum()).willReturn(HashUtilities.generateMD5("ID = " + id + "\nTitle = " + randomAlphanumString +
                "\nProduct = " + randomAlphanumString + "\nVersion = " + randomAlphanumString + "\n\n\n"));
        // and the wrapper has the basic data
        given(contentSpecWrapper.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getProduct()).willReturn(randomAlphanumString);
        given(contentSpecWrapper.getVersion()).willReturn(randomAlphanumString);
        // And an authorised user
        setUpAuthorisedUser();

        // When the ValidateCommand is processed
        try {
            command.process();
        } catch (CheckExitCalled e) {
        }

        // Then VALID should be returned to the console
        assertThat(getStdOutLogs(), containsString("The Content Specification is valid."));
        assertThat(getStdOutLogs(), containsString("\nVALID"));
    }

    //    @Test TODO
//    public void shouldLoadFileFromCsProcessorConfig() {
//        // Given
//
//        // When
//        // command.process();
//
//        // Then
//    }

    private void setValidFileProperties(File file) {
        given(file.isDirectory()).willReturn(false);
        given(file.isFile()).willReturn(true);
        given(file.exists()).willReturn(true);
    }

    private void setUpAuthorisedUser() {
        command.setUsername(username);
        given(userProvider.getUsersByName(username)).willReturn(users);
        given(users.size()).willReturn(1);
        given(users.getItems()).willReturn(Arrays.asList(user));
        given(user.getUsername()).willReturn(username);
    }
}
