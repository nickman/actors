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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.RowId;
import java.util.Date;

/**
 * <p>Title: PosAcctImpl</p>
 * <p>Description: The actor impl.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.PosAcctImpl</code></p>
 */

public class PosAcctImpl implements PosAcct {
	private final RowId rowId;
	private final long posAcctId;
	private String name;
	private BigDecimal balance;
	private Date createDate;
	private Date updateDate;
	
	/** The PosAcct sequence name */
	public static final String SEQ = "POSACCT_SEQ";
	
	/** The SQL to insert a new PosAcct */
	public static final String NEW_INSERT_SQL = 
			"INSERT INTO POSACCT " +
			"(ID,NAME,BALANCE,CREATE_TS) VALUES " +
			"(?,?,?,?) ";
	
	/** SQL to fetch the rowid of a new PosAcct */
	public static final String FETCH_NEW_SQL = 
			"SELECT ROWID FROM POSACCT WHERE ID = ?";
	
	public static void main(String[] args) {
		log("PosAcctTest");
		PosAcctImpl pa = new PosAcctImpl("Hello", new BigDecimal(43));
		log(pa);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Creates a new PosAcctImpl
	 * @param name The name
	 * @param balance The starting balance
	 */
	public PosAcctImpl(final String name, final BigDecimal balance) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		if(balance==null) throw new IllegalArgumentException("The passed balance was null");
		this.name = name.trim();
		this.balance = balance;
		this.createDate = new Date();
		posAcctId = ConnectionPool.getInstance().getSQLWorker().nextSeq(SEQ);
		ConnectionPool.getInstance().getSQLWorker().execute(NEW_INSERT_SQL, posAcctId, name, balance, new java.sql.Timestamp(createDate.getTime()));
		rowId = ConnectionPool.getInstance().getSQLWorker().sqlForRowId(null, FETCH_NEW_SQL, posAcctId);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PosAcct: [" + name + "/" + posAcctId + "]";
	}

	@Override
	public RowId getRowId() {
		return rowId;
	}

	@Override
	public long getPosAcctId() {
		return posAcctId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BigDecimal getBalance() {
		return balance;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	@Override
	public Date getUpdateDate() {
		return updateDate;
	}

}
