package org.jboss.pressgang.ccms.contentspec.client.commands.base;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.UserProvider;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({ClientUtilities.class})
public class BaseCommandImplUnitTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer randomNumber;
    @Arbitrary String randomString;
    @Arbitrary String commandName;
    @Mock JCommander parser;
    @Mock ClientConfiguration clientConfig;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock DataProviderFactory providerFactory;
    @Mock UserProvider userProvider;
    @Mock UserWrapper user;
    @Mock CollectionWrapper<UserWrapper> usersCollection;

    BaseCommandImpl command;

    @Before
    public void setUp() {
        bindStdOut();
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        // Create an actual object and spy on it
        command = spy(new BaseCommandImplImpl(parser, cspConfig, clientConfig));
    }

    @Test
    public void shouldShutdownWhenAuthenticationFails() {
        final List<UserWrapper> users = Collections.EMPTY_LIST;
        // Given that authentication won't succeed
        when(userProvider.getUsersByName(anyString())).thenReturn(usersCollection);
        when(usersCollection.getItems()).thenReturn(users);
        when(usersCollection.size()).thenReturn(users.size());

        // When the authentication method is called
        try {
            command.authenticate(randomString, providerFactory);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(2));
        }

        // Then check that an error message was printed
        verify(command, times(1)).shutdown(anyInt());
        verify(command, times(1)).printError(anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("Unauthorised Request! Please check your username and the server URL is correct."));
    }

    @Test
    public void shouldReturnUserWhenAuthenticationSucceeds() {
        final CollectionWrapper<UserWrapper> usersCollection = mock(CollectionWrapper.class);
        final List<UserWrapper> users = Arrays.asList(user);
        // Given that authentication succeeded
        when(userProvider.getUsersByName(anyString())).thenReturn(usersCollection);
        when(usersCollection.getItems()).thenReturn(users);
        when(usersCollection.size()).thenReturn(users.size());

        // When the authentication method is called
        final UserWrapper user = command.authenticate(randomString, providerFactory);

        // Then the user variable should be returned
        verify(command, times(0)).shutdown(anyInt());
        verify(command, times(0)).printError(anyString(), anyBoolean());
        assertThat(user, is(this.user));
    }

    @Test
    public void shouldShutdownWhenUsernameIsNull() {
        // Given an invalid username
        final String username = null;

        // When the authentication method is called
        try {
            command.authenticate(username, providerFactory);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(2));
        }

        // Then check that an error message was printed
        verify(command, times(1)).shutdown(anyInt());
        verify(command, times(1)).printError(anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString(
                "No username was specified for the server. Please check your configuration files and make sure a username exists."));
    }

    @Test
    public void shouldShutdownWhenUsernameIsBlank() {
        // Given an invalid username
        final String username = "";

        // When the authentication method is called
        try {
            command.authenticate(username, providerFactory);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(2));
        }

        // Then check that an error message was printed
        verify(command, times(1)).shutdown(anyInt());
        verify(command, times(1)).printError(anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString(
                "No username was specified for the server. Please check your configuration files and make sure a username exists."));
    }

    @Test
    public void shouldAlterServerUrlForPressGang() {
        // Given a server url
        final String url = "http://localhost:8080/TopicIndex/";
        command.setServerUrl(url);

        // When calling the get pressgang url method
        final String pressgangUrl = command.getPressGangServerUrl();

        // Then the url should contain /seam/resource/rest/ once
        assertThat(pressgangUrl, is(url + "seam/resource/rest/"));
    }

    @Test
    public void shouldReturnNullWhenNoServerUrlForPressGang() {
        // Given no url entered
        command.setServerUrl(null);

        // When calling the get pressgang url method
        final String pressgangUrl = command.getPressGangServerUrl();

        // Then the url should also be null
        assertNull(pressgangUrl);
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenInvoked() {
        // Given an error code, an error message and any boolean

        // When calling the printErrorAndShutdown method
        ArgumentCaptor<String> errorMsg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> exitStatus = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Boolean> displayHelp = ArgumentCaptor.forClass(Boolean.class);
        try {
            command.printErrorAndShutdown(randomNumber, randomString, false);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(randomNumber));
        }

        // Then ensure that the printError and shutdown methods are called
        verify(command, times(1)).shutdown(exitStatus.capture());
        verify(command, times(1)).printError(errorMsg.capture(), displayHelp.capture());
        assertThat(exitStatus.getValue(), is(randomNumber));
        assertThat(errorMsg.getValue(), is(randomString));
        assertFalse(displayHelp.getValue());
        assertTrue(command.isShutdown());
    }

    @Test
    public void shouldPrintErrorMessageToConsoleAndNotDisplayHelp() {
        // Given an Error message

        // When printing an error message
        command.printError(randomString, false);

        // Then the parser usage command should not be called and the error message should be printed
        verify(parser, times(0)).usage(anyBoolean(), any(String[].class));
        verify(parser, times(0)).usage(anyBoolean());
        verify(parser, times(0)).usage();
        assertThat(getStdOutLogs(), containsString(randomString));
    }

    @Test
    public void shouldPrintErrorMessageToConsoleAndDisplayHelp() {
        // Given a random error message

        // When printing an error message and displaying help
        command.printError(randomString, true);

        // Then the parser usage command should be called and the error message should be printed
        verify(parser, times(1)).usage(anyBoolean(), eq(new String[]{commandName}));
        verify(parser, times(0)).usage(anyBoolean());
        verify(parser, times(0)).usage();
        assertThat(getStdOutLogs(), containsString(randomString));
    }

    @Test
    public void shouldCallSystemExitOnShutdown() {
        // Given any error code

        // When calling shutdown
        try {
            command.shutdown(randomNumber);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(randomNumber));
        }

        // Then ensure that the correct exit status was set and the shutdown bit is true
        assertTrue(command.isShutdown());
    }

    @Test
    public void shouldValidateServerUrlWithValidURL() {
        // Given a URL
        String url = "http://www.example.com";
        command.setServerUrl(url);
        // And that the URL will be valid
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.validateServerExists(anyString())).thenReturn(true);

        // When validating the server url
        boolean result = command.validateServerUrl();

        // Then the result from the method should be true
        assertThat(getStdOutLogs(), containsString("Connecting to PressGang server: " + url));
        assertTrue(result);
    }

    @Test
    public void shouldNotValidateServerUrlWithInvalidURL() {
        // Given a URL
        String url = "http://www.example.com";
        command.setServerUrl(url);
        // And that the URL will not be valid
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.validateServerExists(anyString())).thenReturn(false);

        // When validating the server url
        try {
            command.validateServerUrl();
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(1));
        }

        // Then an error message should have been printed and a System.exit() called
        assertThat(getStdOutLogs(), containsString("Connecting to PressGang server: " + url));
        assertThat(getStdOutLogs(), containsString("Cannot connect to the server, as the server address can't be resolved."));
    }

    /**
     * A generic concrete implementation of the BaseCommandImpl class to be used for testing.
     */
    class BaseCommandImplImpl extends BaseCommandImpl {

        public BaseCommandImplImpl(JCommander parser, ContentSpecConfiguration cspConfig, ClientConfiguration clientConfig) {
            super(parser, cspConfig, clientConfig);
        }

        @Override
        public String getCommandName() {
            return commandName;
        }

        @Override
        public void process() {
            return;
        }

        @Override
        public boolean loadFromCSProcessorCfg() {
            return false;
        }

        @Override
        public boolean requiresExternalConnection() {
            return false;
        }
    }
}
