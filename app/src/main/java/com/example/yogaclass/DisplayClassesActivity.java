package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import androidx.core.content.ContextCompat;

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
    Spinner spinnerFilterType, spinnerFilterDay;
    private DatabaseReference yogaClassesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_classes);

        // Initialize UI components
        classListView = findViewById(R.id.classListView);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilterType = findViewById(R.id.spinnerFilterType);
        spinnerFilterDay = findViewById(R.id.spinnerFilterDay);
        dbHelper = new DBHelper(this);
        yogaClasses = new ArrayList<>();

        // Initialize Firebase reference
        yogaClassesRef = FirebaseDatabase.getInstance().getReference("yogaclasses");

        // Sync data from Firebase to SQLite
        if (isNetworkAvailable()) {
            syncDataFromFirebase();
        } else {
            loadDataFromSQLite();
        }

        // Load classes from SQLite
        loadClasses();

        // Implement search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterByTypeAndDay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        // Setup filters for Type and Day of the Week
        spinnerFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterByTypeAndDay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerFilterDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterByTypeAndDay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // When a user clicks on a class, go to ManageInstancesActivity
        classListView.setOnItemClickListener((parent, view, position, id) -> {
            YogaClass selectedClass = yogaClasses.get(position);

            if (selectedClass.getId() == null || selectedClass.getDayOfWeek() == null) {
                Log.e("DisplayClassesActivity", "Class ID or Day of Week is null for class at position: " + position);
                Toast.makeText(DisplayClassesActivity.this, "Class ID or Day of Week is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(DisplayClassesActivity.this, ManageInstancesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", selectedClass.getId());
            intent.putExtra("DAY_OF_WEEK", selectedClass.getDayOfWeek());
            intent.putExtra("PRICE", selectedClass.getPrice());
            startActivity(intent);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager.class);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Load classes from SQLite when offline
    private void loadDataFromSQLite() {
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
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                YogaClass yogaClass = new YogaClass(
                        String.valueOf(classId),
                        dayOfWeek,
                        time,
                        quantity,
                        duration,
                        type,
                        price,
                        description
                );
                yogaClasses.add(yogaClass);
            } while (cursor.moveToNext());
        }
        cursor.close();

        setupAdapter();
        loadFilterTypes();
        loadFilterDays();
    }

    private void syncDataFromFirebase() {
        yogaClassesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM YogaClass");

                yogaClasses.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    YogaClass yogaClass = snapshot.getValue(YogaClass.class);

                    ContentValues values = new ContentValues();
                    values.put("dayOfWeek", Objects.requireNonNull(yogaClass).getDayOfWeek());
                    values.put("time", yogaClass.getTime());
                    values.put("quantity", yogaClass.getQuantity());
                    values.put("duration", yogaClass.getDuration());
                    values.put("type", yogaClass.getType());
                    values.put("description", yogaClass.getDescription());

                    db.insert("YogaClass", null, values);

                    yogaClasses.add(yogaClass);
                }

                setupAdapter();
                loadFilterTypes();
                loadFilterDays();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DisplayClassesActivity.this, "Failed to sync data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load Types for Spinner Filter from class data
    private void loadFilterTypes() {
        ArrayList<String> types = new ArrayList<>();
        types.add("All");

        for (YogaClass yogaClass : yogaClasses) {
            if (!types.contains(yogaClass.getType())) {
                types.add(yogaClass.getType());
            }
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(typeAdapter);
    }

    // Load Days for Spinner Filter from class data
    private void loadFilterDays() {
        ArrayList<String> days = new ArrayList<>();
        days.add("All");  // Option to show all days
        days.add("Monday");
        days.add("Tuesday");
        days.add("Wednesday");
        days.add("Thursday");
        days.add("Friday");
        days.add("Saturday");
        days.add("Sunday");

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterDay.setAdapter(dayAdapter);
    }

    // Filter classes by Type and Day of the Week
    private void filterByTypeAndDay() {
        String selectedType = spinnerFilterType.getSelectedItem().toString();
        String selectedDay = spinnerFilterDay.getSelectedItem().toString();
        String searchQuery = etSearch.getText().toString().toLowerCase();

        ArrayList<YogaClass> filteredList = new ArrayList<>();

        for (YogaClass yogaClass : yogaClasses) {
            boolean matchesType = selectedType.equals("All") || yogaClass.getType().equals(selectedType);
            boolean matchesDay = selectedDay.equals("All") || yogaClass.getDayOfWeek().equalsIgnoreCase(selectedDay);
            boolean matchesSearch = searchQuery.isEmpty() || yogaClass.getDayOfWeek().toLowerCase().contains(searchQuery);

            if (matchesType && matchesDay && matchesSearch) {
                filteredList.add(yogaClass);
            }
        }

        adapter.updateData(filteredList);
    }


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
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                YogaClass yogaClass = new YogaClass(
                        String.valueOf(classId),
                        dayOfWeek,
                        time,
                        quantity,
                        duration,
                        type,
                        price,
                        description
                );
                yogaClasses.add(yogaClass);
            } while (cursor.moveToNext());
        }
        cursor.close();

        setupAdapter();
    }

    private void setupAdapter() {
        adapter = new YogaClassAdapter(this, R.layout.list_item_yoga_class, yogaClasses, dbHelper, yogaClassesRef);
        classListView.setAdapter(adapter);
    }
}
