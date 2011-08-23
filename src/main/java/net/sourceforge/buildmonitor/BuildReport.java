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

import java.util.Comparator;
import java.util.Date;

/**
 * Build report from a monitor.
 * @author sbrunot
 *
 */
public class BuildReport
{
	/**
	 * A comparator to use to compare BuildReport instance according to their names.
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	public static class NameComparator implements Comparator<BuildReport>
	{

		public int compare(BuildReport o1, BuildReport o2)
		{
			int returnedValue = 0;
			if ((o1 == null) || (o2 == null))
			{
				throw new IllegalArgumentException("Cannot compare with null object");
			}
			String firstBuildReportName = o1.getName();
			String secondBuildReportName = o2.getName();
			
			if (firstBuildReportName == null)
			{
				if (secondBuildReportName == null)
				{
					returnedValue = 0;
				}
				else
				{
					returnedValue = -1;
				}
			}
			else
			{
				if (secondBuildReportName == null)
				{
					returnedValue = 1;
				}
				else
				{
					returnedValue = firstBuildReportName.compareTo(secondBuildReportName);
				}
			}
			return returnedValue;
		}	
	}
	
	/**
	 * A comparator to use to compare BuildReport instance according to their age.
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	public static class AgeComparator implements Comparator<BuildReport>
	{

		public int compare(BuildReport o1, BuildReport o2)
		{
			int returnedValue = 0;
			if ((o1 == null) || (o2 == null))
			{
				throw new IllegalArgumentException("Cannot compare with null object");
			}
			Date firstBuildReportDate = o1.getDate();
			Date secondBuildReportDate = o2.getDate();
			
			if (firstBuildReportDate == null)
			{
				if (secondBuildReportDate == null)
				{
					returnedValue = 0;
				}
				else
				{
					returnedValue = -1;
				}
			}
			else
			{
				if (secondBuildReportDate == null)
				{
					returnedValue = 1;
				}
				else
				{
					returnedValue = firstBuildReportDate.compareTo(secondBuildReportDate);
				}
			}
			return returnedValue;
		}
		
	}

	/**
	 * Status of a build
	 * @author sbrunot
	 *
	 */
	public enum Status {OK, FAILED, EMPTY};

	/**
	 * Id of the build
	 */
	private String id;
	
	/**
	 * Name of the build
	 */
	private String name;
	
	/**
	 * status of the build
	 */
	private Status status;
	
	/**
	 * Date of the build
	 */
	private Date date;

	/**
	 * No args constructor
	 */
	public BuildReport()
	{
		
	}
	
	/**
	 * Create a new instance from an id, a status and a date
	 * @param id the id of the build
	 * @param date the date of the build
	 * @param status the status of the build
	 */
	public BuildReport(String id, Date date, Status status)
	{
		this.id = id;
		this.date = date;
		this.status = status;
	}
	
	/**
	 * Get the date of the build
	 *  @return the date of the build
	 */
	public Date getDate()
	{
		return this.date;
	}

	/**
	 * Set the date of the build
	 * @param dateOfTheBuild the date of the build
	 */
	public void setDate(Date dateOfTheBuild)
	{
		this.date = dateOfTheBuild;
	}

	/**
	 * Get the id of the build
	 * @return the id of the build
	 */
	public String getId()
	{
		return this.id;
	}

	/**
	 * Set the id of the build
	 * @param idOfTheBuild
	 */
	public void setId(String idOfTheBuild)
	{
		this.id = idOfTheBuild;
	}

	/**
	 * Get the name of the build
	 * @return the name of the build
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Set the name of the build
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Get the status of the build
	 * @return the status of the build
	 */
	public Status getStatus()
	{
		return this.status;
	}

	/**
	 * Set the status of the build
	 * @param statusOfTheBuild the statud of the build
	 */
	public void setStatus(Status statusOfTheBuild)
	{
		this.status = statusOfTheBuild;
	}

	/**
	 * Does this build report signal a failed build ?
	 * @return
	 */
	public boolean hasFailed()
	{
		return Status.FAILED.equals(this.status);
	}
}
