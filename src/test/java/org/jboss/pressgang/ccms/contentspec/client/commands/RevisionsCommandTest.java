package org.jboss.pressgang.ccms.contentspec.client.commands;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.entities.RevisionList;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.TopicProvider;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@PrepareForTest({RESTProviderFactory.class, EntityUtilities.class, ContentSpecUtilities.class})
public class RevisionsCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary String revisions;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TopicProvider topicProvider;
    @Mock RevisionList revisionList;

    private RevisionsCommand command;

    @Before
    public void setUp() {
        bindStdout();
        PowerMockito.mockStatic(EntityUtilities.class);
        PowerMockito.mockStatic(ContentSpecUtilities.class);
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        this.command = new RevisionsCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldFailWhenNoIds() {
        // Given a Revisions Command with no ids
        command.setIds(new ArrayList<Integer>());
        // And no csprocessor.cfg data
        given(cspConfig.getContentSpecId()).willReturn(null);

        // When it is processed
        // Then the command should be shutdown and an error message printed
        try {
            command.process();
            // If we get here the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
        assertThat(getStdoutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldFailIfBothContentSpecAndTopicFlagSet() {
        // Given a Revisions Command with both the use content spec and topic flags set to true
        command.setUseContentSpec(true);
        command.setUseTopic(true);

        // When it is processed
        // Then the command should be shutdown and an error message printed
        try {
            command.process();
            // If we get here the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
        assertThat(getStdoutLogs(), containsString("Invalid argument!"));
    }

    @Test
    public void shouldDefaultToContentSpecRevisionsIfNoFlagSet() {
        // Given a Revisions Command with no flags specifically set and a particular ID given
        command.setIds(Arrays.asList(id));
        given(revisionList.toString()).willReturn(revisions);
        // And a particular revision list for that ID can be returned through ContentSpecUtilities
        given(ContentSpecUtilities.getContentSpecRevisionsById(contentSpecProvider, id)).willReturn(revisionList);

        // When the command is processed
        command.process();

        // Then the revision list for that ID should be output
        assertThat(getStdoutLogs(), containsString(revisions));
        // And the Topic Utilities methods should not be called
        verify(topicProvider, never()).getTopic(id);
    }

    @Test
    public void shouldFailIfNoRevisionsFound() {
        // Given a Revisions Command with a valid ID given
        command.setIds(Arrays.asList(id));
        // And no revisions for that ID
        given(ContentSpecUtilities.getContentSpecRevisionsById(contentSpecProvider, id)).willReturn(null);

        // When it is processed
        // Then the command should be shutdown and an error message printed
        try {
            command.process();
            // If we get here the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldDisplayRevisionsForValidContentSpecIdWithRevisions() {
        // Given a Revisions Command with a valid content spec ID given
        command.setIds(Arrays.asList(id));
        // And revisions for that ID
        given(revisionList.toString()).willReturn(revisions);
        given(ContentSpecUtilities.getContentSpecRevisionsById(contentSpecProvider, id)).willReturn(revisionList);

        // When it is processed
        command.process();

        // Then the revision list for that ID should be output
        assertThat(getStdoutLogs(), containsString(revisions));
    }

    @Test
    public void shouldDisplayRevisionsForValidTopicIdWithRevisions() {
        // Given a Revisions Command set to use topics with a valid topic ID given
        command.setIds(Arrays.asList(id));
        command.setUseTopic(true);
        // And revisions for that topic ID
        given(revisionList.toString()).willReturn(revisions);
        given(EntityUtilities.getTopicRevisionsById(topicProvider, id)).willReturn(revisionList);

        // When it is processed
        command.process();

        // Then the revision list for that ID should be output
        assertThat(getStdoutLogs(), containsString(revisions));
    }
}
