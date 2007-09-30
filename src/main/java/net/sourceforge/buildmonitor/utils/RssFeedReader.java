/*
 * Copyright 2007 Sebastien Brunot (sbrunot@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.buildmonitor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A class that can parse an RSS feed and return a Feed object.
 * @author sbrunot
 *
 */
public class RssFeedReader
{
	/////////////////////////////////
	// Nested classes
	/////////////////////////////////

	public class RssFeedContentHandler extends DefaultHandler
	{
		///////////////////////////////
		// Constants
		///////////////////////////////

		private static final int TITLE_ATTRIBUTE = 1;
		private static final int DESCRIPTION_ATTRIBUTE = 2;
		private static final int PUBDATE_ATTRIBUTE = 3;
		private static final int LINK_ATTRIBUTE = 4;
		
		///////////////////////////////
		// Attributes
		///////////////////////////////

		RssFeedDocument rssFeedDocument = null;
		RssFeedItem currentItem = null;
		int currentItemAttributeToSet = -1;
		DateFormat rssFeedDateFormat = null;
		
		///////////////////////////////
		// Constructor
		///////////////////////////////

		public RssFeedContentHandler(DateFormat theRssFeedDateFormat)
		{
			if (theRssFeedDateFormat == null)
			{
				throw new IllegalArgumentException("The RSS Feed date format cannot be null !");
			}
			this.rssFeedDateFormat = theRssFeedDateFormat;
		}
		
		///////////////////////////////
		// ContentHandler implementation
		///////////////////////////////
		
		public void startDocument()
		{
			this.rssFeedDocument = new RssFeedDocument();
		}

		public void startElement(String theNameSpace, String theLocalName, String theQName, Attributes theAttributes)
		{
			if ("item".equals(theLocalName))
			{
				// this is a new Item
				this.currentItem = new RssFeedItem();
			}
			else if ("title".equals(theLocalName))
			{
				this.currentItemAttributeToSet = TITLE_ATTRIBUTE;
			}
			else if ("link".equals(theLocalName))
			{
				this.currentItemAttributeToSet = LINK_ATTRIBUTE;
			}
			else if ("description".equals(theLocalName))
			{
				this.currentItemAttributeToSet = DESCRIPTION_ATTRIBUTE;
			}
			else if ("pubDate".equals(theLocalName))
			{
				this.currentItemAttributeToSet = PUBDATE_ATTRIBUTE;
			}
			else
			{
				this.currentItemAttributeToSet = -1;
			}
		}

		public void endElement(String theNameSpace, String theLocalName, String theQName)
		{
			if ("item".equals(theLocalName))
			{
				// end of the item: add it to the document
				this.rssFeedDocument.add(this.currentItem);
				this.currentItem = null;
			}
		}

		public void characters(char[] theCharacters, int theStartIndex, int theLength)
		{
			String characters = new String(theCharacters, theStartIndex, theLength).trim();
			String trimedCharacters = characters.replace("\n", "");
			if (!"".equals(trimedCharacters))
			{
				setCurrentItemAttribute(characters);
			}
		}

		///////////////////////////////
		// Public methods
		///////////////////////////////

		public RssFeedDocument getDocument()
		{
			return this.rssFeedDocument;
		}

		///////////////////////////////
		// Private methods
		///////////////////////////////

		private void setCurrentItemAttribute(String theValueOfTheAttribute)
		{
			if (this.currentItem != null && this.currentItemAttributeToSet != -1)
			{
				if (this.currentItemAttributeToSet == TITLE_ATTRIBUTE)
				{
					this.currentItem.setTitle(theValueOfTheAttribute);
				}
				else if (this.currentItemAttributeToSet == DESCRIPTION_ATTRIBUTE)
				{
					this.currentItem.setDescription(theValueOfTheAttribute);
				}
				else if (this.currentItemAttributeToSet == LINK_ATTRIBUTE)
				{
					this.currentItem.setLink(theValueOfTheAttribute);
				}
				else if (this.currentItemAttributeToSet == PUBDATE_ATTRIBUTE)
				{
					// TODO: USE A DateFormat
					try
					{
						this.currentItem.setPubDate(this.rssFeedDateFormat.parse(theValueOfTheAttribute));
					}
					catch (ParseException e)
					{
						this.currentItem.setPubDate(null);
						// TODO: ADD A LOG INSTEAD OF SYSTEM.ERR OUTPUT
						System.err.println("WARNING: publication date <" + theValueOfTheAttribute + "> does not follow the expected date format.");
					}
				}
				else
				{
					throw new RuntimeException("Error discovered in RssFeedContentHandler: please contact sbrunot@gmail.com");
				}
			}
		}
	}
	
	/////////////////////////////////
	// Attributes
	/////////////////////////////////

	private URL rssFeedUrl;
	
	private DateFormat rssFeedDateFormat;
	
	/////////////////////////////////
	// Constructor
	/////////////////////////////////
	
	public RssFeedReader(URL theRssFeedUrl, DateFormat theRssFeedDateFormat)
	{
		if (theRssFeedUrl == null)
		{
			throw new IllegalArgumentException("URL of the RSS feed cannot be null.");
		}
		if (theRssFeedDateFormat == null)
		{
			throw new IllegalArgumentException("Date format for the RSS feed cannot be null.");
		}
		this.rssFeedUrl = theRssFeedUrl;
		this.rssFeedDateFormat = theRssFeedDateFormat;
	}

	/////////////////////////////////
	// Public methods
	/////////////////////////////////

	public RssFeedDocument getRssFeedDocument() throws IOException, SAXException
	{
		InputStream rssDocumentInputStream = null;
		try
		{
			rssDocumentInputStream = this.rssFeedUrl.openStream();
			XMLReader rssDocumentReader = XMLReaderFactory.createXMLReader();
			RssFeedContentHandler contentHandler = new RssFeedContentHandler(this.rssFeedDateFormat);
			rssDocumentReader.setContentHandler(contentHandler);
			rssDocumentReader.parse(new InputSource(rssDocumentInputStream));
			return contentHandler.getDocument();
		}
		finally
		{
			if (rssDocumentInputStream != null)
			{
				try
				{
					rssDocumentInputStream.close();
				}
				catch (IOException e)
				{
					// do nothing here: it may mask a previous exception ? (to be verified in java spec)
				}
			}
		}
	}
}
