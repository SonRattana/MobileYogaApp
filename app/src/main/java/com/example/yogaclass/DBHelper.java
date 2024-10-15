package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "YogaClass.db";
    private static final int DATABASE_VERSION = 3;  // Increment this as necessary

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng YogaClass
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

        // Tạo bảng ClassInstance
        db.execSQL(
                "CREATE TABLE ClassInstance (" +
                        "id TEXT PRIMARY KEY, " +
                        "yogaClassId INTEGER, " +
                        "date TEXT, " +
                        "teacher TEXT, " +
                        "additionalComments TEXT, " +
                        "price REAL, " +  // Thêm cột price với kiểu REAL
                        "FOREIGN KEY(yogaClassId) REFERENCES YogaClass(id))"
        );

        // Tạo bảng UsersRole và thêm cột name
        db.execSQL(
                "CREATE TABLE UsersRole (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, " +  // Thêm cột name
                        "email TEXT UNIQUE, " +
                        "password TEXT, " +
                        "role TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Phương thức này được gọi khi cần nâng cấp cơ sở dữ liệu
        if (oldVersion < 3) {
            Log.d("DBHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS ClassInstance");
            db.execSQL("DROP TABLE IF EXISTS YogaClass");
            db.execSQL("DROP TABLE IF EXISTS UsersRole");
            onCreate(db);
        }
    }

    // Chèn lớp Yoga mới vào cơ sở dữ liệu
    public long insertYogaClass(YogaClass yogaClass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dayOfWeek", yogaClass.getDayOfWeek());
        values.put("time", yogaClass.getTime());
        values.put("quantity", yogaClass.getQuantity());
        values.put("duration", yogaClass.getDuration());
        values.put("type", yogaClass.getType());
        values.put("description", yogaClass.getDescription());

        return db.insert("YogaClass", null, values);  // Trả về ID của dòng hoặc -1 nếu thất bại
    }

    // Chèn người dùng mới vào bảng UsersRole (đã thêm trường name)
    public boolean insertUser(String name, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);  // Thêm name vào ContentValues
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);

        long result = db.insert("UsersRole", null, values);
        return result != -1;  // Trả về true nếu chèn thành công
    }

    // Kiểm tra người dùng có tồn tại dựa trên email và password
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM UsersRole WHERE email = ? AND password = ?", new String[]{email, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Lấy vai trò của người dùng dựa trên email
    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM UsersRole WHERE email = ?", new String[]{email});
        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);  // Lấy vai trò từ cột đầu tiên
        }
        cursor.close();
        return role;
    }

    // Lấy tên người dùng dựa trên email
    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM UsersRole WHERE email = ?", new String[]{email});
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);  // Lấy tên từ cột đầu tiên
        }
        cursor.close();
        return name;
    }

    // Chèn phiên bản lớp vào bảng ClassInstance

    // Thêm buổi học mới vào SQLite và Firebase
    public long insertClassInstance(String instanceId, String yogaClassId, String date, String teacher, String comments, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Lưu ID từ Firebase vào SQLite
        values.put("id", instanceId);
        values.put("yogaClassId", yogaClassId);
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("additionalComments", comments);
        values.put("price", price);

        // Thêm dữ liệu vào SQLite và kiểm tra kết quả
        long result = db.insert("ClassInstance", null, values);

        db.close();
        return result;  // Trả về ID của dòng hoặc -1 nếu thất bại
    }


    // Lấy tất cả các phiên bản của lớp Yoga cụ thể
    public Cursor getClassInstancesByCourse(String yogaClassId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance WHERE yogaClassId = ?", new String[]{String.valueOf(yogaClassId)});
    }

    // Retrieve all class instances
    public Cursor getAllClassInstances() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ClassInstance", null);

    }

    // Delete a class instance by ID
    public int deleteClassInstance(int instanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("ClassInstance", "id = ?", new String[]{String.valueOf(instanceId)});
    }
    // Hàm cập nhật instance trong SQLite
    public int updateClassInstance(String instanceId, String date, String teacher, String comments, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Đặt các giá trị cần cập nhật
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("additionalComments", comments);
        values.put("price", price);  // Đảm bảo kiểu dữ liệu của price là double

        // Cập nhật dữ liệu trong SQLite dựa trên instanceId
        int result = db.update("ClassInstance", values, "id = ?", new String[]{instanceId});
        db.close();

        return result;  // Trả về số dòng bị ảnh hưởng
    }


    // Lấy danh sách tất cả giáo viên từ bảng UsersRole
    public Cursor getAllTeachers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT name FROM UsersRole WHERE role = ?", new String[]{"Teacher"});
    }


    // Kiểm tra phiên bản lớp trùng lặp dựa trên lớp Yoga và ngày
    public boolean checkDuplicateInstance(String yogaClassId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM ClassInstance WHERE yogaClassId = ? AND date = ?", new String[]{String.valueOf(yogaClassId), date});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
