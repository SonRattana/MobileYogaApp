package com.example.yogaclass;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class TeacherActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView lvClasses;
    ArrayList<String> classList;
    ArrayAdapter<String> adapter;
    ArrayList<Integer> idList;
    EditText etSearch;
    Spinner spinnerTypeFilter;
    Button btnLogout, btnViewSeason;
    String currentTeacherName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

        lvClasses = findViewById(R.id.lvClasses);
        etSearch = findViewById(R.id.etSearch);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewSeason = findViewById(R.id.btnViewSeason);
        dbHelper = new DBHelper(this);
        classList = new ArrayList<>();
        idList = new ArrayList<>();

        syncDataFromFirebase();
        loadClasses();


        btnLogout.setOnClickListener(v -> logout());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClasses(s.toString(), spinnerTypeFilter.getSelectedItem().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        setupTypeFilter();


        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                filterClasses(etSearch.getText().toString(), selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        String email = getIntent().getStringExtra("email");
        if (email != null) {
            currentTeacherName = dbHelper.getUserName(email);

            if (currentTeacherName != null) {
                loadClasses();
            } else {
                Toast.makeText(TeacherActivity.this, "User data not found in SQLite.", Toast.LENGTH_SHORT).show();
            }
        }


        btnViewSeason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!idList.isEmpty()) {

                    int classId = idList.get(0);
                    Intent intent = new Intent(TeacherActivity.this, ViewSeasonForTeacherActivity.class);
                    intent.putExtra("classId", classId);
                    intent.putExtra("teacherName", currentTeacherName);
                    startActivity(intent);
                } else {
                    Toast.makeText(TeacherActivity.this, "No classes available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupTypeFilter() {
        ArrayList<String> typeList = new ArrayList<>();
        typeList.add("All");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT type FROM YogaClass", null);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                typeList.add(type);
            } while (cursor.moveToNext());
        }
        cursor.close();


        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeList);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(typeAdapter);
    }


    private void filterClasses(String searchText, String selectedType) {
        ArrayList<String> filteredClasses = new ArrayList<>();
        ArrayList<Integer> filteredIds = new ArrayList<>();

        for (int i = 0; i < classList.size(); i++) {
            String classDetails = classList.get(i);


            if (classDetails.toLowerCase().contains(searchText.toLowerCase()) &&
                    (selectedType.equals("All") || classDetails.toLowerCase().contains(selectedType.toLowerCase()))) {
                filteredClasses.add(classDetails);
                if (i < idList.size()) {
                    filteredIds.add(idList.get(i));
                }
            }
        }


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredClasses);
        lvClasses.setAdapter(adapter);
        idList = filteredIds;
    }


    private void logout() {

        Intent intent = new Intent(TeacherActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void syncDataFromFirebase() {
        DatabaseReference yogaClassesRef = FirebaseDatabase.getInstance().getReference("yogaclasses");
        yogaClassesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM YogaClass");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    YogaClass yogaClass = snapshot.getValue(YogaClass.class);

                    ContentValues values = new ContentValues();
                    values.put("dayOfWeek", yogaClass.getDayOfWeek());
                    values.put("time", yogaClass.getTime());
                    values.put("quantity", yogaClass.getQuantity());
                    values.put("duration", yogaClass.getDuration());
                    values.put("type", yogaClass.getType());
                    values.put("price",yogaClass.getPrice());
                    values.put("description", yogaClass.getDescription());

                    db.insert("YogaClass", null, values);
                }

                loadClasses();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TeacherActivity.this, "Failed to sync data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadClasses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM YogaClass", null);

        classList.clear();
        idList.clear();

        if (cursor.moveToFirst()) {
            do {
                int classId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("dayOfWeek"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")); // Lấy giá tiền
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                String classDetails = "Day of Week: " + dayOfWeek + "\n" +
                        "Time: " + time + "\n" +
                        "Type: " + type + "\n" +
                        "Quantity: " + quantity + "\n" +
                        "Duration: " + duration + " mins\n" +
                        "Price: $" + String.format("%.2f", price) + "\n" +
                        "Description: " + (description != null ? description : "N/A");

                classList.add(classDetails);
                idList.add(classId);
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        lvClasses.setAdapter(adapter);
    }

}

