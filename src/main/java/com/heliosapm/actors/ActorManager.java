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

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.actors.behaviors.ProxyServerActor;
import co.paralleluniverse.actors.behaviors.Server;

/**
 * <p>Title: ActorManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.ActorManager</code></p>
 */

public class ActorManager {
	private static volatile ActorManager instance = null;
	private static final Object lock = new Object();

	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);

	public static ActorManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ActorManager();
				}
			}
		}
		return instance;
	}
	
	public PosAcct newPosAcct(final String name, final Number balance) {
		final Server a = new ProxyServerActor(
				false, 		
				new PosAcctImpl(name, new BigDecimal(balance.toString())),
				PosAcct.class
		)
		.spawn();
		
		
	}
	
	
	/**
	 * Creates a new ActorManager
	 */
	private ActorManager() {

	}

}
