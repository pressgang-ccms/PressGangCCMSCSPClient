package com.redhat.contentspec.client.commands;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.pressgangccms.contentspec.ContentSpec;
import org.jboss.pressgangccms.contentspec.rest.RESTManager;
import org.jboss.pressgangccms.contentspec.rest.RESTReader;
import org.jboss.pressgangccms.contentspec.structures.StringToCSNodeCollection;
import org.jboss.pressgangccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgangccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgangccms.rest.v1.collections.RESTTopicCollectionV1;
import org.jboss.pressgangccms.rest.v1.entities.RESTStringConstantV1;
import org.jboss.pressgangccms.rest.v1.entities.RESTTopicV1;
import org.jboss.pressgangccms.rest.v1.entities.RESTTranslatedTopicV1;
import org.jboss.pressgangccms.rest.v1.entities.RESTUserV1;
import org.jboss.pressgangccms.utils.common.CollectionUtilities;
import org.jboss.pressgangccms.utils.common.DocBookUtilities;
import org.jboss.pressgangccms.utils.common.HashUtilities;
import org.jboss.pressgangccms.utils.common.XMLUtilities;
import org.jboss.pressgangccms.utils.constants.CommonConstants;
import org.jboss.pressgangccms.utils.structures.Pair;
import org.jboss.pressgangccms.utils.structures.StringToNodeCollection;
import org.jboss.pressgangccms.zanata.ZanataConstants;
import org.jboss.pressgangccms.zanata.ZanataDetails;
import org.jboss.pressgangccms.zanata.ZanataInterface;
import org.w3c.dom.Document;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.common.ResourceType;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.redhat.contentspec.client.commands.base.BaseCommandImpl;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import com.redhat.contentspec.client.utils.ClientUtilities;
import com.redhat.contentspec.processor.ContentSpecParser.ParsingMode;
import com.redhat.contentspec.processor.ContentSpecProcessor;
import com.redhat.contentspec.processor.structures.ProcessingOptions;

@Parameters(commandDescription = "Push a Content Specification and it's topics to Zanata for translation.")
public class PushTranslationCommand extends BaseCommandImpl
{
	@Parameter(metaVar = "[ID]")
	private List<Integer> ids = new ArrayList<Integer>();
	
	@Parameter(names = Constants.ZANATA_SERVER_LONG_PARAM, description = "The zanata server to be associated with the Content Specification.")
	private String zanataUrl = null;
	
	@Parameter(names = Constants.ZANATA_PROJECT_LONG_PARAM, description = "The zanata project name to be associated with the Content Specification.")
	private String zanataProject = null;
	
	@Parameter(names = Constants.ZANATA_PROJECT_VERSION_LONG_PARAM, description = "The zanata project version to be associated with the Content Specification.")
	private String zanataVersion = null;
	
	private ContentSpecProcessor csp;
	
