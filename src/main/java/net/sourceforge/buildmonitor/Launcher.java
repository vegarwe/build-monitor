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

import java.security.Security;
import javax.swing.UIManager;

import net.sourceforge.buildmonitor.monitors.Monitor;
import net.sourceforge.buildmonitor.monitors.MonitorFactory;
import net.sourceforge.buildmonitor.monitors.BambooMonitor;

/**
 * The class to use to launch the BuildMonitorImpl application.
 * @author sbrunot
 *
 */
public class Launcher
{
	////////////////////////////////////////
	// Constants
	////////////////////////////////////////
	
	public static final String MONITOR_PARAMETER = "-monitor";
	
	public static final String BAMBOO_MONITOR = "bamboo";
	
	////////////////////////////////////////
	// Main
	////////////////////////////////////////
	
	/**
	 * Main method that launch an instance of the BuildMonitorImpl application
	 * @param args
	 * @TODO: DOCUMENT COMMAND LINE ARGS
	 */
	public static void main(String[] args)
	{
		// The monitor to use
		// TODO: DO NOT USE A DEFAULT VALUE, BUT PROMPT THE USER FOR THE MONITOR TO USE IF IT IS NOT DEFINED ON THE COMMAND LINE
		String monitor = BAMBOO_MONITOR;
		
		System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		
		// Check if a monitor has been specified on the command line
		if (args.length == 2)
		{
			// Verify that the first args is the monitor parameter
			if (MONITOR_PARAMETER.equals(args[0]))
			{
				monitor = args[1];
			}
		}

		// Set platform look & feel
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			// This should never occurs
			throw new RuntimeException(e);
		}

		// Create a new instance of BuildMonitor
		BuildMonitorImpl applicationInstance = new BuildMonitorImpl(new MonitorFactory(monitor));
		javax.swing.SwingUtilities.invokeLater(applicationInstance);
	}
}
