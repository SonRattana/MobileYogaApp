package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
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
    ArrayList<ClassInstance> originalInstanceList;
    DatabaseReference classRef;
    String yogaClassId;
    EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_season);

        lvSeason = findViewById(R.id.lvSeason);
        etSearch = findViewById(R.id.etSearch);
        dbHelper = new DBHelper(this);
        classRef = FirebaseDatabase.getInstance().getReference("classinstances");
        instanceList = new ArrayList<>();
        originalInstanceList = new ArrayList<>();

        // Get the yogaClassId passed from the previous activity
        yogaClassId = getIntent().getStringExtra("YOGA_CLASS_ID");

        if (yogaClassId == null) {
            Toast.makeText(this, "Yoga Class ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadClassInstances();

        adapter = new ClassInstanceAdapter(this, instanceList, dbHelper, classRef, "Admin");
        lvSeason.setAdapter(adapter);



        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInstances(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Kiểm tra trạng thái kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void filterInstances(String keyword) {
        if (keyword.isEmpty()) {
            adapter.updateData(originalInstanceList);
            return;
        }

        ArrayList<ClassInstance> filteredList = new ArrayList<>();
        keyword = keyword.toLowerCase();

        for (ClassInstance instance : originalInstanceList) {
            boolean matchesTeacher = instance.getTeacher().toLowerCase().contains(keyword);
            boolean matchesDate = instance.getDate().contains(keyword);

            if (matchesTeacher || matchesDate) {
                filteredList.add(instance);
            }
        }

        adapter.updateData(filteredList);
    }

    private void loadClassInstances() {
        Log.d("ViewSeasonActivity", "YogaClassId: " + yogaClassId);
        Cursor cursor = dbHelper.getClassInstancesByYogaClassId(yogaClassId);
        instanceList.clear();
        originalInstanceList.clear();

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
                originalInstanceList.add(instance);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void syncDataFromFirebase() {
        classRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM ClassInstance"); // Xóa dữ liệu cũ trong SQLite
                for (DataSnapshot classInstanceSnapshot : dataSnapshot.getChildren()) {
                    String yogaClassId = classInstanceSnapshot.getKey();
                    for (DataSnapshot snapshot : classInstanceSnapshot.getChildren()) {
                        String id = snapshot.getKey();
                        String date = snapshot.child("date").getValue(String.class);
                        String teacher = snapshot.child("teacher").getValue(String.class);
                        String additionalComments = snapshot.child("additionalComments").getValue(String.class);
                        Double priceValue = snapshot.child("price").getValue(Double.class);
                        double price = priceValue != null ? priceValue : 0.0;
                        if (yogaClassId != null && date != null && teacher != null) {
                            ContentValues values = new ContentValues();
                            values.put("id", id);
                            values.put("yogaClassId", yogaClassId);
                            values.put("date", date);
                            values.put("teacher", teacher);
                            values.put("additionalComments", additionalComments);
                            values.put("price", price);

                            db.insert("ClassInstance", null, values); // Lưu vào SQLite
                        } else {
                            Log.e("ViewSeasonActivity", "Missing required fields for class instance");
                        }
                    }
                }
                db.close();
                loadClassInstances(); // Sau khi đồng bộ, tải dữ liệu từ SQLite để hiển thị
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewSeasonActivity.this, "Failed to sync data from Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
