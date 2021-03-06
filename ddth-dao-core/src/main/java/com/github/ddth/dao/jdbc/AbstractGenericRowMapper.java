package com.github.ddth.dao.jdbc;

import com.github.ddth.dao.BaseBo;
import com.github.ddth.dao.utils.BoUtils;
import com.github.ddth.dao.utils.DaoException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

/**
 * Abstract generic implementation of {@link IRowMapper}.
 *
 * @param <T>
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.8.0
 */
public abstract class AbstractGenericRowMapper<T> implements IRowMapper<T> {

    private Cache<String, String> cacheSQLs = CacheBuilder.newBuilder().build();

    private String strAllColumns = StringUtils.join(getAllColumns(), ",");
    private String strPkColumns = StringUtils.join(getPrimaryKeyColumns(), ",");
    private String strWherePkClause = StringUtils
            .join(Arrays.asList(getPrimaryKeyColumns()).stream().map(col -> col + "=?").toArray(String[]::new),
                    " AND ");
    private String strUpdateSetClause = StringUtils
            .join(Arrays.asList(getUpdateColumns()).stream().map(col -> col + "=?").toArray(String[]::new), ",");

    /**
     * Generate SELECT statement to select a BO.
     *
     * <p>
     * The generated SQL will look like this
     * {@code SELECT all-columns FROM table WHERE pk-1=? AND pk-2=?...}
     * </p>
     *
     * @param tableName
     * @return
     * @since 0.8.5
     */
    public String generateSqlSelect(String tableName) {
        try {
            return cacheSQLs.get("SELECT:" + tableName, () -> MessageFormat
                    .format("SELECT {2} FROM {0} WHERE {1}", tableName, strWherePkClause, strAllColumns));
        } catch (ExecutionException e) {
            throw new DaoException(e.getCause());
        }
    }

    /**
     * Generate SELECT statement to SELECT all BOs, ordered by primary keys.
     *
     * <p>
     * The generated SQL will look like this
     * {@code SELECT all-columns FROM table ORDER BY pk-1, pk-2...}
     * </p>
     *
     * @param tableName
     * @return
     * @since 0.8.5
     */
    public String generateSqlSelectAll(String tableName) {
        try {
            return cacheSQLs.get("SELECT-ALL:" + tableName, () -> MessageFormat
                    .format("SELECT {2} FROM {0} ORDER BY {1}", tableName, strPkColumns, strAllColumns));
        } catch (ExecutionException e) {
            throw new DaoException(e.getCause());
        }
    }

    /**
     * Generate INSERT statement to insert a BO.
     *
     * <p>
     * The generated SQL will look like this
     * {@code INSERT INTO table (all-columns) VALUES (?,?,...)}
     * </p>
     *
     * @param tableName
     * @return
     * @since 0.8.5
     */
    public String generateSqlInsert(String tableName) {
        try {
            return cacheSQLs.get("INSERT:" + tableName, () -> MessageFormat
                    .format("INSERT INTO {0} ({1}) VALUES ({2})", tableName, strAllColumns,
                            StringUtils.repeat("?", ",", getAllColumns().length)));
        } catch (ExecutionException e) {
            throw new DaoException(e.getCause());
        }
    }

    /**
     * Generate DELETE statement to delete an existing BO.
     *
     * <p>
     * The generated SQL will look like this {@code DELETE FROM table WHERE pk-1=? AND pk-2=?...}
     * </p>
     *
     * @param tableName
     * @return
     * @since 0.8.5
     */
    public String generateSqlDelete(String tableName) {
        try {
            return cacheSQLs.get("DELETE:" + tableName,
                    () -> MessageFormat.format("DELETE FROM {0} WHERE {1}", tableName, strWherePkClause));
        } catch (ExecutionException e) {
            throw new DaoException(e.getCause());
        }
    }

    /**
     * Generate UPDATE statement to update an existing BO.
     *
     * <p>
     * The generated SQL will look like this
     * {@code UPDATE table SET col1=?, col2=?...WHERE pk-1=? AND pk-2=?...}
     * </p>
     *
     * @param tableName
     * @return
     * @since 0.8.5
     */
    public String generateSqlUpdate(String tableName) {
        try {
            return cacheSQLs.get("UPDATE:" + tableName, () -> MessageFormat
                    .format("UPDATE {0} SET {2} WHERE {1}", tableName, strWherePkClause, strUpdateSetClause));
        } catch (ExecutionException e) {
            throw new DaoException(e.getCause());
        }
    }

