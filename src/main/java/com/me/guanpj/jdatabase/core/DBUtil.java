package com.me.guanpj.jdatabase.core;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.me.guanpj.jdatabase.annotation.Column;
import com.me.guanpj.jdatabase.annotation.Table;
import com.me.guanpj.jdatabase.utility.TextUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Jie on 2017/4/17.
 */

public class DBUtil {

    public static final String PK1 = "pk1";
    public static final String PK2 = "pk2";

    public static void createTable(SQLiteDatabase db, Class<?> clz) throws SQLiteException{
        ArrayList<String> statement = getCreateTableStatement(clz);
        if(TextUtil.isValidate(statement)){
            for (String s : statement) {
                db.execSQL(s);
            }
        }
    }

    public static void dropTable(SQLiteDatabase db, Class<?> clz) throws SQLiteException{
        db.execSQL(getDropTableStatement(clz));
    }

    private static ArrayList<String> getCreateTableStatement(Class<?> clz) {
        ArrayList<String> statement = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if(clz.isAnnotationPresent(Table.class)){
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                if(field.isAnnotationPresent(Column.class)){
                    if(field.getAnnotation(Column.class).type() == Column.ColumnType.TMANY){
                        statement.add("create table if not exists " + getAssosiarionTableName(clz, field.getName()) +
                                "(" + PK1 + " TEXT, " + PK2 + " TEXT)");
                    }
                    sb.append(getColumnStatement(field));
                    sb.append(",");
                }
            }
            if (sb.length() > 0) {
                sb.delete(sb.length() - 2, sb.length());
            }
            statement.add("create table if not exists " + getTableName(clz) + " (" + sb + " )");
            return statement;
        }
        return null;
    }

    private static String getDropTableStatement(Class<?> clz) {
        return "drop table if exists " + getTableName(clz);
    }

    private static String getColumnStatement(Field field) {
        Column column = field.getAnnotation(Column.class);
        String name = column.name();
        String columnType = null;
        Class<?> type = field.getType();
        if(TextUtil.isValidate(name)){
            name = "[" + name + "]";
        } else {
            name = "[" + field.getName() + "]";
        }
        if(type == String.class){
            columnType = " TEXT ";
        } else if(type == int.class || type == Integer.class){
            columnType = " integer ";
        } else {
            Column.ColumnType myType = column.type();
            if(myType == Column.ColumnType.SERIALIZABLE){
                columnType = " BLOB ";
            } else if(myType == Column.ColumnType.TONE){
                columnType = " TEXT ";
            } else if(myType == Column.ColumnType.TMANY){

            }
        }
        name += columnType;
        if(column.id()){
            name += " primary key";
        }
        return name;
    }

    public static String getTableName(Class<?> clz) {
        if(clz.isAnnotationPresent(Table.class)){
            String name = clz.getAnnotation(Table.class).name();
            if(TextUtil.isValidate(name)){
                return name;
            } else {
                return clz.getSimpleName().toLowerCase();
            }
        }
        throw new IllegalArgumentException("the class " + clz.getSimpleName() + " can't map to the table");
    }

    public static String getColumnName(Field field){
        Column column = field.getAnnotation(Column.class);
        String name = column.name();
        if(TextUtil.isValidate(name)){
            return name;
        }
        return field.getName();
    }

    public static String getIdColumnName(Class<?> clz){
        if(clz.isAnnotationPresent(Table.class)){
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                if(field.isAnnotationPresent(Column.class)){
                    Column column = field.getAnnotation(Column.class);
                    if(column.id()) {
                        String idName = column.name();
                        if (TextUtil.isValidate(idName)) {
                            return idName;
                        }
                        return field.getName().toLowerCase();
                    }
                }
            }
        }
        return null;
    }

    public static String getIdFieldName(Class<?> clz){
        if(clz.isAnnotationPresent(Table.class)){
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) {
                if(field.isAnnotationPresent(Column.class)){
                    Column column = field.getAnnotation(Column.class);
                    if(column.id()) {
                        return field.getName();
                    }
                }
            }
        }
        return null;
    }

    public static String getAssosiarionTableName(Class<?> clz, String association) {
        return getTableName(clz) + "_" + association;
    }

    public static ArrayList<Field> getForeignFields(Field[] mColumnFields) {
        ArrayList<Field> foreignFields = new ArrayList<>();
        for (Field field : mColumnFields) {
            if(field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if(column.type() == Column.ColumnType.TMANY){
                    foreignFields.add(field);
                }
            }
        }
        return foreignFields;
    }
}
