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
package net.sourceforge.buildmonitor;

import java.net.URI;

/**
 * Exception that signal an error that occurs during the monitoring of a build
 * @author sbrunot
 *
 */
public class MonitoringException extends Exception
{
	/**
	 * Signal that the exception might be related to a bad option (parameter) of the monitor
	 */
	private boolean optionsRelated = false;

	/**
	 * An URI that might be opened in the web browser by the end user in order to solve the problem
	 */
	private URI customRelatedURI = null;
	
	/**
	 * Create a new instance of the exception not related to a bad option (user parameter) of the monitor.
	 * 
	 * @param theMessage the message that explains the exception
	 * @param theCause the cause of the exception
	 * @param theRelatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String theMessage, Throwable theCause, URI theRelatedURI)
	{
		super(theMessage, theCause);
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * Create a new instance of the exception not related to a bad option (user parameter) of the monitor.
	 * 
	 * @param theMessage the message that explains the exception
	 * @param theRelatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String theMessage, URI theRelatedURI)
	{
		super(theMessage);
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * Create a new instance of the exception not related to a bad option (user parameter) of the monitor.
	 * 
	 * @param theCause the cause of the exception
	 * @param theRelatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(Throwable theCause, URI theRelatedURI)
	{
		super(theCause);
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param theMessage the message that explains the exception
	 * @param theCause the cause of the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * option of the monitor
	 * @param theRelatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String theMessage, Throwable theCause, boolean isOptionsRelated, URI theRelatedURI)
	{
		super(theMessage, theCause);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param theMessage the message that explains the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * @param theRelatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String theMessage, boolean isOptionsRelated, URI theRelatedURI)
	{
		super(theMessage);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param theCause the cause of the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * @param theRelatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(Throwable theCause, boolean isOptionsRelated, URI theRelatedURI)
	{
		super(theCause);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = theRelatedURI;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getMessage()
	{
		// If the monitoring exception contains another monitoring exception, return the message of
		// the contained monitoring exception
		if (getCause() != null && getCause() instanceof MonitoringException)
		{
			return getCause().getMessage();
		}
		else
		{
			return super.getMessage();
		}
	}
	
	/**
	 * Is this error related to a bad option (user parameter) of the monitor ?
	 * @return true if the error is related to a bad option, false otherwise
	 */
	public boolean isOptionsRelated()
	{
		return this.optionsRelated;
	}
	
	/**
	 * Get an URI that can be openend by the end user to resolve the problem (null if
	 * not available)
	 * @return an URI that can be openend by the end user to resolve the problem, or null
	 */
	public URI getRelatedURI()
	{
		return this.customRelatedURI;
	}
}
