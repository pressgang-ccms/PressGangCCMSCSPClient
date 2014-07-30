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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ClientUtilities.class)
public class ListCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer randomNumber;
    @Arbitrary String randomString;

    ListCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        command = new ListCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldShutdownWhenNumSpecsOverLimit() {
        final CollectionWrapper<TextContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        // Given a command that will return a collection of content specs
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and there are too many content specs
        given(contentSpecCollection.size()).willReturn(51);
        // and we aren't forcing the list
        command.setForce(false);

        // When the command is processing
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then an error should be print and the command shutdown
        assertThat(getStdOutLogs(), containsString(
                "There are 51 Content Specs on this server. You should probably use \"csprocessor search\" if you have an idea what you "
                        + "are looking for. Otherwise, rerun the list command, and this time use --limit <NUMBER>"));
    }

    @Test
    public void shouldPrintNoSpecsWhenListIsEmpty() {
        final CollectionWrapper<TextContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        // Given a command that will return a collection of content specs
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and there are no content specs
        given(contentSpecCollection.size()).willReturn(0);
        // and we aren't forcing the list
        command.setForce(false);

        // When the command is processing
        command.process();

        // Then a message about no specs should be printed
        assertThat(getStdOutLogs(), containsString("No Content Specifications were found on the Server."));
    }

    @Test
    public void shouldPrintSpecListWhenListExists() {
        final CollectionWrapper<TextContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<TextContentSpecWrapper> contentSpecs = mock(List.class);
        final SpecList specList = mock(SpecList.class);
        // Given a command that will return a collection of content specs
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and there are some content specs
        given(contentSpecCollection.size()).willReturn(5);
        // and the collection contains items
        given(contentSpecCollection.getItems()).willReturn(contentSpecs);
        given(contentSpecs.subList(anyInt(), anyInt())).willReturn(contentSpecs);
        // and we want to mock the Client Utilities methods
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.buildSpecList(anyList(), eq(providerFactory), any(ServerEntitiesWrapper.class))).thenReturn(specList);
        when(ClientUtilities.generateContentSpecList(eq(specList))).thenReturn(randomString);

        // When the command is processing
        final ArgumentCaptor<Integer> numResults = ArgumentCaptor.forClass(Integer.class);
        command.process();

        // Then the output should contain the random string and the ClientUtilities methods should have been called
        verify(contentSpecs, times(1)).subList(anyInt(), numResults.capture());
        assertThat(numResults.getValue(), is(5));
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory), any(ServerEntitiesWrapper.class));
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.generateContentSpecList(eq(specList));
        assertThat(getStdOutLogs(), containsString(randomString));
    }

    @Test
    public void shouldPrintSpecListWhenSizeIsGreaterThanLimitAndForce() {
        final CollectionWrapper<TextContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<TextContentSpecWrapper> contentSpecs = mock(List.class);
        // Given a command that will return a collection of content specs
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and there are too many content spec
        given(contentSpecCollection.size()).willReturn(51);
        // and force is on
        command.setForce(true);
        // and we want to mock the getItems and sublist output
        given(contentSpecCollection.getItems()).willReturn(contentSpecs);
        given(contentSpecs.subList(anyInt(), anyInt())).willReturn(contentSpecs);
        // and the the test should stop if the build spec list method is called
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doThrow(new CheckExitCalled(-2)).when(ClientUtilities.class);
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory), any(ServerEntitiesWrapper.class));

        // When the command is processing
        final ArgumentCaptor<Integer> numResults = ArgumentCaptor.forClass(Integer.class);
        try {
            command.process();
            // Then the program is shut down due to the above throw condition
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-2));
        }

        // Then check that the right number of results to display is passed
        verify(contentSpecs, times(1)).subList(anyInt(), numResults.capture());
        assertThat(numResults.getValue(), is(51));
    }

    @Test
    public void shouldPrintSpecListWhenSizeIsGreaterThanLimitAndLimitSpecified() {
        final CollectionWrapper<TextContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<TextContentSpecWrapper> contentSpecs = mock(List.class);
        // Given a command that will return a collection of content specs
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and the limit is specified
        command.setLimit(randomNumber);
        // and there are more content specs than the limit
        given(contentSpecCollection.size()).willReturn(50 + randomNumber);
        // and we want to mock the getItems and sublist output
        given(contentSpecCollection.getItems()).willReturn(contentSpecs);
        given(contentSpecs.subList(anyInt(), anyInt())).willReturn(contentSpecs);
        // and the the test should stop if the build spec list method is called
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doThrow(new CheckExitCalled(-2)).when(ClientUtilities.class);
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory), any(ServerEntitiesWrapper.class));

        // When the command is processing
        final ArgumentCaptor<Integer> numResults = ArgumentCaptor.forClass(Integer.class);
        try {
            command.process();
            // Then the program is shut down due to the above throw condition
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-2));
        }

        // Then check that the right number of results to display is passed
        verify(contentSpecs, times(1)).subList(anyInt(), numResults.capture());
        assertThat(numResults.getValue(), is(randomNumber));
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

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "list"
        assertThat(commandName, is("list"));
    }
}