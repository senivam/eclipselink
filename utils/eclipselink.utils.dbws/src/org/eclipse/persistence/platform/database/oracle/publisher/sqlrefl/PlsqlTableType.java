package org.eclipse.persistence.platform.database.oracle.publisher.sqlrefl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import org.eclipse.persistence.platform.database.oracle.publisher.PublisherException;
import org.eclipse.persistence.platform.database.oracle.publisher.Util;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.ElemInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.PlsqlElemHelper;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.PlsqlElemInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.ViewCache;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.ALL_ARGUMENTS;

/**
 * Describe PL/SQL table type, including index-by tables
 */
@SuppressWarnings("unchecked")
public class PlsqlTableType extends SqlCollectionType {
    public PlsqlTableType(SqlName sqlName, int typeCode, ElemInfo elemInfo, SqlType elemType,
        int[] details, boolean generateMe, SqlType parentType, SqlReflector reflector) {
        super(sqlName, typeCode, generateMe, parentType, reflector);
        m_elemInfo = elemInfo;
        m_elementType = elemType;
        m_elemTypeLength = details[DETAILS_TYPE_LENGTH];
        m_elemTypePrecision = details[DETAILS_TYPE_PRECISION];
        m_elemTypeScale = details[DETAILS_TYPE_SCALE];
    }

    protected ElemInfo getElemInfo() {
        return m_elemInfo;
    }

