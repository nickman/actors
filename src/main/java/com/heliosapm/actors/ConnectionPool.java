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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.heliosapm.tsdbex.sqlbinder.SQLWorker;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


/**
 * <p>Title: ConnectionPool</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.actors.ConnectionPool</code></p>
 */

public class ConnectionPool {
	private static volatile ConnectionPool instance = null;
	private static final Object lock = new Object();
	
	/** Static class logger */
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);
	
	/** The type map applied to all connections */
	private final Map<String, Class<?>> typeMap = new ConcurrentHashMap<String, Class<?>>();
	
	final HikariDataSource dataSource;
	
	/**
	 * @param iface
	 * @return
	 * @throws SQLException
	 * @see com.zaxxer.hikari.HikariDataSource#unwrap(java.lang.Class)
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return dataSource.unwrap(iface);
	}



	final MetricRegistry registry;
	final JmxReporter reporter;
	final SQLWorker sqlWorker;
	
	
	public static ConnectionPool getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ConnectionPool();
				}
			}
		}
		return instance;
	}
	
	
	public MetricRegistry getMetricRegistry() {
		return registry;
	}
	
	private ConnectionPool() {
		registry = new MetricRegistry();
		reporter = JmxReporter.forRegistry(registry).build();
		reporter.start();
		
		// ==== known type mappings 
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("oracle.jdbc.OracleDriver");
		//config.setJdbcUrl("jdbc:oracle:thin:@//tporacle:1521/ORCL");
		//config.setJdbcUrl("jdbc:oracle:thin:@//leopard:1521/XE");
		//config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XE");
		config.setMetricRegistry(registry);
		//config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XE");
		config.setJdbcUrl("jdbc:oracle:thin:@//10.22.114.37:1521/ORCL");
		//config.setJdbcUrl("jdbc:oracle:thin:@(DESCRIPTION=(CONNECT_DATA=(SERVICE_NAME=ECS))(failover_mode=(type=select)(method=basic))(ADDRESS_LIST=(load_balance=off)(failover=on)(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.163)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.161)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.162)(PORT=1521))))");
		
		
		config.setUsername("tqreactor");
		config.setPassword("tq");
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.setMaximumPoolSize(100);
		config.setMinimumIdle(20);
		config.setConnectionTestQuery("SELECT SYSDATE FROM DUAL");
		config.setConnectionTimeout(1002);
		config.setAutoCommit(false);
		config.setRegisterMbeans(true);
		config.setPoolName("TQReactorPool");
		dataSource = new HikariDataSource(config);
		sqlWorker = SQLWorker.getInstance(dataSource);
		
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	public SQLWorker getSQLWorker() {
		return sqlWorker;
	}
	
	public void putMappingType(final String dbTypeName, final Class<?> type) {
		if(dbTypeName==null || dbTypeName.trim().isEmpty()) throw new IllegalArgumentException("The passed DB Type Name was null or empty");
		if(type==null) throw new IllegalArgumentException("The passed ORAData type was null");
		typeMap.put(dbTypeName, type);
	}
	

	
	public Connection getConnection() {
		try {
			final Connection conn = dataSource.getConnection();
			final Map<String, Class<?>> tm = conn.getTypeMap();
			tm.putAll(typeMap);
			conn.setTypeMap(tm);
			return conn;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	


}
