package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({ClientUtilities.class, RESTProviderFactory.class})
public class InfoCommandUnitTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary Integer secondId;
    @Arbitrary Integer topicCount;
    @Arbitrary String contentSpecTitle;
    @Arbitrary String checksum;
    @Arbitrary String username;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;
    @Mock SpecTopic specTopic;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TopicProvider topicProvider;
    @Mock UserWrapper user;
    @Mock TopicWrapper topicWrapper;

    InfoCommand command;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        this.command = new InfoCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecNotFound() {
        // Given an InfoCommand called without an ID
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
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldPrintExpectedOutput() {
        final List<SpecTopic> specTopics = Arrays.asList(specTopic);
        final CollectionWrapper<TopicWrapper> topics = mock(CollectionWrapper.class);
        final List<TopicWrapper> topicList = Arrays.asList(topicWrapper);
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and a matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getTitle()).willReturn(contentSpecTitle);
        given(contentSpecWrapper.getRevision()).willReturn(revision);
        // Referenced topics list
        given(contentSpec.getSpecTopics()).willReturn(specTopics);
        given(specTopic.getDBId()).willReturn(secondId);
        // and the calculate number of complete topics method works
        given(topicProvider.getTopics(anyList())).willReturn(topics);
        given(topics.getItems()).willReturn(topicList);
        given(topicWrapper.getXml()).willReturn(contentSpecTitle);
        // and the content spec will be successfully transformed
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.transformContentSpec(any(ContentSpecWrapper.class))).thenReturn(contentSpec);

        // When the command is executed
        command.process();

        // Then the output should contain an Id, Revision, Title and stats
        final String logs = getStdOutLogs();
        assertThat(logs, containsString("Content Specification ID: " + id));
        assertThat(logs, containsString("Content Specification Revision: " + revision));
        assertThat(logs, containsString("Content Specification Title: " + contentSpecTitle));
        assertThat(logs, containsString(
                "Total Number of Topics: " + specTopics.size() + "\nNumber of Topics with XML: " + specTopics.size() + "\nPercentage " +
                        "Complete:"));
    }

    @Test
    public void shouldCalculateNumberOfCompleteTopics() {
        // Setup a collection of 3 mocked topics
        final CollectionWrapper<TopicWrapper> topics = mock(CollectionWrapper.class);
        final TopicWrapper topicWrapper2 = mock(TopicWrapper.class);
        final TopicWrapper topicWrapper3 = mock(TopicWrapper.class);
        final List<TopicWrapper> topicList = Arrays.asList(topicWrapper, topicWrapper2, topicWrapper3);
        // Given a list of three topics: 1 with XML, 1 with null xml content and 1 with an empty string as xml
        given(topicProvider.getTopics(anyList())).willReturn(topics);
        given(topics.getItems()).willReturn(topicList);
        given(topicWrapper.getXml()).willReturn(contentSpecTitle);
        given(topicWrapper2.getXml()).willReturn(null);
        given(topicWrapper3.getXml()).willReturn("");

        // When calculating the number of completed topics
        final Integer count = command.calculateNumTopicsComplete(topicProvider, new ArrayList<Integer>());

        // Then the result should be one
        assertThat(count, is(1));
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

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "info"
        assertThat(commandName, is("info"));
    }
}
