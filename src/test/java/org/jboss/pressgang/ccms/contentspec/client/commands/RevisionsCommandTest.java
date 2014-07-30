/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.entities.RevisionList;
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

@PrepareForTest({EntityUtilities.class, ContentSpecUtilities.class})
public class RevisionsCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary String revisions;

    @Mock RevisionList revisionList;

    private RevisionsCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(EntityUtilities.class);
        PowerMockito.mockStatic(ContentSpecUtilities.class);
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
        assertThat(getStdOutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
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
        assertThat(getStdOutLogs(), containsString("Invalid argument!"));
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
        assertThat(getStdOutLogs(), containsString(revisions));
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
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
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
        assertThat(getStdOutLogs(), containsString(revisions));
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
        assertThat(getStdOutLogs(), containsString(revisions));
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "revisions"
        assertThat(commandName, is("revisions"));
    }
}