    /**
     * Action to extract table column data.
     */
    protected interface ColumnDataExtractor<R> {
        R perform(String colName) throws SQLException;
    }

    /**
     * Db table column -> BO attribute mapping.
     */
    protected static class ColAttrMapping {
        public final String colName;
        public final String attrName;
        public final Class<?> attrClass;

        public ColAttrMapping(String colName, String attrName, Class<?> attrClass) {
            this.colName = colName;
            this.attrName = attrName;
            this.attrClass = attrClass;
            setterName = "set" + StringUtils.capitalize(attrName);
            getterName = "get" + StringUtils.capitalize(attrName);
        }

        private final String setterName;
        private final String getterName;

        /**
         * Extract data from DB table column and populate to BO attribute.
         *
         * @param bo
         * @param func
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws InvocationTargetException
         * @throws NoSuchMethodException
         * @throws SecurityException
         * @throws SQLException
         */
        public void extractColumData(Object bo, ColumnDataExtractor<?> func)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                NoSuchMethodException, SecurityException, SQLException {
            Method method = getSetterMethod(bo);
            method.invoke(bo, func.perform(colName));
        }

        /**
         * @param bo
         * @return
         * @throws NoSuchMethodException
         * @throws SecurityException
         * @since 0.8.0.2
         */
        protected Method getSetterMethod(Object bo) throws NoSuchMethodException, SecurityException {
            Class<?>[] primitiveAndWrapperClasses = { boolean.class, Boolean.class, byte.class, Byte.class, short.class,
                    Short.class, int.class, Integer.class, long.class, Long.class, float.class, Float.class,
                    double.class, Double.class, char.class, Character.class };
            for (int i = 0, n = primitiveAndWrapperClasses.length / 2; i < n; i++) {
                if (attrClass == primitiveAndWrapperClasses[i * 2] || attrClass == primitiveAndWrapperClasses[i * 2
                        + 1]) {
                    return getSetter(bo, setterName, primitiveAndWrapperClasses[i * 2],
                            primitiveAndWrapperClasses[i * 2 + 1]);
                }
            }
            return bo.getClass().getMethod(setterName, attrClass);
        }

        private static Method getSetter(Object obj, String methodName, Class<?> primitiveClass, Class<?> wrapperClass)
                throws NoSuchMethodException, SecurityException {
            try {
                return obj.getClass().getMethod(methodName, primitiveClass);
            } catch (NoSuchMethodException | SecurityException _e) {
                return obj.getClass().getMethod(methodName, wrapperClass);
            }
        }

        /**
         * Extract attribute value from a BO.
         *
         * @param bo
         * @return
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws InvocationTargetException
         * @throws NoSuchMethodException
         * @throws SecurityException
         */
        public Object extractAttrValue(Object bo)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                NoSuchMethodException, SecurityException {
            Method method = bo.getClass().getMethod(getterName);
            return method.invoke(bo);
        }

