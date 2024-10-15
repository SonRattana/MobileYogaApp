package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class DisplayClassesActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView classListView;
    ArrayList<YogaClass> yogaClasses;
    YogaClassAdapter adapter;
    EditText etSearch;
    private DatabaseReference yogaClassesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_classes);

        // Initialize UI components
        classListView = findViewById(R.id.classListView);
        etSearch = findViewById(R.id.etSearch);
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
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // When an item is clicked, navigate to ManageInstancesActivity
        // In DisplayClassesActivity.java
        classListView.setOnItemClickListener((parent, view, position, id) -> {
            YogaClass selectedClass = yogaClasses.get(position);

            // Pass the class ID to ManageInstancesActivity
            Intent intent = new Intent(DisplayClassesActivity.this, ManageInstancesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", selectedClass.getId());
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
                    values.put("dayOfWeek", yogaClass.getDayOfWeek());
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DisplayClassesActivity.this, "Failed to sync data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
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
