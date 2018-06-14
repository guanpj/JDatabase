package com.me.guanpj.jdatabase.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.me.guanpj.jdatabase.DatabaseManager;
import com.me.guanpj.jdatabase.annotation.Column;
import com.me.guanpj.jdatabase.annotation.Table;
import com.me.guanpj.jdatabase.utility.SerializeUtil;
import com.me.guanpj.jdatabase.utility.TextUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jie on 2017/4/19.
 */

public class BaseDao<T> {
    private Context mContext;
    private Class<T> mClz;
    private SQLiteDatabase mDatabase;
    private String mTableName;
    private Field[] mColumnFields;
    private String mIdFieldName;
    private String mIdColumnName;
    private Field mIdField;
    private ArrayList<Field> mForeignFields;

    public BaseDao(Context context, Class<T> clz, SQLiteDatabase db){
        mContext = context;
        mClz = clz;
        mDatabase = db;
        try {
            mColumnFields = mClz.getDeclaredFields();
            mTableName = DBUtil.getTableName(mClz);
            mIdFieldName = DBUtil.getIdFieldName(mClz);
            mIdColumnName = DBUtil.getIdColumnName(mClz);
            mForeignFields = DBUtil.getForeignFields(mColumnFields);
            mIdField = mClz.getDeclaredField(mIdFieldName);
            mIdField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void newOrUpdate(String tableName, ContentValues values) {
        mDatabase.replace(tableName, null, values);
    }

    public <T> void newOrUpdate(T t) {
        if (t.getClass().isAnnotationPresent(Table.class)) {
            ContentValues values = new ContentValues();
            try {
                for (Field field : mColumnFields) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        if (fieldType == String.class) {
                            Object value = field.get(t);
                            if (value != null) {
                                values.put(DBUtil.getColumnName(field), value.toString());
                            }
                        } else if (fieldType == int.class || fieldType == Integer.class) {
                            values.put(DBUtil.getColumnName(field), field.getInt(t));
                        } else {
                            Column column = field.getAnnotation(Column.class);
                            Column.ColumnType myType = column.type();
                            if (!TextUtil.isValidate(myType.name())) {
                                throw new IllegalArgumentException("you should set myType to the special column:" + t.getClass().getSimpleName() + "."
                                        + field.getName());
                            }
                            if (myType == Column.ColumnType.SERIALIZABLE) {
                                values.put(DBUtil.getColumnName(field), SerializeUtil.serialize(field.get(t)));
                            } else if (myType == Column.ColumnType.TONE) {
                                Object tone = field.get(t);
                                if(tone == null){
                                    continue;
                                }
                                if(column.autofresh()) {
                                    //newOrUpdate(tone);
                                    DatabaseManager.getInstance().getDao(tone.getClass()).newOrUpdate(tone);
                                }
                                if(tone.getClass().isAnnotationPresent(Table.class)) {
                                    String toneIdFieldName = DBUtil.getIdFieldName(tone.getClass());
                                    Field toneIdField = tone.getClass().getDeclaredField(toneIdFieldName);
                                    if(toneIdField != null) {
                                        toneIdField.setAccessible(true);
                                        values.put(DBUtil.getColumnName(field), toneIdField.get(tone).toString());
                                    }
                                }
                            } else if(myType == Column.ColumnType.TMANY) {
                                String idFieldValue = null;
                                if(mIdField != null){
                                    idFieldValue = mIdField.get(t).toString();
                                    mDatabase.delete(DBUtil.getAssociationTableName(t.getClass(), field.getName()), DBUtil.PK1 + "=?", new String[]{idFieldValue});
                                }
                                List<Object> tmany = (List<Object>) field.get(t);
                                if(tmany != null && tmany.size() > 0) {
                                    String assosiationTableName = DBUtil.getAssociationTableName(t.getClass(), field.getName());
                                    ContentValues assosiationValues = new ContentValues();
                                    for (Object object : tmany) {
                                        if(column.autofresh()) {
                                            //newOrUpdate(object);
                                            DatabaseManager.getInstance().getDao(object.getClass()).newOrUpdate(object);
                                        }
                                        assosiationValues.clear();
                                        assosiationValues.put(DBUtil.PK1, idFieldValue);
                                        String tmanyIdFieldName = DBUtil.getIdFieldName(object.getClass());
                                        Field tmanyIdField = object.getClass().getDeclaredField(tmanyIdFieldName);
                                        if(tmanyIdField != null) {
                                            tmanyIdField.setAccessible(true);
                                            assosiationValues.put(DBUtil.PK2, tmanyIdField.get(object).toString());
                                        }
                                        newOrUpdate(assosiationTableName, assosiationValues);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            newOrUpdate(mTableName, values);
        }
    }

    public <T> void delete(T t) {
        try {
            if (mIdField != null) {
                String idValue = mIdField.get(t).toString();
                delete(idValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String id) {
        try {
            delete(mTableName, mIdColumnName + "=?", new String[]{id});
            for (Field foreignField : mForeignFields) {
                delete(DBUtil.getAssociationTableName(mClz, foreignField.getName()), DBUtil.PK1 + "=?", new String[]{id});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String tableName, String where, String[] args) {
        mDatabase.delete(tableName, where, args);
    }

    public Cursor rawQuery(String tableName, String where, String[] args) {
        return mDatabase.rawQuery("select * from " + tableName + " where " + where, args);
    }

    public T queryById(String id) {
        Cursor cursor = rawQuery(mTableName, mIdColumnName + "=?", new String[]{id});
        T t = null;
        if (cursor.moveToNext()) {
            try {
                t = mClz.newInstance();
                for (Field field : mColumnFields) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        int columnIndex = cursor.getColumnIndex(DBUtil.getColumnName(field));
                        if (fieldType == String.class) {
                            field.set(t, cursor.getString(columnIndex));
                        } else if (fieldType == int.class || fieldType == Integer.class) {
                            field.setInt(t, cursor.getInt(columnIndex));
                        } else {
                            Column column = field.getAnnotation(Column.class);
                            Column.ColumnType myType = column.type();
                            if (myType == Column.ColumnType.SERIALIZABLE) {
                                field.set(t, SerializeUtil.deserialize(cursor.getBlob(columnIndex)));
                            } else if(myType == Column.ColumnType.TONE) {
                                String toneId = cursor.getString(columnIndex);
                                if(!TextUtil.isValidate(toneId)){
                                    continue;
                                }
                                Object tone = null;
                                if(column.autofresh()){
                                    //tone = queryById(tone.getClass(), toneId);
                                    tone = DatabaseManager.getInstance().getDao(field.getType()).queryById(id);
                                } else {
                                    if(fieldType.isAnnotationPresent(Table.class)){
                                        tone = fieldType.newInstance();
                                        String toneIdFieldName = DBUtil.getIdFieldName(tone.getClass());
                                        Field toneIdField = tone.getClass().getDeclaredField(toneIdFieldName);
                                        toneIdField.setAccessible(true);
                                        toneIdField.set(tone, toneId);
                                    }
                                }
                                field.set(t, tone);
                            } else if(myType == Column.ColumnType.TMANY) {
                                Class relatedClass = (Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                                Cursor assosiationCursor = mDatabase.rawQuery("select * from " + DBUtil.getAssociationTableName(t.getClass(), field.getName()) +
                                        " where " + DBUtil.PK1 + "=?", new String[]{id});
                                ArrayList<Object> list = new ArrayList<>();
                                while (assosiationCursor.moveToNext()) {
                                    String assosiationId = assosiationCursor.getString(assosiationCursor.getColumnIndex(DBUtil.PK2));
                                    Object relatedObject = null;
                                    if(column.autofresh()) {
                                        //relatedObject = queryById(relatedClass, assosiationId);
                                        relatedObject = DatabaseManager.getInstance().getDao(relatedClass).queryById(assosiationId);
                                    } else {
                                        if(relatedClass.isAnnotationPresent(Table.class)) {
                                            relatedObject = relatedClass.newInstance();
                                            String relatedObjectIdFieldName = DBUtil.getIdFieldName(relatedObject.getClass());
                                            Field relatedObjectIdField = relatedObject.getClass().getDeclaredField(relatedObjectIdFieldName);
                                            relatedObjectIdField.setAccessible(true);
                                            relatedObjectIdField.set(relatedObject, assosiationId);
                                        }
                                    }
                                    list.add(relatedObject);
                                }
                                if (!TextUtil.isValidate(list)) {
                                    continue;
                                }
                                field.set(t, list);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return t;
    }
}