    public static ElemInfo getElemInfo(String _schema, String _typeName, String packageName,
        String methodName, String methodNo, ViewCache viewCache) throws SQLException {

        String schema = Util.getSchema(_schema, _typeName);
        String typeName = Util.getType(_schema, _typeName);
        if (typeName.indexOf('.') >= 0) {
            typeName = typeName.substring(typeName.indexOf('.') + 1);
        }

        @SuppressWarnings("unused")
        Vector rowV = new Vector();
        Iterator iter;
        if (packageName != null && packageName.length() > 0) {
            iter = viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{Util.PACKAGE_NAME,
                Util.OBJECT_NAME, Util.OVERLOAD}, new Object[]{packageName, methodName, methodNo},
            // new String[0]);
                new String[]{"SEQUENCE"});
        }
        else { // For toplevel publishing
            iter = viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{}, new Object[]{},
                new String[0]);
        }
        PlsqlElemHelper[] info = PlsqlElemHelper.getPlsqlElemHelper(iter);

        // element type is either right before or after the table type
        int i0 = 0;
        int i1 = -1;
        for (i0 = 1; i0 < info.length - 1; i0++) {
            if (schema != null && schema.equals(info[i0].typeOwner) && typeName != null
                && typeName.equals(info[i0].typeSubname)) {
                if (info[i0 - 1].sequence == (info[i0].sequence + 1)
                    && info[i0 - 1].dataLevel == (info[i0].dataLevel + 1)
                    && info[i0 - 1].objectName.equals(info[i0].objectName)
                    && (info[i0 - 1].overload == info[i0].overload || (info[i0 - 1].overload != null && info[i0 - 1].overload
                        .equals(info[i0].overload)))) {
                    i1 = i0 - 1;
                    break;
                }
                else if (info[i0 + 1].sequence == (info[i0].sequence + 1)
                    && info[i0 + 1].dataLevel == (info[i0].dataLevel + 1)
                    && info[i0 + 1].objectName.equals(info[i0].objectName)
                    && (info[i0 - 1].overload == info[i0].overload || (info[i0 - 1].overload != null && info[i0 - 1].overload
                        .equals(info[i0].overload)))) {
                    i1 = i0 + 1;
                    break;
                }
            }
        }
        if (i1 == -1) {
            i0 = 0;
            if (schema != null && schema.equals(info[i0].typeOwner) && typeName != null
                && typeName.equals(info[i0].typeSubname)) {
                i1 = 1;
            }
            i0 = info.length - 1;
            if (schema != null && schema.equals(info[i0].typeOwner) && typeName != null
                && typeName.equals(info[i0].typeSubname)) {
                i1 = i0 - 1;
            }
        }

        if (i1 >= info.length || i1 < 0) {
            throw new java.sql.SQLException("Error reflecting element type for collection type "
                + typeName);
        }

        PlsqlElemInfo peti = new PlsqlElemInfo(info[i1]);
        return peti;
    }

    public Type getComponentType() {
        return m_elementType;
    }

    public static final int NUMBER_OF_DETAILS = 3;
    public static final int DETAILS_TYPE_LENGTH = 0;
    public static final int DETAILS_TYPE_PRECISION = 1;
    public static final int DETAILS_TYPE_SCALE = 2;

    static Type getComponentType(ElemInfo elemInfo, SqlReflector reflector, SqlType parentType,
        int[] details) throws SQLException, PublisherException {
        SqlType result = null;
        try {
            PlsqlElemInfo info = null;
            info = (PlsqlElemInfo)elemInfo;
            String elemTypeName = info.elemTypeName;
            String elemSchemaName = info.elemTypeOwner;
            String elemTypeMod = info.elemTypeMod;
            String elemPackageName = info.elemTypePackageName;
            String elemMethodName = info.elemTypeMethodName;
            String elemMethodNo = info.elemTypeMethodNo;
            int elemTypeSequence = info.elemTypeSequence;
            details[0] = info.elemTypeLength;
            details[1] = info.elemTypePrecision;
            details[2] = info.elemTypeScale;

            result = reflector.addPlsqlDBType(elemSchemaName, elemTypeName, null, elemTypeMod,
                false, elemPackageName, elemMethodName, elemMethodNo, elemTypeSequence, parentType); // this
        }
        catch (SQLException e) {
            System.err.println(e.getMessage());
            if (result == null) {
                result = SqlReflector.UNKNOWN_TYPE;
            }
        }
        return result;
    }

    /*
     * Figure out the index type for PL/SQL INDEXBY TABLE
     */
    private static String getIndexType(SqlName sqlName, ViewCache viewCache) throws SQLException {
        return "NUMBER";
        /*
         * Don't think I know how to figure out the index type String indexType = "NUMBER"; String
         * schema = sqlName.getSchemaName(); String typeName = sqlName.getTypeName(); if
         * (typeName.indexOf('.')>=0) { typeName = typeName.substring(typeName.indexOf('.') +1); }
         * 
         * Vector rowV = new Vector(); Iterator iter; String[] keys = new String[0]; // toplevel
         * publishing Object[] values = new Object[0]; // toplevel publishing if (packageName!=null
         * && packageName.length()>0) { keys = new String[]{Util.PACKAGE_NAME}; values = new
         * Object[]{packageName}; } iter = viewCache.getRows(m_options.getPlsqlView(), keys,
         * values); String objectName= null; int position = -1; int sequence = -1; int dataLevel =
         * -1; while (iter.hasNext() && objectName==null) { AllArguments row = (AllArguments)
         * iter.next(); if (objectName==null && schema!=null && schema.equals(row.TYPE_OWNER) &&
         * typeName!=null && typeName.equals(row.TYPE_SUBNAME)) { objectName = row.OBJECT_NAME;
         * position = row.POSITION; sequence = row.SEQUENCE; dataLevel = row.DATA_LEVEL; } }
         * sequence++; dataLevel++; iter = viewCache.getRows(m_options.getPlsqlView(), keys,
         * values); while (iter.hasNext()) { AllArguments row = (AllArguments) iter.next(); if
         * (position == row.POSITION && sequence==row.SEQUENCE && dataLevel==row.DATA_LEVEL &&
         * objectName.equals(row.OBJECT_NAME)) { indexType = row.PLS_TYPE; if (indexType==null)
         * indexType = row.DATA_TYPE; if (indexType!=null) break; } } return indexType;
         */
    }

    /**
     * Create a PL/SQL table type. If -plsqlindexbytable is set and whenever possible, the method
     * will return predefined scalar index-by table types, which will be mapped into Java arrays.
     * 
     * @param sqlName
     * @param typeCode
     * @param elemInfo
     * @param elemType
     * @param details
     * @param generateMe
     * @param parentType
     * @param isGrandparent
     * @param options
     * @param reflector
     * @return either a newly defined PL/SQL Table type or a predefined scalar PL/SQL index-by table
     *         type
     * @throws SQLException
     * @throws PublisherException
     */

    public static SqlType newInstance(SqlName sqlName, int typeCode, ElemInfo elemInfo,
        SqlType elemType, int[] details, boolean generateMe, SqlType parentType,
        boolean isGrandparent, SqlReflector reflector) throws java.sql.SQLException,
        org.eclipse.persistence.platform.database.oracle.publisher.PublisherException {
        /**
         * Identify toplevel scalar PL/SQL indexby table, to be mapped to predefined PL/SQL indexby
         * table type. Under the following situations, predefined scalar index-by table types cannot
         * be used: - in pre 10.2 compatible mode - when the PL/SQL table is used as a field of a
         * record (isGrandparent)
         */
        if (parentType != null
            && (!isGrandparent && parentType != null && parentType instanceof SqlPackageType)
            && typeCode == OracleTypes.PLSQL_INDEX_TABLE) {
            @SuppressWarnings("unused")
            String indexType = getIndexType(sqlName, reflector.getViewCache());
            /*
             * if ("NUMBER".equals(indexType)) { Map map = new JavaMap(elemType, reflector); SqlType
             * plsqlTableType = map.getPlsqlTableType(elemType); if (plsqlTableType != null) {
             * return plsqlTableType; } }
             */
        }
        // mapped to user-defined SQL table type
        return new PlsqlTableType(sqlName, typeCode, elemInfo, elemType, details, generateMe,
            parentType, reflector);
    }

    private ElemInfo m_elemInfo;
}
