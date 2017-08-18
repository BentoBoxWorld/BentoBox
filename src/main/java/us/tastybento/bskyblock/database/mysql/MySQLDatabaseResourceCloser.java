package us.tastybento.bskyblock.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLDatabaseResourceCloser {

    /**
     * Closes the provided ResultSets
     *
     * @param resultSets
     *            ResultSets that should be closed
     */
    public static void close(ResultSet... resultSets) {

        if (resultSets == null)
            return;

        for (ResultSet resultSet : resultSets) {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    /* Do some exception-logging here. */
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Closes the provided Statements
     *
     * @param statements
     *            Statements that should be closed
     */
    public static void close(Statement... statements) {
        /*
         * No need to create methods for PreparedStatement and
         * CallableStatement, because they extend Statement.
         */

        if (statements == null)
            return;

        for (Statement statement : statements) {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    /* Do some exception-logging here. */
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Closes the provided Connections
     *
     * @param connections
     *            Connections that should be closed
     */
    public static void close(Connection... connections) {
        if (connections == null)
            return;

        for (Connection connection : connections) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    /* Do some exception-logging here. */
                    e.printStackTrace();
                }
            }
        }
    }
}

