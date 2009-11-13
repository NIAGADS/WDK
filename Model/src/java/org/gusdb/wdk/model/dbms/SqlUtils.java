/**
 * 
 */
package org.gusdb.wdk.model.dbms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.QueryMonitor;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;

/**
 * @author Jerric Gao
 * 
 */
public final class SqlUtils {

    private static final Logger logger = Logger.getLogger(SqlUtils.class);

    /**
     * Close the resultSet and the underlying statement, connection
     * 
     * @param resultSet
     * @throws SQLException
     * @throws SQLException
     */
    public static void closeResultSet(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                // close the statement in any way
                Statement stmt = null;
                try {
                    try {
                        stmt = resultSet.getStatement();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    closeStatement(stmt);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Close the statement and underlying connection
     * 
     * @param stmt
     * @throws SQLException
     */
    public static void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                // close the connection in any way
                Connection connection = null;
                try {
                    try {
                        connection = stmt.getConnection();
                    } finally {
                        stmt.close();
                    }
                } finally {
                    if (connection != null) connection.close();
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PreparedStatement getPreparedStatement(DataSource dataSource,
            String sql) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            return ps;
        } catch (SQLException ex) {
            logger.error("Failed to prepare query:\n" + sql);
            closeStatement(ps);

            if (ps == null && connection != null) connection.close();
            throw ex;
        }
    }

    /**
     * execute the update, and returns the number of rows affected.
     * 
     * @param dataSource
     * @param sql
     * @return
     * @throws SQLException
     * @throws WdkModelException
     * @throws WdkUserException
     */
    public static int executeUpdate(WdkModel wdkModel, DataSource dataSource,
            String sql) throws SQLException, WdkUserException,
            WdkModelException {
        Connection connection = null;
        Statement stmt = null;
        try {
            long start = System.currentTimeMillis();
            connection = dataSource.getConnection();
            stmt = connection.createStatement();
            int result = stmt.executeUpdate(sql);
            verifyTime(wdkModel, sql, start);
            return result;
        } catch (SQLException ex) {
            logger.error("Failed to run nonQuery:\n" + sql);
            throw ex;
        } finally {
            closeStatement(stmt);
            if (stmt == null && connection != null) connection.close();
        }
    }

    /**
     * Run a query and returns a resultSet. the calling code is responsible for
     * closing the resultSet using the helper method in SqlUtils.
     * 
     * @param dataSource
     * @param sql
     * @return
     * @throws SQLException
     * @throws WdkModelException
     * @throws WdkUserException
     */
    public static ResultSet executeQuery(WdkModel wdkModel,
            DataSource dataSource, String sql) throws SQLException,
            WdkUserException, WdkModelException {
        ResultSet resultSet = null;
        Connection connection = null;
        try {
            long start = System.currentTimeMillis();
            connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);
            verifyTime(wdkModel, sql, start);
            return resultSet;
        } catch (SQLException ex) {
            logger.error("Failed to run query:\n" + sql);
            if (resultSet == null && connection != null) connection.close();
            closeResultSet(resultSet);
            throw ex;
        }
    }

    /**
     * Run the scalar value and returns a single value. If the query returns no
     * rows or more than one row, a WdkModelException will be thrown; if the
     * query returns a single row with many columns, the value in the first
     * column will be returned.
     * 
     * @param dataSource
     * @param sql
     * @return the first column of the first row in the result
     * @throws SQLException
     *             database or query failure
     * @throws WdkModelException
     * @throws WdkModelException
     *             query returns no row
     * @throws WdkUserException
     */
    public static Object executeScalar(WdkModel wdkModel,
            DataSource dataSource, String sql) throws SQLException,
            WdkModelException, WdkUserException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(wdkModel, dataSource, sql);
            if (!resultSet.next())
                throw new WdkModelException("The SQL doesn't return any row:\n"
                        + sql);
            return resultSet.getObject(1);
        } finally {
            closeResultSet(resultSet);
        }
    }

    public static Set<String> getColumnNames(ResultSet resultSet)
            throws SQLException {
        Set<String> columns = new LinkedHashSet<String>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int count = metaData.getColumnCount();
        for (int i = 0; i < count; i++) {
            columns.add(metaData.getColumnName(i));
        }
        return columns;
    }

    /**
     * Escapes the input string for use in LIKE clauses to allow matching
     * special chars
     * 
     * @param value
     * @return the input value with special characters escaped
     */
    public static String escapeWildcards(String value) {
        return value.replaceAll("%", "{%}").replaceAll("_", "{_}");
    }

    public static void verifyTime(WdkModel wdkModel, String sql, long fromTime)
            throws WdkUserException, WdkModelException {
        double seconds = (System.currentTimeMillis() - fromTime) / 1000D;
        logger.debug("SQL executed in " + seconds + " seconds.");

        if (seconds < 0) {
            logger.error("code error, negative exec time:");
            new Exception().printStackTrace();
        }
        QueryMonitor monitor = wdkModel.getQueryMonitor();
        // convert the time to seconds
        // check if it is a slow query
        if (seconds >= monitor.getSlowQueryThreshold()) {
            if (!monitor.isIgnoredSlowQuery(sql))
                logger.warn("SLOW SQL: " + seconds + " seconds.\n" + sql);
        }
        // check if it is a broken query
        if (seconds >= monitor.getBrokenQueryThreshold()) {
            if (!monitor.isIgnoredBrokenQuery(sql)) {
                logger.warn("BROKEN SQL: " + seconds + " seconds.\n" + sql);
                // also send email to admin
                String email = wdkModel.getModelConfig().getAdminEmail();
                if (email != null) {
                    String subject = "[" + wdkModel.getProjectId()
                            + "] Broken Query " + seconds + " seconds";

                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss");
                    String content = "<p>Recorded: "
                            + sdf.format(cal.getTime()) + "</p>\n<p>" + sql
                            + "</p>";
                    Utilities.sendEmail(wdkModel, email, email, subject,
                            content);
                }
            }
        }
    }

    /**
     * private constructor, make sure SqlUtils cannot be instanced.
     */
    private SqlUtils() {}
}
