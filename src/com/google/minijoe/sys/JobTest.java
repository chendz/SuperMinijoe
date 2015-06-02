package com.google.minijoe.sys;

import com.xeiam.sundial.SundialJobScheduler;

public class JobTest {

	public static void main(String[] args) {
		SundialJobScheduler.startScheduler();
		
		// TODO Auto-generated method stub
		SundialJobScheduler.addJob("SampleJob", "com.google.minijoe.sys.SampleJob");
		
		SundialJobScheduler.startJob("SampleJob");
	}

}
