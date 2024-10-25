package com.example.yogaclass;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class ManageUsersActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView lvUsers;
    ArrayList<User> userList;
    ArrayList<String> userIds; // Danh sách userIds
    UserAdapter adapter;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        dbHelper = new DBHelper(this);
        lvUsers = findViewById(R.id.lvUsers);
        userList = new ArrayList<>();
        userIds = new ArrayList<>(); // Khởi tạo danh sách userIds

        // Initialize Firebase database reference
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Khởi tạo UserAdapter và truyền cả userIds
        adapter = new UserAdapter(this, userList, userIds, dbHelper);  // Truyền userIds vào đây
        lvUsers.setAdapter(adapter);

        loadUsersFromSQLite(); // Load users from SQLite
        syncDataFromFirebase(); // Sync data from Firebase
    }

    private void loadUsersFromSQLite() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Users", null); // Assuming Users table is already created

        userList.clear(); // Clear previous data
        userIds.clear(); // Clear previous emails/ids

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String email = cursor.getString(cursor.getColumnIndex("email"));
                String role = cursor.getString(cursor.getColumnIndex("role"));
                User user = new User("", name, email, role, ""); // Tạo đối tượng User không có ID
                userList.add(user); // Thêm người dùng vào danh sách
                userIds.add(email);  // Ở SQLite, tạm dùng email thay cho userId
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void syncDataFromFirebase() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM Users"); // Clear existing users in SQLite

                userList.clear();  // Xóa danh sách người dùng
                userIds.clear();   // Xóa danh sách ID người dùng

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();  // Lấy ID của người dùng (chuỗi ngẫu nhiên)

                    // Lấy dữ liệu email và role trực tiếp từ snapshot
                    String email = snapshot.hasChild("email") ? snapshot.child("email").getValue(String.class) : "Unknown";
                    String role = snapshot.hasChild("role") ? snapshot.child("role").getValue(String.class) : "Unknown";

                    // Lấy thông tin người dùng từ snapshot, sử dụng class User nếu phù hợp
                    User user = snapshot.getValue(User.class);

                    // Cập nhật email và role nếu đối tượng user không có các thuộc tính này
                    if (user != null) {
                        user.setEmail(email);  // Gán email vào user
                        user.setRole(role);    // Gán role vào user

                        userList.add(user);    // Thêm người dùng vào danh sách
                        userIds.add(userId);   // Lưu ID người dùng
                    }
                }

                // Cập nhật adapter
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this, "Failed to sync users from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
