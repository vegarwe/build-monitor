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
import java.io.FileNotFoundException;
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
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.apache.commons.codec.binary.Base64;

import net.sourceforge.buildmonitor.BuildMonitor;
import net.sourceforge.buildmonitor.BuildReport;
import net.sourceforge.buildmonitor.MonitoringException;
import net.sourceforge.buildmonitor.BuildReport.Status;
import net.sourceforge.buildmonitor.dialogs.BambooPropertiesDialog;


/**
 * The run method of this Runnable implementation monitor a Bamboo build
 * 
 * @author sbrunot
 * 
 */
public class BambooMonitor implements Monitor
{
	private class BuildPlan
	{
		public String key;
		public String name;
	}

	private static final String URL_ENCODING = "UTF-8";

	private BuildMonitor buildMonitorInstance = null;
	private boolean stop = false;
	private BambooProperties bambooProperties = new BambooProperties();
	private BambooPropertiesDialog optionsDialog = null;
	
	public BambooMonitor(BuildMonitor buildMonitorInstance) throws FileNotFoundException, IOException
	{
		this.buildMonitorInstance = buildMonitorInstance;

		bambooProperties.loadFromFile();

		if (monitorPropertiesNotDefined())
		{
			BambooPropertiesDialog optionsDialog = displayOptionsDialog(true);
			if (optionsDialog.getLastClickedButton() != BambooPropertiesDialog.BUTTON_OK)
			{
				System.exit(0);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		while (!stop)
		{
			try
			{
				String bambooServerBaseUrl  = bambooProperties.getServerBaseUrl();
				List<BuildReport> lastBuildStatus = new ArrayList<BuildReport>();
				for (BuildPlan plan : getProjects(bambooServerBaseUrl))
				{
					lastBuildStatus.addAll(getResultsForProject(bambooServerBaseUrl, plan));
				}
	
				buildMonitorInstance.updateBuildStatus(lastBuildStatus);
				sleepInSeconds(bambooProperties.getUpdatePeriodInSeconds());
			}
			catch (MonitoringException e)
			{
				buildMonitorInstance.reportMonitoringException(e);
				sleepInSeconds(1);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void stop()
	{
		stop = true;
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

	private BambooPropertiesDialog displayOptionsDialog(boolean isDialogOpenedForPropertiesCreation)
	{
		BambooPropertiesDialog optionsDialog = null;
		optionsDialog = bambooProperties.displayOptionsDialog(isDialogOpenedForPropertiesCreation);

		if (optionsDialog.getLastClickedButton() == BambooPropertiesDialog.BUTTON_OK)
		{
			// make sure that the new properties are taken into account immediately ?
			buildMonitorInstance.reportConfigurationUpdatedToBeTakenIntoAccountImmediately();
		}
		return optionsDialog;
	}

	
	private List<BuildPlan> getProjects(String bambooServerBaseUrl) throws MonitoringException
	{
		List returnList = new ArrayList<BuildReport>();
		try
		{
			String methodURL = bambooServerBaseUrl + "/rest/api/latest/plan"
					+ "?os_authType=basic";
			if (bambooProperties.getFavouriteProjectsOnly())
			{
				methodURL += "&favourite";
			}
			String serverResponse = callBambooApi(new URL(methodURL));
			InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));

			NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate("/plans/plans/plan", serverResponseIS, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Element e = (Element) nodes.item(i);
				BuildPlan plan = new BuildPlan();
				plan.key = e.getAttribute("key");
				plan.name = e.getAttribute("name");
				returnList.add(plan);
			}
		}
		catch (Throwable t)
		{
			throw new MonitoringException(t, null);
		}
		return returnList;
	}

	private List<BuildReport> getResultsForProject(String bambooServerBaseUrl, BuildPlan plan) throws MonitoringException
	{
		List returnList = new ArrayList<BuildReport>();
		try
		{
			String methodURL = bambooServerBaseUrl + "/rest/api/latest/result/" + plan.key
					+ "?os_authType=basic"
					+ "&expand=results[0].result";
			if (bambooProperties.getFavouriteProjectsOnly())
			{
				methodURL += "&favourite";
			}
			String serverResponse = callBambooApi(new URL(methodURL));
			InputSource serverResponseIS = new InputSource(new StringReader(serverResponse));

			NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate("/results/results/result", serverResponseIS, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Element result = (Element) nodes.item(i);
				String dateString = getNamedChildNodeValue(result, "buildCompletedTime");
				String buildState = result.getAttribute("state");

				BuildReport report = new BuildReport();
				report.setId(result.getAttribute("key"));
				report.setName(plan.name);
				report.setDate(parseDate(dateString));
				report.setStatus(parseBuildState(buildState));

				returnList.add(report);
			}

		}
		catch (Throwable t)
		{
			throw new MonitoringException(t, null);
		}
		return returnList;
	}

	private String getNamedChildNodeValue(Node node, String nodeName) throws MonitoringException
	{
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			if (nodeName.equals(nodes.item(i).getNodeName()))
			{
				return nodes.item(i).getFirstChild().getNodeValue();
			}
		}
		throw new MonitoringException("Unable to find node with name" + nodeName, null);
	}

	private Date parseDate(String dateString) throws MonitoringException
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		// Strip the ':' in the timezone of "2011-08-12T11:25:48.000+02:00"
		dateString = dateString.substring(0, 26) + dateString.substring(27);

		try
		{
			return dateFormat.parse(dateString);
		}
		catch (ParseException e)
		{
			throw new MonitoringException(e, null);
		}
	}

	private Status parseBuildState(String buildState) throws MonitoringException
	{
		if ("Successful".equals(buildState))
		{
			return Status.OK;
		}
		else if ("Failed".equals(buildState))
		{
			return Status.FAILED;
		}
		else if ("".equals(buildState))
		{
			//return Status.EMPTY;
			return Status.FAILED;
		}
		else
		{
			throw new MonitoringException("Unknown build state '" + buildState + "' returned", null);
		}
	}
	
	/**
	 * Call a bamboo REST api method and return the result (or throw a MonitoringException)
	 * @param url
	 * @return
	 * ticket is not valid (anymore) and needs to be renewed.
	 */
	private String callBambooApi(URL url) throws MonitoringException
	{
		String returnedValue = null;
		try
		{
			returnedValue = getServerResponse(url);
		}
		catch (ClassCastException e)
		{
			throw new MonitoringException("Problem: the base URL defined for the Bamboo server in Options is not an http URL.", true, null);
		}
		catch (UnknownHostException e)
		{
			throw new MonitoringException("Problem: cannot find host " + url.getHost() + " on the network.", true, null);
		}
		catch (ConnectException e)
		{
			throw new MonitoringException("Problem: cannot connect to port " + url.getPort() + " on host " + url.getHost() + ".", true, null);
		}
		catch (FileNotFoundException e)
		{
			throw new MonitoringException("Problem: cannot find the Bamboo server REST api using the base URL defined for the Bamboo server in Options. Seems that this URL is not the one to your Bamboo server home page...", true, null);
		}
		catch(SocketException e)
		{
			throw new MonitoringException("Problem: network error, connection lost.", null);
		}
		catch (IOException e)
		{
			if (e.getMessage().contains("Server returned HTTP response code: 401"))
			{
				throw new MonitoringException("Problem: Authentication failed. Please check your username and password", null);
			}
			else
			{
				throw new MonitoringException(e, null);
			}
		}

		if (returnedValue.contains("<title>Bamboo Setup Wizard - Atlassian Bamboo</title>"))
		{
			throw new MonitoringException("Your Bamboo server installation is not finished! Double click here to complete the Bamboo Setup Wizard !", getMainPageURI());
		}

		return returnedValue;
	}

	private String getServerResponse(URL url) throws IOException
	{
		String authString = bambooProperties.getUsername() + ":" + bambooProperties.getPassword();
		authString = new String(Base64.encodeBase64(authString.getBytes()));

		String returnedValue = null;
		HttpURLConnection urlConnection = null;
		BufferedReader urlConnectionReader = null;
		try
		{
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setRequestProperty("Authorization", "Basic " + authString);
			urlConnection.connect();
			urlConnectionReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			String line = null;
			StringBuffer serverResponse = new StringBuffer();
			while ((line = urlConnectionReader.readLine()) != null)
			{
				serverResponse.append(line);
			}
			returnedValue = serverResponse.toString();
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

	private boolean monitorPropertiesNotDefined()
	{
		return (
			(bambooProperties.getServerBaseUrl() == null) ||
			(bambooProperties.getUpdatePeriodInSeconds() == null) ||
			(bambooProperties.getUsername() == null) ||
			(bambooProperties.getPassword() == null));
	}

	private void sleepInSeconds(Integer seconds)
	{
		try
		{
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e)
		{
			// Nothing to do: continue!
		}
	}
}
