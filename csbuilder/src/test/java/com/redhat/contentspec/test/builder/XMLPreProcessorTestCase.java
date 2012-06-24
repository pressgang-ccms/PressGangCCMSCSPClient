package com.redhat.contentspec.test.builder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.redhat.contentspec.SpecTopic;
import com.redhat.contentspec.builder.XMLPreProcessor;
import com.redhat.contentspec.builder.constants.BuilderConstants;
import com.redhat.contentspec.builder.utils.XMLUtilities;
import com.redhat.contentspec.entities.BugzillaOptions;
import com.redhat.ecs.constants.CommonConstants;
import com.redhat.ecs.services.docbookcompiling.DocbookBuildingOptions;
import com.redhat.topicindex.rest.collections.BaseRestCollectionV1;
import com.redhat.topicindex.rest.entities.PropertyTagV1;
import com.redhat.topicindex.rest.entities.TagV1;
import com.redhat.topicindex.rest.entities.TopicV1;
import com.redhat.topicindex.rest.entities.TranslatedTopicV1;

public class XMLPreProcessorTestCase
{
	private static final XMLPreProcessor<TopicV1> topicPreProcessor = new XMLPreProcessor<TopicV1>();
	private static final XMLPreProcessor<TranslatedTopicV1> translatedTopicPreProcessor = new XMLPreProcessor<TranslatedTopicV1>();
	private static SpecTopic specTopic;
	private static SpecTopic specTranslatedTopic;
	private static TopicV1 topic;
	private static TranslatedTopicV1 translatedTopic;
	private static Document baseDocument;
	
	@BeforeClass
	public static void setUp()
	{
		specTopic = new SpecTopic(100, "Test Title");
		specTranslatedTopic = new SpecTopic(100, "Test Translated Title");
		
		/* Create the basic topic data */
		topic = new TopicV1();
		topic.setId(100);
		topic.setRevision(50);
		topic.setLastModified(new Date());
		topic.setLocale("en-US");
		topic.setTitle("Test Title");
		
		translatedTopic = new TranslatedTopicV1();
		translatedTopic.setId(101);
		translatedTopic.setTopicId(100);
		translatedTopic.setTopicRevision(50);
		translatedTopic.setTopic(topic);
		translatedTopic.setLocale("en-US");
		translatedTopic.setTitle("Test Translated Title");
		
		specTopic.setTopic(topic);
		specTranslatedTopic.setTopic(translatedTopic);
		
		/* Setup the property tags that will be used */
		final PropertyTagV1 bugzillaProduct = new PropertyTagV1();
		bugzillaProduct.setId(CommonConstants.BUGZILLA_PRODUCT_PROP_TAG_ID);
		bugzillaProduct.setValue("JBoss Enterprise Application Platform");
		
		final PropertyTagV1 bugzillaVersion = new PropertyTagV1();
		bugzillaVersion.setId(CommonConstants.BUGZILLA_VERSION_PROP_TAG_ID);
		bugzillaVersion.setValue("6.0");
		
		final PropertyTagV1 bugzillaComponent = new PropertyTagV1();
		bugzillaComponent.setId(CommonConstants.BUGZILLA_COMPONENT_PROP_TAG_ID);
		bugzillaComponent.setValue("documentation");
		
		final PropertyTagV1 bugzillaKeywords = new PropertyTagV1();
		bugzillaKeywords.setId(CommonConstants.BUGZILLA_KEYWORDS_PROP_TAG_ID);
		bugzillaKeywords.setValue("Documentation");
		
		final PropertyTagV1 bugzillaWriter = new PropertyTagV1();
		bugzillaWriter.setId(CommonConstants.BUGZILLA_PROFILE_PROPERTY);
		bugzillaWriter.setValue("lnewson@redhat.com");
		
		/* Create the REST collections */
		final BaseRestCollectionV1<PropertyTagV1> writerTags = new BaseRestCollectionV1<PropertyTagV1>();
		writerTags.addItem(bugzillaWriter);
		
		final BaseRestCollectionV1<PropertyTagV1> releaseTags = new BaseRestCollectionV1<PropertyTagV1>();
		releaseTags.addItem(bugzillaProduct);
		releaseTags.addItem(bugzillaKeywords);
		releaseTags.addItem(bugzillaComponent);
		releaseTags.addItem(bugzillaVersion);
		
		/* Create the Tags */
		final TagV1 assignedWriter = new TagV1();
		assignedWriter.setProperties(writerTags);
		
		final TagV1 releaseTag = new TagV1();
		releaseTag.setProperties(releaseTags);
		
		/* Add the tags to the topics */
		final BaseRestCollectionV1<TagV1> topicTags = new BaseRestCollectionV1<TagV1>();
		topicTags.addItem(assignedWriter);
		topicTags.addItem(releaseTag);
		
		topic.setTags(topicTags);
		translatedTopic.setTags(topicTags);
		
		/* Create the base document */
		try {
			baseDocument = XMLUtilities.convertStringToDocument("<section>\n<title>Test Topic</title>\n<para>This is a Test Paragraph</para>\n</section>");
		} catch (SAXException e) {
			e.printStackTrace();
			fail("Unable to transform the base XML document");
		}
	}

