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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;
import net.sourceforge.buildmonitor.dialogs.CruiseControlPropertiesDialog;
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
	//////////////////////////////
	// Nested classes
	//////////////////////////////

	/**
	 * Set of properties needed to monitor a Cruise Control server.
	 */
	private class CruiseControlProperties
	{
		//////////////////////////////
		// Constants
		//////////////////////////////

		private static final String CC_FEED_DATE_FORMAT_PROPERTY_KEY = "cc.feed.date.format";

		private static final String UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY = "update.period.in.seconds";

		private static final String CC_RSS_FEED_URL_PROPERTY_KEY = "cc.rss.feed.url";

		private static final String USER_PROPERTIES_FILE = "cc-monitor.properties";

		//////////////////////////////
		// Instance attributes
		//////////////////////////////

		private String rssFeedUrl = null;
		
		private String feedDateFormat = null;
		
		private Integer updatePeriodInSeconds = null;
		
		//////////////////////////////
		// Getters and Setters
		//////////////////////////////

		/**
		 * Get URL to the cc server rss feed
		 * @return the URL to the cc server rss feed
		 */
		public String getRssFeedUrl()
		{
			return this.rssFeedUrl;
		}

		/**
		 * Get URL to the cc server instance (deduced from the rss feed url)
		 * @return the URL to the cc server instance (deduced from the rss feed url)
		 */
		public String getMainPageDeducedFromRssFeedUrl()
		{
			String returnedValue = null;
			if (this.rssFeedUrl != null)
			{
				returnedValue = this.rssFeedUrl.replace("/rss", "");
			}
			return returnedValue;
		}

		/**
		 * Set URL to the cc server rss feed
		 * @param theRssFeedUrl the URL to the cc server rss feed
		 */
		public void setRssFeedUrl(String theRssFeedUrl)
		{
			this.rssFeedUrl = theRssFeedUrl;
		}

		/**
		 * Get the period (in seconds) of build status update
		 * @return the period (in seconds) of build status update
		 */
		public Integer getUpdatePeriodInSeconds()
		{
			return this.updatePeriodInSeconds;
		}

		/**
		 * Set the period (in seconds) of build status update
		 * @param theUpdatePeriodInSeconds the period (in seconds) of build status update
		 */
		public void setUpdatePeriodInSeconds(Integer theUpdatePeriodInSeconds)
		{
			this.updatePeriodInSeconds = theUpdatePeriodInSeconds;
		}

		/**
		 * Get the cc feed date format
		 * @return the cc feed date format
		 * @see DateFormat
		 */
		public String getFeedDateFormat()
		{
			return this.feedDateFormat;
		}

		/**
		 * Set the cc feed date format
		 * @param theFeedDateFormat the cc feed date format
		 * @see DateFormat
		 */
		public void setFeedDateFormat(String theFeedDateFormat)
		{
			this.feedDateFormat = theFeedDateFormat;
		}
		
		//////////////////////////////
		// File persistence
		//////////////////////////////
		
		/**
		 * Load the properties from the {@link #USER_PROPERTIES_FILE} file in the
		 * user home directory.
		 */
		public void loadFromFile() throws FileNotFoundException, IOException
		{
			// Load the content of the properties file into a Properties object
			Properties ccMonitorProperties = new Properties();
			File ccMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
			if (ccMonitorPropertiesFile.exists())
			{
				FileInputStream ccMonitorPropertiesFileIS = new FileInputStream(ccMonitorPropertiesFile);
				ccMonitorProperties.load(ccMonitorPropertiesFileIS);
				ccMonitorPropertiesFileIS.close();
			}
			
			// Update the attributes using the values defined in the properties file
			synchronized (this)
			{
				setRssFeedUrl(ccMonitorProperties.getProperty(CC_RSS_FEED_URL_PROPERTY_KEY));

				String updatePeriodInSecondsAsString = ccMonitorProperties.getProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY);
				if (updatePeriodInSecondsAsString != null)
				{
					try
					{
						setUpdatePeriodInSeconds(Integer.parseInt(updatePeriodInSecondsAsString));
					}
					catch(NumberFormatException e)
					{
						// Use a default value
						setUpdatePeriodInSeconds(300);
					}
				}
				setFeedDateFormat(ccMonitorProperties.getProperty(CC_FEED_DATE_FORMAT_PROPERTY_KEY));
			}
		}
		
		/**
		 * Save the properties from the {@link #USER_PROPERTIES_FILE} file in the
		 * user home directory.
		 */
		public void saveToFile() throws FileNotFoundException, IOException
		{
			// Build a Properties object that contains the values of the cc properties
			Properties ccMonitorProperties = new Properties();
			synchronized (this)
			{
				ccMonitorProperties.setProperty(CC_RSS_FEED_URL_PROPERTY_KEY, this.rssFeedUrl);
				ccMonitorProperties.setProperty(CC_FEED_DATE_FORMAT_PROPERTY_KEY, this.feedDateFormat);
				ccMonitorProperties.setProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY, "" + this.getUpdatePeriodInSeconds());
			}
			
			// Store the Properties object in the file
			File ccMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
			FileOutputStream ccMonitorPropertiesOutputStream = new FileOutputStream(ccMonitorPropertiesFile);
			ccMonitorProperties.store(ccMonitorPropertiesOutputStream, "File last updated on " + new Date());
			ccMonitorPropertiesOutputStream.close();
		}
	}

	//////////////////////////////////
	// Instance attributes
	//////////////////////////////////

	/**
	 * The RssFeedReader instance that we use for monitoring CC rss feed.
	 */
	private RssFeedReader rssFeedReader = null;

	/**
	 * A Map that contains the last builds status indexed by build project name
	 */
	private Map<String, RssFeedItem> lastBuildsStatus;

	/**
	 * The instance of the build monitor application this thread is running for
	 */
	private BuildMonitor buildMonitorInstance = null;

	/**
	 * Should the thread stop its execution ?
	 */
	private boolean stop = false;
	
	private CruiseControlProperties ccProperties = new CruiseControlProperties();
	
	private CruiseControlPropertiesDialog optionsDialog = null;

	//////////////////////////////////
	// Constructor
	//////////////////////////////////

	/**
	 * Create a new instance.
	 * 
	 * @param theBuildMonitorInstance
	 *            the build monitor instance this thread is running for
	 */
	public CruiseControlRssMonitor(BuildMonitor theBuildMonitorInstance) throws FileNotFoundException, IOException
	{
		this.buildMonitorInstance = theBuildMonitorInstance;

		// build the options dialog
		this.optionsDialog = new CruiseControlPropertiesDialog(null, true);
		this.optionsDialog.setIconImage(this.buildMonitorInstance.getDialogsDefaultIcon());
		this.optionsDialog.setTitle("CruiseControl server monitoring parameters");
		this.optionsDialog.pack();

		// load the monitor properties
		this.ccProperties.loadFromFile();

		// if at least one of the properties is not defined, open a window to ask for their definition
		if ((this.ccProperties.getRssFeedUrl() == null) || (this.ccProperties.getUpdatePeriodInSeconds() == null) || (this.ccProperties.getFeedDateFormat() == null))
		{
			displayOptionsDialog(true);
			if (this.optionsDialog.getLastClickedButton() != CruiseControlPropertiesDialog.BUTTON_OK)
			{
				System.exit(0);
			}
		}

	}

	//////////////////////////////////
	// Monitor implementation
	//////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		while (!this.stop)
		{
			// update the build status
			try
			{
				Integer updatePeriodInSeconds = null;
				try
				{
					synchronized (this.ccProperties)
					{
						this.rssFeedReader = new RssFeedReader(new URL(this.ccProperties.getRssFeedUrl()), new SimpleDateFormat(this.ccProperties.getFeedDateFormat()));
						updatePeriodInSeconds = this.ccProperties.getUpdatePeriodInSeconds();
					}
				}
				catch (MalformedURLException e)
				{
					throw new MonitoringException("The RSS Feed URL is not a valid URL.", e, true, null);
				}					
				updateStatus();
				updateBuildStatusGui();
				// wait before updating the build status again
				try
				{
					Thread.sleep(updatePeriodInSeconds * 1000);
				} catch (InterruptedException e)
				{
				}
			}
			catch (MonitoringException e)
			{
				this.buildMonitorInstance.reportMonitoringException(e);
				// wait one second before trying again
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e2)
				{
					// Nothing to do: continue !
				}
			}

		}
	}

	/**
	 * {@inheritDoc}
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
			synchronized (this.ccProperties)
			{
				returnedValue = new URI(this.ccProperties.getMainPageDeducedFromRssFeedUrl());
			}
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
		URI returnedValue = null;
		try
		{
			synchronized (this.ccProperties)
			{
				returnedValue = new URI(this.ccProperties.getMainPageDeducedFromRssFeedUrl() + "/buildresults/" + theIdOfTheBuild);
			}
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
	public String getSystemTrayIconTooltipHeader()
	{
		synchronized (this.ccProperties)
		{
			return "Monitoring CC server at " + this.ccProperties.getMainPageDeducedFromRssFeedUrl();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void displayOptionsDialog()
	{
		displayOptionsDialog(false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getMonitoredBuildSystemName()
	{
		return "Cruise Control";
	}

	//////////////////////////////////
	// Utilities methods
	//////////////////////////////////

	/**
	 * TODO: DOCUMENT ME !
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

		this.buildMonitorInstance.updateBuildStatus(buildsReport);
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

	//////////////////////////////
	// Private methods
	//////////////////////////////

	private void displayOptionsDialog(boolean isDialogOpenedForPropertiesCreation)
	{
		if (!this.optionsDialog.isVisible())
		{
			// Init Rss Feed URL field
			if (this.ccProperties.getRssFeedUrl() != null)
			{
				this.optionsDialog.rssFeedURLField.setText(this.ccProperties.getRssFeedUrl());
			}
			else
			{
				this.optionsDialog.rssFeedURLField.setText("http://localhost:8080/rss");
			}
			
			// Init Feed Date Format field
			if (this.ccProperties.getFeedDateFormat() != null)
			{
				this.optionsDialog.dateFormatField.setText(this.ccProperties.getFeedDateFormat());
			}
			else
			{
				this.optionsDialog.dateFormatField.setText("EEE, dd MMM yyyy HH:mm:ss Z");
			}
			
			// Init update period (in minutes) field
			if (this.ccProperties.getUpdatePeriodInSeconds() != null)
			{
				this.optionsDialog.updatePeriodField.setValue(this.ccProperties.getUpdatePeriodInSeconds() / 60);
			}
			else
			{
				this.optionsDialog.updatePeriodField.setValue(5);			
			}

			// If the dialog is opened for properties edition (not creation), update fields status (ok / error)
			if (!isDialogOpenedForPropertiesCreation)
			{
				this.optionsDialog.updateBaseURLFieldStatus();
				// TODO: this.optionsDialog.updateDateFormatFieldStatus();
			}

			// Show the options dialog
			if (!this.optionsDialog.isDisplayable())
			{
				this.optionsDialog.pack();
			}
			this.optionsDialog.setVisible(true);
			this.optionsDialog.toFront();

			if (this.optionsDialog.getLastClickedButton() == BambooPropertiesDialog.BUTTON_OK)
			{
				// Update the properties and save them
				synchronized (this.ccProperties)
				{
					this.ccProperties.setRssFeedUrl(this.optionsDialog.rssFeedURLField.getText());
					this.ccProperties.setFeedDateFormat(this.optionsDialog.dateFormatField.getText());
					this.ccProperties.setUpdatePeriodInSeconds((Integer) (this.optionsDialog.updatePeriodField.getValue()) * 60);
				}
				try
				{
					this.ccProperties.saveToFile();
				}
				catch (FileNotFoundException e)
				{
					throw new RuntimeException(e);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
				
				// make sure that the new properties are taken into account immediately ?
				this.buildMonitorInstance.reportConfigurationUpdatedToBeTakenIntoAccountImmediately();
			}			
		}
		else
		{
			// Give focus to the options windows if it masked by another window
			this.optionsDialog.setVisible(true);
			this.optionsDialog.toFront();
		}
	}
}
