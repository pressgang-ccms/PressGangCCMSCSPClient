package org.jboss.pressgang.ccms.contentspec.client.commands;

import com.beust.jcommander.JCommander;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.contentspec.rest.RESTManager;
import org.jboss.pressgang.ccms.contentspec.rest.RESTReader;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgang.ccms.rest.v1.entities.RESTUserV1;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ChecksumCommand tests that involve mocking static methods. These are separated from the
 * other tests as PowerMock causes issues with System Rules. Hopefully this incompatibility
 * will be fixed in the future.
 *
 * @author kamiller@redhat.com (Katie Miller)
 */
@PrepareForTest(HashUtilities.class)
public class ChecksumCommandStaticTest extends BaseUnitTest {

    @Rule public PowerMockRule rule = new PowerMockRule();

    @Arbitrary Integer id;
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

    @Before
    public void setUp() {
        when(restManager.getReader()).thenReturn(restReader);
        this.command = new ChecksumCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldRemoveExistingChecksumLineInContentSpec() {
        // Given a ChecksumCommand called with an ID linked to a content spec with content and a checksum
        command.setIds(Arrays.asList(id));
        given(restReader.getPostContentSpecById(id, null)).willReturn(contentSpec);
        given(contentSpec.getXml()).willReturn("CHECKSUM=" + checksum + "\n" + contentSpecXml);

        PowerMockito.mockStatic(HashUtilities.class);
        when(HashUtilities.generateMD5(anyString())).thenCallRealMethod();

        // When process is called
        command.process(restManager, elm);

        // Then the checksum is removed from the content spec content before a new checksum is calculated
        ArgumentCaptor<String> alteredContentSpec = ArgumentCaptor.forClass(String.class);
        PowerMockito.verifyStatic();
        HashUtilities.generateMD5(alteredContentSpec.capture());
        assertThat(alteredContentSpec.getValue(), not(containsString(checksum)));
    }
}