	@Test
	public void testSkynetBugzillaInjection() throws UnsupportedEncodingException
	{
		/* Setup the building options */
		final DocbookBuildingOptions docbookBuildingOptions = new DocbookBuildingOptions();
		docbookBuildingOptions.setInsertBugzillaLinks(true);
		
		/* 
		 * Create the basic information that will be built by the processTopicBugzillaLink method.
		 * This information will be used later to compare the output
		 */
		final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		final Date buildDate = new Date();
		final String buildDateString = URLEncoder.encode(formatter.format(buildDate), "UTF-8");
		final String buildName = "Test Build Name";
		final String buildNameString = URLEncoder.encode(buildName, "UTF-8");
		final String searchTagsUrl = "CSProcessor Builder Version 1.3";
		
		final String bugzillaBuildID = URLEncoder.encode(topic.getBugzillaBuildId(), "UTF-8");
		final String translatedBugzillaBuildID = URLEncoder.encode(translatedTopic.getBugzillaBuildId(), "UTF-8");
		
		/* Create the XML Documents to modify */
		final Document topicDoc = (Document) baseDocument.cloneNode(true);
		final Document translatedTopicDoc = (Document) baseDocument.cloneNode(true);
		
		/* Do the processing on the translation and normal topic */
		topicPreProcessor.processTopicBugzillaLink(specTopic, topicDoc, null, docbookBuildingOptions, null, searchTagsUrl, buildDate);
		translatedTopicPreProcessor.processTopicBugzillaLink(specTranslatedTopic, translatedTopicDoc, null, docbookBuildingOptions, buildName, null, buildDate);
	
		/* Convert the document to a string so that it can be compared */
		final String topicXml = XMLUtilities.convertNodeToString(topicDoc, Arrays.asList(BuilderConstants.VERBATIM_XML_ELEMENTS.split(",")), Arrays.asList(BuilderConstants.INLINE_XML_ELEMENTS.split(",")),
				Arrays.asList(BuilderConstants.CONTENTS_INLINE_XML_ELEMENTS.split(",")), true);
		
		/* Compare the processed topic XML to the expected XML output */
		assertEquals(topicXml, "<section>\n\t<title>Test Topic</title>\n\t<para>\n\t\tThis is a Test Paragraph\n\t</para>\n\t<simplesect>\n" +
					"\t\t<title/>\n\t\t<para role=\"RoleCreateBugPara\">\n" +
					"\t\t\t<ulink url=\"https://bugzilla.redhat.com/enter_bug.cgi?product=JBoss+Enterprise+Application+Platform&amp;component=documentation&amp;version=6.0&amp;keywords=Documentation&amp;assigned_to=lnewson%40redhat.com&amp;cf_environment=Instance+Name%3A+Not+Defined%0ABuild%3A+null%0ABuild+Filter%3A+CSProcessor+Builder+Version+1.3%0ABuild+Name%3A+%0ABuild+Date%3A+" + buildDateString + "&amp;cf_build_id=" + bugzillaBuildID + "\">Report a bug</ulink>\n\t\t</para>\n\t</simplesect>\n</section>");
		
		/* Convert the translated document to a string so that it can be compared */
		final String translatedTopicXml = XMLUtilities.convertNodeToString(translatedTopicDoc, Arrays.asList(BuilderConstants.VERBATIM_XML_ELEMENTS.split(",")), Arrays.asList(BuilderConstants.INLINE_XML_ELEMENTS.split(",")),
				Arrays.asList(BuilderConstants.CONTENTS_INLINE_XML_ELEMENTS.split(",")), true);
		
		/* Compare the processed translated topic XML to the expected XML output */
		assertEquals(translatedTopicXml, "<section>\n\t<title>Test Topic</title>\n\t<para>\n\t\tThis is a Test Paragraph\n\t</para>\n\t<simplesect>\n" +
				"\t\t<title/>\n\t\t<para role=\"RoleCreateBugPara\">\n" +
				"\t\t\t<ulink url=\"https://bugzilla.redhat.com/enter_bug.cgi?product=JBoss+Enterprise+Application+Platform&amp;component=documentation&amp;version=6.0&amp;keywords=Documentation&amp;assigned_to=lnewson%40redhat.com&amp;cf_environment=Instance+Name%3A+Not+Defined%0ABuild%3A+" + buildNameString + "%0ABuild+Filter%3A+null%0ABuild+Name%3A+%0ABuild+Date%3A+" + buildDateString + "&amp;cf_build_id=" + translatedBugzillaBuildID + "\">Report a bug</ulink>\n\t\t</para>\n\t</simplesect>\n</section>");
	}
	
