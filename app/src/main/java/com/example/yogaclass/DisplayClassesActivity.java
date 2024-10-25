package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DisplayClassesActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView classListView;
    ArrayList<YogaClass> yogaClasses;
    YogaClassAdapter adapter;
    EditText etSearch;
    Spinner spinnerFilterType;
    private DatabaseReference yogaClassesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_classes);

        // Initialize UI components
        classListView = findViewById(R.id.classListView);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilterType = findViewById(R.id.spinnerFilterType);  // Thêm spinner lọc theo Type
        dbHelper = new DBHelper(this);
        yogaClasses = new ArrayList<>();

        // Initialize Firebase reference
        yogaClassesRef = FirebaseDatabase.getInstance().getReference("yogaclasses");

        // Sync data from Firebase to SQLite
        syncDataFromFirebase();

        // Load data from SQLite
        loadClasses();

        // Implement search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());  // Tìm kiếm theo từ khóa nhập vào
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Lọc theo Type khi người dùng chọn từ spinner
        spinnerFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = (String) parent.getItemAtPosition(position);
                filterByType(selectedType);  // Gọi hàm lọc theo Type
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Khi người dùng nhấp vào một lớp học, chuyển đến trang quản lý instances (ManageInstancesActivity)
        classListView.setOnItemClickListener((parent, view, position, id) -> {
            YogaClass selectedClass = yogaClasses.get(position);

            // Kiểm tra nếu yogaClassId hoặc dayOfWeek bị null
            if (selectedClass.getId() == null || selectedClass.getDayOfWeek() == null) {
                Log.e("DisplayClassesActivity", "Class ID or Day of Week is null for class at position: " + position);
                Toast.makeText(DisplayClassesActivity.this, "Class ID or Day of Week is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log để kiểm tra dữ liệu trước khi truyền qua Intent
            Log.d("DisplayClassesActivity", "Class ID: " + selectedClass.getId() + ", Day of Week: " + selectedClass.getDayOfWeek());

            // Truyền class ID và dayOfWeek qua Intent
            Intent intent = new Intent(DisplayClassesActivity.this, ManageInstancesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", selectedClass.getId());
            intent.putExtra("DAY_OF_WEEK", selectedClass.getDayOfWeek());
            startActivity(intent);
        });
    }

    // Sync data from Firebase to SQLite
    private void syncDataFromFirebase() {
        yogaClassesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM YogaClass");  // Clear old data in SQLite

                yogaClasses.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    YogaClass yogaClass = snapshot.getValue(YogaClass.class);

                    // Save class to SQLite
                    ContentValues values = new ContentValues();
                    values.put("dayOfWeek", Objects.requireNonNull(yogaClass).getDayOfWeek());
                    values.put("time", yogaClass.getTime());
                    values.put("quantity", yogaClass.getQuantity());
                    values.put("duration", yogaClass.getDuration());
                    values.put("type", yogaClass.getType());
                    values.put("description", yogaClass.getDescription());

                    db.insert("YogaClass", null, values);

                    // Add class to display list
                    yogaClasses.add(yogaClass);
                }

                setupAdapter();  // Set up Adapter to display the classes
                loadFilterTypes();  // Load các type để hiển thị trong spinner
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DisplayClassesActivity.this, "Failed to sync data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load các Type cho Spinner Filter từ dữ liệu lớp học đã đồng bộ
    private void loadFilterTypes() {
        ArrayList<String> types = new ArrayList<>();
        types.add("All");  // Lựa chọn để hiển thị tất cả các lớp

        for (YogaClass yogaClass : yogaClasses) {
            if (!types.contains(yogaClass.getType())) {
                types.add(yogaClass.getType());  // Thêm type vào danh sách nếu chưa có
            }
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(typeAdapter);
    }

    // Lọc các lớp theo Type được chọn
    private void filterByType(String selectedType) {
        ArrayList<YogaClass> filteredList = new ArrayList<>();

        if (selectedType.equals("All")) {
            // Nếu người dùng chọn "All", hiển thị tất cả các lớp
            filteredList.addAll(yogaClasses);
        } else {
            for (YogaClass yogaClass : yogaClasses) {
                if (yogaClass.getType().equals(selectedType)) {
                    filteredList.add(yogaClass);
                }
            }
        }

        // Cập nhật adapter với danh sách đã lọc
        adapter.updateData(filteredList);
    }

    // Load classes from SQLite
    private void loadClasses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM YogaClass", null);

        yogaClasses.clear();

        if (cursor.moveToFirst()) {
            do {
                int classId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("dayOfWeek"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                YogaClass yogaClass = new YogaClass(
                        String.valueOf(classId),
                        dayOfWeek,
                        time,
                        quantity,
                        duration,
                        type,
                        description
                );
                yogaClasses.add(yogaClass);
            } while (cursor.moveToNext());
        }
        cursor.close();

        setupAdapter();  // Set up the adapter to show classes
    }

    // Set up adapter for ListView
    private void setupAdapter() {
        adapter = new YogaClassAdapter(this, R.layout.list_item_yoga_class, yogaClasses, dbHelper, yogaClassesRef);
        classListView.setAdapter(adapter);
    }
}
