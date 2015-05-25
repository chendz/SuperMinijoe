/**
 * Copyright 2011 - 2013 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeiam.sundial;

import org.quartz.core.JobExecutionContext;

import com.xeiam.sundial.exceptions.JobInterruptException;

/**
 * The highest class of the Job hierarchy which contains the ThreadLocal instance, the JobContext, and handles terminating of Jobs.
 *
 * @author timmolter
 */
public abstract class JobContainer {

  /** ThreadLocal container */
  private static ThreadLocal<JobContext> contextContainer = new ThreadLocal<JobContext>();

  /** terminate flag */
  private boolean terminate = false;

  /**
   * Initialize the ThreadLocal with a JobExecutionContext object
   *
   * @param pJobContext
   */
  protected void initContextContainer(JobExecutionContext jobExecutionContext) {

    JobContext jobContext = new JobContext();
    jobContext.addQuartzContext(jobExecutionContext);
    contextContainer.set(jobContext);
  }

  /**
   * Empty the ThreadLocal container
   */
  protected void destroyContext() {

    contextContainer.remove();
  }

  /**
   * Get the JobContext object
   *
   * @return
   */
  protected JobContext getJobContext() {

    return JobContainer.getContext();
  }

  /**
   * Get the JobContext object
   *
   * @return
   */
  private static JobContext getContext() {

    return contextContainer.get();
  }

  /**
   * Check if the terminate flag is true, and throw a JobInterruptException if it is.
   */
  public void checkTerminated() {

    if (terminate) {
      throw new JobInterruptException();
    }
  }

  /**
   * Set the terminate flag to true
   */
  public void setTerminate() {

    terminate = true;
  }

}
