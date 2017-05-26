package us.tastybento.bskyblock.database.mysql;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

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

    private Connection connection = null;
    private static HashMap<String, String> mySQLmapping;
    {
        mySQLmapping = new HashMap<String, String>();
        mySQLmapping.put(boolean.class.getTypeName(), "BOOL");
        mySQLmapping.put(byte.class.getTypeName(), "TINYINT");
        mySQLmapping.put(short.class.getTypeName(), "SMALLINT");
        mySQLmapping.put(int.class.getTypeName(), "INTEGER");
        mySQLmapping.put(long.class.getTypeName(), "BIGINT");
        mySQLmapping.put(double.class.getTypeName(), "DOUBLE PRECISION");
        mySQLmapping.put(Boolean.class.getTypeName(), "BOOL");
        mySQLmapping.put(Byte.class.getTypeName(), "TINYINT");
        mySQLmapping.put(Short.class.getTypeName(), "SMALLINT");
        mySQLmapping.put(Integer.class.getTypeName(), "INTEGER");
        mySQLmapping.put(Long.class.getTypeName(), "BIGINT");
        mySQLmapping.put(Double.class.getTypeName(), "DOUBLE PRECISION");
        mySQLmapping.put(BigDecimal.class.getTypeName(), "DECIMAL(13,0)");
        mySQLmapping.put(String.class.getTypeName(), "VARCHAR(254)");
        mySQLmapping.put(Date.class.getTypeName(), "DATE");
        mySQLmapping.put(Time.class.getTypeName(), "TIME");
        mySQLmapping.put(Timestamp.class.getTypeName(), "TIMESTAMP");
        mySQLmapping.put(UUID.class.getTypeName(), "VARCHAR(32)"); // TODO: How long is a UUID to string?

        // Bukkit Mappings
        mySQLmapping.put(Location.class.getTypeName(), "VARCHAR(254)");
        mySQLmapping.put(World.class.getTypeName(), "VARCHAR(254)");
        
        // Sets - this stores just the name of the set which is another table
        mySQLmapping.put(Set.class.getTypeName(), "VARCHAR(254)");
          
     }

    public MySQLDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
        // Check if the table exists in the database and if not, create it
        try {
            createSchema();
        } catch (IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Creates the table in the database if it doesn't exist already
     * @throws IntrospectionException
     * @throws SQLException
     */
    private void createSchema() throws IntrospectionException, SQLException {
        PreparedStatement pstmt = null;
        connection = databaseConnecter.createConnection();
        try {
            String sql = "CREATE TABLE IF NOT EXISTS " + type.getSimpleName() + "(";
            for (Field field : type.getDeclaredFields()) {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                plugin.getLogger().info("DEBUG: Field = " + field.getName());
                String mapping = mySQLmapping.get(propertyDescriptor.getPropertyType().getTypeName());
                if (mapping != null) {
                    sql += field.getName() + " " + mapping + ",";
                } else {
                    sql += field.getName() + " VARCHAR(254),";
                    plugin.getLogger().severe("Unknown type! Hoping it'll fit in a string!");
                }
            }
            sql = sql.substring((sql.length()-1), sql.length()) + ")";
            plugin.getLogger().info("DEBUG: SQL string = " + sql);
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MySQLDatabaseResourceCloser.close(pstmt);
            MySQLDatabaseResourceCloser.close(pstmt);
        }
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
