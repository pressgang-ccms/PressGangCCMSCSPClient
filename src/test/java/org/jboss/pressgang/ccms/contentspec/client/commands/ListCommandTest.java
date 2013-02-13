package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
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

@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class})
public class ListCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer randomNumber;
    @Arbitrary String randomString;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;

    ListCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = new ListCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldShutdownWhenNumSpecsOverLimit() {
        final CollectionWrapper<ContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        // Given a command that will return a collection of content specs
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
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
        final CollectionWrapper<ContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        // Given a command that will return a collection of content specs
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
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
        final CollectionWrapper<ContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<ContentSpecWrapper> contentSpecs = mock(List.class);
        final SpecList specList = mock(SpecList.class);
        // Given a command that will return a collection of content specs
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
        // and there are some content specs
        given(contentSpecCollection.size()).willReturn(5);
        // and the collection contains items
        given(contentSpecCollection.getItems()).willReturn(contentSpecs);
        given(contentSpecs.subList(anyInt(), anyInt())).willReturn(contentSpecs);
        // and we want to mock the Client Utilities methods
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.buildSpecList(anyList(), eq(providerFactory))).thenReturn(specList);
        when(ClientUtilities.generateContentSpecListResponse(eq(specList))).thenReturn(randomString);

        // When the command is processing
        final ArgumentCaptor<Integer> numResults = ArgumentCaptor.forClass(Integer.class);
        command.process();

        // Then the output should contain the random string and the ClientUtilities methods should have been called
        verify(contentSpecs, times(1)).subList(anyInt(), numResults.capture());
        assertThat(numResults.getValue(), is(5));
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory));
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.generateContentSpecListResponse(eq(specList));
        assertThat(getStdOutLogs(), containsString(randomString));
    }

    @Test
    public void shouldPrintSpecListWhenSizeIsGreaterThanLimitAndForce() {
        final CollectionWrapper<ContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<ContentSpecWrapper> contentSpecs = mock(List.class);
        // Given a command that will return a collection of content specs
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
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
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory));

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
        final CollectionWrapper<ContentSpecWrapper> contentSpecCollection = mock(CollectionWrapper.class);
        final List<ContentSpecWrapper> contentSpecs = mock(List.class);
        // Given a command that will return a collection of content specs
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(contentSpecCollection);
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
        ClientUtilities.buildSpecList(anyList(), eq(providerFactory));

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