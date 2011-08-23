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
	 * @param message the message that explains the exception
	 * @param cause the cause of the exception
	 * @param relatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String message, Throwable cause, URI relatedURI)
	{
		super(message, cause);
		this.customRelatedURI = relatedURI;
	}

	/**
	 * Create a new instance of the exception not related to a bad option (user parameter) of the monitor.
	 * 
	 * @param message the message that explains the exception
	 * @param relatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String message, URI relatedURI)
	{
		super(message);
		this.customRelatedURI = relatedURI;
	}

	/**
	 * Create a new instance of the exception not related to a bad option (user parameter) of the monitor.
	 * 
	 * @param cause the cause of the exception
	 * @param relatedURI an URI that could be opened in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(Throwable cause, URI relatedURI)
	{
		super(cause);
		this.customRelatedURI = relatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param message the message that explains the exception
	 * @param cause the cause of the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * option of the monitor
	 * @param relatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String message, Throwable cause, boolean isOptionsRelated, URI relatedURI)
	{
		super(message, cause);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = relatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param message the message that explains the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * @param relatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(String message, boolean isOptionsRelated, URI relatedURI)
	{
		super(message);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = relatedURI;
	}

	/**
	 * Create a new instance of the exception.
	 * 
	 * @param cause the cause of the exception
	 * @param isOptionsRelated a boolean that signal if the error is related to a bad
	 * @param relatedURI if isOptionsRelated is false, an URI that could be opened
	 * in a browser by the end user to resolve the problem.
	 */
	public MonitoringException(Throwable cause, boolean isOptionsRelated, URI relatedURI)
	{
		super(cause);
		this.optionsRelated = isOptionsRelated;
		this.customRelatedURI = relatedURI;
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
