package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.modules.junit4.rule.PowerMockRule;

public class SetupCommandTest extends BaseUnitTest {
    @Rule public final PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String apikey;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;

    SetupCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        command = new SetupCommand(parser, cspConfig, clientConfig);

        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
    }

    @Test
    public void shouldSetRootDirectory() {
        // Given some input as a root directory
        setStdInput("./\n");

        // When getting the root directory from the user
        StringBuilder builder = new StringBuilder();
        command.setupRootDirectory(builder);

        // Then the output should have the directory header and have the root directory set
        assertThat(builder.toString(), is("[directory]\nroot=" + System.getProperty("user.dir") + File.separator + "\n"));
    }

    @Test
    public void shouldSetRootDirectoryWhenInputIsEmpty() {
        // Given some input as a root directory
        setStdInput("\n");

        // When getting the root directory from the user
        StringBuilder builder = new StringBuilder();
        command.setupRootDirectory(builder);

        // Then the output should have the directory header and have an empty root directory
        assertThat(builder.toString(), is("[directory]\nroot=\n"));
    }

    @Test
    public void shouldSetPublicanOptions() throws IOException {
        // Given some input
        String buildOptions = "--formats html-single,html --langs en-US";
        String previewFormat = "html";
        setStdInput(buildOptions + "\n" + previewFormat + "\n");

        // When getting the publican options from the user
        StringBuilder builder = new StringBuilder();
        command.setupPublican(builder);

        // Then the output should have the publican header and options set
        assertThat(builder.toString(), is("[publican]\nbuild.parameters=" + buildOptions + "\n" + "preview.format=" + previewFormat +
                "\n"));
    }

    @Test
    public void shouldSetDefaultPublicanOptionsWhenNoInput() throws IOException {
        // Given some input
        String buildOptions = "--langs=en-US --formats=html-single";
        String previewFormat = "html-single";
        setStdInput("\n\n");

        // When getting the publican options from the user
        StringBuilder builder = new StringBuilder();
        command.setupPublican(builder);

        // Then the output should have the publican header and options set
        assertThat(builder.toString(), is("[publican]\nbuild.parameters=" + buildOptions + "\n" + "preview.format=" + previewFormat +
                "\n"));
    }

    @Test
    public void shouldSetPublishOptions() throws IOException {
        // Given some input
        String kojiUrl = "http://www.example.com/kojihub";
        String publishCommand = "fdpkg --lang en-US";
        setStdInput(kojiUrl + "\n" + publishCommand + "\n");

        // When getting the publish options from the user
        StringBuilder builder = new StringBuilder();
        command.setupPublish(builder);

        // Then the output should have the publish command and koji url
        assertThat(builder.toString(), is("[publish]\nkoji.huburl=" + kojiUrl + "\n" + "command=" + publishCommand +
                "\n"));
    }

    @Test
    public void shouldSetDefaultPublishOptions() throws IOException {
        // Given some input
        String kojiUrl = Constants.DEFAULT_KOJIHUB_URL;
        String publishCommand = Constants.DEFAULT_PUBLISH_COMMAND;
        setStdInput("\n\n");

        // When getting the publish options from the user
        StringBuilder builder = new StringBuilder();
        command.setupPublish(builder);

        // Then the output should have the publish command and koji url
        assertThat(builder.toString(), is("[publish]\nkoji.huburl=" + kojiUrl + "\n" + "command=" + publishCommand + "\n"));
    }

    @Test
    public void shouldSetZanataOptions() {
        // Given some input
        String zanataServerName = "public";
        String zanataUrl = "http://translate.zanata.org/";
        String zanataUsername = username;
        String zanataKey = apikey;
        String zanataProjectName = "project";
        String zanataVersionName = "version";
        setStdInput(zanataProjectName + "\n" + zanataVersionName + "\n" + "1" + "\n" + zanataServerName + "\n" + zanataUrl + "\n" +
                zanataUsername + "\n" + zanataKey + "\n");

        // When getting the zanata options from the user
        StringBuilder builder = new StringBuilder();
        command.setupZanata(builder);

        // Then the output should have the zanata details set
        assertThat(builder.toString(), is("[zanata]\ndefault=" + zanataServerName + "\ndefault.project=" + zanataProjectName +
                "\ndefault.project-version=" + zanataVersionName + "\n\npublic.url=" + zanataUrl + "\npublic.username=" + username +
                "\npublic.key=" + apikey + "\n"));
    }

    @Test
    public void shouldKeepAskingForNumZanataServersIfInputInvalid() {
        // Given some input
        String zanataServerName = "public";
        String zanataUrl = "http://translate.zanata.org/";
        String zanataUsername = username;
        String zanataKey = apikey;
        String zanataProjectName = "project";
        String zanataVersionName = "version";
        setStdInput(zanataProjectName + "\n" + zanataVersionName + "\n" + "one\n0\n1" + "\n" + zanataServerName + "\n" + zanataUrl + "\n" +
                zanataUsername + "\n" + zanataKey + "\n");

        // When getting the zanata options from the user
        StringBuilder builder = new StringBuilder();
        command.setupZanata(builder);

        // Then the output should have the zanata details set
        assertThat(builder.toString(), is("[zanata]\ndefault=" + zanataServerName + "\ndefault.project=" + zanataProjectName +
                "\ndefault.project-version=" + zanataVersionName + "\n\npublic.url=" + zanataUrl + "\npublic.username=" + username +
                "\npublic.key=" + apikey + "\n"));
    }

    @Test
    public void shouldSetEmptyValuesForSetZanataOptions() {
        // Given some input
        setStdInput("\n\n1\npublic\n\n\n\n");

        // When getting the zanata options from the user
        StringBuilder builder = new StringBuilder();
        command.setupZanata(builder);

        // Then the output should have the empty zanata details set
        assertThat(builder.toString(),
                is("[zanata]\ndefault=public\ndefault.project=\ndefault.project-version=\n\npublic.url=\npublic.username=\npublic" + "" +
                        ".key=\n"));
    }

    @Test
    public void shouldUseDefaultConfigurationForServersWhenAsked() {
        // Given some input
        String defaultServer = "test";
        setStdInput("y\n" + defaultServer + "\n" + username + "\n");

        // When getting the server options from the user
        StringBuilder builder = new StringBuilder();
        command.setupServers(builder);

        // Then the output should have the default server setup
        assertThat(builder.toString(),
                is("[servers]\ndefault=" + defaultServer + "\ndefault.username=" + username + "\n\ntest.url=" + Constants
                        .DEFAULT_TEST_SERVER + "\ntest.username=\n\nproduction.url=" + Constants.DEFAULT_PROD_SERVER + "\nproduction" +
                        ".username=\n"));
    }

    @Test
    public void shouldSetEmptyValuesForSetServerOptions() {
        // Given some input
        setStdInput("n\n1\npublic\n\n\n");

        // When getting the server options from the user
        StringBuilder builder = new StringBuilder();
        command.setupServers(builder);

        // Then the output should have the empty server details set
        assertThat(builder.toString(), is("[servers]\ndefault=public\ndefault.username=\n\npublic.url=\npublic.username=\n"));
    }

    @Test
    public void shouldShutdownWhenUseDefaultServerSetupIsInvalid() {
        // Given some incorrect input
        setStdInput(username + "\n");

        // When getting the server options from the user
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then check an error was printed
        assertThat(getStdOutLogs(), containsString("Invalid argument!"));
    }

    @Test
    public void shouldNotRequireAnExternalConnection() {
        // Given an already initialised command

        // When invoking the method
        boolean result = command.requiresExternalConnection();

        // Then the answer should be false
        assertFalse(result);
    }

    @Test
    public void shouldNotLoadFromCsprocessorCfg() {
        // Given a command with no arguments

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }
}
