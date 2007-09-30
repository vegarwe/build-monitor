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

/**
 * TODO: DOCUMENTS ME !
 * Monitor instance may be interrupted at any time by the build monitor main thread in
 * order to immediately take into account a change in the configuration.
 * @author sbrunot
 *
 */
public interface Monitor extends Runnable
{
	/**
	 * stop the monitor.
	 */
	public void stop();
	
	/**
	 * Return the name of the monitored build system (to be used in tray icon popup menu entries)
	 * @return the name of the monitored build system
	 */
	public String getMonitoredBuildSystemName();

	/**
	 * Return an URI to the main page of the monitored build system
	 */
	public URI getMainPageURI();
	
	/**
	 * Return an URI to the dedicated page of a build on the monitored build system
	 * @param theIdOfTheBuild the Id of the build (as in the BuildReport)
	 * @return an URI to the dedicated page of a build on the monitored build system
	 */
	public URI getBuildURI(String theIdOfTheBuild);
	
	/**
	 * Get the String to display as the first line of the Tooltip of the system tray icon.
	 * @return the String to display as the first line of the Tooltip of the system tray icon.
	 */
	public String getSystemTrayIconTooltipHeader();
	
	/**
	 * Display the options dialog on screen.
	 */
	public void displayOptionsDialog();
}
