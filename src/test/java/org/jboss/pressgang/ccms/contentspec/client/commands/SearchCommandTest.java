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
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ClientUtilities.class)
public class SearchCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @ArbitraryString(type = StringType.ALPHANUMERIC) String query1;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String query2;
    @Arbitrary String queryResult;

    @Mock TextContentSpecWrapper result1;
    @Mock TextContentSpecWrapper result2;
    @Mock CollectionWrapper<TextContentSpecWrapper> collectionWrapper;
    @Mock SpecList specList;

    private SearchCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(ClientUtilities.class);
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
        verify(textContentSpecProvider).getTextContentSpecsWithQuery(query.capture());
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
        given(textContentSpecProvider.getTextContentSpecsWithQuery(anyString())).willReturn(collectionWrapper);
        List<TextContentSpecWrapper> resultList = Arrays.asList(result1, result2);
        given(collectionWrapper.getItems()).willReturn(resultList);
        given(ClientUtilities.buildSpecList(eq(resultList), eq(providerFactory), any(ServerEntitiesWrapper.class))).willReturn(specList);
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
