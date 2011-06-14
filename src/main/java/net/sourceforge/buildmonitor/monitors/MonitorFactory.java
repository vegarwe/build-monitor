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

import net.sourceforge.buildmonitor.BuildMonitorImpl;
import net.sourceforge.buildmonitor.Launcher;


/**
 * Factory class for creating the correct Monitor implementation based on the
 * input string
 *
 * @author vegarwe
 *
 */
public class MonitorFactory
{
	private String monitorName;

	public MonitorFactory(String theMonitorName)
	{
		this.monitorName = theMonitorName;
	}

	public Monitor getMonitor(BuildMonitorImpl impl) throws java.io.FileNotFoundException, java.io.IOException
	{
		if (Launcher.BAMBOO_MONITOR.equals(monitorName))
		{
			return new BambooMonitor(impl);
		}
		else if (Launcher.CRUISE_CONTROL_MONITOR.equals(monitorName))
		{
			return new CruiseControlRssMonitor(impl);
		}
		else
		{
			throw new RuntimeException("\n\n" + monitorName + " is not a supported monitor.\n"
					+ "Supported monitors are:\n"
					+ "\tbamboo (for monitoring Atlassian Bamboo continuous build server)\n"
					+ "\tcc (for monitoring Cruise Control continuous build server)\n");
		}
		
	}
}
