package us.tastybento.bskyblock.database.mysql;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.util.Util;

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

        // TODO: Collections - these need to create another table and link to it
        // Collections are stored as additional tables. The boolean indicates whether there 
        // is any data in it or not (maybe)
        mySQLmapping.put(Set.class.getTypeName(), "BOOL");
        mySQLmapping.put(Map.class.getTypeName(), "BOOL");
        mySQLmapping.put(HashMap.class.getTypeName(), "BOOL");
        mySQLmapping.put(ArrayList.class.getTypeName(), "BOOL");

    }

    public MySQLDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
        try {
            connection = databaseConnecter.createConnection();
        } catch (SQLException e1) {
            plugin.getLogger().severe(e1.getMessage());
            return;
        }
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
        try {
            String sql = "CREATE TABLE IF NOT EXISTS " + type.getSimpleName() + "(";
            for (Field field : type.getDeclaredFields()) {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                plugin.getLogger().info("DEBUG: Field = " + field.getName() + "(" + propertyDescriptor.getPropertyType().getTypeName() + ")");
                String mapping = mySQLmapping.get(propertyDescriptor.getPropertyType().getTypeName());
                if (mapping != null) {
                    sql += "`" + field.getName() + "` " + mapping + ",";
                    // Create set and map tables. 
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        String setSql = "CREATE TABLE IF NOT EXISTS " + type.getSimpleName() + "_" + field.getName() + " (";
                        // Get the type
                        setSql += getMethodParameterTypes(propertyDescriptor.getWriteMethod());
                        plugin.getLogger().info(setSql);
                        PreparedStatement collections = connection.prepareStatement(setSql);
                        collections.executeUpdate();
                    }
                } else {
                    sql += field.getName() + " VARCHAR(254),";
                    plugin.getLogger().severe("Unknown type! Hoping it'll fit in a string!");
                }
            }
            //plugin.getLogger().info("DEBUG: SQL before trim string = " + sql);
            sql = sql.substring(0,(sql.length()-1)) + ")";
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

    /**
     * Gets the types for parameters in a method
     * @param writeMethod
     * @return List of strings with the SQL for parameter and type set
     */
    private String getMethodParameterTypes(Method method) {
        String result = "";
        // Get the return type
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            if( genericParameterTypes[i] instanceof ParameterizedType ) {
                Type[] parameters = ((ParameterizedType)genericParameterTypes[i]).getActualTypeArguments();
                //parameters[0] contains java.lang.String for method like "method(List<String> value)"
                int index = 0;
                String firstColumn = "";
                for (Type type : parameters) {
                    plugin.getLogger().info("DEBUG: set type = " + type.getTypeName());
                    String setMapping = mySQLmapping.get(type.getTypeName());
                    String notNull = "";
                    if (index == 0) {
                        firstColumn = "`" + type.getTypeName() + "_" + index + "`";
                        notNull = " NOT NULL";
                    }
                    if (setMapping != null) {
                        result += "`" + type.getTypeName() + "_" + index + "` " + setMapping + notNull + ",";
                    } else {
                        result += "`" + type.getTypeName() + "_" + index + "` VARCHAR(254)" + notNull + ",";
                        plugin.getLogger().severe("Unknown type! Hoping it'll fit in a string!");
                    }
                    index++;
                }
                // Add primary key
                result += " PRIMARY KEY (" + firstColumn + "))";
            }
        }
        return result;
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
                plugin.getLogger().info("DEBUG: Field = " + field.getName() + "(" + propertyDescriptor.getPropertyType().getTypeName() + ")");
                //sql += "`" + field.getName() + "` " + mapping + ",";
                
                Object value = method.invoke(instance);
                
                // Create set and map tables. 
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // Collection
                    // TODO Set the values in the subsidiary tables.
                    value = true;
                }
                // Types that need to be serialized
                if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                    value = ((UUID)value).toString();
                }
                // Bukkit Types
                if (propertyDescriptor.getPropertyType().equals(Location.class)) {
                    // Serialize
                    value = Util.getStringLocation(((Location)value));
                }
                if (propertyDescriptor.getPropertyType().equals(World.class)) {
                    // Serialize - get the name
                    value = ((World)value).getName();
                }
                

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
                
                // Create set and map tables. 
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // Collection
                    // TODO Set the values in the subsidiary tables.
                    value = null;
                }
                // Types that need to be serialized
                if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                    value = UUID.fromString((String)value);
                }
                // Bukkit Types
                if (propertyDescriptor.getPropertyType().equals(Location.class)) {
                    // Serialize
                    value = Util.getLocationString(((String)value));
                }
                if (propertyDescriptor.getPropertyType().equals(World.class)) {
                    // Serialize - get the name
                    value = plugin.getServer().getWorld((String)value);
                }
                method.invoke(instance, value);
            }

            list.add(instance);
        }
        return list;
    }

}
