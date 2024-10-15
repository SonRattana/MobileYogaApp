package com.example.yogaclass;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TeacherActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView classListView;
    ArrayList<String> classList;
    ArrayAdapter<String> adapter;
    ArrayList<Integer> idList;
    EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_classes);

        classListView = findViewById(R.id.classListView);
        etSearch = findViewById(R.id.etSearch);
        dbHelper = new DBHelper(this);
        classList = new ArrayList<>();
        idList = new ArrayList<>();

        syncDataFromFirebase();
        loadClasses();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClasses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        classListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the classId of the selected class
                int classId = idList.get(position);

                Intent intent = new Intent(TeacherActivity.this, ViewSeasonForTeacherActivity.class);
                intent.putExtra("classId", classId);  // Pass the classId of the selected class
                startActivity(intent);

            }
        });


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
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                String classDetails =
                        "Day of Week: " + dayOfWeek + "\n" +
                        "Time: " + time + "\n" +
                        "Type: " + type + "\n" +
                        "Quantity: " + quantity + "\n" +
                        "Duration: " + duration + " mins\n" +

                        "Description: " + (description != null ? description : "N/A");

                classList.add(classDetails);
                idList.add(classId);
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        classListView.setAdapter(adapter);
    }

    private void filterClasses(String searchText) {
        ArrayList<String> filteredClasses = new ArrayList<>();
        ArrayList<Integer> filteredIds = new ArrayList<>();

        for (int i = 0; i < classList.size(); i++) {
            String classDetails = classList.get(i);

            if (classDetails.toLowerCase().contains(searchText.toLowerCase())) {
                filteredClasses.add(classDetails);
                if (i < idList.size()) {
                    filteredIds.add(idList.get(i));
                }
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredClasses);
        classListView.setAdapter(adapter);
        idList = filteredIds;
    }
}
