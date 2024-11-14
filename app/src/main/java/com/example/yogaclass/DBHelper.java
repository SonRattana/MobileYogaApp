package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "YogaClass.db";
    private static final int DATABASE_VERSION = 4;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE YogaClass (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "dayOfWeek TEXT, " +
                        "time TEXT, " +
                        "quantity INTEGER, " +
                        "duration INTEGER, " +
                        "price REAL, " +
                        "type TEXT, " +
                        "description TEXT, " +
                        "teacher TEXT)"
        );
        db.execSQL(
                "CREATE TABLE ClassInstance (" +
                        "id TEXT PRIMARY KEY, " +
                        "yogaClassId INTEGER, " +
                        "date TEXT, " +
                        "teacher TEXT, " +
                        "additionalComments TEXT, " +
                        "price REAL, " +
                        "FOREIGN KEY(yogaClassId) REFERENCES YogaClass(id))"
        );
        db.execSQL(
                "CREATE TABLE UsersRole (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, " +
                        "email TEXT UNIQUE, " +
                        "password TEXT, " +
                        "role TEXT)"
        );
        db.execSQL(
                "CREATE TABLE Users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, " +
                        "email TEXT UNIQUE, " +
                        "password TEXT, " +
                        "role TEXT)"
        );
        db.execSQL(
                "CREATE TABLE Categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT UNIQUE)"
        );

        // Tạo bảng Teachers để lưu trữ danh sách giáo viên
        db.execSQL("CREATE TABLE IF NOT EXISTS Teachers (name TEXT PRIMARY KEY)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            Log.d("DBHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS ClassInstance");
            db.execSQL("DROP TABLE IF EXISTS YogaClass");
            db.execSQL("DROP TABLE IF EXISTS UsersRole");
            db.execSQL("DROP TABLE IF EXISTS Teachers");
            onCreate(db);
        }
    }

    public long insertYogaClass(YogaClass yogaClass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dayOfWeek", yogaClass.getDayOfWeek());
        values.put("time", yogaClass.getTime());
        values.put("quantity", yogaClass.getQuantity());
        values.put("duration", yogaClass.getDuration());
        values.put("price", yogaClass.getPrice());
        values.put("type", yogaClass.getType());
        values.put("description", yogaClass.getDescription());

        return db.insert("YogaClass", null, values);
    }

    public boolean insertUserToUsers(String name, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);

        long result = db.insert("Users", null, values);
        return result != -1;
    }

    public boolean insertUser(String name, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);

        long result = db.insert("UsersRole", null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM UsersRole WHERE email = ? AND password = ?", new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM UsersRole WHERE email = ?", new String[]{email});
        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }
        cursor.close();
        return role;
    }

    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM UsersRole WHERE email = ?", new String[]{email});
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }

    public long insertClassInstance(String instanceId, String yogaClassId, String date, String teacher, String comments, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", instanceId);
        values.put("yogaClassId", yogaClassId);
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("additionalComments", comments);
        values.put("price", price);

        long result = db.insert("ClassInstance", null, values);
        db.close();
        return result;
    }

    public double getYogaClassPriceById(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double price = 0.0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT price FROM YogaClass WHERE id = ?", new String[]{yogaClassId});
            if (cursor.moveToFirst()) {
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return price;
    }

    public Cursor getClassInstancesByYogaClassId(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance WHERE yogaClassId = ?", new String[]{yogaClassId});
    }

    public Cursor getClassInstancesByTeacherName(String teacherName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance WHERE teacher = ?", new String[]{teacherName});
    }

    public Cursor getClassInstancesByCourse(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance WHERE yogaClassId = ?", new String[]{yogaClassId});
    }

    public String getDayOfWeekByYogaClassId(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String dayOfWeek = null;
        Cursor cursor = db.rawQuery("SELECT dayOfWeek FROM YogaClass WHERE id = ?", new String[]{yogaClassId});
        if (cursor.moveToFirst()) {
            dayOfWeek = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return dayOfWeek;
    }

    public Cursor getAllClassInstances() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance", null);
    }

    public int deleteClassInstance(int instanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("ClassInstance", "id = ?", new String[]{String.valueOf(instanceId)});
    }

    public int updateClassInstance(String instanceId, String date, String teacher, String comments, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("additionalComments", comments);
        values.put("price", price);

        int result = db.update("ClassInstance", values, "id = ?", new String[]{instanceId});
        db.close();
        return result;
    }

    public String getYogaClassIdByInstanceId(String instanceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT yogaClassId FROM ClassInstance WHERE id = ?", new String[]{instanceId});
        String yogaClassId = null;
        if (cursor.moveToFirst()) {
            yogaClassId = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return yogaClassId;
    }

    public boolean checkDuplicateInstance(String yogaClassId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM ClassInstance WHERE yogaClassId = ? AND date = ?", new String[]{yogaClassId, date});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);

        return db.insert("Categories", null, values);
    }

    public int updateCategory(String oldName, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);

        return db.update("Categories", values, "name = ?", new String[]{oldName});
    }

    public int deleteCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("Categories", "name = ?", new String[]{name});
    }

    public int deleteAllCategories() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("Categories", null, null);
    }

    public Cursor getAllCategories() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT name FROM Categories", null);
    }

    public boolean hasClassInstances(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM ClassInstance WHERE yogaClassId = ?", new String[]{yogaClassId});
        boolean hasInstances = false;
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            hasInstances = count > 0;
        }
        cursor.close();
        db.close();
        return hasInstances;
    }

    // Phương thức lưu danh sách giáo viên vào SQLite
    public void saveTeachersToSQLite(ArrayList<String> teacherList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM Teachers"); // Xóa dữ liệu cũ
        for (String teacher : teacherList) {
            ContentValues values = new ContentValues();
            values.put("name", teacher);
            db.insert("Teachers", null, values);
        }
        db.close();
    }

    // Phương thức lấy danh sách giáo viên từ SQLite
    public ArrayList<String> getAllTeachersFromSQLite() {
        ArrayList<String> teacherList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM Teachers", null);
        if (cursor.moveToFirst()) {
            do {
                teacherList.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return teacherList;
    }
}
