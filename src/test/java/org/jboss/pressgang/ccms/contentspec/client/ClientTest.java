package org.jboss.pressgang.ccms.contentspec.client;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;

import net.sf.ipsedixit.integration.junit.JUnit4IpsedixitTestRunner;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.constants.TemplateConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.runner.RunWith;

// TODO Integration tests
@RunWith(JUnit4IpsedixitTestRunner.class)
public class ClientTest {

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule public final StandardOutputStreamLog outputLog = new StandardOutputStreamLog();

    @Before
    public void setUp() {
        File file = FileUtils.toFile(ClassLoader.getSystemResource(""));
        System.setProperty("user.home", file.getAbsolutePath());
    }

    @Test
    public void shouldPerformTemplateCommandAsExpectedWithNoArguments() {
        Client.main(new String[]{"template"});

        assertThat(outputLog.getLog(), containsString(TemplateConstants.EMPTY_TEMPLATE));
    }

    @Test
    public void shouldPerformTemplateCommandAsExpectedWithCommentedOutputArgument() {
        Client.main(new String[]{"template", "--commented"});

        assertThat(outputLog.getLog(), containsString(TemplateConstants.FULLY_COMMENTED_TEMPLATE));
    }
}
