package us.tastybento.bskyblock.database.mysql;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;

/**
 * 
 * Class that inserts a <T> into the corresponding database-table.
 * 
 * @author tastybento
 * 
 * @param <T>
 */
public class MySQLDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    public MySQLDatabaseHandler(BSkyBlock plugin, Class<T> type,
            DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }
    
    @Override
    protected String createSelectQuery() {

        StringBuilder sb = new StringBuilder();

        sb.append("SELECT ");
        sb.append(super.getColumns(false));
        sb.append(" FROM ");

        /* We assume the table-name exactly matches the simpleName of T */
        sb.append(type.getSimpleName());

        return sb.toString();
    }
    
    @Override
    protected String createInsertQuery() {

        StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO ");
        sb.append(type.getSimpleName());
        sb.append("(");
        sb.append(super.getColumns(false));
        sb.append(")");
        sb.append(" VALUES (");
        sb.append(super.getColumns(true));
        sb.append(")");

        return sb.toString();
    }

    /**
     * Inserts a <T> into the corresponding database-table
     *  
     * @param instance <T> that should be inserted into the corresponding database-table
     * @throws SQLException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     */
    @Override
    public void insertObject(T instance) throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = databaseConnecter.createConnection();
            preparedStatement = connection.prepareStatement(selectQuery);

            int i = 0;

            for (Field field : type.getDeclaredFields()) {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                        field.getName(), type);

                Method method = propertyDescriptor
                        .getReadMethod();

                Object value = method.invoke(instance);

                preparedStatement.setObject(++i, value);
            }

            preparedStatement.addBatch();

            preparedStatement.executeBatch();

        } finally {
            MySQLDatabaseResourceCloser.close(preparedStatement);
            MySQLDatabaseResourceCloser.close(preparedStatement);
        }
    }

    /**
     * Creates a list of <T>s filled with values from the corresponding
     * database-table
     * 
     * @return List of <T>s filled with values from the corresponding
     *         database-table
     * 
     * @throws SQLException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     */
    @Override
    public List<T> selectObjects() throws SQLException,
            SecurityException, IllegalArgumentException,
            InstantiationException, IllegalAccessException,
            IntrospectionException, InvocationTargetException {

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = databaseConnecter.createConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(selectQuery);

            return createObjects(resultSet);

        } finally {
            MySQLDatabaseResourceCloser.close(resultSet);
            MySQLDatabaseResourceCloser.close(statement);
            MySQLDatabaseResourceCloser.close(connection);
        }
    }

    @Override
    protected T selectObject(String uniqueId) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, IntrospectionException {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    /**
     * 
     * Creates a list of <T>s filled with values from the provided ResultSet
     * 
     * @param resultSet
     *            ResultSet that contains the result of the
     *            database-select-query
     * 
     * @return List of <T>s filled with values from the provided ResultSet
     * 
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     */
    private List<T> createObjects(ResultSet resultSet)
            throws SecurityException, IllegalArgumentException,
            SQLException, InstantiationException,
            IllegalAccessException, IntrospectionException,
            InvocationTargetException {

        List<T> list = new ArrayList<T>();

        while (resultSet.next()) {

            T instance = type.newInstance();

            for (Field field : type.getDeclaredFields()) {

                /* We assume the table-column-names exactly match the variable-names of T */
                Object value = resultSet.getObject(field.getName());

                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                        field.getName(), type);

                Method method = propertyDescriptor.getWriteMethod();

                method.invoke(instance, value);
            }

            list.add(instance);
        }
        return list;
    }

}
