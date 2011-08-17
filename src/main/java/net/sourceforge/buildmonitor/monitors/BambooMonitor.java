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
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;

import org.xml.sax.InputSource;

/**
 * The run method of this Runnable implementation monitor a Bamboo build
 * 
 * @author sbrunot
 * 
 */
public class BambooMonitor implements Monitor
{
	//////////////////////////////
	// Constants
	//////////////////////////////

	private static final String URL_ENCODING = "UTF-8";

	//////////////////////////////
	// Instance attributes
	//////////////////////////////

	private BuildMonitor buildMonitorInstance = null;
	
	private boolean stop = false;

	private BambooProperties bambooProperties = new BambooProperties();

	private BambooPropertiesDialog optionsDialog = null;
	
	//////////////////////////////
	// Constructors
	//////////////////////////////

	public BambooMonitor(BuildMonitor buildMonitorInstance) throws FileNotFoundException, IOException
	{
		this.buildMonitorInstance = buildMonitorInstance;

		// build the options dialog
		this.optionsDialog = new BambooPropertiesDialog(null, true);
		this.optionsDialog.setIconImage(this.buildMonitorInstance.getDialogsDefaultIcon());
		this.optionsDialog.setTitle("Bamboo server monitoring parameters");
		this.optionsDialog.pack();

		// load the monitor properties
		this.bambooProperties.loadFromFile();

		// if at least one of the properties is not defined, open a window to ask for their definition
		if ((this.bambooProperties.getServerBaseUrl() == null) || (this.bambooProperties.getUpdatePeriodInSeconds() == null) || (this.bambooProperties.getUsername() == null) || (this.bambooProperties.getPassword() == null))
		{
			displayOptionsDialog(true);
			if (this.optionsDialog.getLastClickedButton() != BambooPropertiesDialog.BUTTON_OK)
			{
				System.exit(0);
			}
		}
	}

