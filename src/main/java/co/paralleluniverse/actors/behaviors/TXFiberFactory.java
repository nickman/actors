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
package co.paralleluniverse.actors.behaviors;

import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.actors.TXFiber;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.strands.SuspendableCallable;

/**
 * <p>Title: TXFiberFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>co.paralleluniverse.actors.behaviors.TXFiberFactory</code></p>
 */

public class TXFiberFactory implements FiberFactory {
	final AtomicLong serial = new AtomicLong(0);
	final String name;
	/**
	 * Creates a new TXFiberFactory
	 */
	public TXFiberFactory(final String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * @see co.paralleluniverse.fibers.FiberFactory#newFiber(co.paralleluniverse.strands.SuspendableCallable)
	 */
	@Override
	public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
		final Fiber<T> f = new TXFiber<T>(target);
		f.setName(name + "#" + serial.incrementAndGet());
		System.err.println("Created fiber [" + f + "]");
		return f;
	}

}
