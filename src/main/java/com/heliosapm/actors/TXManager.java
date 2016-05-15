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

import java.lang.management.ManagementFactory;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.heliosapm.utils.jmx.JMXHelper;

/**
 * <p>Title: TXManager</p>
 * <p>Description: Transaction status enum and TX helper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.TXManager</code></p>
 */

public enum TXManager {
	/** Transaction Status for ACTIVE */
	ACTIVE,
	/** Transaction Status for MARKED_ROLLBACK */
	MARKED_ROLLBACK,
	/** Transaction Status for PREPARED */
	PREPARED,
	/** Transaction Status for COMMITTED */
	COMMITTED,
	/** Transaction Status for ROLLEDBACK */
	ROLLEDBACK,
	/** Transaction Status for UNKNOWN */
	UNKNOWN,
	/** Transaction Status for NO_TRANSACTION */
	NO_TRANSACTION,
	/** Transaction Status for PREPARING */
	PREPARING,
	/** Transaction Status for COMMITTING */
	COMMITTING,
	/** Transaction Status for ROLLING_BACK */
	ROLLING_BACK;

	private TXManager() {
		status = ordinal();
	}
	
	private static final TXManager[] values = values();
	private static final int MAX_ORD = values.length-1;
	
	/** The status code */
	public final int status;
	
	private static final TransactionManager txManager;
	private static final TransactionSynchronizationRegistry txRegistry;
	
	static {
		try {
			arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
			txManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
			final JTAEnvironmentBean envBean = jtaPropertyManager.getJTAEnvironmentBean();
			JMXHelper.registerMBean(envBean, JMXHelper.objectName("com.arjuna:service=JTAEnvironmentBean"));
			txRegistry = jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the transaction bound to the calling thread
	 * @return the transaction bound to the calling thread
	 */
	public static Transaction currentTransaction() {
		try {
			return txManager.getTransaction();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static String currentTransactionUid() {
		try {
			final Transaction tx = currentTransaction();
			if(tx==null) return "No Transaction";
			return ((TransactionImple)tx).get_uid().toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the state of the transaction bound to the calling thread
	 * @return the state of the transaction bound to the calling thread
	 */
	public static TXManager currentTransactionState() {
		try {
			final Transaction tx = currentTransaction();
			return tx==null ? NO_TRANSACTION : decode(tx.getStatus());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	
	/**
	 * Returns the TXManager for the passed status code
	 * @param status The status code
	 * @return the TXManager
	 */
	public static TXManager decode(final int status) {
		if(status < 0 || status > MAX_ORD) throw new IllegalArgumentException("Invalid status: " + status);
		return values[status];
	}
	
	public static void main(String[] args) {
		for(TXManager tx: TXManager.values()) {
//			System.out.println(tx.name() + ":" + tx.status);
		}
		for(int i = 0; i <= MAX_ORD; i++) {
			final TXManager tx = decode(i);
			System.out.println(tx.name() + ":" + tx.status);
		}
	}

	/**
	 * @throws NotSupportedException
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#begin()
	 */
	public static void begin() throws NotSupportedException, SystemException {
		txManager.begin();
	}

	/**
	 * @throws RollbackException
	 * @throws HeuristicMixedException
	 * @throws HeuristicRollbackException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#commit()
	 */
	public static void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		txManager.commit();
	}


	/**
	 * @param tobj
	 * @throws InvalidTransactionException
	 * @throws IllegalStateException
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
	 */
	public static void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		txManager.resume(tobj);
	}

	/**
	 * @throws IllegalStateException
	 * @throws SecurityException
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#rollback()
	 */
	public static void rollback() throws IllegalStateException, SecurityException, SystemException {
		txManager.rollback();
	}

	/**
	 * @throws IllegalStateException
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#setRollbackOnly()
	 */
	public static void setRollbackOnly() throws IllegalStateException, SystemException {
		txManager.setRollbackOnly();
	}

	/**
	 * @param seconds
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#setTransactionTimeout(int)
	 */
	public static void setTransactionTimeout(int seconds) throws SystemException {
		txManager.setTransactionTimeout(seconds);
	}

	/**
	 * @return
	 * @throws SystemException
	 * @see javax.transaction.TransactionManager#suspend()
	 */
	public static Transaction suspend() throws SystemException {
		return txManager.suspend();
	}

}
