package com.example.yogaclass;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;

public class ViewSeasonActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView lvSeason;
    ClassInstanceAdapter adapter;
    ArrayList<ClassInstance> instanceList;
    DatabaseReference classRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_season);

        lvSeason = findViewById(R.id.lvSeason);
        dbHelper = new DBHelper(this);
        classRef = FirebaseDatabase.getInstance().getReference("classinstances");
        instanceList = new ArrayList<>();

        // Initialize adapter and set it to the ListView
        adapter = new ClassInstanceAdapter(this, instanceList, dbHelper, classRef, "Admin");
        lvSeason.setAdapter(adapter);

        loadClassInstances();  // Load data into ListView
    }


    private void loadClassInstances() {
        Cursor cursor = dbHelper.getAllClassInstances();
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


}
