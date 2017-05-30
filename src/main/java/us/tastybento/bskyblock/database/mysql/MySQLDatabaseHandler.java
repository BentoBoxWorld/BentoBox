package us.tastybento.bskyblock.database.mysql;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
        mySQLmapping.put(UUID.class.getTypeName(), "VARCHAR(36)");

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
            String sql = "CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() + "` (";
            for (Field field : type.getDeclaredFields()) {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                plugin.getLogger().info("DEBUG: Field = " + field.getName() + "(" + propertyDescriptor.getPropertyType().getTypeName() + ")");
                // Get default SQL mappings
                Method writeMethod = propertyDescriptor.getWriteMethod();
                String columnName = field.getName();
                String mapping = mySQLmapping.get(propertyDescriptor.getPropertyType().getTypeName());
                if (mapping != null) {
                    sql += "`" + columnName + "` " + mapping + ",";
                    // Create set and map tables. 
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        // The ID in this table relates to the parent table
                        String setSql = "CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() + "." + field.getName() + "` ("
                                + "uniqueId VARCHAR(36) NOT NULL, ";
                        // Get columns separated by commas
                        setSql += getCollectionColumns(writeMethod,false,true);
                        // Add primary key
                        setSql += ")";

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
            sql += " PRIMARY KEY (uniqueId))";
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
     * Returns a string of columns separated by commas that represent the parameter types of this method
     * @param writeMethod
     * @param usePlaceHolders
     *            true, if PreparedStatement-placeholders ('?') should be used
     *            instead of the names of the variables
     * @param createSchema if true contains the columns types
     * @return Returns a string of columns separated by commas.
     */
    private String getCollectionColumns(Method writeMethod, boolean usePlaceHolders, boolean createSchema) {
        String columns = "";
        // Get the return type
        Type[] genericParameterTypes = writeMethod.getGenericParameterTypes();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            if( genericParameterTypes[i] instanceof ParameterizedType ) {
                Type[] parameters = ((ParameterizedType)genericParameterTypes[i]).getActualTypeArguments();
                //parameters[0] contains java.lang.String for method like "method(List<String> value)"
                int index = 0;
                boolean first = true;
                for (Type type : parameters) {
                    plugin.getLogger().info("DEBUG: set type = " + type.getTypeName());
                    if (first)
                        first = false;
                    else
                        columns += ", ";
                    if (usePlaceHolders) {
                        columns +="?";
                    } else {
                        String setMapping = mySQLmapping.get(type.getTypeName());
                        if (setMapping != null) {
                            columns += "`" + type.getTypeName() + "_" + index + "`";
                            if (createSchema) {
                                columns += " " + setMapping;
                            }
                        } else {
                            columns += "`" + type.getTypeName() + "_" + index + "`";
                            if (createSchema) {
                                columns += " VARCHAR(254)";
                                plugin.getLogger().warning("Unknown type! Hoping it'll fit in a string!");
                            }
                        }
                    }
                    index++;
                }
            }
        }
        return columns;
    }

    @Override
    protected String createSelectQuery() {

        StringBuilder sb = new StringBuilder();

        sb.append("SELECT ");
        sb.append(super.getColumns(false));
        sb.append(" FROM ");

        /* We assume the table-name exactly matches the canonical Name of T */
        sb.append("`");
        sb.append(type.getCanonicalName());
        sb.append("`");

        return sb.toString();
    }

    @Override
    protected String createInsertQuery() {

        StringBuilder sb = new StringBuilder();

        sb.append("REPLACE INTO ");
        sb.append("`");
        sb.append(type.getCanonicalName());
        sb.append("`");
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
     * @throws NoSuchMethodException 
     */
    @Override
    public void insertObject(T instance) throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException, NoSuchMethodException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = databaseConnecter.createConnection();
            preparedStatement = connection.prepareStatement(insertQuery);
            // Get the uniqueId
            Method getUniqueId = type.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            plugin.getLogger().info("Unique Id = " + uniqueId);
            if (uniqueId.isEmpty()) {
                throw new SQLException("uniqueId is blank");
            }
            int i = 0;
            plugin.getLogger().info("DEBUG: insert Query " + insertQuery);
            for (Field field : type.getDeclaredFields()) {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                        field.getName(), type);

                Method method = propertyDescriptor.getReadMethod();
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
                    // TODO clear the table?
                    String setSql = "INSERT INTO `" + type.getCanonicalName() + "." + field.getName() + "` (uniqueId, ";
                    setSql += getCollectionColumns(propertyDescriptor.getWriteMethod(), false, false) + ") ";
                    setSql += "VALUES ('" + uniqueId + "'," + getCollectionColumns(propertyDescriptor.getWriteMethod(), true, false) + ")";
                    PreparedStatement collStatement = connection.prepareStatement(setSql);
                    plugin.getLogger().info("DEBUG: collection insert =" + setSql);
                    // Do single dimension types (set and list)
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        plugin.getLogger().info("DEBUG: set class for ");
                        // Loop through the collection
                        Collection<?> collection = (Collection<?>)value;
                        Iterator<?> it = collection.iterator();
                        while (it.hasNext()) {
                            Object setValue = it.next();
                            if (setValue instanceof UUID) {
                                // Serialize everything
                                setValue = serialize(setValue, setValue.getClass());
                            }
                            collStatement.setObject(1, setValue);
                            plugin.getLogger().info("DEBUG: " + collStatement.toString());
                            collStatement.execute();
                        }
                    } else if (propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class)) {
                        // Loop through the collection
                        Map<?,?> collection = (Map<?,?>)value;
                        Iterator<?> it = collection.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<?,?> en = (Entry<?, ?>) it.next();
                            Object key = serialize(en.getKey(), en.getKey().getClass());
                            plugin.getLogger().info("DEBUG: key class = " + en.getKey().getClass().getTypeName());
                            Object mapValue = serialize(en.getValue(), en.getValue().getClass());;
                            collStatement.setObject(1, key);
                            collStatement.setObject(2, mapValue);
                            plugin.getLogger().info("DEBUG: " + collStatement.toString());
                            collStatement.execute();
                        }
                    }
                    // Set value for the main insert
                    value = true;
                } else {
                    value = serialize(value, propertyDescriptor.getPropertyType());
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
     * Serializes value
     * @param value
     * @param clazz - the known class of value
     * @return
     */
    private Object serialize(Object value, Class<? extends Object> clazz) { 
        plugin.getLogger().info("DEBUG: serialize - class is " + clazz.getTypeName());
        if (value == null) {
            return "null";
        }
        // Types that need to be serialized
        if (clazz.equals(UUID.class)) {
            value = ((UUID)value).toString();
        } else
        // Bukkit Types
        if (clazz.equals(Location.class)) {
            // Serialize
            value = Util.getStringLocation(((Location)value));
        } else
        if (clazz.equals(World.class)) {
             // Serialize - get the name
             value = ((World)value).getName();            
        } else
        if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(Enum.class)) {
            //Custom enums are a child of the Enum class. Just get the names of each one.
            value = ((Enum<?>)value).name();
        }
        if (value == null) {
            return "null";
        }
        return value;

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
