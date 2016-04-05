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
import java.sql.RowId;
import java.util.Date;

import org.jboss.stm.annotations.Transactional;

import co.paralleluniverse.actors.ActorRef;

/**
 * <p>Title: PosAcct</p>
 * <p>Description: Defines the actor attributes and operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.PosAcct</code></p>
 */
@Transactional
public interface PosAcct {
	public RowId getRowId();
	public long getPosAcctId();
	public String getName();
	public BigDecimal getBalance();
	public Date getCreateDate();
	public Date getUpdateDate();
	public void deposit(BigDecimal amt);
	public void withdraw(BigDecimal amt);
	public void blowUp() throws Throwable;
	public void setActorRef(ActorRef ref);
	
}
