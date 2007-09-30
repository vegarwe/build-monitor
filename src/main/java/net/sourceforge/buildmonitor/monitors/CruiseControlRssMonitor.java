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
package net.sourceforge.buildmonitor.monitors;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.utils.RssFeedDocument;
import net.sourceforge.buildmonitor.utils.RssFeedItem;
import net.sourceforge.buildmonitor.utils.RssFeedReader;

/**
 * The run method of this Runnable implementation monitor a Cruise Control build
 * using Cruise Control RSS feeds.
 * 
 * @author sbrunot
 * 
 */
public class CruiseControlRssMonitor implements Monitor
{
	// ////////////////////////////////
	// Instance attributes
	// ////////////////////////////////

	/**
	 * The RssFeedReader instance that we use for monitoring CC rss feed.
	 */
	private RssFeedReader rssFeedReader = null;

	/**
	 * Delay between two update of the build status, in seconds
	 */
	private int updatePeriodInSeconds = 0;

	/**
	 * A Map that contains the last builds status indexed by build project name
	 */
	private Map<String, RssFeedItem> lastBuildsStatus;

	/**
	 * The instance of the build monitor application this thread is running for
	 */
	private BuildMonitor applicationInstance = null;

	/**
	 * Should the thread stop its execution ?
	 */
	private boolean stop = false;

	// ////////////////////////////////
	// Constructor
	// ////////////////////////////////

	/**
	 * Create a new instance.
	 * 
	 * @param theApplicationInstance
	 *            the build monitor instance this thread is running for
	 * @param theUrlToTheCruiseControlRssFeed
	 *            URL to the cruise control rss feed to monitor
	 * @param theUpdatePeriodInSeconds
	 *            period in seconds between to checks of the rss feed to update
	 *            the build status
	 */
	public CruiseControlRssMonitor(BuildMonitor theApplicationInstance,
			URL theUrlToTheCruiseControlRssFeed,
			DateFormat theRssFeedDateFormat, int theUpdatePeriodInSeconds)
	{
		this.applicationInstance = theApplicationInstance;
		this.rssFeedReader = new RssFeedReader(theUrlToTheCruiseControlRssFeed,
				theRssFeedDateFormat);
		this.updatePeriodInSeconds = theUpdatePeriodInSeconds;
	}

	// ////////////////////////////////
	// Monitor implementation
	// ////////////////////////////////

	/**
	 * TODO: IMPLEMENTS AND DOCUMENTS ME !
	 */
	public void run()
	{
		while (!this.stop)
		{
			// update the build status
			try
			{
				updateStatus();
				updateBuildStatusGui();
			} catch (MonitoringException e)
			{
				this.applicationInstance.reportMonitoringException(e);
			}

			// wait before updating the build status again
			try
			{
				Thread.sleep(this.updatePeriodInSeconds * 1000);
			} catch (InterruptedException e)
			{
			}
		}
	}

	/**
	 * TODO: DO SOMETHING BETTER HERE.... Stop the thread execution
	 */
	public void stop()
	{
		this.stop = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public URI getMainPageURI()
	{
		URI returnedValue = null;
		try
		{
			returnedValue = new URI("http://cruisecontrol.sourceforge.net");
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
		return returnedValue;
	}

	/**
	 * {@inheritDoc}
	 */
	public URI getBuildURI(String theIdOfTheBuild)
	{
		// Not implemented yet: returns the main page
		return getMainPageURI();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSystemTrayIconTooltipHeader()
	{
		return "Monitoring Cruise Control build";
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void displayOptionsDialog()
	{
		// NOT IMPLEMENTED YET
		JOptionPane.showMessageDialog(null, "Sorry, no options available yet for the Cruise Control monitor.");
	}

	/**
	 * {@inheritDoc}
	 */
	public String getMonitoredBuildSystemName()
	{
		return "Cruise Control";
	}

	// ////////////////////////////////
	// Utilities methods
	// ////////////////////////////////

	/**
	 * TODO: IMPLEMENTS ME AND DOCUMENT ME !
	 * 
	 */
	protected void updateStatus() throws MonitoringException
	{
		// parse the document to update the build status
		try
		{
			RssFeedDocument rssFeedDocument = this.rssFeedReader
					.getRssFeedDocument();
			if (rssFeedDocument.size() == 0)
			{
				throw new MonitoringException(
						"No build project(s) to monitor !", null);
			}
			this.lastBuildsStatus = new Hashtable<String, RssFeedItem>();
			for (int i = 0; i < rssFeedDocument.size(); i++)
			{
				RssFeedItem currentBuildStatus = rssFeedDocument.getItem(i);
				this.lastBuildsStatus.put(currentBuildStatus.getTitle()
						.substring(0,
								currentBuildStatus.getTitle().indexOf(' ')),
						currentBuildStatus);
			}
		} catch (Throwable t)
		{
			throw new MonitoringException(t, null);
		}
	}

	/**
	 * TODO: WHAT IS THE LIMITATION ON TOOLTIPS LENGTH ?
	 * 
	 */
	protected void updateBuildStatusGui()
	{
		List<BuildReport> buildsReport = new ArrayList<BuildReport>();

		if (this.lastBuildsStatus.keySet().isEmpty())
		{
			// This hould never happens (because a MonitoringException is raised
			// if there is no project to monitor)
			throw new RuntimeException(
					"They are no build project... what should the application do ???");
		}

		for (String currentProject : this.lastBuildsStatus.keySet())
		{
			BuildReport currentReport = new BuildReport();
			currentReport.setId(currentProject);
			currentReport.setName(currentProject);
			RssFeedItem currentBuildStatus = (RssFeedItem) this.lastBuildsStatus
					.get(currentProject);
			currentReport.setDate(currentBuildStatus.getPubDate());
			if (currentBuildStatus.getDescription().contains("FAILED"))
			{
				currentReport.setStatus(Status.FAILED);
			} else
			{
				currentReport.setStatus(Status.OK);
			}
			buildsReport.add(currentReport);
		}

		this.applicationInstance.updateBuildStatus(buildsReport);
	}

	/**
	 * Exposes last builds status for unit testing...
	 * 
	 * @return the last builds status (see internal documentation)
	 */
	protected Map<String, RssFeedItem> getLastBuildsStatus()
	{
		return this.lastBuildsStatus;
	}

}
