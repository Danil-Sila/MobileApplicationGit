package com.example.mobileapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper {
    private static final String DB = "NewsDB.db";  //название БД
    private static final int version = 1;   //версия БД
    //название таблицы
    public static String table = "news";
    //названия столбцов
    public static String col_id = "_id";
    public static String col_title = "title";
    public static String col_img = "img";
    public static String col_news_date = "news_date";
    public static String col_news_date_uts = "news_date_uts";
    public static String col_annotation = "annotation";
    public static String col_mobile_url = "mobile_url";
    //столбец для скрытия новостей
    public static String col_visible = "visible";

    //создание таблицы с новостями
    private static  final String DB_CREATE_TABLE =
            "CREATE TABLE " + table + " (" +
                    col_id + " INTEGER PRIMARY KEY, " +
                    col_title + " TEXT, " +
                    col_img + " TEXT, " +
                    col_news_date + " TEXT, " +
                    col_news_date_uts + " TEXT, " +
                    col_annotation + " TEXT, " +
                    col_mobile_url + " TEXT, " +
                    col_visible + " INTEGER);";

    private final Context mCtx;
    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    public DataBaseHelper(Context ctx) {
        mCtx = ctx;
    }

    // открыть подключение
    public void open() {
        mDBHelper = new DBHelper(mCtx, DB, null, version);
        mDB = mDBHelper.getWritableDatabase();
    }

    // закрыть подключение
    public void close() {
        if (mDBHelper!=null) mDBHelper.close();
    }

    //добавить или обновить записи в списке новостей
    public void AddOrUpdData (int id, String title, String img, String news_date, String news_date_uts, String annotation, String mobile_url, Integer visible){
        ContentValues cvAdd = new ContentValues();
        ContentValues cvUpd = new ContentValues();
        Cursor c = mDB.rawQuery("SELECT * FROM "+table,null,null);
        int f = 0;
        if (c.moveToFirst()){
            do{
                if (c.getInt(0) == id){
                    f = 1;
                    break;
                }
            }while(c.moveToNext());
        }
        if (f == 0){
            cvAdd.put(col_id, id);
            cvAdd.put(col_title, title);
            cvAdd.put(col_img, img);
            cvAdd.put(col_news_date, news_date);
            cvAdd.put(col_news_date_uts, news_date_uts);
            cvAdd.put(col_annotation, annotation);
            cvAdd.put(col_mobile_url, mobile_url);
            cvAdd.put(col_visible, visible);
            mDB.insert(table, null,cvAdd);
        }else {
            cvUpd.put(col_id, id);
            cvUpd.put(col_title, title);
            cvUpd.put(col_img, img);
            cvUpd.put(col_news_date, news_date);
            cvUpd.put(col_news_date_uts, news_date_uts);
            cvUpd.put(col_annotation, annotation);
            cvUpd.put(col_mobile_url, mobile_url);
            mDB.update(table,cvUpd,col_id + "=" + id, null);
        }
    }

    //отчистить БД
    public void delData(){
        mDB.delete(table,null,null);
    }

    //получить записи из таблицы
    public Cursor getData(){
        Cursor c;
        String select = "SELECT *FROM " + table +
                " WHERE " + col_visible + "= 0"+
                " ORDER BY " + col_id + " desc";
        c = mDB.rawQuery(select,null,null);
        return c;
    }

    public Cursor getAllData(){
        return mDB.query(table,null,null,null,null,null,null);
    }

    //фильтрация/фильтрация по новосям
    public Cursor getDataFind(CharSequence constraint){
        String select = "SELECT * FROM " +table +
                " WHERE "+ col_visible + "= 0"+
                " AND ("+ col_title + " like "+"'%" + constraint.toString() + "%'" +
                " OR " + col_annotation + " like " + "'%" + constraint.toString() + "%')" +
                " ORDER BY "+col_id+" desc";
        return mDB.rawQuery(select,null,null);
    }


    //получение url по ID
    public String getMobileUrl(long id){
        Cursor c;
        String select = "SELECT "+col_mobile_url+" FROM "+table+" WHERE "+col_id+" = "+id;
        c = mDB.rawQuery(select,null,null);
        c.moveToFirst();
        return c.getString(0);
    }

    //скрытие новости 0-не скрыта , 1-скрыта
    public void visibleItemList(long id){
        ContentValues cv = new ContentValues();
        cv.put(col_visible, 1);
        mDB.update(table, cv, col_id + "="+id, null);
    }

    //получение количества записей в списке
    public int countData(){
        Cursor c;
        int count = 0;
        c = getData();
        if (c.moveToFirst()){
            do {
                count++;
            }while (c.moveToNext());
        }
        return count;
    }

    public class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                        int version) {
            super(context, DB, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
