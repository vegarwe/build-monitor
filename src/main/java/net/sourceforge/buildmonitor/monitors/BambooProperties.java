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
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;

import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;

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

	private String serverBaseUrl;
	private String username;
	private String password;
	private Integer updatePeriodInSeconds;
	private Boolean favouriteProjectsOnly;

	public BambooProperties()
	{
		this.serverBaseUrl = "http://localhost:8085";
		this.username = "";
		this.password = "";
		this.updatePeriodInSeconds = 300;
		this.favouriteProjectsOnly = new Boolean(false);
	}

	/**
	 * Load the properties from the {@link #USER_PROPERTIES_FILE} file in the
	 * user home directory.
	 */
	public void loadFromFile() throws FileNotFoundException, IOException
	{
		loadFromFile(new File(System.getProperty("user.home"), USER_PROPERTIES_FILE));
	}
	
	/**
	 * Load the properties file
         * @param bambooMonitorPropertiesFile the properties file
	 */
	public void loadFromFile(File bambooMonitorPropertiesFile) throws FileNotFoundException, IOException
	{
		// Load the content of the properties file into a Properties object
		Properties bambooMonitorProperties = new Properties();
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
			setFavouriteProjectsOnly(bambooMonitorProperties.getProperty(BAMBOO_FAVOURITE_PROJECTS_ONLY));
			String proppassword = bambooMonitorProperties.getProperty(BAMBOO_PASSWORD_PROPERTY_KEY);
			if (proppassword != null && proppassword.startsWith("{base64}"))
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
			bambooMonitorProperties.setProperty(BAMBOO_SERVER_BASE_URL_PROPERTY_KEY, getServerBaseUrl());
			bambooMonitorProperties.setProperty(BAMBOO_USERNAME_PROPERTY_KEY, getUsername());
			bambooMonitorProperties.setProperty(BAMBOO_PASSWORD_PROPERTY_KEY, proppassword);
			bambooMonitorProperties.setProperty(BAMBOO_FAVOURITE_PROJECTS_ONLY, "" + getFavouriteProjectsOnly());
			bambooMonitorProperties.setProperty(UPDATE_PERIOD_IN_SECONDS_PROPERTY_KEY, "" + getUpdatePeriodInSeconds());
		}
		
		// Store the Properties object in the file
		File bambooMonitorPropertiesFile = new File(System.getProperty("user.home"), USER_PROPERTIES_FILE);
		FileOutputStream buildMonitorPropertiesOutputStream = new FileOutputStream(bambooMonitorPropertiesFile);
		bambooMonitorProperties.store(buildMonitorPropertiesOutputStream, "File last updated on " + new Date());
		buildMonitorPropertiesOutputStream.close();
	}

	public BambooPropertiesDialog displayOptionsDialog(boolean isDialogOpenedForPropertiesCreation)
	{

		BambooPropertiesDialog optionsDialog = new BambooPropertiesDialog(null, true);
		//optionsDialog.setIconImage(buildMonitorInstance.getDialogsDefaultIcon());
		optionsDialog.setTitle("Bamboo server monitoring parameters");
		optionsDialog.pack();

		if (optionsDialog.isVisible())
		{
			optionsDialog.setVisible(true);
			optionsDialog.toFront();
			return optionsDialog;
		}

		optionsDialog.baseURLField.setText(getServerBaseUrl());
		optionsDialog.usernameField.setText(getUsername());
		optionsDialog.passwordField.setText(getPassword());
		optionsDialog.updatePeriodField.setValue(getUpdatePeriodInSeconds() / 60);
		optionsDialog.favouriteProjectsOnly.setSelected(getFavouriteProjectsOnly());

		// If the dialog is opened for properties edition (not creation), update fields status (ok / error)
		if (!isDialogOpenedForPropertiesCreation)
		{
			optionsDialog.updateBaseURLFieldStatus();
			optionsDialog.updateUsernameFieldStatus();
			optionsDialog.updatePasswordFieldStatus();
		}

		// Show the options dialog
		if (!optionsDialog.isDisplayable())
		{
			optionsDialog.pack();
		}
		optionsDialog.setVisible(true);
		optionsDialog.toFront();

		if (optionsDialog.getLastClickedButton() == BambooPropertiesDialog.BUTTON_OK)
		{
			// Update the properties and save them
			synchronized (this)
			{
				setServerBaseUrl(optionsDialog.baseURLField.getText());
				setUsername(optionsDialog.usernameField.getText());
				setPassword(new String(optionsDialog.passwordField.getPassword()));
				setUpdatePeriodInSeconds((Integer) (optionsDialog.updatePeriodField.getValue()) * 60);
				setFavouriteProjectsOnly(optionsDialog.favouriteProjectsOnly.isSelected());
			}
			try
			{
				saveToFile();
			}
			catch (FileNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}			
		return optionsDialog;
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
}
