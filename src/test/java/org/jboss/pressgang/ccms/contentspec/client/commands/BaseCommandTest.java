package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.beust.jcommander.JCommander;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.provider.BlobConstantProvider;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.RESTTextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTTopicProvider;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.UserProvider;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@Ignore
@PrepareForTest(RESTProviderFactory.class)
public abstract class BaseCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();

    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;

    @Mock ServerSettingsProvider serverSettingsProvider;
    @Mock ServerSettingsWrapper serverSettings;
    @Mock ServerEntitiesWrapper serverEntities;

    @Mock ContentSpecProvider contentSpecProvider;
    @Mock RESTTextContentSpecProvider textContentSpecProvider;
    @Mock RESTTopicProvider topicProvider;
    @Mock BlobConstantProvider blobConstantProvider;
    @Mock UserProvider userProvider;

    @Before
    public void setUpProviders() {
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);

        when(clientConfig.getPublicanBuildOptions()).thenReturn("");

        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TextContentSpecProvider.class)).thenReturn(textContentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        when(providerFactory.getProvider(RESTTopicProvider.class)).thenReturn(topicProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        when(providerFactory.getProvider(BlobConstantProvider.class)).thenReturn(blobConstantProvider);

        when(providerFactory.getProvider(ServerSettingsProvider.class)).thenReturn(serverSettingsProvider);
        when(serverSettingsProvider.getServerSettings()).thenReturn(serverSettings);
        when(serverSettings.getEntities()).thenReturn(serverEntities);
        TestUtil.setUpServerSettings(serverSettings, serverEntities);
    }
}
