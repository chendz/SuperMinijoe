package com.google.minijoe.sys;

import com.xeiam.sundial.Job;
import com.xeiam.sundial.exceptions.JobInterruptException;

public class SampleJob extends Job{
	  @Override
	  public void doRun() throws JobInterruptException {
	    // Do something interesting...
		  System.out.println("work...");
	  }
}
