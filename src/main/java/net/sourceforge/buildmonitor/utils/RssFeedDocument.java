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

import java.util.ArrayList;
import java.util.List;

/**
 * An RSS feed document.
 * @author sbrunot
 *
 */
public class RssFeedDocument
{
	//////////////////////////////
	// Attributes
	//////////////////////////////

	private List<RssFeedItem> items = new ArrayList<RssFeedItem>();
	
	//////////////////////////////
	// Constructor
	//////////////////////////////
	
	public RssFeedDocument()
	{
	}

	//////////////////////////////
	// Public method
	//////////////////////////////

	/**
	 * Get the Nth item of the document.
	 * @param theIndexOfTheItemInTheDocument index of the item in the document (index of the first item is 0).
	 * @return the theIndexOfTheItemInTheDocument th item in the document.
	 * @throws IndexOutOfBoundsException if theIndexOfTheItemInTheDocument is < 0 or >= size().
	 */
	public RssFeedItem getItem(int theIndexOfTheItemInTheDocument) throws IndexOutOfBoundsException
	{
		return this.items.get(theIndexOfTheItemInTheDocument);
	}
	
	/**
	 * Returns the size of the document (aka the number of items it contains).
	 * @return the size of the document (aka the number of items it contains).
	 */
	public int size()
	{
		return items.size();
	}
	
	/**
	 * Add an RSS Feed item to the document
	 * @param theRssFeedItemToAdd the item to add to the document
	 */
	public void add(RssFeedItem theRssFeedItemToAdd)
	{
		this.items.add(theRssFeedItemToAdd);
	}
}
