package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.jboss.pressgang.ccms.zanata.ZanataInterface;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

// TODO Write these tests
@Ignore
@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class, ZanataInterface.class})
public class SyncTranslationCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer version;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String url;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String project;
    @ArbitraryString(type = StringType.ALPHA) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String token;

    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ZanataDetails zanataDetails;
    @Mock ZanataInterface zanataInterface;

    SyncTranslationCommand command;

    @Before
    public void setUp() throws Exception {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);

        command = new SyncTranslationCommand(parser, cspConfig, clientConfig);

        TestUtil.setUpZanataDetails(zanataDetails, url, project, version.toString(), username, token);
    }

    @Test
    public void shouldFailWithInvalidLanguage() throws Exception {
        // Given a command with no ids
        command.setIds(new ArrayList<Integer>());
        // and no csprocessor.cfg data
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and a language is set
        command.setLocales("blah");
        // and the validation will fail
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doReturn(false).when(ClientUtilities.class);
        ClientUtilities.validateLanguages(eq(command), eq(providerFactory), any(String[].class));
        // and the zanata details are valid
        given(cspConfig.getZanataDetails()).willReturn(zanataDetails);
        // and the ZanataInterface constructor shouldn't be run
        Constructor<ZanataInterface> zanataInterfaceConstructor =  PowerMockito.constructor(ZanataInterface.class, double.class,
                String.class);
        PowerMockito.suppress(zanataInterfaceConstructor);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
    }
}