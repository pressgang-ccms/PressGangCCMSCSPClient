package org.jboss.pressgang.ccms.contentspec.client.commands.base;

import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.provider.UserProvider;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;

/**
 * Methods shared across tests.
 *
 * @author kamiller@redhat.com (Katie Miller)
 */
public class TestUtil {
    public static File createRealFile(String newFilename, String contents) throws IOException {
        File file = new File(newFilename);
        FileUtils.writeStringToFile(file, contents);
        return file;
    }

    public static void setValidFileProperties(File fileMock) {
        given(fileMock.isDirectory()).willReturn(false);
        given(fileMock.isFile()).willReturn(true);
        given(fileMock.exists()).willReturn(true);
    }

    public static void setValidLevelMocking(Level levelMock, String title) {
        given(levelMock.getType()).willReturn(LevelType.BASE);
        given(levelMock.getNumberOfSpecTopics()).willReturn(1);
        given(levelMock.getTitle()).willReturn(title);
    }

    public static void setValidContentSpecWrapperMocking(ContentSpecWrapper contentSpecWrapperMock, String randomAlphanumString, Integer id) {
        given(contentSpecWrapperMock.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecWrapperMock.getId()).willReturn(id);
        given(contentSpecWrapperMock.getProduct()).willReturn(randomAlphanumString);
        given(contentSpecWrapperMock.getVersion()).willReturn(randomAlphanumString);
        given(contentSpecWrapperMock.getRevision()).willReturn(id);
    }

    public static void setValidContentSpecMocking(ContentSpec contentSpecMock, Level levelMock, String randomAlphanumString, Integer id) {
        given(contentSpecMock.getBaseLevel()).willReturn(levelMock);
        given(contentSpecMock.getPreProcessedText()).willReturn(Arrays.asList(randomAlphanumString));
        given(contentSpecMock.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecMock.getProduct()).willReturn(randomAlphanumString);
        given(contentSpecMock.getVersion()).willReturn("1-A");
        given(contentSpecMock.getDtd()).willReturn("Docbook 4.5");
        given(contentSpecMock.getCopyrightHolder()).willReturn(randomAlphanumString);
        given(contentSpecMock.getId()).willReturn(id);
        given(contentSpecMock.getChecksum()).willReturn(HashUtilities.generateMD5("ID = " + id + "\nTitle = " + randomAlphanumString +
                "\nProduct = " + randomAlphanumString + "\nVersion = " + randomAlphanumString + "\n\n\n"));
    }

    public static void setUpAuthorisedUser(BaseCommand command, UserProvider userProviderMock, CollectionWrapper<UserWrapper> usersMock, UserWrapper userMock, String username) {
        command.setUsername(username);
        given(userProviderMock.getUsersByName(username)).willReturn(usersMock);
        given(usersMock.size()).willReturn(1);
        given(usersMock.getItems()).willReturn(Arrays.asList(userMock));
        given(userMock.getUsername()).willReturn(username);
    }
}
