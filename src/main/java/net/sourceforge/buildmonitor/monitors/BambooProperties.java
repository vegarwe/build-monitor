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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;

import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;

import org.xml.sax.InputSource;

/**
 * Set of properties needed to monitor a bamboo server.
 */
public class BambooProperties
{
	private static final String BAMBOO_PASSWORD_PROPERTY_KEY = "bamboo.password";
	private static final String BAMBOO_USERNAME_PROPERTY_KEY = "bamboo.username";
	private static final String UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY = "update.period.in.seconds";
	private static final String BAMBOO_SERVER_BASE_URL_PROPERTY_KEY = "bamboo.server.base.url";
	private static final String BAMBOO_PROJECT_PROPERTY_KEY = "bamboo.server.project_keys";
	private static final String BAMBOO_FAVOURITE_PROJECTS_ONLY = "bamboo.favourite.projects.only";
	private static final String USER_PROPERTIES_FILE = "bamboo-monitor.properties";

	private String serverBaseUrl = null;
	private String username = null;
	private String password = null;
	private Integer updatePeriodInSeconds = null;
	private Boolean favouriteProjectsOnly = null;
	private String projectKeys = null;
	
	/**
	 * Load the properties from the {@link #USER_PROPERTIES_FILE} file in the
	 * user home directory.
	 */
	public void loadFromFile() throws FileNotFoundException, IOException
	{
		// Load the content of the properties file into a Properties object
		Properties bambooMonitorProperties = new Properties();
		File bambooMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
		if (bambooMonitorPropertiesFile.exists())
		{
			FileInputStream bambooMonitorPropertiesFileIS = new FileInputStream(bambooMonitorPropertiesFile);
			bambooMonitorProperties.load(bambooMonitorPropertiesFileIS);
			bambooMonitorPropertiesFileIS.close();
		}
		
		// Update the attributes using the values defined in the properties file
		synchronized (this)
		{
			setServerBaseUrl(bambooMonitorProperties.getProperty(BAMBOO_SERVER_BASE_URL_PROPERTY_KEY));
			setUpdatePeriodInSeconds(bambooMonitorProperties.getProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY));
			setUsername(bambooMonitorProperties.getProperty(BAMBOO_USERNAME_PROPERTY_KEY));
			setProjectKeys(bambooMonitorProperties.getProperty(BAMBOO_PROJECT_PROPERTY_KEY));
			setFavouriteProjectsOnly(bambooMonitorProperties.getProperty(BAMBOO_FAVOURITE_PROJECTS_ONLY));
			String proppassword = bambooMonitorProperties.getProperty(BAMBOO_PASSWORD_PROPERTY_KEY);
			if (proppassword.startsWith("{base64}"))
			{
				proppassword = proppassword.substring(8);
				proppassword = new String(Base64.decodeBase64(proppassword.getBytes()));
			}
			setPassword(proppassword);
		}
	}
	
	/**
	 * Save the properties from the {@link #USER_PROPERTIES_FILE} file in the
	 * user home directory.
	 */
	public void saveToFile() throws FileNotFoundException, IOException
	{
		// Build a Properties object that contains the values of the bamboo properties
		Properties bambooMonitorProperties = new Properties();
		synchronized (this)
		{
			String proppassword = "{base64}" + new String(Base64.encodeBase64(getPassword().getBytes()));
			String propProjectKeys = this.projectKeys;
			if (propProjectKeys == null)
			{
				propProjectKeys = "";
			}
			bambooMonitorProperties.setProperty(BAMBOO_SERVER_BASE_URL_PROPERTY_KEY, getServerBaseUrl());
			bambooMonitorProperties.setProperty(BAMBOO_USERNAME_PROPERTY_KEY, getUsername());
			bambooMonitorProperties.setProperty(BAMBOO_PASSWORD_PROPERTY_KEY, proppassword);
			bambooMonitorProperties.setProperty(BAMBOO_FAVOURITE_PROJECTS_ONLY, "" + getFavouriteProjectsOnly());
			bambooMonitorProperties.setProperty(BAMBOO_PROJECT_PROPERTY_KEY, propProjectKeys);
			bambooMonitorProperties.setProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY, "" + getUpdatePeriodInSeconds());
		}
		
		// Store the Properties object in the file
		File bambooMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
		FileOutputStream buildMonitorPropertiesOutputStream = new FileOutputStream(bambooMonitorPropertiesFile);
		bambooMonitorProperties.store(buildMonitorPropertiesOutputStream, "File last updated on " + new Date());
		buildMonitorPropertiesOutputStream.close();
	}


	/**
	 * Get URL to the bamboo server
	 * @return the URL to the bamboo server
	 */
	public String getServerBaseUrl()
	{
		return this.serverBaseUrl;
	}

	/**
	 * Set URL to the bamboo server
	 * @param serverBaseUrl the URL to the bamboo server
	 */
	public void setServerBaseUrl(String serverBaseUrl)
	{
		this.serverBaseUrl = serverBaseUrl;
		// trim the remaining / in server base url if it exists
		if (this.serverBaseUrl != null && this.serverBaseUrl.endsWith("/"))
		{
			this.serverBaseUrl = this.serverBaseUrl.substring(0, this.serverBaseUrl.length() - 1);
		}
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
	 * @param updatePeriodInSeconds the period (in seconds) of build status update
	 */
	public void setUpdatePeriodInSeconds(Integer updatePeriodInSeconds)
	{
		this.updatePeriodInSeconds = updatePeriodInSeconds;
	}

	/**
	 * Set the period (in seconds) of build status update
	 * @param updatePeriodInSeconds the period (in seconds) of build status update
	 */
	public void setUpdatePeriodInSeconds(String updatePeriodInSeconds)
	{
		if (updatePeriodInSeconds != null)
		{
			try
			{
				setUpdatePeriodInSeconds(Integer.parseInt(updatePeriodInSeconds));
			}
			catch(NumberFormatException e)
			{
				setUpdatePeriodInSeconds(300);
			}
		}
		else
		{
			setUpdatePeriodInSeconds(300);
		}
	}

	/**
	 * Get the bamboo user name
	 * @return the bamboo user name
	 */
	public String getUsername()
	{
		return this.username;
	}

	/**
	 * Set the bamboo user name
	 * @param username the bamboo user name
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	/**
	 * Get the bamboo user password
	 */
	public String getPassword()
	{
		return this.password;
	}

	/**
	 * Set the bamboo user password
	 * @param password the bamboo user password
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	/**
	 * Get the favourite projects only flag
	 */
	public Boolean getFavouriteProjectsOnly()
	{
		return this.favouriteProjectsOnly;
	}

	/**
	 * Set the favourite projects only flag
	 * @param favouriteProjectsOnly the bamboo user password
	 */
	public void setFavouriteProjectsOnly(Boolean favouriteProjectsOnly)
	{
		this.favouriteProjectsOnly = favouriteProjectsOnly;
	}

	/**
	 * Set the favourite projects only flag
	 * @param favouriteProjectsOnly the bamboo user password
	 */
	public void setFavouriteProjectsOnly(String favouriteProjectsOnly)
	{
		if (favouriteProjectsOnly != null)
		{
			setFavouriteProjectsOnly(Boolean.parseBoolean(favouriteProjectsOnly));
		}
		else
		{
			setFavouriteProjectsOnly(new Boolean(false));
		}
	}
	
	/**
	 * Get the bamboo project keys
	 */
	public List<String> getProjectKeys()
	{
		if (this.projectKeys == null || this.projectKeys.equals(""))
		{
			return null;
		}
		return new ArrayList<String>(Arrays.asList(this.projectKeys.split(",")));
	}

	/**
	 * Set the bamboo project keys
	 * @param projectKeys the bamboo user password
	 */
	public void setProjectKeys(String projectKeys)
	{
		this.projectKeys = projectKeys;
	}
}
