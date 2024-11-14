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
    ArrayList<ClassInstance> originalInstanceList; // Danh sách gốc
    ClassInstanceAdapter adapter;
    EditText etSearchDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_season_for_teacher);

        lvSeason = findViewById(R.id.lvSeason);
        etSearchDate = findViewById(R.id.etSearchDate);
        dbHelper = new DBHelper(this);
        instanceList = new ArrayList<>();
        originalInstanceList = new ArrayList<>(); // Khởi tạo danh sách gốc

        Intent intent = getIntent();
        String teacherName = intent.getStringExtra("teacherName");

        // Initialize the adapter and set it to the ListView
        adapter = new ClassInstanceAdapter(this, instanceList, dbHelper, null, "Teacher");
        lvSeason.setAdapter(adapter);

        // Load class instances filtered by the teacher name
        loadClassInstances(teacherName);

        // Setup search filters for teacher name and date
        setupSearchFilters();
    }

    private void loadClassInstances(String teacherName) {
        Cursor cursor = dbHelper.getClassInstancesByTeacherName(teacherName);
        instanceList.clear();
        originalInstanceList.clear(); // Xóa danh sách gốc trước khi tải lại

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
                originalInstanceList.add(instance); // Lưu vào danh sách gốc
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        adapter.notifyDataSetChanged();
    }

    private void setupSearchFilters() {
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

    private void filterByDate(String date) {

        if (date.isEmpty()) {
            adapter.updateList(originalInstanceList);
            return;
        }

        ArrayList<ClassInstance> filteredList = new ArrayList<>();
        for (ClassInstance instance : originalInstanceList) {
            if (instance.getDate().contains(date)) {
                filteredList.add(instance);
            }
        }
        adapter.updateList(filteredList);
    }
}
