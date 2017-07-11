/*
 * Copyright (C) 2017 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package au.edu.uq.rcc.nimrod.optim;

import java.sql.*;
import java.util.Arrays;
import java.util.Properties;

public class NimCache implements AutoCloseable {

	private final String m_InsertQuery;
	private final String m_UpdateQuery;

	public final String dbUrl;
	public final String tableName;

	private Properties m_Properties;

	private Connection m_Connection;

	private final String[] m_CoordinateNames;
	private final String[] m_ObjectiveNames;

	public NimCache(String dbUrl, Properties properties, String tableName, String[] coords, String[] objectives) throws SQLException {
		if((this.dbUrl = dbUrl) == null) {
			throw new IllegalArgumentException("dbUrl cannot be null");
		}

		if((this.tableName = tableName) == null) {
			throw new IllegalArgumentException("tableName cannot be null");
		}

		validateName(tableName);

		if(properties == null) {
			throw new IllegalArgumentException("properties cannot be null");
		}

		if(coords == null) {
			throw new IllegalArgumentException("coords must be > 0");
		}

		if(objectives == null) {
			throw new IllegalArgumentException("objectives must be > 0");
		}

		m_CoordinateNames = Arrays.copyOf(coords, coords.length);
		m_ObjectiveNames = Arrays.copyOf(objectives, objectives.length);

		validateNames(m_CoordinateNames);
		validateNames(m_ObjectiveNames);

		m_InsertQuery = buildInsertQuery();
		m_UpdateQuery = buildUpdateQuery();

		m_Properties = new Properties(properties);

		m_Connection = DriverManager.getConnection(dbUrl, m_Properties);

		setupTable(m_Connection);
	}

	private void validateName(String name) {
		for(int i = 0; i < name.length(); ++i) {
			char c = name.charAt(i);

			/* These two are safe. */
			if(c == '-' || c == '_')
				continue;

			/*
			0x30 -> 0x39 = Digits
			0x41 -> 0x5A = Uppercase
			0x61 -> 0x7A = Lowercase
			*/
			if(c < 0x30 || c > 0x7A) {
				throw new IllegalArgumentException(String.format("Invalid character '%s'", Character.toString(c)));
			}

			if((c < 0x30)
					|| (c > 0x39 && c < 0x41)
					|| (c > 0x5A && c < 0x61)
					|| (c > 0x7A)) {
				throw new IllegalArgumentException(String.format("Invalid character '%s'", Character.toString(c)));
			}

		}
	}

	private void validateNames(String[] names) {
		for(String n : names) {
			validateName(n);
		}
	}

	private Connection getOrCreateConnection() throws SQLException {
		if(m_Connection == null || m_Connection.isClosed()) {
			/* No connection for this thread or it's closed, create one. */
			m_Connection = DriverManager.getConnection(dbUrl, m_Properties);
		}

		return m_Connection;
	}

	public double[] lookupPoint(double[] coords) throws SQLException {
		ResultSet rs = findPoint(coords);

		if(rs == null) {
			return null;
		}

		double[] results = new double[m_ObjectiveNames.length];
		for(int i = 0; i < m_ObjectiveNames.length; ++i) {
			results[i] = rs.getDouble(m_ObjectiveNames[i]);
		}
		return results;
	}

	public boolean addOrUpdate(double[] coords, double[] objectives) throws SQLException {
		ResultSet rs = findPoint(coords);

		Connection c = getOrCreateConnection();

		PreparedStatement ps;
		if(rs == null) {
			/* Add */
			ps = c.prepareStatement(m_InsertQuery);
			int idx = 1;
			ps.setLong(idx++, hashPoint(coords));
			for(int i = 0; i < coords.length; ++i) {
				ps.setDouble(idx++, coords[i]);
			}

			for(int i = 0; i < objectives.length; ++i) {
				ps.setDouble(idx++, objectives[i]);
			}

		} else {
			/* Update */
			ps = c.prepareStatement(m_UpdateQuery);
			int idx = 1;
			for(int i = 0; i < objectives.length; ++i) {
				ps.setDouble(idx++, objectives[i]);
			}

			ps.setInt(idx++, rs.getInt("id"));
		}

		return ps.executeUpdate() != 0;
	}

	private ResultSet findPoint(double[] coords) throws SQLException {
		Connection c = getOrCreateConnection();

		long hash = hashPoint(coords);
		PreparedStatement ps = c.prepareStatement("SELECT * FROM `" + tableName + "` WHERE hash = ?");
		ps.setLong(1, hash);

		ResultSet rs = ps.executeQuery();

		while(rs.next()) {
			if(rowMatches(rs, coords)) {
				return rs;
			}
		}

		return null;

	}

	private boolean rowMatches(ResultSet rs, double[] coords) throws SQLException {
		for(int i = 0; i < coords.length; ++i) {
			if(Math.abs(coords[i] - rs.getDouble(m_CoordinateNames[i])) > 0.00001) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Hash a double[]. This is guaranteed to be deterministic.
	 *
	 * @param coords
	 * @return
	 */
	private static long hashPoint(double[] coords) {

		byte[] buffer = new byte[4];

		long hash = 7;

		for(int i = 0; i < coords.length; ++i) {
			/* Convert to 15.16 fixed-point */
			int fixed = ((int)((coords[i]) * 65536.0f));

			buffer[0] = (byte)((fixed & 0xFF000000) >>> 24);
			buffer[1] = (byte)((fixed & 0x00FF0000) >>> 16);
			buffer[2] = (byte)((fixed & 0x0000FF00) >>> 8);
			buffer[3] = (byte)((fixed & 0x000000FF));

			hash = 89 * hash + XXHash.hash64(buffer);

		}

		return hash;
	}

	private void setupTable(Connection c) throws SQLException {
		StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS`");
		sb.append(tableName);
		sb.append("`\n(\n");
		sb.append("\t`id` INTEGER PRIMARY KEY AUTO_INCREMENT,\n");
		sb.append("\t`hash` BIGINT NOT NULL,\n");

		for(String s : m_CoordinateNames) {
			sb.append("\t`");
			sb.append(s);
			sb.append("` DECIMAL(10,5) NOT NULL,\n");
		}

		for(int i = 0; i < m_ObjectiveNames.length; ++i) {
			sb.append("\t`");
			sb.append(m_ObjectiveNames[i]);
			sb.append("` DECIMAL(10,5) NOT NULL");

			if(i != m_ObjectiveNames.length - 1) {
				sb.append(",\n");
			}
		}

		sb.append(")");

		Statement s = c.createStatement();
		s.execute(sb.toString());
	}

	private String buildInsertQuery() {
		StringBuilder sb = new StringBuilder("INSERT INTO `");
		sb.append(tableName);
		sb.append("` (`hash`, ");

		for(String s : m_CoordinateNames) {
			sb.append("`");
			sb.append(s);
			sb.append("`, ");
		}

		for(int i = 0; i < m_ObjectiveNames.length; ++i) {
			sb.append("`");
			sb.append(m_ObjectiveNames[i]);
			sb.append("`");
			if(i != m_ObjectiveNames.length - 1) {
				sb.append(", ");
			}
		}

		sb.append(")\n");
		sb.append("VALUES (?, ");

		for(String s : m_CoordinateNames) {
			sb.append("?, ");
		}

		for(int i = 0; i < m_ObjectiveNames.length; ++i) {
			sb.append("?");
			if(i != m_ObjectiveNames.length - 1) {
				sb.append(", ");
			}
		}

		sb.append(")");
		return sb.toString();
	}

	private String buildUpdateQuery() {
		StringBuilder sb = new StringBuilder("UPDATE `");
		sb.append(tableName);
		sb.append("`\n");
		sb.append("SET ");

		for(int i = 0; i < m_ObjectiveNames.length; ++i) {
			sb.append("`");
			sb.append(m_ObjectiveNames[i]);
			sb.append("`=?");

			if(i != m_ObjectiveNames.length - 1) {
				sb.append(",");
			}
		}

		sb.append(" WHERE `id` = ?");

		return sb.toString();
	}

	@Override
	public void close() throws SQLException {
		if(m_Connection != null && !m_Connection.isClosed()) {
			m_Connection.close();
		}
	}
}
