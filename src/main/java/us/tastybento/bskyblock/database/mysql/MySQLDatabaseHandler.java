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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.AdapterInterface;
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
    private static final HashMap<String, String> MYSQL_MAPPING = new HashMap<>();
    private static final String STRING_MAP = "VARCHAR(254)";

    static {
        MYSQL_MAPPING.put(boolean.class.getTypeName(), "BOOL");
        MYSQL_MAPPING.put(byte.class.getTypeName(), "TINYINT");
        MYSQL_MAPPING.put(short.class.getTypeName(), "SMALLINT");
        MYSQL_MAPPING.put(int.class.getTypeName(), "INTEGER");
        MYSQL_MAPPING.put(long.class.getTypeName(), "BIGINT");
        MYSQL_MAPPING.put(double.class.getTypeName(), "DOUBLE PRECISION");
        MYSQL_MAPPING.put(Boolean.class.getTypeName(), "BOOL");
        MYSQL_MAPPING.put(Byte.class.getTypeName(), "TINYINT");
        MYSQL_MAPPING.put(Short.class.getTypeName(), "SMALLINT");
        MYSQL_MAPPING.put(Integer.class.getTypeName(), "INTEGER");
        MYSQL_MAPPING.put(Long.class.getTypeName(), "BIGINT");
        MYSQL_MAPPING.put(Double.class.getTypeName(), "DOUBLE PRECISION");
        MYSQL_MAPPING.put(BigDecimal.class.getTypeName(), "DECIMAL(13,0)");
        MYSQL_MAPPING.put(String.class.getTypeName(), STRING_MAP);
        MYSQL_MAPPING.put(Date.class.getTypeName(), "DATE");
        MYSQL_MAPPING.put(Time.class.getTypeName(), "TIME");
        MYSQL_MAPPING.put(Timestamp.class.getTypeName(), "TIMESTAMP");
        MYSQL_MAPPING.put(UUID.class.getTypeName(), "VARCHAR(36)");

        // Bukkit Mappings
        MYSQL_MAPPING.put(Location.class.getTypeName(), STRING_MAP);
        MYSQL_MAPPING.put(World.class.getTypeName(), STRING_MAP);

        // Collections are stored as additional tables. The boolean indicates whether there
        // is any data in it or not (maybe)
        MYSQL_MAPPING.put(Set.class.getTypeName(), "BOOL");
        MYSQL_MAPPING.put(Map.class.getTypeName(), "BOOL");
        MYSQL_MAPPING.put(HashMap.class.getTypeName(), "BOOL");
        MYSQL_MAPPING.put(ArrayList.class.getTypeName(), "BOOL");

        // Enums
        MYSQL_MAPPING.put(Enum.class.getTypeName(), STRING_MAP);

    }

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - BSkyBlock plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param databaseConnecter - authentication details for the database
     */
    public MySQLDatabaseHandler(Plugin plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
        connection = databaseConnecter.createConnection();
        // Check if the table exists in the database and if not, create it
        createSchema();
    }

    /**
     * Creates the table in the database if it doesn't exist already
     */
    private void createSchema() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `");
        sql.append(dataObject.getCanonicalName());
        sql.append("` (");
        // Run through the fields of the class using introspection
        for (Field field : dataObject.getDeclaredFields()) {
            try {
                // Get the description of the field
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
                // Get default SQL mappings
                // Get the write method for this field. This method will take an argument of the type of this field.
                Method writeMethod = propertyDescriptor.getWriteMethod();
                // The SQL column name is the name of the field
                String columnName = field.getName();
                // Get the mapping for this field from the hashmap
                String typeName = propertyDescriptor.getPropertyType().getTypeName();
                if (propertyDescriptor.getPropertyType().isEnum()) {
                    typeName = "Enum";
                }
                String mapping = MYSQL_MAPPING.get(typeName);
                // If it exists, then create the SQL
                if (mapping != null) {
                    // Note that the column name must be enclosed in `'s because it may include reserved words.
                    sql.append("`");
                    sql.append(columnName);
                    sql.append("` ");
                    sql.append(mapping);
                    sql.append(",");
                    // Create set and map tables if the type is a collection
                    if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                            propertyDescriptor.getPropertyType().equals(Map.class) ||
                            propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                            propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                        // The ID in this table relates to the parent table and is unique
                        StringBuilder setSql = new StringBuilder();
                        setSql.append("CREATE TABLE IF NOT EXISTS `");
                        setSql.append(dataObject.getCanonicalName());
                        setSql.append(".");
                        setSql.append(field.getName());
                        setSql.append("` (");
                        setSql.append("uniqueId VARCHAR(36) NOT NULL, ");
                        // Get columns separated by commas
                        setSql.append(getCollectionColumnString(writeMethod,false,true));
                        // Close the SQL string
                        setSql.append(")");
                        // Execute the statement
                        try (PreparedStatement collections = connection.prepareStatement(setSql.toString())) {
                            collections.executeUpdate();
                        } catch (SQLException e) {
                            plugin.getLogger().severe(() -> "Getter or setter missing in data object. Cannot create schema! " + e.getMessage());
                        }
                    }
                } else {
                    // The Java type is not in the hashmap, so we'll just guess that it can be stored in a string
                    // This should NOT be used in general because every type should be in the hashmap
                    sql.append(field.getName());
                    sql.append(" ");
                    sql.append(STRING_MAP);
                    sql.append(",");
                    plugin.getLogger().severe("Unknown type! Hoping it'll fit in a string!");
                    plugin.getLogger().severe(propertyDescriptor.getPropertyType().getTypeName());
                }
            } catch (IntrospectionException e) {
                plugin.getLogger().severe(() -> "Getter or setter missing in data object. Cannot create schema! " + e.getMessage());
            }
        }
        // For the main table for the class, the unique ID is the primary key
        sql.append(" PRIMARY KEY (uniqueId))");
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }

    /**
     * Creates a comma-separated-String with the names of the variables in this
     * class
     * Not used in Flat File database.
     * @param usePlaceHolders
     *            true, if PreparedStatement-placeholders ('?') should be used
     *            instead of the names of the variables
     * @return a comma-separated-String with the names of the variables
     */
    public String getColumns(boolean usePlaceHolders) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        /* Iterate the column-names */
        for (Field f : dataObject.getDeclaredFields()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            if (usePlaceHolders) {
                sb.append("?");
            } else {
                sb.append("`" + f.getName() + "`");
            }
        }

        return sb.toString();
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
    private String getCollectionColumnString(Method writeMethod, boolean usePlaceHolders, boolean createSchema) {
        StringBuilder sb = new StringBuilder();
        List<String> cols = getCollentionColumnList(writeMethod, createSchema);
        boolean first = true;
        for (String col : cols) {
            // Add commas
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            // this is used if the string is going to be used to insert something so the value will replace the ?
            if (usePlaceHolders) {
                sb.append("?");
            } else {
                sb.append(col);
            }
        }
        return sb.toString();
    }

    /**
     * Returns a list of columns that represent the parameter types of this method
     * @param method
     * @param createSchema if true contains the columns types
     * @return Returns a list of columns separated by commas.
     */
    private List<String> getCollentionColumnList(Method method, boolean createSchema) {
        List<String> columns = new ArrayList<>();
        for (Entry<String,String> en : getCollectionColumnMap(method).entrySet()) {
            StringBuilder col = new StringBuilder();
            col.append(en.getKey());
            if (createSchema) {
                col.append(" ");
                col.append(en.getValue());
            }
            columns.add(col.toString());
        }

        return columns;
    }

    /**
     * Returns a map of column names and their types
     * @param method
     * @return
     */
    private Map<String,String> getCollectionColumnMap(Method method) {
        Map<String,String> columns = new LinkedHashMap<>();
        // Get the return type
        // This uses a trick to extract what the arguments are of the writeMethod of the field.
        // In this way, we can deduce what type needs to be written at runtime.
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        // There could be more than one argument, so step through them
        for (Type genericParameterType : genericParameterTypes) {
            // If the argument is a parameter, then do something - this should always be true if the parameter is a collection
            if (genericParameterType instanceof ParameterizedType) {
                // Get the actual type arguments of the parameter
                Type[] parameters = ((ParameterizedType)genericParameterType).getActualTypeArguments();
                //parameters[0] contains java.lang.String for method like "method(List<String> value)"
                // Run through them one by one and create a SQL string
                int index = 0;
                for (Type type : parameters) {
                    // This is a request for column names.
                    String setMapping = MYSQL_MAPPING.get(type.getTypeName());
                    // This column name format is typeName_# where # is a number incremented from 0
                    columns.put("`" + type.getTypeName() + "_" + index + "`", setMapping != null ? setMapping : STRING_MAP);
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
        sb.append(getColumns(false));
        sb.append(" FROM ");
        sb.append("`");
        sb.append(dataObject.getCanonicalName());
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
        sb.append(dataObject.getCanonicalName());
        sb.append("`");
        sb.append("(");
        sb.append(getColumns(false));
        sb.append(")");
        sb.append(" VALUES (");
        sb.append(getColumns(true));
        sb.append(")");

        return sb.toString();
    }

    @Override
    protected String createDeleteQuery() {
        return "DELETE FROM [table_name] WHERE uniqueId = ?";
    }

    /**
     * Inserts a <T> into the corresponding database-table
     *
     * @param instance <T> that should be inserted into the corresponding database-table. Must extend DataObject.
     
     
     
     
     
     
     
     
     */
    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#insertObject(java.lang.Object)
     */
    @Override
    public void saveObject(T instance) throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException, NoSuchMethodException {

        // insertQuery is created in super from the createInsertQuery() method
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            // Get the uniqueId. As each class extends DataObject, it must have this method in it.
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", dataObject);
            Method getUniqueId = propertyDescriptor.getReadMethod();
            final String uniqueId = (String) getUniqueId.invoke(instance);
            if (uniqueId.isEmpty()) {
                throw new SQLException("uniqueId is blank");
            }
            // Create the insertion
            int i = 0;
            // Run through the fields in the class using introspection
            for (Field field : dataObject.getDeclaredFields()) {
                // Get the field's property descriptor
                propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
                // Get the read method for this field
                Method method = propertyDescriptor.getReadMethod();
                //sql += "`" + field.getName() + "` " + mapping + ",";
                // Invoke the read method to obtain the value from the class - this is the value we need to store in the database
                Object value = method.invoke(instance);
                // Adapter Notation
                Adapter adapterNotation = field.getAnnotation(Adapter.class);
                if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                    // A conversion adapter has been defined
                    value = ((AdapterInterface<?,?>)adapterNotation.value().newInstance()).deserialize(value);
                }
                // Create set and map table inserts if this is a Collection
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // Collection
                    // The table is cleared for this uniqueId every time the data is stored
                    StringBuilder clearTableSql = new StringBuilder();
                    clearTableSql.append("DELETE FROM  `");
                    clearTableSql.append(dataObject.getCanonicalName());
                    clearTableSql.append(".");
                    clearTableSql.append(field.getName());
                    clearTableSql.append("` WHERE uniqueId = ?");
                    try (PreparedStatement collStatement = connection.prepareStatement(clearTableSql.toString())) {
                        collStatement.setString(1, uniqueId);
                        collStatement.execute();
                    }
                    // Insert into the table
                    StringBuilder setSql = new StringBuilder();
                    setSql.append("INSERT INTO `");
                    setSql.append(dataObject.getCanonicalName());
                    setSql.append(".");
                    setSql.append(field.getName());
                    setSql.append("` (uniqueId, ");
                    // Get the columns we are going to insert, just the names of them
                    setSql.append(getCollectionColumnString(propertyDescriptor.getWriteMethod(), false, false));
                    setSql.append(") ");
                    // Get all the ?'s for the columns
                    setSql.append("VALUES (?,");
                    setSql.append(getCollectionColumnString(propertyDescriptor.getWriteMethod(), true, false));
                    setSql.append(")");
                    // Prepare the statement
                    try (PreparedStatement collStatement = connection.prepareStatement(setSql.toString())) {
                        // Set the uniqueId
                        collStatement.setString(1, uniqueId);
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
                                collStatement.setObject(2, setValue);
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
                                // Get the value and serialize it
                                Object mapValue = serialize(en.getValue(), en.getValue().getClass());
                                // Write the objects into prepared statement
                                collStatement.setObject(2, key);
                                collStatement.setObject(3, mapValue);
                                // Write to database
                                collStatement.execute();
                            }
                        }
                        // Set value for the main insert. For collections, this is just a dummy value because the real values are in the
                        // additional table.
                        value = true;
                    }
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
            value = value.toString();
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
     
     
     
     
     
     
     
     
     */
    @Override
    public List<T> loadObjects() throws SQLException,
    SecurityException, IllegalArgumentException,
    InstantiationException, IllegalAccessException,
    IntrospectionException, InvocationTargetException, ClassNotFoundException {

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(selectQuery)) {
            return createObjects(resultSet);
        } 
    }


    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#selectObject(java.lang.String)
     */
    @Override
    public T loadObject(String uniqueId) throws InstantiationException,
    IllegalAccessException, IllegalArgumentException,
    InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException {

        // Build the select query
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append(getColumns(false));
        query.append(" FROM `");
        query.append(dataObject.getCanonicalName());
        query.append("` WHERE uniqueId = ? LIMIT 1");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            preparedStatement.setString(1, uniqueId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // If there is a result, we only want/need the first one
                List<T> result = createObjects(resultSet);
                if (!result.isEmpty()) {
                    return result.get(0);
                }
            }
        }
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
     
     
     
     
     
     
     
     
     */
    @SuppressWarnings("unchecked")
    private List<T> createObjects(ResultSet resultSet)
            throws SecurityException, IllegalArgumentException,
            SQLException, InstantiationException,
            IllegalAccessException, IntrospectionException,
            InvocationTargetException, ClassNotFoundException {

        List<T> list = new ArrayList<>();
        // The database can return multiple results in one go, e.g., all the islands in the database
        // Run through them one by one
        while (resultSet.next()) {
            // Create a new instance of this type
            T instance = dataObject.newInstance();
            // Get the unique ID from the results
            String uniqueId = resultSet.getString("uniqueId");
            if (uniqueId == null) {
                throw new SQLException("No unique ID in the results!");
            }
            // Use introspection to run through all the fields in this type class
            for (Field field : dataObject.getDeclaredFields()) {
                /* We assume the table-column-names exactly match the variable-names of T */
                Object value = resultSet.getObject(field.getName());
                // Get the property descriptor of this type
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
                // Get the write method for this field, because we are going to use it to write the value
                // once we get the value from the database
                Method method = propertyDescriptor.getWriteMethod();
                // If the type is a Collection, then we need to deal with set and map tables
                if (Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType())
                        || Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Collection
                    // value is just of type boolean right now
                    StringBuilder setSql = new StringBuilder();
                    setSql.append("SELECT ");
                    // Get the columns, just the names of them, no ?'s or types
                    setSql.append(getCollectionColumnString(method, false, false));
                    setSql.append(" ");
                    setSql.append("FROM `");
                    setSql.append(dataObject.getCanonicalName());
                    setSql.append(".");
                    setSql.append(field.getName());
                    setSql.append("` ");
                    // We will need to fill in the ? later with the unique id of the class from the database
                    setSql.append("WHERE uniqueId = ?");
                    // Prepare the statement
                    try (PreparedStatement collStatement = connection.prepareStatement(setSql.toString())) {
                        // Set the unique ID
                        collStatement.setObject(1, uniqueId);
                        try (ResultSet collectionResultSet = collStatement.executeQuery()) {

                            //plugin.getLogger().info("DEBUG: collectionResultSet = " + collectionResultSet.toString());
                            // Do single dimension types (set and list)
                            if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                                // Loop through the collection resultset
                                // Note that we have no idea what type this is
                                List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                                // collectionTypes should be only 1 long
                                Type setType = collectionTypes.get(0);
                                value = new HashSet<>();
                                while (collectionResultSet.next()) {
                                    ((Set<Object>) value).add(deserialize(collectionResultSet.getObject(1),Class.forName(setType.getTypeName())));
                                }
                            } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                                // Loop through the collection resultset
                                // Note that we have no idea what type this is
                                List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                                // collectionTypes should be only 1 long
                                Type setType = collectionTypes.get(0);
                                value = new ArrayList<>();
                                //plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                                while (collectionResultSet.next()) {
                                    // Add to the list
                                    ((List<Object>) value).add(deserialize(collectionResultSet.getObject(1),Class.forName(setType.getTypeName())));
                                }
                            } else if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType()) ||
                                    HashMap.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                                // Loop through the collection resultset
                                // Note that we have no idea what type this is
                                List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                                // collectionTypes should be 2 long
                                Type keyType = collectionTypes.get(0);
                                Type valueType = collectionTypes.get(1);
                                value = new HashMap<>();
                                while (collectionResultSet.next()) {
                                    // Work through the columns
                                    // Key
                                    Object key = deserialize(collectionResultSet.getObject(1),Class.forName(keyType.getTypeName()));
                                    Object mapValue = deserialize(collectionResultSet.getObject(2),Class.forName(valueType.getTypeName()));
                                    ((Map<Object,Object>) value).put(key,mapValue);
                                }
                            } else {
                                // Set value for the main insert. For collections, this is just a dummy value because the real values are in the
                                // additional table.
                                value = true;
                            }
                        }
                    }
                } else {
                    value = deserialize(value, propertyDescriptor.getPropertyType());
                }
                // Adapter
                // Check if there is an annotation on the field
                Adapter adapterNotation = field.getAnnotation(Adapter.class);
                if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                    // A conversion adapter has been defined
                    value = ((AdapterInterface<?,?>)adapterNotation.value().newInstance()).serialize(value);
                }
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
        if (Enum.class.isAssignableFrom(clazz)) {
            //Custom enums are a child of the Enum class.
            // Find out the value
            try {
                Class<Enum> enumClass = (Class<Enum>)clazz;
                value = Enum.valueOf(enumClass, (String)value);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not deserialize enum! " + e.getMessage());
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
        // Try to connect to the database
        try (Connection conn = databaseConnecter.createConnection()){
            // Get the uniqueId. As each class extends DataObject, it must have this method in it.
            Method getUniqueId = dataObject.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            //plugin.getLogger().info("DEBUG: Unique Id = " + uniqueId);
            if (uniqueId.isEmpty()) {
                throw new SQLException("uniqueId is blank");
            }
            // Delete from the main table
            // First substitution is the table name
            // deleteQuery is created in super from the createInsertQuery() method
            try (PreparedStatement preparedStatement = conn.prepareStatement(deleteQuery.replace("[table_name]", "`" + dataObject.getCanonicalName() + "`"))) {
                // Second is the unique ID
                preparedStatement.setString(1, uniqueId);
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            }

            // Delete from any sub tables created from the object
            // Run through the fields in the class using introspection
            for (Field field : dataObject.getDeclaredFields()) {
                // Get the field's property descriptor
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
                // Delete Collection tables
                if (propertyDescriptor.getPropertyType().equals(Set.class) ||
                        propertyDescriptor.getPropertyType().equals(Map.class) ||
                        propertyDescriptor.getPropertyType().equals(HashMap.class) ||
                        propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    // First substitution is the table name
                    try (PreparedStatement preparedStatement2 = conn.prepareStatement(deleteQuery.replace("[table_name]", "`" + dataObject.getCanonicalName() + "." + field.getName() + "`"))) {
                        // Second is the unique ID
                        preparedStatement2.setString(1, uniqueId);
                        preparedStatement2.addBatch();
                        // Execute
                        preparedStatement2.executeBatch();
                    }
                }
            }
        } 
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#objectExists(java.lang.String)
     */
    @Override
    public boolean objectExists(String key) {
        // Create the query to see if this key exists
        StringBuilder query = new StringBuilder();
        query.append("SELECT  IF ( EXISTS( SELECT * FROM `");
        query.append(dataObject.getCanonicalName());
        query.append("` WHERE `uniqueId` = ?), 1, 0)");

        try (Connection conn = databaseConnecter.createConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(query.toString())) {
            preparedStatement.setString(1, key);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not check if key exists in database! " + key + " " + e.getMessage());
        }
        return false;
    }

    @Override
    public void saveSettings(T instance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        // This method should not be used because configs are not stored in MySQL

    }

    @Override
    public T loadSettings(String uniqueId, T dbConfig) throws InstantiationException, IllegalAccessException,
    IllegalArgumentException, InvocationTargetException, ClassNotFoundException, IntrospectionException {
        // This method should not be used because configs are not stored in MySQL
        return null;
    }

}
