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
        userIds = new ArrayList<>();

        // Initialize Firebase database reference
        usersRef = FirebaseDatabase.getInstance().getReference("users");


        adapter = new UserAdapter(this, userList, userIds, dbHelper);
        lvUsers.setAdapter(adapter);

        loadUsersFromSQLite();
        syncDataFromFirebase();
    }

    private void loadUsersFromSQLite() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Users", null);

        userList.clear();
        userIds.clear();

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String email = cursor.getString(cursor.getColumnIndex("email"));
                String role = cursor.getString(cursor.getColumnIndex("role"));
                User user = new User("", name, email, role, "");
                userList.add(user);
                userIds.add(email);
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
                db.execSQL("DELETE FROM Users");
                userList.clear();
                userIds.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    String email = snapshot.hasChild("email") ? snapshot.child("email").getValue(String.class) : "Unknown";
                    String role = snapshot.hasChild("role") ? snapshot.child("role").getValue(String.class) : "Unknown";
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setEmail(email);
                        user.setRole(role);
                        userList.add(user);
                        userIds.add(userId);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this, "Failed to sync users from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
