package org.jboss.pressgang.ccms.contentspec.client.commands.base;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
}
