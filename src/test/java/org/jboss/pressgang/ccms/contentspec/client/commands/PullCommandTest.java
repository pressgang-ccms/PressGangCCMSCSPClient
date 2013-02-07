package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class})
public class PullCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TopicProvider topicProvider;

    PullCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdout();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        command = spy(new PullCommand(parser, cspConfig, clientConfig));

        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);
    }

    @Test
    public void shouldShutdownWithNoIds() {
        // Given a command with no ids
        command.setIds(new ArrayList<Integer>());
        // and no csprocessor.cfg data
        given(cspConfig.getContentSpecId()).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecNotFound() {
        // Given a command called without an ID
        command.setIds(new ArrayList<Integer>());
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecRevisionNotFound() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the revision is set
        command.setRevision(revision);
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(id, revision)).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID and revision!"));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenTopicNotFound() {
        // Given a command called with an id
        command.setIds(Arrays.asList(id));
        // and we are looking up a topic
        command.setTopic(true);
        // And no matching topic
        given(topicProvider.getTopic(anyInt(), anyInt())).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void should() {
        // Given
        // When
        // Then
    }

    @Test
    public void shouldReturnFalseWhenPullingContentSpecAndTopic() {
        // Given a command with invalid arguments
        command.setContentSpec(true);
        command.setTopic(true);

        // When the command is validating
        boolean valid = command.isValid();

        // Then the command shouldn't be valid
        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseWhenPullingTopicXmlAndTopicHtml() {
        // Given a command with invalid arguments
        command.setTopic(true);
        command.setUseHtml(true);
        command.setUseXml(true);

        // When the command is validating
        boolean valid = command.isValid();

        // Then the command shouldn't be valid
        assertFalse(valid);
    }

    @Test
    public void shouldReturnFalseWhenPullingContentSpecXmlOrHtml() {
        // Given a command with invalid arguments
        command.setContentSpec(true);
        command.setUseHtml(true);
        command.setUseXml(true);

        // When the command is validating
        boolean valid = command.isValid();

        // Then the command shouldn't be valid
        assertFalse(valid);
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
    public void shouldNotLoadFromCsprocessorCfgWhenIdsSpecified() {
        // Given a command with ids specified
        command.setIds(Arrays.asList(id));

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }
}
