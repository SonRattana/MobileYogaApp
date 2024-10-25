package com.example.yogaclass;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
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

public class ViewSeasonActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView lvSeason;
    ClassInstanceAdapter adapter;
    ArrayList<ClassInstance> instanceList;
    DatabaseReference classRef;
    String yogaClassId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_season);

        lvSeason = findViewById(R.id.lvSeason);
        dbHelper = new DBHelper(this);
        classRef = FirebaseDatabase.getInstance().getReference("classinstances");
        instanceList = new ArrayList<>();

        // Get the yogaClassId passed from the previous activity
        yogaClassId = getIntent().getStringExtra("YOGA_CLASS_ID");

        // Check if yogaClassId is null
        if (yogaClassId == null) {
            Toast.makeText(this, "Yoga Class ID is missing!", Toast.LENGTH_SHORT).show();
            finish(); // Exit the activity if the ID is not provided
            return; // Ensure the rest of the onCreate method does not execute
        }

        // Initialize adapter and set it to the ListView
        adapter = new ClassInstanceAdapter(this, instanceList, dbHelper, classRef, "Admin");
        lvSeason.setAdapter(adapter);

        // Đồng bộ hóa dữ liệu từ Firebase về SQLite và sau đó tải dữ liệu từ SQLite
        syncDataFromFirebase();
    }

    private void loadClassInstances() {
        Log.d("ViewSeasonActivity", "YogaClassId: " + yogaClassId); // Log the yogaClassId
        Cursor cursor = dbHelper.getClassInstancesByYogaClassId(yogaClassId);
        instanceList.clear();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String instanceId = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                String yogaClassId = cursor.getString(cursor.getColumnIndexOrThrow("yogaClassId"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher"));
                String comments = cursor.getString(cursor.getColumnIndexOrThrow("additionalComments"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

                ClassInstance instance = new ClassInstance(instanceId, yogaClassId, date, teacher, comments, price);
                instanceList.add(instance);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        adapter.notifyDataSetChanged();
    }

    private void syncDataFromFirebase() {
        classRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Mở kết nối với SQLite
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM ClassInstance"); // Xóa dữ liệu cũ trước khi thêm dữ liệu mới

                // Duyệt qua các classinstances
                for (DataSnapshot classInstanceSnapshot : dataSnapshot.getChildren()) {
                    String yogaClassId = classInstanceSnapshot.getKey(); // Đây là yogaClassId

                    for (DataSnapshot snapshot : classInstanceSnapshot.getChildren()) {
                        String id = snapshot.getKey(); // Instance ID
                        String date = snapshot.child("date").getValue(String.class);
                        String teacher = snapshot.child("teacher").getValue(String.class);
                        String additionalComments = snapshot.child("additionalComments").getValue(String.class);

                        // Kiểm tra nếu price là null, gán giá trị mặc định là 0.0
                        Double priceValue = snapshot.child("price").getValue(Double.class);
                        double price = priceValue != null ? priceValue : 0.0;

                        // Chỉ thêm vào SQLite nếu yogaClassId, date và teacher không bị null
                        if (yogaClassId != null && date != null && teacher != null) {
                            ContentValues values = new ContentValues();
                            values.put("id", id);
                            values.put("yogaClassId", yogaClassId); // Sử dụng yogaClassId từ snapshot cha
                            values.put("date", date);
                            values.put("teacher", teacher);
                            values.put("additionalComments", additionalComments);
                            values.put("price", price);

                            db.insert("ClassInstance", null, values);
                        } else {
                            Log.e("ViewSeasonActivity", "Missing required fields for class instance");
                        }
                    }
                }

                db.close(); // Đóng kết nối SQLite

                // Sau khi đồng bộ, tải dữ liệu từ SQLite để hiển thị
                loadClassInstances();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewSeasonActivity.this, "Failed to sync data from Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
