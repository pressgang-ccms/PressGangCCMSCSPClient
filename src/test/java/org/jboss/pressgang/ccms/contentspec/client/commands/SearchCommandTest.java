package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class})
public class SearchCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @ArbitraryString(type = StringType.ALPHANUMERIC) String query1;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String query2;
    @Arbitrary String queryResult;
    @Mock ContentSpecWrapper result1;
    @Mock ContentSpecWrapper result2;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock CollectionWrapper<ContentSpecWrapper> collectionWrapper;
    @Mock SpecList specList;

    private SearchCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        PowerMockito.mockStatic(ClientUtilities.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = new SearchCommand(parser, cspConfig, clientConfig);
        // and getting error messages works
        TestUtil.setUpMessages();
    }

    @Test
    public void shouldCreateDatabaseQueriesAsExpected() {
        // Given a query set
        command.setQueries(Arrays.asList(query1, query2));
        // And an expected query that should result from that set
        String expectedSearchString = query1 + " " + query2;
        String expectedQuery = new StringBuilder("query;contentSpecTitle=").append(expectedSearchString).append(
                ";contentSpecVersion=").append(expectedSearchString).append(";contentSpecProduct=").append(expectedSearchString).append(
                ";logic=Or;propertyTag14=").append(expectedSearchString).append(";").toString();

        // When the SearchCommand is processed
        command.process();

        // Then the expected search query should be sent to the database
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(contentSpecProvider).getContentSpecsWithQuery(query.capture());
        assertThat(query.getValue(), is(expectedQuery));
    }

    @Test
    public void shouldPrintMessageWhenNoResultsFound() {
        // Given a query that has no matching results
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(null);

        // When the SearchCommand is processed
        command.process();

        // Then an appropriate message is written to the console
        assertThat(getStdOutLogs(), containsString("No Content Specifications were found on the Server."));
    }

    @Test
    public void shouldReturnQueryResults() {
        // Given a query that has results
        given(contentSpecProvider.getContentSpecsWithQuery(anyString())).willReturn(collectionWrapper);
        List<ContentSpecWrapper> resultList = Arrays.asList(result1, result2);
        given(collectionWrapper.getItems()).willReturn(resultList);
        given(ClientUtilities.buildSpecList(resultList, providerFactory)).willReturn(specList);
        given(ClientUtilities.generateContentSpecList(specList)).willReturn(queryResult);

        // When the SearchCommand is processed
        command.process();

        // Then those results should be written to the console
        assertThat(getStdOutLogs(), containsString(queryResult));
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "search"
        assertThat(commandName, is("search"));
    }
}
