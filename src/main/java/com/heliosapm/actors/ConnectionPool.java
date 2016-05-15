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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.xa.client.OracleXADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.heliosapm.tsdbex.sqlbinder.SQLWorker;
import com.heliosapm.utils.jmx.JMXHelper;
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
	
	private static final InheritableThreadLocal<Connection> LOCAL_CONNECTION = new InheritableThreadLocal<Connection>(); 
	
	final HikariDataSource dataSource;
	final TransactionManager txManager;
	final TransactionSynchronizationRegistry txRegistry;
	final OracleXADataSource xaDataSource;
	///home/nwhitehead/3projects/jbosstm/jca-and-tomcat/src/main/java/org/jboss/narayana/quickstart/jca/listener/ServletContextListenerImpl.java
	
	/**
	 * @param iface
	 * @return
	 * @throws SQLException
	 * @see com.zaxxer.hikari.HikariDataSource#unwrap(java.lang.Class)
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return dataSource.unwrap(iface);
	}

	public static Connection setLocalConnection() {
		Connection conn = LOCAL_CONNECTION.get();
		if(conn!=null) {
			LOG.warn("Local Connection Found Open...");
			try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
		conn = getInstance().getConnection();
		LOCAL_CONNECTION.set(conn);
		return conn;
	}
	
	public static void closeLocalConnection() {
		Connection conn = LOCAL_CONNECTION.get();
		if(conn!=null) {			
			try { conn.close(); } catch (Exception ex) {/* No Op */}
		} else {
			LOG.warn("No Local Connection Found...");
		}		
	}
	
	public static Connection getLocalConnection(final boolean create) {
		Connection conn = LOCAL_CONNECTION.get();
		if(conn==null) {			
			if(create) {
				return setLocalConnection();
			} else {
				throw new IllegalStateException("No local connection set");
			}
		}
		return conn;
	}


	final MetricRegistry registry;
	final JmxReporter reporter;
	final SQLWorker sqlWorker;
	final SQLWorker xaSqlWorker;
	
	
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
		try {
      System.setProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
      System.setProperty("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
//      LOG.info("JNP: {}", org.jnp.server.Main.class.getProtectionDomain().getCodeSource().getLocation());
//      org.jnp.server.Main main = new org.jnp.server.Main();
//      NamingBean naming = new NamingBeanImpl();
//      main.setNamingInfo(naming);
//      main.start();
			txManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
			txRegistry = jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();
//			JNDIManager.bindJTAImplementations(new InitialContext());
//			LOG.info("JTA Impls Bound");
			xaDataSource = new OracleXADataSource();
			xaDataSource.setConnectionCacheName("OracleXAConnectionCache");
			xaDataSource.setURL("jdbc:oracle:thin:@//localhost:1521/XE");
			xaDataSource.setUser("tqreactor");
			xaDataSource.setPassword("tq");
			
		} catch (Exception e) {
			LOG.error("Failed to initialize arjuna", e);
			throw new RuntimeException(e);
		}
		registry = new MetricRegistry();
		reporter = JmxReporter.forRegistry(registry).build();
		reporter.start();
		 
		// ==== known type mappings 
		HikariConfig config = new HikariConfig();
//		config.setDriverClassName("oracle.jdbc.OracleDriver");
		config.setDataSourceClassName("oracle.jdbc.xa.client.OracleXADataSource");
		//config.setJdbcUrl("jdbc:oracle:thin:@//tporacle:1521/ORCL");
		//config.setJdbcUrl("jdbc:oracle:thin:@//leopard:1521/XE");
		//config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XE");
		config.setMetricRegistry(registry);
//		config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XE");
//		config.setJdbcUrl("jdbc:oracle:thin:@//10.22.114.37:1521/ORCL");
//		config.setJdbcUrl("jdbc:oracle:thin:@//10.22.114.37:1521/ORCL");
		//config.setJdbcUrl("jdbc:oracle:thin:@(DESCRIPTION=(CONNECT_DATA=(SERVICE_NAME=ECS))(failover_mode=(type=select)(method=basic))(ADDRESS_LIST=(load_balance=off)(failover=on)(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.163)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.161)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.5.202.162)(PORT=1521))))");
		
		
		config.setUsername("tqreactor");
		config.setPassword("tq");
//		config.addDataSourceProperty("cachePrepStmts", "true");
//		config.addDataSourceProperty("prepStmtCacheSize", "250");
//		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//		config.addDataSourceProperty("URL", "jdbc:oracle:thin:@//10.22.114.37:1521/ORCL");
		config.addDataSourceProperty("URL", "jdbc:oracle:thin:@//localhost:1521/XE");
		config.setMaximumPoolSize(100);
		config.setMinimumIdle(20);
		config.setConnectionTestQuery("SELECT SYSDATE FROM DUAL");
		config.setConnectionTimeout(1002);
		config.setAutoCommit(false);
		config.setRegisterMbeans(true);
		config.setPoolName("TQReactorPool");
		config.setAutoCommit(false);
		dataSource = new HikariDataSource(config);
		dataSource.setAutoCommit(true);
		dataSource.validate();
		sqlWorker = SQLWorker.getInstance(dataSource);
		xaSqlWorker = SQLWorker.getInstance(xaDataSource);
		
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	public DataSource getXADataSource() {
		return xaDataSource;
	}
	
	
	public SQLWorker getSQLWorker() {
		return sqlWorker;
	}
	
	public SQLWorker getXASQLWorker() {
		return xaSqlWorker;
	}
	
	public void putMappingType(final String dbTypeName, final Class<?> type) {
		if(dbTypeName==null || dbTypeName.trim().isEmpty()) throw new IllegalArgumentException("The passed DB Type Name was null or empty");
		if(type==null) throw new IllegalArgumentException("The passed ORAData type was null");
		typeMap.put(dbTypeName, type);
	}
	
	private static URL getURL(final String fileName) throws MalformedURLException {
		return ConnectionPool.class.getClassLoader().getResource(fileName);
	}
	
	public XAConnection getXAConnection() {
		try {
			Transaction tx = txManager.getTransaction();
			if(tx==null || tx.getStatus()==Status.STATUS_NO_TRANSACTION) {
				txManager.begin();
				tx = txManager.getTransaction();
			}
			final XAConnection conn = xaDataSource.getXAConnection();
			tx.enlistResource(conn.getXAResource());
			
			txRegistry.putResource(txRegistry.getTransactionKey(), conn);
			return conn;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public TransactionManager getTransactionManager() {
		return txManager;
	}

	public TransactionSynchronizationRegistry getTransactionRegistry() {
		return txRegistry;
	}
	
	public Connection getConnection() {
		
		try {
			Transaction tx = txManager.getTransaction();
			if(tx==null || tx.getStatus()==Status.STATUS_NO_TRANSACTION) {
				txManager.begin();
				tx = txManager.getTransaction();
			}
			return dataSource.getConnection();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	

	


}
