package com.example.yogaclass;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class ManageInstancesActivity extends AppCompatActivity {

    DBHelper dbHelper;
    EditText etDate, etComments;
    Spinner spTeacher;
    Button btnSaveInstance;
    TextView tvPrice;
    String yogaClassId;
    DatabaseReference classRef;
    String selectedDayOfWeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_instances);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        classRef = FirebaseDatabase.getInstance().getReference("classinstances");

        dbHelper = new DBHelper(this);

        etDate = findViewById(R.id.etDate);
        spTeacher = findViewById(R.id.spTeacher);
        etComments = findViewById(R.id.etComments);
        tvPrice = findViewById(R.id.tvPrice);
        btnSaveInstance = findViewById(R.id.btnSaveInstance);


        // Lấy yogaClassId và dayOfWeek từ lớp Yoga
        yogaClassId = getIntent().getStringExtra("YOGA_CLASS_ID");
        selectedDayOfWeek = getIntent().getStringExtra("DAY_OF_WEEK");
        double price = getIntent().getDoubleExtra("PRICE",0.0);



        if (yogaClassId == null) {
            Toast.makeText(this, "Invalid Yoga Class ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        tvPrice.setText(String.format(Locale.getDefault(), "Price: $%.2f", price));

        Button btnViewSeason = findViewById(R.id.btnViewSeason);

        btnViewSeason.setOnClickListener(v -> {
            Intent intent = new Intent(ManageInstancesActivity.this, ViewSeasonActivity.class);
            intent.putExtra("YOGA_CLASS_ID", yogaClassId);
            intent.putExtra("DAY_OF_WEEK", selectedDayOfWeek);
            startActivity(intent);
        });


        // Set up DatePicker chỉ cho phép chọn các ngày trùng với dayOfWeek
        etDate.setOnClickListener(v -> openDatePicker());

        // Save class instance when Save button is clicked
        btnSaveInstance.setOnClickListener(v -> {
            saveClassInstance(); // Save to SQLite

            loadClassInstances(); // Reload the instances after saving
        });

        loadClassInstances();  // Load class instances from SQLite

        loadTeachersIntoSpinner();  // Load teachers into Spinner
    }

    private void openDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis(); // Lấy thời gian hiện tại

        // Tạo DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String date = sdf.format(selectedDate.getTime());

            // Kiểm tra nếu selectedDayOfWeek là null
            if (selectedDayOfWeek == null) {
                Toast.makeText(this, "Day of week is not set", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra xem ngày chọn có trùng với dayOfWeek của lớp học không
            if (isCorrectDayOfWeek(selectedDayOfWeek, date)) {
                etDate.setText(sdf.format(selectedDate.getTime()));  // Hiển thị ngày hợp lệ
            } else {
                Toast.makeText(this, "Please select a valid " + selectedDayOfWeek, Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Đặt giới hạn ngày nhỏ nhất là ngày hiện tại
        datePickerDialog.getDatePicker().setMinDate(currentTime);

        datePickerDialog.show();
    }


    // Hàm kiểm tra xem ngày chọn có trùng với dayOfWeek của lớp học hay không
    private boolean isCorrectDayOfWeek(String selectedDayOfWeek, String selectedDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            // Chuyển đổi selectedDate từ String sang Date
            Date date = sdf.parse(selectedDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Lấy giá trị ngày trong tuần dưới dạng int từ Calendar, sau đó chuyển thành chuỗi
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String dayOfWeekNumber = String.valueOf(dayOfWeek);  // Chuyển đổi từ int thành String

            // Chuyển đổi từ chuỗi số sang chuỗi tên ngày trong tuần
            String dayOfWeekString = mapDayOfWeek(dayOfWeekNumber);

            // So sánh chuỗi ngày trong tuần (ignore case để so sánh không phân biệt chữ hoa/thường)
            return dayOfWeekString.equalsIgnoreCase(selectedDayOfWeek);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;  // Trả về false nếu có lỗi xảy ra trong quá trình parse ngày
        }
    }

    // Hàm chuyển đổi giá trị từ Calendar.DAY_OF_WEEK thành dạng chuỗi (ví dụ: "Monday", "Tuesday")
    private String mapDayOfWeek(String dayOfWeek) {
        switch (dayOfWeek) {
            case "1":  // Sunday
                return "Sunday";
            case "2":  // Monday
                return "Monday";
            case "3":  // Tuesday
                return "Tuesday";
            case "4":  // Wednesday
                return "Wednesday";
            case "5":  // Thursday
                return "Thursday";
            case "6":  // Friday
                return "Friday";
            case "7":  // Saturday
                return "Saturday";
            default:
                return "Invalid day";
        }
    }


    private void saveClassInstance() {
        String date = etDate.getText().toString();
        String teacher = spTeacher.getSelectedItem().toString();
        String comments = etComments.getText().toString();

        if (date.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Please fill in all information", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCorrectDayOfWeek(selectedDayOfWeek, date)) {
            Toast.makeText(this, "Selected date does not match the required day of the week: " + selectedDayOfWeek, Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy giá trị của price từ intent (giá trị đã truyền vào khi mở activity này)
        double price = getIntent().getDoubleExtra("PRICE", 0.0);

        // Tạo ID mới cho Firebase
        String instanceId = classRef.push().getKey();

        // Lưu vào SQLite
        long result = dbHelper.insertClassInstance(instanceId, yogaClassId, date, teacher, comments, price);

        if (result != -1) {
            syncDataToFirebase(instanceId, yogaClassId, date, teacher, comments, price);
            Toast.makeText(this, "Class instance added successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ManageInstancesActivity.this, ViewSeasonActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to add class instance to SQLite", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClassInstance() {
        String date = etDate.getText().toString();
        String teacher = spTeacher.getSelectedItem().toString();
        String comments = etComments.getText().toString();

        if (date.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Please fill in all information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy ID của instance hiện tại (instance mà bạn đang muốn cập nhật)
        String instanceId = getIntent().getStringExtra("INSTANCE_ID");
        if (instanceId == null || instanceId.isEmpty()) {
            Toast.makeText(this, "No instance ID provided", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy giá từ YogaClass thay vì để người dùng nhập
        double price = dbHelper.getYogaClassPriceById(yogaClassId);

        // Cập nhật buổi học vào SQLite
        int result = dbHelper.updateClassInstance(instanceId, date, teacher, comments, price);

        if (result != -1) {
            // Nếu cập nhật thành công trong SQLite, đồng bộ dữ liệu lên Firebase
            syncDataToFirebase(instanceId, yogaClassId, date, teacher, comments, price);

            Toast.makeText(this, "Class instance updated successfully!", Toast.LENGTH_SHORT).show();
            // Quay lại màn hình hiển thị danh sách sau khi cập nhật thành công
            Intent intent = new Intent(ManageInstancesActivity.this, ViewSeasonActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to update class instance in SQLite", Toast.LENGTH_SHORT).show();
        }
    }

    // Load the class instances from SQLite

    private void loadClassInstances() {
        Cursor cursor = dbHelper.getClassInstancesByCourse(yogaClassId);
        ArrayList<String> instanceList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher"));
                String comments = cursor.getString(cursor.getColumnIndexOrThrow("additionalComments"));
                String price = cursor.getString(cursor.getColumnIndexOrThrow("price"));

                String instanceDetails = "Date: " + date + "\nTeacher: " + teacher + "\nPrice: $" + price;
                if (!comments.isEmpty()) {
                    instanceDetails += "\nComments: " + comments;
                }
                instanceList.add(instanceDetails);
            } while (cursor.moveToNext());
        }
        cursor.close();



    }


    private void syncDataToFirebase(String instanceId, String yogaClassId, String date, String teacher, String comments, double price) {
        if (instanceId == null || instanceId.isEmpty()) {
            Toast.makeText(ManageInstancesActivity.this, "No instance ID provided", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference instanceRef = classRef.child(yogaClassId).child(instanceId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("date", date);
        updates.put("teacher", teacher);
        updates.put("additionalComments", comments);
        updates.put("price", price);

        instanceRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManageInstancesActivity.this, "Successfully updated in Firebase!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ManageInstancesActivity.this, "Update failed in Firebase.", Toast.LENGTH_SHORT).show();
                });
    }


    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    // Load teachers into Spinner
    private void loadTeachersIntoSpinner() {
        ArrayList<String> teacherList = dbHelper.getAllTeachersFromSQLite();
        if (teacherList.isEmpty()) {
            teacherList.add("No Teachers Available");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageInstancesActivity.this, android.R.layout.simple_spinner_item, teacherList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTeacher.setAdapter(adapter);
    }


}