	//////////////////////////////
	// Monitor implementation
	//////////////////////////////
	
	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		while (!this.stop)
		{
			try
			{
				String bambooServerBaseUrl  = bambooProperties.getServerBaseUrl();

				List<BuildReport> lastBuildStatus = getFavouriteProjects(bambooServerBaseUrl);

				this.buildMonitorInstance.updateBuildStatus(lastBuildStatus);
	
				// wait before updating the build status again
				try
				{
					Thread.sleep(bambooProperties.getUpdatePeriodInSeconds() * 1000);
				} catch (InterruptedException e)
				{
					// Nothing to do: continue !
				}
			}
			catch (MonitoringException e)
			{
				this.buildMonitorInstance.reportMonitoringException(e);
				// wait one second before trying again
				try { Thread.sleep(1000); } catch (InterruptedException e2) { }
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
			synchronized (this.bambooProperties)
			{
				returnedValue = new URI(this.bambooProperties.getServerBaseUrl());
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
			synchronized (this.bambooProperties)
			{
				returnedValue = new URI(this.bambooProperties.getServerBaseUrl() + "/browse/" + theIdOfTheBuild);
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
		synchronized (this.bambooProperties)
		{
			return "Monitoring Bamboo server at " + this.bambooProperties.getServerBaseUrl();
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
		return "Bamboo server";
	}

	//////////////////////////////
	// Private methods
	//////////////////////////////

	private void displayOptionsDialog(boolean isDialogOpenedForPropertiesCreation)
	{
		bambooProperties.displayOptionsDialog(isDialogOpenedForPropertiesCreation, optionsDialog);

		if (optionsDialog.getLastClickedButton() == BambooPropertiesDialog.BUTTON_OK)
		{
			
			// make sure that the new properties are taken into account immediately ?
			buildMonitorInstance.reportConfigurationUpdatedToBeTakenIntoAccountImmediately();
		}
	}

	
	private String getNamedChildNodeValue(NodeList nodes, String nodeName) throws MonitoringException
	{
		for (int i = 0; i < nodes.getLength(); i++)
		{
			if (nodeName.equals(nodes.item(i).getNodeName()))
			{
				return nodes.item(i).getFirstChild().getNodeValue();
			}
		}
		throw new MonitoringException("Unable to find node with name" + nodeName, null);
	}

	/**
	 * Provides the latest build results for the given buildName.
	 * @param bambooServerBaseUrl
	 * @return
	 */
	private List<BuildReport> getFavouriteProjects(String bambooServerBaseUrl) throws MonitoringException
	{
		List returnList = new ArrayList<BuildReport>();
		try
		{
			String methodURL = bambooServerBaseUrl + "/rest/api/latest/result"
					+ "?os_authType=basic&os_username=" + URLEncoder.encode(bambooProperties.getUsername(), URL_ENCODING)
					+ "&os_password=" + URLEncoder.encode(bambooProperties.getPassword(), URL_ENCODING);
			if (bambooProperties.getFavouriteProjectsOnly())
			{
				methodURL += "&favourite";
			}
			String serverResponse = callBambooApi(new URL(methodURL));

			InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));
			System.out.println("frosken " + methodURL + "\n\n" + serverResponse);

			NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate("/results/results/result", serverResponseIS, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++)
			{
				BuildReport report = new BuildReport();
				Element result = (Element) nodes.item(i);
				//String buildState = XPathFactory.newInstance().newXPath().evaluate("buildState", serverResponseIS);
				System.out.println("fisken " + result.getAttribute("key"));
				report.setId(result.getAttribute("key"));
				report.setName(result.getAttribute("key"));

				String buildState = result.getAttribute("state");
				if ("Successful".equals(buildState))
				{
					report.setStatus(Status.OK);
				}
				else if ("Failed".equals(buildState))
				{
					report.setStatus(Status.FAILED);
				}
				else if ("".equals(buildState))
				{
					//returnedValue.setStatus(Status.EMPTY);
					report.setStatus(Status.FAILED);
				}
				else
				{
					throw new MonitoringException("Unknown build state '" + buildState + "' returned", null);
				}
				returnList.add(report);
			}

		}
		catch(MonitoringException e)
		{
			throw e;
		}
		catch (Throwable t)
		{
			throw new MonitoringException(t, null);
		}
		return returnList;
	}
	
	/**
	 * Call a bamboo REST api method and return the result (or throw a MonitoringException)
	 * @param theURL
	 * @return
	 * @throws BambooTicketNeedToBeRenewedError error thrown is the current bamboo authentication
	 * ticket is not valid (anymore) and needs to be renewed.
	 */
	private String callBambooApi(URL theURL) throws MonitoringException, BambooTicketNeedToBeRenewedError
	{
		String returnedValue = null;
		HttpURLConnection urlConnection = null;
		BufferedReader urlConnectionReader = null;
		try
		{
			// Call the Bamboo api and retrieve the server response
			urlConnection = (HttpURLConnection) theURL.openConnection();
			urlConnection.connect();
			urlConnectionReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line = null;
			StringBuffer serverResponse = new StringBuffer();
			while ((line = urlConnectionReader.readLine()) != null)
			{
				serverResponse.append(line);
			}
			// parse the server response
			returnedValue = serverResponse.toString();
			// TODO: IF THE SERVER INSTALLATION IS NOT FINISHED, AN HTML PAGE IS RETURNED BY THE URL... WE SHOULD TRY TO DETECT IF IT IS NOT THE CASE HERE !!!
			if (returnedValue.contains("<title>Bamboo Setup Wizard - Atlassian Bamboo</title>"))
			{
				// TODO: OUVRIR BROWSER VERS L'INSTANCE BAMBOO ?
				throw new MonitoringException("Your Bamboo server installation is not finished ! Double click here to complete the Bamboo Setup Wizard !", getMainPageURI());
			}
			InputSource is = new InputSource(new StringReader(serverResponse.toString()));
			XPath xpath = XPathFactory.newInstance().newXPath();
			String error = xpath.evaluate("/errors/error", is);
			if (!"".equals(error))
			{
				if ("User not authenticated yet, or session timed out.".equals(error))
				{
					// A new authentication ticket should be requested !
					throw new BambooTicketNeedToBeRenewedError();
				}
				else
				{
					boolean isErrorOptionsRelated = false;
					URI uriForNonOptionsRelatedErrors = getMainPageURI();
					if ("Invalid username or password.".equals(error))
					{
						isErrorOptionsRelated = true;
					}
					if ("The remote API has been disabled.".equals(error))
					{
						error += " Double click here to enable it.";
						// Build the URI to the Bamboo general configuration setting page (BASE_SERVER_URL/admin/configure!default.action)
						try
						{
							synchronized (this.bambooProperties)
							{
								uriForNonOptionsRelatedErrors = new URI(this.bambooProperties.getServerBaseUrl() + "/admin/configure!default.action");
							}
						}
						catch (URISyntaxException e)
						{
							throw new RuntimeException(e);
						}
					}

					throw new MonitoringException("Error reported by the Bamboo server: " + error, isErrorOptionsRelated, uriForNonOptionsRelatedErrors);
				}
			}
		}
		catch (ClassCastException e)
		{
			// This error should only occurs if the user has modified the properties file "by hand"
			throw new MonitoringException("Problem: the base URL defined for the Bamboo server in Options is not an http URL.", true, null);
		}
		catch (UnknownHostException e)
		{
			throw new MonitoringException("Problem: cannot find host " + theURL.getHost() + " on the network.", true, null);
		}
		catch (ConnectException e)
		{
			throw new MonitoringException("Problem: cannot connect to port " + theURL.getPort() + " on host " + theURL.getHost() + ".", true, null);
		}
		catch (FileNotFoundException e)
		{
			throw new MonitoringException("Problem: cannot find the Bamboo server REST api using the base URL defined for the Bamboo server in Options. Seems that this URL is not the one to your Bamboo server home page...", true, null);
		}
		catch(SocketException e)
		{
			throw new MonitoringException("Problem: network error, connection lost.", null);
		}
		catch (XPathExpressionException e)
		{
			throw new MonitoringException("Problem: the Bamboo Server returned an unexpected content for attribute <error>: " + returnedValue, null);
		}
		catch (MonitoringException e)
		{
			throw e;
		}
		catch (Throwable t)
		{
			throw new MonitoringException(t, null);
		}
		finally
		{
			if (urlConnectionReader != null)
			{
				try
				{
					urlConnectionReader.close();
				}
				catch (IOException e)
				{
					// Nothing to be done here
				}
			}
			if (urlConnection != null)
			{
				urlConnection.disconnect();
			}
		}
		return returnedValue;
	}

}
