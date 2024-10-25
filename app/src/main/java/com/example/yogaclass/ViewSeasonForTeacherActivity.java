package com.example.yogaclass;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ViewSeasonForTeacherActivity extends AppCompatActivity {

    DBHelper dbHelper;
    ListView lvSeason;
    ArrayList<ClassInstance> instanceList;
    ClassInstanceAdapter adapter;
    EditText etSearchDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_season_for_teacher);

        lvSeason = findViewById(R.id.lvSeason);
//        etSearchTeacher = findViewById(R.id.etSearchTeacher);
        etSearchDate = findViewById(R.id.etSearchDate);
        dbHelper = new DBHelper(this);
        instanceList = new ArrayList<>();

        Intent intent = getIntent();
        String teacherName = intent.getStringExtra("teacherName");

        // Initialize the adapter and set it to the ListView
        adapter = new ClassInstanceAdapter(this, instanceList, dbHelper, null, "Teacher");  // Không cần Firebase reference
        lvSeason.setAdapter(adapter);

        // Load class instances filtered by the teacher name
        loadClassInstances(teacherName);

        // Setup search filters for teacher name and date
        setupSearchFilters();
    }

    private void loadClassInstances(String teacherName) {
        Cursor cursor = dbHelper.getClassInstancesByTeacherName(teacherName);  // Lọc các instance theo tên giáo viên
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


    private void setupSearchFilters() {
        // Xóa các phần liên quan đến etSearchTeacher nếu không còn sử dụng nó
        etSearchDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterByDate(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    private void filterByTeacher(String teacherName) {
        ArrayList<ClassInstance> filteredList = new ArrayList<>();
        for (ClassInstance instance : instanceList) {
            if (instance.getTeacher().toLowerCase().contains(teacherName.toLowerCase())) {
                filteredList.add(instance);
            }
        }
        adapter.updateList(filteredList);
    }

    private void filterByDate(String date) {
        ArrayList<ClassInstance> filteredList = new ArrayList<>();
        for (ClassInstance instance : instanceList) {
            if (instance.getDate().contains(date)) {
                filteredList.add(instance);
            }
        }
        adapter.updateList(filteredList);
    }
}
