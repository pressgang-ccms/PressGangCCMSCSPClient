package org.jboss.pressgang.ccms.contentspec.client;

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

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
}
