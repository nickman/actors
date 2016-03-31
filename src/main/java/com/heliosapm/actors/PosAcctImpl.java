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
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import co.paralleluniverse.actors.ActorRef;

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
	private ActorRef ref = null;
	
	/** The PosAcct sequence name */
	public static final String SEQ = "POSACCT_SEQ";
	
	/** The SQL to insert a new PosAcct */
	public static final String NEW_INSERT_SQL = 
			"INSERT INTO POSACCT " +
			"(ID,NAME,BALANCE,CREATE_TS) VALUES " +
			"(?,?,?,?) ";
	
	/** The SQL to insert a new PosAcct */
	public static final String NEW_INSERT_RET_SQL = 
			"INSERT INTO POSACCT " +
			"(ID,NAME,BALANCE,CREATE_TS) VALUES " +
			"(POSACCT_SEQ.NEXTVAL,?,?,?) " + 
			"RETURNING ID, ROWID INTO ?,?";
	
	
	/** SQL to fetch the rowid of a new PosAcct */
	public static final String FETCH_NEW_SQL = 
			"SELECT ROWID FROM POSACCT WHERE ID = ?";

	/** SQL to load all PosAccts */
	public static final String LOAD_ALL_SQL = 
			"SELECT ROWID, ID, NAME, BALANCE, CREATE_TS, UPDATE_TS FROM POSACCT";
	
	
	/** SQL to load a PosAcct */
	public static final String LOAD_SQL = 
			LOAD_ALL_SQL + " WHERE ID = ?";
	
	/** SQL to deposit */
	public static final String DEPOSIT_SQL = 
			"UPDATE POSACCT ROWID, ID, NAME, BALANCE, CREATE_TS, UPDATE_TS FROM POSACCT";
	
	
	
	
	public static PosAcct load(final long id) {
		try {
			final ResultSet rs = ConnectionPool.getInstance().getSQLWorker().executeQuery(LOAD_SQL, true, id);
			return new PosAcctImpl(rs);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load PosAcct [" + id + "]", ex);
		}
	}
	
	public static Iterator<PosAcct> load() {		
		try {			
			final ResultSet rs = ConnectionPool.getInstance().getSQLWorker().executeQuery(LOAD_ALL_SQL, 1024, false);
					//.executeQuery(LOAD_ALL_SQL, false);
			return new Iterator<PosAcct>() {

				@Override
				public boolean hasNext() {
					try {
						final boolean hasnext = rs.next();
						if(!hasnext) rs.close();
						return hasnext; 
					} catch (Exception ex) { throw new RuntimeException(ex); }
				}

				@Override
				public PosAcct next() {
					try { return new PosAcctImpl(rs); } catch (Exception ex) { throw new RuntimeException(ex); }
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();					
				}
				
			};
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load all PosAccts", ex);
		}
	}
	
	public void setActorRef(ActorRef ref) {
		this.ref = ref;
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
//		// =====
//		final Object[][] ret = ConnectionPool.getInstance().getSQLWorker().executeUpdateReturning(null, NEW_INSERT_RET_SQL, name, balance, new java.sql.Timestamp(createDate.getTime()));
//		posAcctId = ((Long)ret[0][0]).longValue();
//		rowId = (RowId)ret[0][1];
//		// ===
		posAcctId = ConnectionPool.getInstance().getSQLWorker().nextSeq(SEQ);
		ConnectionPool.getInstance().getSQLWorker().execute(NEW_INSERT_SQL, posAcctId, name, balance, new java.sql.Timestamp(createDate.getTime()));
		rowId = ConnectionPool.getInstance().getSQLWorker().sqlForRowId(null, FETCH_NEW_SQL, posAcctId);
	}
	
	private PosAcctImpl(final ResultSet rset) throws SQLException {
		//ROWID, ID, NAME, BALANCE, CREATE_TS, UPDATE_TS
		rowId = rset.getRowId("ROWID");
		posAcctId = rset.getLong("ID");
		name = rset.getString("NAME");
		balance = rset.getBigDecimal("BALANCE");
		createDate = rset.getDate("CREATE_TS");
		updateDate = rset.getDate("UPDATE_TS");
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.actors.PosAcct#deposit(java.math.BigDecimal)
	 */
	@Override
	public void deposit(BigDecimal amt) {
		balance = balance.add(amt);
		updateDate = new Date(System.currentTimeMillis());
		ConnectionPool.getInstance().getSQLWorker().executeUpdate("UPDATE POSACCT SET BALANCE = ?, UPDATE_TS = ? WHERE ROWID = ?", balance, new java.sql.Timestamp(updateDate.getTime()), rowId);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.actors.PosAcct#withdraw(java.math.BigDecimal)
	 */
	@Override
	public void withdraw(BigDecimal amt) {
		balance = balance.subtract(amt);
		ConnectionPool.getInstance().getSQLWorker().executeUpdate("UPDATE POSACCT SET BALANCE = ?, UPDATE_TS = ? WHERE ROWID = ?", balance, new java.sql.Timestamp(updateDate.getTime()), rowId);		
	}
	
	public void blowUp() throws Throwable  {
		ref.close();
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
