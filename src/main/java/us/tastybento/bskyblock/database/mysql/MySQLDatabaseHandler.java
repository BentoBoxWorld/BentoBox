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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
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

    /**
     * Connection to the database
     */
    private Connection connection = null;
    /**
     * This hashmap maps Java types to MySQL SQL types because they are not the same
     */
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

        // Collections are stored as additional tables. The boolean indicates whether there 
        // is any data in it or not (maybe)
        mySQLmapping.put(Set.class.getTypeName(), "BOOL");
        mySQLmapping.put(Map.class.getTypeName(), "BOOL");
        mySQLmapping.put(HashMap.class.getTypeName(), "BOOL");
        mySQLmapping.put(ArrayList.class.getTypeName(), "BOOL");

    }

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored. 
     * @param plugin
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param databaseConnecter - authentication details for the database
     */
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
            // Run through the fields of the class using introspection
            for (Field field : type.getDeclaredFields()) {
                // Get the description of the field
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                //plugin.getLogger().info("DEBUG: Field = " + field.getName() + "(" + propertyDescriptor.getPropertyType().getTypeName() + ")");
                // Get default SQL mappings
                // Get the write method for this field. This method will take an argument of the type of this field.
                Method writeMethod = propertyDescriptor.getWriteMethod();
                // The SQL column name is the name of the field
                String columnName = field.getName();
                // Get the mapping for this field from the hashmap
                String mapping = mySQLmapping.get(propertyDescriptor.getPropertyType().getTypeName());
                // If it exists, then create the SQL
                if (mapping != null) {
                    // Note that the column name must be enclosed in `'s because it may include reserved words.
                    sql += "`" + columnName + "` " + mapping + ",";
                    // Create set and map tables if the type is a collection
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        // The ID in this table relates to the parent table and is unique
                        String setSql = "CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() + "." + field.getName() + "` ("
                                + "uniqueId VARCHAR(36) NOT NULL, ";
                        // Get columns separated by commas
                        setSql += getCollectionColumns(writeMethod,false,true);
                        // Close the SQL string
                        setSql += ")";

                        //plugin.getLogger().info(setSql);
                        // Execute the statement
                        PreparedStatement collections = connection.prepareStatement(setSql);
                        collections.executeUpdate();
                    }
                } else {
                    // The Java type is not in the hashmap, so we'll jus guess that it can be stored in a string
                    // This should NOT be used in general because every type should be in the hashmap
                    sql += field.getName() + " VARCHAR(254),";
                    plugin.getLogger().severe("Unknown type! Hoping it'll fit in a string!");
                }
            }
            //plugin.getLogger().info("DEBUG: SQL before trim string = " + sql);
            // For the main table for the class, the unique ID is the primary key
            sql += " PRIMARY KEY (uniqueId))";
            //plugin.getLogger().info("DEBUG: SQL string = " + sql);
            // Prepare and execute the database statements
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the database properly
            MySQLDatabaseResourceCloser.close(pstmt);
            MySQLDatabaseResourceCloser.close(pstmt);
        }
    }


    /**
     * Returns a string of columns separated by commas that represent the parameter types of this method
     * this is used to get into parameters like HashMap<Location, Boolean> at runtime and find out Location
     * and Boolean.
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
        // This uses a trick to extract what the arguments are of the writeMethod of the field.
        // In this way, we can deduce what type needs to be written at runtime.
        Type[] genericParameterTypes = writeMethod.getGenericParameterTypes();
        // There could be more than one argument, so step through them
        for (int i = 0; i < genericParameterTypes.length; i++) {
            // If the argument is a parameter, then do something - this should always be true if the parameter is a collection
            if( genericParameterTypes[i] instanceof ParameterizedType ) {
                // Get the actual type arguments of the parameter 
                Type[] parameters = ((ParameterizedType)genericParameterTypes[i]).getActualTypeArguments();
                //parameters[0] contains java.lang.String for method like "method(List<String> value)"
                // Run through them one by one and create a SQL string
                int index = 0;
                boolean first = true;
                for (Type type : parameters) {
                    //plugin.getLogger().info("DEBUG: set type = " + type.getTypeName());
                    // first is used to add commas in the right place
                    if (first)
                        first = false;
                    else
                        columns += ", ";
                    // this is used if the string is going to be used to insert something so the value will replace the ?
                    if (usePlaceHolders) {
                        columns +="?";
                    } else {
                        // This is a request for column names. 
                        String setMapping = mySQLmapping.get(type.getTypeName());
                        // We know the mapping of this type
                        if (setMapping != null) {
                            // Write the name of this type, followed by an index number to make it unique
                            columns += "`" + type.getTypeName() + "_" + index + "`";
                            // If this is the schema, then we also need to add the mapping type
                            if (createSchema) {
                                columns += " " + setMapping;
                            }
                        } else {
                            // We do not know the type, oops
                            // Write the name of this type, followed by an index number to make it unique
                            columns += "`" + type.getTypeName() + "_" + index + "`";
                            if (createSchema) {
                                // If this is the schema, then guess the mapping type
                                columns += " VARCHAR(254)";
                                plugin.getLogger().warning("Unknown type! Hoping it'll fit in a string!");
                            }
                        }
                    }
                    // Increment the index so each column has a unique name
                    index++;
                }
            }
        }
        return columns;
    }


    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#createSelectQuery()
     */
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

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#createInsertQuery()
     */
    @Override
    protected String createInsertQuery() {

        StringBuilder sb = new StringBuilder();
        // Replace into is used so that any data in the table will be replaced with updated data
        sb.append("REPLACE INTO ");
        sb.append("`");
        // The table name is the canonical name, so that add-ons can be sure of a unique table in the database
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

    protected String createDeleteQuery() {
        return "DELETE FROM [table_name] WHERE uniqueId = ?";        
    }

    /**
     * Inserts a <T> into the corresponding database-table
     *  
     * @param instance <T> that should be inserted into the corresponding database-table. Must extend DataObject.
     * @throws SQLException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException 
     */
    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#insertObject(java.lang.Object)
     */
    @Override
    public void saveObject(T instance) throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException, NoSuchMethodException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // Try to connect to the database
            connection = databaseConnecter.createConnection();
            // insertQuery is created in super from the createInsertQuery() method
            preparedStatement = connection.prepareStatement(insertQuery);
            // Get the uniqueId. As each class extends DataObject, it must have this method in it.
            Method getUniqueId = type.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            //plugin.getLogger().info("DEBUG: Unique Id = " + uniqueId);
            if (uniqueId.isEmpty()) {
                throw new SQLException("uniqueId is blank");
            }
            // Create the insertion
            int i = 0;
            //plugin.getLogger().info("DEBUG: insert Query " + insertQuery);
            // Run through the fields in the class using introspection
            for (Field field : type.getDeclaredFields()) {
                // Get the field's property descriptor
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                // Get the read method for this field
                Method method = propertyDescriptor.getReadMethod();
                //plugin.getLogger().info("DEBUG: Field = " + field.getName() + "(" + propertyDescriptor.getPropertyType().getTypeName() + ")");
                //sql += "`" + field.getName() + "` " + mapping + ",";
                // Invoke the read method to obtain the value from the class - this is the value we need to store in the database
                Object value = method.invoke(instance);

                // Create set and map table inserts if this is a Collection
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // Collection
                    // The table is cleared for this uniqueId every time the data is stored
                    String clearTableSql = "DELETE FROM  `" + type.getCanonicalName() + "." + field.getName() + "` WHERE uniqueId = ?";
                    PreparedStatement collStatement = connection.prepareStatement(clearTableSql);
                    collStatement.setString(1, uniqueId);
                    collStatement.execute();
                    // Insert into the table
                    String setSql = "INSERT INTO `" + type.getCanonicalName() + "." + field.getName() + "` (uniqueId, ";
                    // Get the columns we are going to insert, just the names of them
                    setSql += getCollectionColumns(propertyDescriptor.getWriteMethod(), false, false) + ") ";
                    // Get all the ?'s for the columns
                    setSql += "VALUES ('" + uniqueId + "'," + getCollectionColumns(propertyDescriptor.getWriteMethod(), true, false) + ")";
                    // Prepare the statement
                    collStatement = connection.prepareStatement(setSql);
                    //plugin.getLogger().info("DEBUG: collection insert =" + setSql);
                    // Do single dimension types (set and list)
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        //plugin.getLogger().info("DEBUG: set class for ");
                        // Loop through the set or list
                        // Note that we have no idea what type this is
                        Collection<?> collection = (Collection<?>)value;
                        Iterator<?> it = collection.iterator();
                        while (it.hasNext()) {
                            Object setValue = it.next();
                            //if (setValue instanceof UUID) {
                            // Serialize everything
                            setValue = serialize(setValue, setValue.getClass());
                            //}
                            // Set the value from ? to whatever it is
                            collStatement.setObject(1, setValue);
                            //plugin.getLogger().info("DEBUG: " + collStatement.toString());
                            // Execute the SQL in the database
                            collStatement.execute();
                        }
                    } else if (propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class)) {
                        // Loop through the map
                        Map<?,?> collection = (Map<?,?>)value;
                        Iterator<?> it = collection.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<?,?> en = (Entry<?, ?>) it.next();
                            // Get the key and serialize it
                            Object key = serialize(en.getKey(), en.getKey().getClass());
                            //plugin.getLogger().info("DEBUG: key class = " + en.getKey().getClass().getTypeName());
                            // Get the value and serialize it
                            Object mapValue = serialize(en.getValue(), en.getValue().getClass());
                            // Write the objects into prepared statement
                            collStatement.setObject(1, key);
                            collStatement.setObject(2, mapValue);
                            //plugin.getLogger().info("DEBUG: " + collStatement.toString());
                            // Write to database
                            collStatement.execute();
                        }
                    }
                    // Set value for the main insert. For collections, this is just a dummy value because the real values are in the
                    // additional table.
                    value = true;
                } else {
                    // If the value is not a collection, it just needs to be serialized to go into the database.
                    value = serialize(value, propertyDescriptor.getPropertyType());
                }
                // Set the value in the main prepared statement and increment the location
                // Note that with prepared statements, they count from 1, not 0, so the ++ goes on the front of i.
                preparedStatement.setObject(++i, value);
            }
            // Add the statements to a batch
            preparedStatement.addBatch();
            // Execute
            preparedStatement.executeBatch();

        } finally {
            // Close properly
            MySQLDatabaseResourceCloser.close(preparedStatement);
            MySQLDatabaseResourceCloser.close(preparedStatement);
        }
    }

    /**
     * Serializes values if required to go into a database.
     * TODO: This method will need expanding to include additional Java types
     * @param value
     * @param clazz - the known class of value
     * @return the object to write to the database
     */
    private Object serialize(Object value, Class<? extends Object> clazz) { 
        //plugin.getLogger().info("DEBUG: serialize - class is " + clazz.getTypeName());
        if (value == null) {
            // If the value is null to start, return null as a string
            return "null";
        }
        // Types that need to be serialized
        // TODO - add others, like Date, Timestamp, etc.
        if (clazz.equals(UUID.class)) {
            value = ((UUID)value).toString();
        } 
        else
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
            // The value could become null from the above checks
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
     * @throws ClassNotFoundException 
     */
    @Override
    public List<T> loadObjects() throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException, ClassNotFoundException {

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

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#selectObject(java.lang.String)
     */
    @Override
    protected T loadObject(String uniqueId) throws InstantiationException,
    IllegalAccessException, IllegalArgumentException,
    InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = databaseConnecter.createConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(selectQuery);

            List<T> result = createObjects(resultSet);
            if (!result.isEmpty()) {
                return result.get(0);
            }
            return null;

        } finally {
            MySQLDatabaseResourceCloser.close(resultSet);
            MySQLDatabaseResourceCloser.close(statement);
            MySQLDatabaseResourceCloser.close(connection);
        }
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
     * @throws ClassNotFoundException 
     */
    @SuppressWarnings("unchecked")
    private List<T> createObjects(ResultSet resultSet)
            throws SecurityException, IllegalArgumentException,
            SQLException, InstantiationException,
            IllegalAccessException, IntrospectionException,
            InvocationTargetException, ClassNotFoundException {

        List<T> list = new ArrayList<T>();
        // The database can return multiple results in one go, e.g., all the islands in the database
        // Run through them one by one
        while (resultSet.next()) {
            // Create a new instance of this type
            T instance = type.newInstance();
            // Get the unique ID from the results
            String uniqueId = resultSet.getString("uniqueId");
            if (uniqueId == null) {
                throw new SQLException("No unique ID in the results!");
            }
            // Use introspection to run through all the fields in this type class
            for (Field field : type.getDeclaredFields()) {
                /* We assume the table-column-names exactly match the variable-names of T */
                Object value = resultSet.getObject(field.getName());
                // Get the property descriptor of this type
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                // Get the write method for this field, because we are going to use it to write the value
                // once we get the value from the database
                Method method = propertyDescriptor.getWriteMethod();
                // If the type is a Collection, then we need to deal with set and map tables 
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // Collection
                    //plugin.getLogger().info("DEBUG: Collection");
                    // TODO Get the values from the subsidiary tables.
                    // value is just of type boolean right now
                    String setSql = "SELECT ";
                    // Get the columns, just the names of them, no ?'s or types
                    setSql += getCollectionColumns(method, false, false) + " ";
                    setSql += "FROM `" + type.getCanonicalName() + "." + field.getName() + "` ";
                    // We will need to fill in the ? later with the unique id of the class from the database
                    setSql += "WHERE uniqueId = ?";
                    // Prepare the statement
                    PreparedStatement collStatement = connection.prepareStatement(setSql);
                    // Set the unique ID
                    collStatement.setObject(1, uniqueId);
                    //plugin.getLogger().info("DEBUG: collStatement = " + collStatement.toString());
                    ResultSet collectionResultSet = collStatement.executeQuery();                   
                    //plugin.getLogger().info("DEBUG: collectionResultSet = " + collectionResultSet.toString());
                    // Do single dimension types (set and list)
                    if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                        //plugin.getLogger().info("DEBUG: adding a set");
                        // Loop through the collection resultset 
                        // Note that we have no idea what type this is
                        List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                        // collectionTypes should be only 1 long
                        Type setType = collectionTypes.get(0);
                        value = new HashSet<Object>();
                        //plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                        //plugin.getLogger().info("DEBUG: setType = " + setType.getTypeName());
                        while (collectionResultSet.next()) {
                            //plugin.getLogger().info("DEBUG: collectionResultSet size = " + collectionResultSet.getFetchSize());
                            ((Set<Object>) value).add(deserialize(collectionResultSet.getObject(1),Class.forName(setType.getTypeName())));
                        }
                    } else if (propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        //plugin.getLogger().info("DEBUG: Adding a list ");
                        // Loop through the collection resultset 
                        // Note that we have no idea what type this is
                        List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                        // collectionTypes should be only 1 long
                        Type setType = collectionTypes.get(0);
                        value = new ArrayList<Object>();
                        //plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                        while (collectionResultSet.next()) {
                            //plugin.getLogger().info("DEBUG: adding to the list");
                            //plugin.getLogger().info("DEBUG: collectionResultSet size = " + collectionResultSet.getFetchSize());
                            ((List<Object>) value).add(deserialize(collectionResultSet.getObject(1),Class.forName(setType.getTypeName())));
                        }
                    } else if (propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class)) {
                        //plugin.getLogger().info("DEBUG: Adding a map ");
                        // Loop through the collection resultset 
                        // Note that we have no idea what type this is
                        List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                        // collectionTypes should be 2 long
                        Type keyType = collectionTypes.get(0);
                        Type valueType = collectionTypes.get(1);
                        value = new HashMap<Object, Object>();
                        //plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                        while (collectionResultSet.next()) {
                            //plugin.getLogger().info("DEBUG: adding to the map");
                            //plugin.getLogger().info("DEBUG: collectionResultSet size = " + collectionResultSet.getFetchSize());
                            // Work through the columns
                            // Key
                            Object key = deserialize(collectionResultSet.getObject(1),Class.forName(keyType.getTypeName()));
                            //plugin.getLogger().info("DEBUG: key = " + key);
                            Object mapValue = deserialize(collectionResultSet.getObject(2),Class.forName(valueType.getTypeName()));
                            //plugin.getLogger().info("DEBUG: value = " + mapValue);
                            ((Map<Object,Object>) value).put(key,mapValue);
                        }
                    } else {
                        // Set value for the main insert. For collections, this is just a dummy value because the real values are in the
                        // additional table.
                        value = true;
                    }
                } else {
                    //plugin.getLogger().info("DEBUG: regular type");
                    value = deserialize(value, propertyDescriptor.getPropertyType());
                }
                //plugin.getLogger().info("DEBUG: invoking method " + method.getName());
                //plugin.getLogger().info("DEBUG: value class = " + value.getClass().getName());
                // Write the value to the class
                method.invoke(instance, value);
            }
            // Write the result into the list we are going to return
            list.add(instance);
        }
        return list;
    }

    /**
     * Deserialize any values according to their class
     * TODO: expand to include additional types
     * @param value
     * @param clazz
     * @return the deserialized value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object deserialize(Object value, Class<? extends Object> clazz) { 
        //plugin.getLogger().info("DEBUG: deserialize - class is " + clazz.getTypeName());
        if (value instanceof String && value.equals("null")) {
            // If the value is null as a string, return null 
            return null;
        }
        // Types that need to be deserialized
        if (clazz.equals(UUID.class)) {
            value = UUID.fromString((String)value);
        }
        // Bukkit Types
        if (clazz.equals(Location.class)) {
            // Get Location from String - may be null...
            value = Util.getLocationString(((String)value));
        }
        if (clazz.equals(World.class)) {
            // Get world by name - may be null...
            value = plugin.getServer().getWorld((String)value);
        }
        // Enums
        if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(Enum.class)) {
            //Custom enums are a child of the Enum class.
            // Find out the value
            try {
                Class<Enum> enumClass = (Class<Enum>)clazz;
                value = Enum.valueOf(enumClass, (String)value);
            } catch (Exception e) {
                // Maybe this value does not exist?
                // TODO return something?
                e.printStackTrace();
            }
        }
        return value;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#deleteObject(java.lang.Object)
     */
    @Override
    public void deleteObject(T instance)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, IntrospectionException, SQLException, NoSuchMethodException, SecurityException {
        // Delete this object from all tables
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // Try to connect to the database
            connection = databaseConnecter.createConnection();
            // Get the uniqueId. As each class extends DataObject, it must have this method in it.
            Method getUniqueId = type.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            //plugin.getLogger().info("DEBUG: Unique Id = " + uniqueId);
            if (uniqueId.isEmpty()) {
                throw new SQLException("uniqueId is blank");
            }
            // Delete from the main table
            // First substitution is the table name
            // deleteQuery is created in super from the createInsertQuery() method
            preparedStatement = connection.prepareStatement(deleteQuery.replace("[table_name]", "`" + type.getCanonicalName() + "`"));
            // Second is the unique ID
            preparedStatement.setString(1, uniqueId);
            preparedStatement.addBatch();
            plugin.getLogger().info("DEBUG: DELETE Query " + preparedStatement.toString());
            preparedStatement.executeBatch();
            // Delete from any sub tables created from the object
            // Run through the fields in the class using introspection
            for (Field field : type.getDeclaredFields()) {
                // Get the field's property descriptor
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                // Delete Collection tables
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // First substitution is the table name
                    preparedStatement = connection.prepareStatement(deleteQuery.replace("[table_name]", "`" + type.getCanonicalName() + "." + field.getName() + "`"));
                    // Second is the unique ID
                    preparedStatement.setString(1, uniqueId);
                    preparedStatement.addBatch();
                    // Execute
                    plugin.getLogger().info("DEBUG: " + preparedStatement.toString());
                    preparedStatement.executeBatch();
                }
            }
        } finally {
            // Close properly
            MySQLDatabaseResourceCloser.close(preparedStatement);
        }        

    }



}
