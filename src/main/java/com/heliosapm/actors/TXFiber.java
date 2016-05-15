/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.actors;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import jsr166e.LongAdder;

/**
 * <p>Title: TXFiber</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.TXFiber</code></p>
 */

public class TXFiber<V> extends Fiber<V> {
	
	/**  */
	private static final long serialVersionUID = 3408330051770502236L;
	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(LockYoImpl.class);

//	protected Transaction currentTransaction = null;
	protected final LongAdder parks = new LongAdder();
	protected final LongAdder resumes = new LongAdder();
	protected final LongAdder unresumedParks = new LongAdder();
	protected Transaction currentTransaction = null;
	
	@Override
	protected void onParked() {
		final TXManager txs = TXManager.currentTransactionState();
		System.err.println("Parking\n\tFiber [" + getName() + "], \n\tThread [" + Thread.currentThread().getName() + "]\n\tTXState: [" + txs + "]");
//		LOG.info("Parking [{}]", this);
		if(txs!=TXManager.NO_TRANSACTION) {			
			parks.increment();
			unresumedParks.increment();
			try {
				final boolean init = currentTransaction==null;
				if(init) {
					TXManager.currentTransaction().registerSynchronization(new Synchronization(){
						@Override
						public void afterCompletion(final int status) {
							if(Status.STATUS_COMMITTED==status) {
								LOG.info("TX Committed:" + currentTransaction);
								currentTransaction = null;
							}
						}
						@Override
						public void beforeCompletion() {
							
						}					
						
					}); System.err.println("Sync Registered");
				}
				currentTransaction = TXManager.suspend();
			} catch (Exception ex) {
				LOG.error("Failed to park", ex);
				throw new RuntimeException(ex);
			}
		} else {
			currentTransaction = null;
		}
		super.onParked();
	}
	
	@Override
	protected void onResume() throws SuspendExecution, InterruptedException {
		super.onResume();
//		LOG.info("Resuming [{}]", this);
		System.err.println("Resuming Fiber [" + getName() + "]");
		if(currentTransaction!=null) {
			resumes.increment();
			unresumedParks.decrement();
			try {
				TXManager.resume(currentTransaction);
			} catch (Exception ex) {
				LOG.error("Failed to resume", ex);
				throw new RuntimeException(ex);
			} finally {
				currentTransaction = null;
			}
		}
		
	}
	
	

	/**
	 * Creates a new TXFiber
	 */
	public TXFiber() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param fiber
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(Fiber fiber, FiberScheduler scheduler, SuspendableCallable<V> target) {
		super(fiber, scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param fiber
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(Fiber fiber, FiberScheduler scheduler, SuspendableRunnable target) {
		super(fiber, scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param fiber
	 * @param target
	 */
	public TXFiber(Fiber fiber, SuspendableCallable<V> target) {
		super(fiber, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param fiber
	 * @param target
	 */
	public TXFiber(Fiber fiber, SuspendableRunnable target) {
		super(fiber, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(FiberScheduler scheduler, SuspendableCallable<V> target) {
		super(scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(FiberScheduler scheduler, SuspendableRunnable target) {
		super(scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param scheduler
	 */
	public TXFiber(FiberScheduler scheduler) {
		super(scheduler);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 * @param stackSize
	 * @param target
	 */
	public TXFiber(String name, FiberScheduler scheduler, int stackSize, SuspendableCallable<V> target) {
		super(name, scheduler, stackSize, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 * @param stackSize
	 * @param target
	 */
	public TXFiber(String name, FiberScheduler scheduler, int stackSize, SuspendableRunnable target) {
		super(name, scheduler, stackSize, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 * @param stackSize
	 */
	public TXFiber(String name, FiberScheduler scheduler, int stackSize) {
		super(name, scheduler, stackSize);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(String name, FiberScheduler scheduler, SuspendableCallable<V> target) {
		super(name, scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 * @param target
	 */
	public TXFiber(String name, FiberScheduler scheduler, SuspendableRunnable target) {
		super(name, scheduler, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param scheduler
	 */
	public TXFiber(String name, FiberScheduler scheduler) {
		super(name, scheduler);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param stackSize
	 * @param target
	 */
	public TXFiber(String name, int stackSize, SuspendableCallable<V> target) {
		super(name, stackSize, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param stackSize
	 * @param target
	 */
	public TXFiber(String name, int stackSize, SuspendableRunnable target) {
		super(name, stackSize, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param stackSize
	 */
	public TXFiber(String name, int stackSize) {
		super(name, stackSize);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param target
	 */
	public TXFiber(String name, SuspendableCallable<V> target) {
		super(name, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 * @param target
	 */
	public TXFiber(String name, SuspendableRunnable target) {
		super(name, target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param name
	 */
	public TXFiber(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param target
	 */
	public TXFiber(SuspendableCallable<V> target) {
		super(target);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new TXFiber
	 * @param target
	 */
	public TXFiber(SuspendableRunnable target) {
		super(target);
		// TODO Auto-generated constructor stub
	}

}