	@Test
	public void testCSPBugzillaInjection() throws UnsupportedEncodingException
	{
		/* Setup the building options */
		final DocbookBuildingOptions docbookBuildingOptions = new DocbookBuildingOptions();
		docbookBuildingOptions.setInsertBugzillaLinks(true);
		
		final BugzillaOptions bzOptions = new BugzillaOptions();
		bzOptions.setProduct("JBoss Enterprise SOA Platform");
		bzOptions.setComponent("Documentation");
		bzOptions.setVersion("5.3.2");
		
		/* 
		 * Create the basic information that will be built by the processTopicBugzillaLink method.
		 * This information will be used later to compare the output
		 */
		final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		final Date buildDate = new Date();
		final String buildDateString = URLEncoder.encode(formatter.format(buildDate), "UTF-8");
		final String buildName = "Test Build Name";
		final String buildNameString = URLEncoder.encode(buildName, "UTF-8");
		final String searchTagsUrl = "CSProcessor Builder Version 1.3";
		
		final String bugzillaBuildID = URLEncoder.encode(topic.getBugzillaBuildId(), "UTF-8");
		final String translatedBugzillaBuildID = URLEncoder.encode(translatedTopic.getBugzillaBuildId(), "UTF-8");
		
		final Document topicDoc = (Document) baseDocument.cloneNode(true);
		final Document translatedTopicDoc = (Document) baseDocument.cloneNode(true);
		
		/* Do the processing on the translation and normal topic */
		topicPreProcessor.processTopicBugzillaLink(specTopic, topicDoc, bzOptions, docbookBuildingOptions, null, searchTagsUrl, buildDate);
		translatedTopicPreProcessor.processTopicBugzillaLink(specTranslatedTopic, translatedTopicDoc, bzOptions, docbookBuildingOptions, buildName, null, buildDate);
	
		/* Convert the document to a string so that it can be compared */
		final String topicXml = XMLUtilities.convertNodeToString(topicDoc, Arrays.asList(BuilderConstants.VERBATIM_XML_ELEMENTS.split(",")), Arrays.asList(BuilderConstants.INLINE_XML_ELEMENTS.split(",")),
				Arrays.asList(BuilderConstants.CONTENTS_INLINE_XML_ELEMENTS.split(",")), true);
		
		/* Compare the processed topic XML to the expected XML output */
		assertEquals(topicXml, "<section>\n\t<title>Test Topic</title>\n\t<para>\n\t\tThis is a Test Paragraph\n\t</para>\n\t<simplesect>\n" +
					"\t\t<title/>\n\t\t<para role=\"RoleCreateBugPara\">\n" +
					"\t\t\t<ulink url=\"https://bugzilla.redhat.com/enter_bug.cgi?product=JBoss+Enterprise+SOA+Platform&amp;component=Documentation&amp;version=5.3.2&amp;assigned_to=lnewson%40redhat.com&amp;cf_environment=Instance+Name%3A+Not+Defined%0ABuild%3A+null%0ABuild+Filter%3A+CSProcessor+Builder+Version+1.3%0ABuild+Name%3A+%0ABuild+Date%3A+" + buildDateString + "&amp;cf_build_id=" + bugzillaBuildID + "\">Report a bug</ulink>\n\t\t</para>\n\t</simplesect>\n</section>");
		
		/* Convert the document to a string so that it can be compared */
		final String translatedTopicXml = XMLUtilities.convertNodeToString(translatedTopicDoc, Arrays.asList(BuilderConstants.VERBATIM_XML_ELEMENTS.split(",")), Arrays.asList(BuilderConstants.INLINE_XML_ELEMENTS.split(",")),
				Arrays.asList(BuilderConstants.CONTENTS_INLINE_XML_ELEMENTS.split(",")), true);
		
		/* Compare the processed translated topic XML to the expected XML output */
		assertEquals(translatedTopicXml, "<section>\n\t<title>Test Topic</title>\n\t<para>\n\t\tThis is a Test Paragraph\n\t</para>\n\t<simplesect>\n" +
				"\t\t<title/>\n\t\t<para role=\"RoleCreateBugPara\">\n" +
				"\t\t\t<ulink url=\"https://bugzilla.redhat.com/enter_bug.cgi?product=JBoss+Enterprise+SOA+Platform&amp;component=Documentation&amp;version=5.3.2&amp;assigned_to=lnewson%40redhat.com&amp;cf_environment=Instance+Name%3A+Not+Defined%0ABuild%3A+" + buildNameString + "%0ABuild+Filter%3A+null%0ABuild+Name%3A+%0ABuild+Date%3A+" + buildDateString + "&amp;cf_build_id=" + translatedBugzillaBuildID + "\">Report a bug</ulink>\n\t\t</para>\n\t</simplesect>\n</section>");
	}
}