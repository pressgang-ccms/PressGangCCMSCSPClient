package org.jboss.pressgang.ccms.contentspec.client;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import net.sf.ipsedixit.integration.junit.JUnit4IpsedixitTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
@RunWith(JUnit4IpsedixitTestRunner.class)
@Ignore // We don't expect any tests on this class
public class BaseUnitTest {
    protected static final String SYSTEM_EXIT_ERROR = "Program did not call System.exit()";
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    protected void bindStdout() {
        try {
            System.setOut(new PrintStream(baos, false, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getStdoutLogs() {
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