	public PushTranslationCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig)
	{
		super(parser, cspConfig, clientConfig);
	}

	public List<Integer> getIds()
	{
		return ids;
	}

	public void setIds(final List<Integer> ids)
	{
		this.ids = ids;
	}
	
	public String getZanataUrl()
	{
		return zanataUrl;
	}

	public void setZanataUrl(final String zanataUrl)
	{
		this.zanataUrl = zanataUrl;
	}

	public String getZanataProject()
	{
		return zanataProject;
	}

	public void setZanataProject(final String zanataProject)
	{
		this.zanataProject = zanataProject;
	}

	public String getZanataVersion()
	{
		return zanataVersion;
	}

	public void setZanataVersion(final String zanataVersion)
	{
		this.zanataVersion = zanataVersion;
	}

	@Override
	public void printHelp()
	{
		printHelp(Constants.PUSH_TRANSLATION_COMMAND_NAME);
	}

	@Override
	public void printError(final String errorMsg, final boolean displayHelp)
	{
		printError(errorMsg, displayHelp, Constants.PUSH_TRANSLATION_COMMAND_NAME);
	}

	@Override
	public RESTUserV1 authenticate(final RESTReader reader)
	{
		return authenticate(getUsername(), reader);
	}
	
	@Override
	public void validateServerUrl()
	{
		// Print the server url
		JCommander.getConsole().println(String.format(Constants.WEBSERVICE_MSG, getServerUrl()));
		
		// Test that the server address is valid
		if (!ClientUtilities.validateServerExists(getServerUrl()))
		{
			// Print a line to separate content
			JCommander.getConsole().println("");
			
			printError(Constants.UNABLE_TO_FIND_SERVER_MSG, false);
			shutdown(Constants.EXIT_NO_SERVER);
		}
		
		final ZanataDetails zanataDetails = cspConfig.getZanataDetails();
		
		// Print the zanata server url
		JCommander.getConsole().println(String.format(Constants.ZANATA_WEBSERVICE_MSG, zanataDetails.getServer()));
		
		// Test that the server address is valid
		if (!ClientUtilities.validateServerExists(zanataDetails.getServer()))
		{
			// Print a line to separate content
			JCommander.getConsole().println("");
			
			printError(Constants.UNABLE_TO_FIND_SERVER_MSG, false);
			shutdown(Constants.EXIT_NO_SERVER);
		}
	}
	
	/**
	 * Sets the zanata options applied by the command line
	 * to the options that were set via configuration files.
	 */
	protected void setupZanataOptions()
	{
		// Set the zanata url
		if (this.zanataUrl != null)
		{
			// Find the zanata server if the url is a reference to the zanata server name
			for (final String serverName: clientConfig.getZanataServers().keySet())
			{
				if (serverName.equals(zanataUrl))
				{
					zanataUrl = clientConfig.getZanataServers().get(serverName).getUrl();
					break;
				}
			}
			
			cspConfig.getZanataDetails().setServer(ClientUtilities.validateHost(zanataUrl));
		}
		
		// Set the zanata project
		if (this.zanataProject != null)
		{
			cspConfig.getZanataDetails().setProject(zanataProject);
		}
		
		// Set the zanata version
		if (this.zanataVersion != null)
		{
			cspConfig.getZanataDetails().setVersion(zanataVersion);
		}
	}
	
	protected boolean isValid()
	{
		setupZanataOptions();
		final ZanataDetails zanataDetails = cspConfig.getZanataDetails();
		
		// Check that we even have some zanata details.
		if (zanataDetails == null) return false;
		
		// Check that none of the fields are invalid.
		if (zanataDetails.getServer() == null || zanataDetails.getServer().isEmpty()
				|| zanataDetails.getProject() == null || zanataDetails.getProject().isEmpty()
				|| zanataDetails.getVersion() == null || zanataDetails.getVersion().isEmpty()
				|| zanataDetails.getToken() == null || zanataDetails.getToken().isEmpty()
				|| zanataDetails.getUsername() == null || zanataDetails.getUsername().isEmpty())
		{
			return false;
		}
		
		// At this point the zanata details are valid, so save the details.
		System.setProperty(ZanataConstants.ZANATA_SERVER_PROPERTY, zanataDetails.getServer());
		System.setProperty(ZanataConstants.ZANATA_PROJECT_PROPERTY, zanataDetails.getProject());
		System.setProperty(ZanataConstants.ZANATA_PROJECT_VERSION_PROPERTY, zanataDetails.getVersion());
		System.setProperty(ZanataConstants.ZANATA_USERNAME_PROPERTY, zanataDetails.getUsername());
		System.setProperty(ZanataConstants.ZANATA_TOKEN_PROPERTY, zanataDetails.getToken());
		
		return true;
	}

	@Override
	public void process(final RESTManager restManager, final ErrorLoggerManager elm, final RESTUserV1 user)
	{
		final RESTReader reader = restManager.getReader();
		
		// Add the details for the csprocessor.cfg if no ids are specified
		if (loadFromCSProcessorCfg())
		{
			// Check that the config details are valid
			if (cspConfig != null && cspConfig.getContentSpecId() != null)
			{
				setIds(CollectionUtilities.toArrayList(cspConfig.getContentSpecId()));
			}
		}
		
		// Check that an id was entered
		if (ids.size() == 0)
		{
			printError(Constants.ERROR_NO_ID_MSG, false);
			shutdown(Constants.EXIT_ARGUMENT_ERROR);
		}
		else if (ids.size() > 1)
		{
			printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
			shutdown(Constants.EXIT_ARGUMENT_ERROR);
		}
		
		// Check that the zanata details are valid
		if (!isValid())
		{
			printError(Constants.ERROR_PUSH_NO_ZANATA_DETAILS_MSG, false);
			shutdown(Constants.EXIT_CONFIG_ERROR);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		final RESTTopicV1 contentSpecTopic = reader.getContentSpecById(ids.get(0), null);
		
		if (contentSpecTopic == null || contentSpecTopic.getXml() == null)
		{
			printError(Constants.ERROR_NO_ID_FOUND_MSG, false);
			shutdown(Constants.EXIT_FAILURE);
		}
				
		// Setup the processing options
		final ProcessingOptions processingOptions = new ProcessingOptions();
		processingOptions.setPermissiveMode(true);
		processingOptions.setValidating(true);
		processingOptions.setIgnoreChecksum(true);
		processingOptions.setAllowNewTopics(false);
		
		// Validate and parse the Content Specification
		csp = new ContentSpecProcessor(restManager, elm, processingOptions);
		boolean success = false;
		try
		{
			success = csp.processContentSpec(contentSpecTopic.getXml(), user, ParsingMode.EITHER);
		}
		catch (Exception e)
		{
			JCommander.getConsole().println(elm.generateLogs());
			shutdown(Constants.EXIT_FAILURE);
		}
		
		// Print the error/warning messages
		JCommander.getConsole().println(elm.generateLogs());
		
		// Check that everything validated fine
		if (!success)
		{
			shutdown(Constants.EXIT_TOPIC_INVALID);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Get the topics that were processed by the ContentSpecProcessor. (They should be stored in cache)
		RESTTopicCollectionV1 topics = reader.getTopicsByIds(csp.getParser().getReferencedLatestTopicIds(), false);
		
		if (topics == null)
			topics = new RESTTopicCollectionV1();
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		// Get the revision topics that were processed by the ContentSpecProcessor. (They should be stored in cache)
		final List<Pair<Integer, Integer>> referencedRevisionTopicIds = csp.getParser().getReferencedRevisionTopicIds();
		for (final Pair<Integer, Integer> referencedRevisionTopic : referencedRevisionTopicIds)
		{
			final RESTTopicV1 revisionTopic = reader.getTopicById(referencedRevisionTopic.getFirst(), referencedRevisionTopic.getSecond());
			topics.addItem(revisionTopic);
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return;
		}
		
		if (!pushCSTopicsToZanata(restManager, topics, contentSpecTopic, csp.getContentSpec()))
		{
			printError(Constants.ERROR_ZANATA_PUSH_FAILED_MSG, false);
			System.exit(Constants.EXIT_FAILURE);
		}
		else
		{
			JCommander.getConsole().println(Constants.SUCCESSFUL_ZANATA_PUSH_MSG);
		}
	}

	@Override
	public boolean loadFromCSProcessorCfg()
	{
		return ids.size() == 0;
	}
	
	protected boolean pushCSTopicsToZanata(final RESTManager restManager, final RESTTopicCollectionV1 topics, final RESTTopicV1 contentSpecTopic, final ContentSpec contentSpec)
	{
		final Map<RESTTopicV1, Document> topicToDoc = new HashMap<RESTTopicV1, Document>();
		boolean error = false;
		final ZanataInterface zanataInterface = new ZanataInterface();
		
		/* Build the formatting properties */
		final Properties prop = new Properties();
		try
		{
			final RESTStringConstantV1 xmlElementProperties = restManager.getRESTClient().getJSONStringConstant(CommonConstants.XML_ELEMENTS_STRING_CONSTANT_ID, "");
			prop.load(new StringReader(xmlElementProperties.getValue()));
		}
		catch (Exception e)
		{
			printError(Constants.ERROR_FAILED_LOAD_XML_PROPS_MSG, false);
			System.exit(Constants.EXIT_FAILURE);
		}
		final String verbatimElementsString = prop.getProperty(CommonConstants.VERBATIM_XML_ELEMENTS_PROPERTY_KEY);
		final String inlineElementsString = prop.getProperty(CommonConstants.INLINE_XML_ELEMENTS_PROPERTY_KEY);
		final String contentsInlineElementsString = prop.getProperty(CommonConstants.CONTENTS_INLINE_XML_ELEMENTS_PROPERTY_KEY);
		
		final List<String> verbatimElements = verbatimElementsString == null ? new ArrayList<String>() : CollectionUtilities.toArrayList(verbatimElementsString.split("[\\s]*,[\\s]*"));
		final List<String> inlineElements = inlineElementsString == null ? new ArrayList<String>() : CollectionUtilities.toArrayList(inlineElementsString.split("[\\s]*,[\\s]*"));
		final List<String> contentsInlineElements = contentsInlineElementsString == null ? new ArrayList<String>() : CollectionUtilities.toArrayList(contentsInlineElementsString.split("[\\s]*,[\\s]*"));
		
		// Convert all the topics to DOM Documents first so we know if any are invalid
		for (final RESTTopicV1 topic : topics.getItems())
		{
			/*
			 * make sure the section title is the same as the
			 * topic title
			 */
			Document doc = null;
			try
			{
				final Document tempDoc = XMLUtilities.convertStringToDocument(topic.getXml());
				final String fixedXML = XMLUtilities.convertNodeToString(tempDoc, verbatimElements, inlineElements, contentsInlineElements, true);
				doc = XMLUtilities.convertStringToDocument(fixedXML);
			}
			catch (Exception e)
			{
				// Do Nothing as we handle the error below.
			}
			
			if (doc == null)
			{
				JCommander.getConsole().println("ERROR: Topic ID " + topic.getId() + ", Revison " + topic.getRevision() + " does not have valid XML");
				error = true;
			}
			else
			{
				DocBookUtilities.setSectionTitle(topic.getTitle(), doc);
				topicToDoc.put(topic, doc);
			}
			
			// Good point to check for a shutdown
			if (isAppShuttingDown())
			{
				shutdown.set(true);
				return false;
			}
		}
		
		// Return if creating the documents failed
		if (error)
		{
			return false;
		}
		
		// Good point to check for a shutdown
		if (isAppShuttingDown())
		{
			shutdown.set(true);
			return false;
		}
		
		final float total = topics.getItems().size() + 1;
		float current = 0;
		final int showPercent = 5;
		int lastPercent = 0;
		
		JCommander.getConsole().println("You are about to push " + ((int)total) + " topics to zanata. Continue? (Yes/No)");
		String answer = JCommander.getConsole().readLine();
		
		final List<String> messages = new ArrayList<String>();
		
		if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y"))
		{
			JCommander.getConsole().println("Starting to push topics to zanata...");
			
			// Loop through each topic and upload it to zanata
			for (final RESTTopicV1 topic : topicToDoc.keySet())
			{
				++current;
				final int percent = Math.round(current / total * 100);
				if (percent - lastPercent >= showPercent)
				{
					lastPercent = percent;
					JCommander.getConsole().println("\tPushing topics to zanata " + percent + "% Done");
				}
				
				final Document doc = topicToDoc.get(topic);
				final String zanataId = topic.getId() + "-" + topic.getRevision();
				
				/*
				 * deleting existing resources is useful for debugging,
				 * but not for production
				 */
				final boolean zanataFileExists = zanataInterface.getZanataResourceExists(zanataId);

				if (!zanataFileExists)
				{
					final boolean translatedTopicExists = restManager.getReader().getTranslatedTopicByTopicId(topic.getId(), topic.getRevision(), topic.getLocale()) != null;
					
					final Resource resource = new Resource();
	
					resource.setContentType(ContentType.TextPlain);
					resource.setLang(LocaleId.fromJavaName(topic.getLocale()));
					resource.setName(zanataId);
					resource.setRevision(1);
					resource.setType(ResourceType.FILE);
	
					final List<StringToNodeCollection> translatableStrings = XMLUtilities.getTranslatableStrings(doc, false);

					for (final StringToNodeCollection translatableStringData : translatableStrings)
					{
						final String translatableString = translatableStringData.getTranslationString();
						if (!translatableString.trim().isEmpty())
						{										
							final TextFlow textFlow = new TextFlow();
							textFlow.setContent(translatableString);
							textFlow.setLang(LocaleId.fromJavaName(topic.getLocale()));
							textFlow.setId(createZanataUniqueId(topic, translatableString));
							textFlow.setRevision(1);

							resource.getTextFlows().add(textFlow);
						}
					}

					if (!zanataInterface.createFile(resource))
					{
						messages.add("Topic ID " + topic.getId() + ", Revision " + topic.getRevision() + " failed to be created in Zanata.");
					}
					else if (!translatedTopicExists)
					{
						final RESTTranslatedTopicV1 translatedTopic = createTranslatedTopic(contentSpecTopic);
						try
						{
							restManager.getRESTClient().createJSONTranslatedTopic("", translatedTopic);
						}
						catch (Exception e)
						{
							/*
							 * Do nothing here as it shouldn't fail. If it does then it'll be created 
							 * by the sync service anyways.
							 */
						}
					}
				}
				else
				{
					messages.add("Topic ID " + topic.getId() + ", Revision " + topic.getRevision() + " already exists - Skipping.");
				}
			}
			// Upload the content specification to zanata
			final String zanataId = contentSpecTopic.getId() + "-" + contentSpecTopic.getRevision();
			final boolean zanataFileExists = zanataInterface.getZanataResourceExists(zanataId);

			if (!zanataFileExists)
			{
				final boolean translatedTopicExists = restManager.getReader().getTranslatedContentSpecById(contentSpecTopic.getId(), contentSpecTopic.getRevision(), contentSpecTopic.getLocale()) != null;
				
				final Resource resource = new Resource();
	
				resource.setContentType(ContentType.TextPlain);
				resource.setLang(LocaleId.fromJavaName(contentSpecTopic.getLocale()));
				resource.setName(contentSpecTopic.getId() + "-" + contentSpecTopic.getRevision());
				resource.setRevision(1);
				resource.setType(ResourceType.FILE);
	
				final List<StringToCSNodeCollection> translatableStrings = ContentSpecUtilities.getTranslatableStrings(contentSpec, false);
	
				for (final StringToCSNodeCollection translatableStringData : translatableStrings)
				{
					final String translatableString = translatableStringData.getTranslationString();
					if (!translatableString.trim().isEmpty())
					{										
						final TextFlow textFlow = new TextFlow();
						textFlow.setContent(translatableString);
						textFlow.setLang(LocaleId.fromJavaName(contentSpecTopic.getLocale()));
						textFlow.setId(createZanataUniqueId(contentSpecTopic, translatableString));
						textFlow.setRevision(1);

						resource.getTextFlows().add(textFlow);
					}
				}

				if (!zanataInterface.createFile(resource))
				{
					messages.add("Content Spec ID " + contentSpecTopic.getId() + ", Revision " + contentSpecTopic.getRevision() + " failed to be created in Zanata.");
				}
				else if (!translatedTopicExists)
				{
					// Save the translated topic
					final RESTTranslatedTopicV1 translatedTopic = createTranslatedTopic(contentSpecTopic);
					try
					{
						restManager.getRESTClient().createJSONTranslatedTopic("", translatedTopic);
					}
					catch (Exception e)
					{
						/*
						 * Do nothing here as it shouldn't fail. If it does then it'll be created 
						 * by the sync service anyways.
						 */
					}
				}
			}
			else
			{
				messages.add("Content Spec ID " + contentSpecTopic.getId() + ", Revision " + contentSpecTopic.getRevision() + " already exists - Skipping.");
			}
		}
		
		// Print the info/error messages
		if (messages.size() > 0)
		{
			JCommander.getConsole().println("Output:");
			for (final String message : messages)
			{
				JCommander.getConsole().println("\t" + message);
			}
		}

		return !error;
	}
	
	private static String createZanataUniqueId(final RESTTopicV1 topic, final String text)
	{
		final String sep = "\u0000";
		final String hashBase = text + sep + topic.getId() + sep + topic.getRevision();
		return HashUtilities.generateMD5(hashBase);
	}
	
	/**
	 * Create a TranslatedTopic based on the content from a normal Topic.
	 * 
	 * @param topic The topic to transform to a TranslatedTopic
	 * @return The new TranslatedTopic initialised with data from the topic.
	 */
	protected RESTTranslatedTopicV1 createTranslatedTopic(final RESTTopicV1 topic)
	{
		final RESTTranslatedTopicV1 translatedTopic = new RESTTranslatedTopicV1();
		translatedTopic.explicitSetLocale(topic.getLocale());
		translatedTopic.explicitSetTranslationPercentage(100);
		translatedTopic.explicitSetTopicId(topic.getId());
		translatedTopic.explicitSetTopicRevision(topic.getRevision());
		translatedTopic.explicitSetXml(topic.getXml());
		translatedTopic.explicitSetHtml(topic.getHtml());
		translatedTopic.explicitSetHtmlUpdated(new Date());
		return translatedTopic;
	}
}
