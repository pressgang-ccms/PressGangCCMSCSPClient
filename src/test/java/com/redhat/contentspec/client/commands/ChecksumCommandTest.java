package com.redhat.contentspec.client.commands;

import com.beust.jcommander.JCommander;
import com.redhat.contentspec.client.BaseUnitTest;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTUserV1;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ChecksumCommandTest extends BaseUnitTest {

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule public final StandardOutputStreamLog outputLog = new StandardOutputStreamLog();

    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
    @Arbitrary String contentSpecXml;
    @Arbitrary String checksum;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTManager restManager;
    @Mock RESTReader restReader;
    @Mock RESTTopicV1 contentSpec;
    @Mock ErrorLoggerManager elm;
    @Mock RESTUserV1 user;

    ChecksumCommand command;

    private final String systemExitError = "Program did not call System.exit()";

    @Before
    public void setUp() {
        when(restManager.getReader()).thenReturn(restReader);
        this.command = new ChecksumCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldPrintErrorAndShutdownOnProcessWhenNoId() {
        // Given a ChecksumCommand called with no ID
        command.setIds(Collections.EMPTY_LIST);
        // And that there is no ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(null);

        // When it is processed
        try {
            command.process(restManager, elm);
        // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
        assertThat(command.isShutdown(), is(true));
        assertThat(outputLog.getLog(), containsString(Constants.ERROR_NO_ID_MSG));
    }

    @Test
    public void shouldPrintErrorAndShutdownOnProcessWhenMultipleIds() {
        // Given a ChecksumCommand called with two IDs
        command.setIds(Arrays.asList(id, secondId));

        // When it is processed
        try {
            command.process(restManager, elm);
        // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
        assertThat(command.isShutdown(), is(true));
        assertThat(outputLog.getLog(), containsString(Constants.ERROR_MULTIPLE_ID_MSG));
    }

    @Test
    public void shouldLoadIdFromFileOnProcessWhenNoIdSupplied() {
        // Given a ChecksumCommand called with no ID
        command.setIds(Collections.EMPTY_LIST);
        // And an ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(id);
        // And that a matching spec is found with content
        given(restReader.getPostContentSpecById(id, null)).willReturn(contentSpec);
        given(contentSpec.getXml()).willReturn(contentSpecXml);

        // When it is processed
        command.process(restManager, elm);

        assertThat(command.getIds().get(0), is(id));
        assertThat(outputLog.getLog(), containsString("CHECKSUM="));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecNotFound() {
        // Given a ChecksumCommand called with an ID
        command.setIds(Arrays.asList(id));
        // And no matching content spec
        given(restReader.getPostContentSpecById(id, null)).willReturn(null);

        // When it is processed
        try {
            command.process(restManager, elm);
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }
        assertThat(command.isShutdown(), is(true));
        assertThat(outputLog.getLog(), containsString(Constants.ERROR_NO_ID_FOUND_MSG));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecXmlNull() {
        // Given a ChecksumCommand called with an ID
        command.setIds(Arrays.asList(id));
        // And a matching content spec with no content
        given(restReader.getPostContentSpecById(id, null)).willReturn(contentSpec);
        given(contentSpec.getXml()).willReturn(null);

        // When it is processed
        try {
            command.process(restManager, elm);
            // Then an error is printed and the program is shut down
            fail(systemExitError);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }
        assertThat(command.isShutdown(), is(true));
        assertThat(outputLog.getLog(), containsString(Constants.ERROR_NO_ID_FOUND_MSG));
    }
}