        private String cachedToString;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (cachedToString == null) {
                ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
                tsb.append("column", colName).append("attr", attrName).append("attrClass", attrClass);
                cachedToString = tsb.toString();
            }
            return cachedToString;
        }
    }

    private Class<T> typeClass;

    @SuppressWarnings("unchecked")
    public AbstractGenericRowMapper() {
        Class<?> clazz = getClass();
        Type type = clazz.getGenericSuperclass();
        while (type != null) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type type1 = parameterizedType.getActualTypeArguments()[0];
                if (type1 instanceof ParameterizedType) {
                    // for the case MyRowMapper extends AbstractGenericRowMapper<AGeneticClass<T>>
                    this.typeClass = (Class<T>) ((ParameterizedType) type1).getRawType();
                } else {
                    // for the case MyRowMapper extends AbstractGenericRowMapper<AClass>
                    this.typeClass = (Class<T>) type1;
                }
                break;
            } else {
                // current class does not have parameter(s), but its super might
                // e.g. MyChildRowMapper extends MyRowMapper extends AbstractGenericRowMapper<T>
                clazz = clazz.getSuperclass();
                type = clazz != null ? clazz.getGenericSuperclass() : null;
            }
        }
    }

    /**
     * @return
     * @since 0.8.0.4
     */
    protected Class<T> getTypeClass() {
        return typeClass;
    }

    /**
     * Class loader used by {@link #mapRow(ResultSet, int)}. Sub-class may override this method to
     * supply its custom class loader.
     *
     * @return
     * @since 0.8.0.4
     */
    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            Map<String, ColAttrMapping> colAttrMappings = getColumnAttributeMappings();
            T bo = BoUtils.createObject(typeClass.getName(), getClassLoader(), typeClass);
            for (Entry<String, ColAttrMapping> entry : colAttrMappings.entrySet()) {
                ColAttrMapping mapping = entry.getValue();
                if (mapping.attrClass == boolean.class || mapping.attrClass == Boolean.class) {
                    mapping.extractColumData(bo, rs::getBoolean);
                } else if (mapping.attrClass == char.class || mapping.attrClass == Character.class
                        || mapping.attrClass == String.class) {
                    mapping.extractColumData(bo, rs::getString);
                } else if (mapping.attrClass == byte.class || mapping.attrClass == Byte.class) {
                    mapping.extractColumData(bo, rs::getByte);
                } else if (mapping.attrClass == short.class || mapping.attrClass == Short.class) {
                    mapping.extractColumData(bo, rs::getShort);
                } else if (mapping.attrClass == int.class || mapping.attrClass == Integer.class) {
                    mapping.extractColumData(bo, rs::getInt);
                } else if (mapping.attrClass == long.class || mapping.attrClass == Long.class
                        || mapping.attrClass == BigInteger.class) {
                    mapping.extractColumData(bo, rs::getLong);
                } else if (mapping.attrClass == float.class || mapping.attrClass == Float.class) {
                    mapping.extractColumData(bo, rs::getFloat);
                } else if (mapping.attrClass == double.class || mapping.attrClass == Double.class) {
                    mapping.extractColumData(bo, rs::getDouble);
                } else if (mapping.attrClass == BigDecimal.class) {
                    mapping.extractColumData(bo, rs::getBigDecimal);
                } else if (mapping.attrClass == byte[].class) {
                    mapping.extractColumData(bo, rs::getBytes);
                } else if (mapping.attrClass == Blob.class) {
                    mapping.extractColumData(bo, rs::getBlob);
                } else if (mapping.attrClass == Clob.class) {
                    mapping.extractColumData(bo, rs::getClob);
                } else if (mapping.attrClass == NClob.class) {
                    mapping.extractColumData(bo, rs::getNClob);
                } else if (mapping.attrClass == Date.class || mapping.attrClass == Timestamp.class) {
                    mapping.extractColumData(bo, rs::getTimestamp);
                } else if (mapping.attrClass == java.sql.Date.class) {
                    mapping.extractColumData(bo, rs::getDate);
                } else if (mapping.attrClass == java.sql.Time.class) {
                    mapping.extractColumData(bo, rs::getTime);
                } else {
                    throw new IllegalArgumentException("Unsupported attribute class " + mapping.attrClass);
                }
            }
            if (bo instanceof BaseBo) {
                ((BaseBo) bo).markClean();
            }
            return bo;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Extract attribute values from a BO for corresponding DB table columns
     *
     * @param bo
     * @return
     */
    public Object[] valuesForColumns(T bo, String... columns) {
        Map<String, ColAttrMapping> columnAttributeMappings = getColumnAttributeMappings();
        Object[] result = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            ColAttrMapping colAttrMapping = columnAttributeMappings.get(columns[i]);
            try {
                result[i] = colAttrMapping != null ? colAttrMapping.extractAttrValue(bo) : null;
            } catch (Exception e) {
                throw e instanceof DaoException ? (DaoException) e : new DaoException(e);
            }
        }
        return result;
    }

    private String[] cachedAllColumns;

    /**
     * Get all DB table column names.
     *
     * @return
     */
    public String[] getAllColumns() {
        if (cachedAllColumns == null) {
            cachedAllColumns = new ArrayList<>(getColumnAttributeMappings().keySet())
                    .toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        }
        return cachedAllColumns;
    }

    /**
     * Get DB table column names used for inserting.
     *
     * @return
     */
    public String[] getInsertColumns() {
        return getAllColumns();
    }

    /**
     * Get name of checksum column.
     *
     * @return
     * @since 0.8.1
     */
    public String getChecksumColumn() {
        return null;
    }

    /**
     * Get primary-key column names.
     *
     * @return
     */
    public abstract String[] getPrimaryKeyColumns();

    /**
     * Get DB table column names used for updating.
     *
     * @return
     */
    public abstract String[] getUpdateColumns();

    /**
     * Get DB table column -> BO attribute mappings.
     *
     * @return mappings {@code column-name -> ColAttrMapping}
     */
    public abstract Map<String, ColAttrMapping> getColumnAttributeMappings();
}
