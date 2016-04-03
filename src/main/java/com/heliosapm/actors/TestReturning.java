/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heliosapm.actors;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heliosapm.tsdbex.sqlbinder.SQLWorker;

/**
 * <p>Title: TestReturning</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.TestReturning</code></p>
 */

public class TestReturning {
	protected static final Logger log = LoggerFactory.getLogger(IDProxyServerActor.class);
	public static final String NEW_INSERT_RET_SQL = 
			"INSERT INTO POSACCT " +
			"(ID,NAME,BALANCE,CREATE_TS) VALUES " +
			"(?,?,?,?) " + 
			"RETURNING TO_CHAR(ID), ROWIDTOCHAR(ROWID) INTO ?,?";
	
	/** The SQL to insert a new PosAcct */
	public static final String NEW_INSERT_SQL = 
			"INSERT INTO POSACCT " +
			"(ID,NAME,BALANCE,CREATE_TS) VALUES " +
			"(?,?,?,?)";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("TestReturning");
		final SQLWorker sqlWorker = ConnectionPool.getInstance().getSQLWorker();
		sqlWorker.execute("TRUNCATE TABLE POSACCT");
		log.info("Truncated PosAcct");
		//sqlWorker.executeUpdate(NEW_INSERT_RET_SQL, "FOO", new BigDecimal(47), new java.sql.Date(System.currentTimeMillis()));
		final long posAcctId = ConnectionPool.getInstance().getSQLWorker().nextSeq("POSACCT_SEQ");
		sqlWorker.executeUpdate(NEW_INSERT_RET_SQL, posAcctId, "FOO", new BigDecimal(47), new java.sql.Date(System.currentTimeMillis()));
		log.info("Inserted");
		

	}

}
