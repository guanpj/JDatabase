package com.me.guanpj.jdatabase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.me.guanpj.jdatabase.core.BaseDao;

import java.util.HashMap;

/**
 * Created by Jie on 2017/4/19.
 */

public class DatabaseManager {
    private Context mContext;
    private static DatabaseManager mInstance;
    private SQLiteOpenHelper mHelper;
    private SQLiteDatabase mDatabase;
    private HashMap<String, BaseDao> mCachedDaos;

    private DatabaseManager(Context context, SQLiteOpenHelper heler) {
        mContext = context;
        mHelper = heler;
        mDatabase = mHelper.getWritableDatabase();
        mCachedDaos = new HashMap<>();
    }

    public static void init(Context context, SQLiteOpenHelper helper) {
        if (mInstance == null) {
            mInstance = new DatabaseManager(context, helper);
        }
    }

    public static DatabaseManager getInstance() {
        return mInstance;
    }

    public <T> BaseDao<T> getDao(Class<T> clz){
        if(mCachedDaos.containsKey(clz.getSimpleName())) {
            return mCachedDaos.get(clz.getSimpleName());
        } else {
            BaseDao<T> baseDao = new BaseDao<>(mContext, clz, mDatabase);
            mCachedDaos.put(clz.getSimpleName(), baseDao);
            return baseDao;
        }
    }
}
