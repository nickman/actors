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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.behaviors.ProxyServerActor;
import co.paralleluniverse.actors.behaviors.Server;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.concurrent.CountDownLatch;

/**
 * <p>Title: LockYoImpl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.LockYoImpl</code></p>
 */

public class LockYoImpl implements LockYo {
	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(LockYoImpl.class);

	/**
	 * Creates a new LockYoImpl
	 */
	public LockYoImpl() {
	}
	
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		LOG.info("Transaction Test");
		JMXHelper.fireUpJMXMPServer(9998);
		final FiberFactory ff = new TXFiberFactory("LockYoFiber");
//		final FiberFactory ff = new FiberFactory() {
			final AtomicLong serial = new AtomicLong();
			@Override
			public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
				
				final Fiber<T> f = new Fiber<T>(target);
				f.setName("YoFiber#" + serial.incrementAndGet());
				System.err.println("Created fiber [" + f + "]");
				return f;
			}
		};
		final MailboxConfig mbox = new MailboxConfig(1024, OverflowPolicy.THROW);
		for(int x = 0; x < 5; x++) {
			final String name = "LockYo#" + x;
  		final ProxyServerActor psa = new ProxyServerActor(name, null, mbox, false, new LockYoImpl(), new Class[]{LockYo.class}); 
  		psa.monitor();
			final Server actor = psa.spawn(ff);
			Thread t = new Thread() {
				public void run() {					
					while(true) {
						try {
							((LockYo)actor).runLockTest(new CountDownLatch(3));
							Thread.currentThread().join(1000);
						} catch (Exception ex) {
							LOG.error("Server error", ex);
							return;
						}						
					}
				}				
			};
			t.setDaemon(true);
			t.start();			
		}
		StdInCommandHandler.getInstance().run();			
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.actors.LockYo#runLockTest()
	 */
	@Override
	public void runLockTest(final CountDownLatch latch) throws SuspendExecution {
		try {
			TXManager.setTransactionTimeout(10);
			TXManager.begin();			
			final String uid = TXManager.currentTransactionUid();
			TXManager.currentTransaction().registerSynchronization(new Synchronization(){
				@Override
				public void afterCompletion(final int status) {
					if(Status.STATUS_COMMITTED==status) {
						LOG.info("TX Committed:" + uid);
					}
				}
				@Override
				public void beforeCompletion() {
					
				}					
				
			}); System.err.println("Sync Registered");
			
			System.err.println("Started TX\n\tFiber [" + Fiber.currentFiber().getName() + "]\n\tThread [" + Thread.currentThread().getName() + "]\n\tTX Status [" + TXManager.currentTransactionState() + "]");
			
			for(int i = 0; i < 3; i++) {
				if(Strand.interrupted()) Strand.interrupted();
				Fiber.sleep(300);
				LOG.info("\n\tWake Up #" + i + ":" + Thread.currentThread().getName() + "/" + Fiber.currentFiber().getName() + "/TX:" + TXManager.currentTransactionUid());
			}			
			TXManager.commit();
		} catch (Exception ex) {
			LOG.error("RunLockTest Failed", ex);
			throw new RuntimeException(ex);
		}
	}
	
	
	
public void runTXTest() throws SuspendExecution {
	try {
		TXManager.setTransactionTimeout(10);
		TXManager.begin();			
		final String uid = TXManager.currentTransactionUid();
		TXManager.currentTransaction().registerSynchronization(new Synchronization(){
			@Override
			public void afterCompletion(final int status) {
				if(Status.STATUS_COMMITTED==status) {
					LOG.info("TX Committed:" + uid);
				}
			}
			@Override
			public void beforeCompletion() {					
			}									
		}); 			
		for(int i = 0; i < 3; i++) {
			if(Strand.interrupted()) Strand.interrupted();
			Fiber.sleep(300);
			LOG.info("\n\tWake Up #" + i + ":" + Thread.currentThread().getName() + "/" + Fiber.currentFiber().getName() + "/TX:" + TXManager.currentTransactionUid());
		}			
		TXManager.commit();
	} catch (Exception ex) {
		LOG.error("RunLockTest Failed", ex);
		throw new RuntimeException(ex);
	}
}
	

}
