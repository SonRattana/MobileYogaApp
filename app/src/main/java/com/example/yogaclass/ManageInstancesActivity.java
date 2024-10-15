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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class ManageInstancesActivity extends AppCompatActivity {

    DBHelper dbHelper;
    EditText etDate, etComments, etPrice;
    Spinner spTeacher;
    Button btnSaveInstance;

    String yogaClassId;
    DatabaseReference classRef;
    String selectedDayOfWeek;  // Biến lưu giá trị của dayOfWeek

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
        etPrice = findViewById(R.id.etPrice);
        btnSaveInstance = findViewById(R.id.btnSaveInstance);


        // Lấy yogaClassId và dayOfWeek từ lớp Yoga
        yogaClassId = getIntent().getStringExtra("YOGA_CLASS_ID");
        selectedDayOfWeek = getIntent().getStringExtra("DAY_OF_WEEK");

        if (yogaClassId == null) {
            Toast.makeText(this, "Invalid Yoga Class ID", Toast.LENGTH_SHORT).show();
            finish();
        }
        Button btnViewSeason = findViewById(R.id.btnViewSeason);

        btnViewSeason.setOnClickListener(v -> {
            Intent intent = new Intent(ManageInstancesActivity.this, ViewSeasonActivity.class);
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

    // Hàm mở DatePicker và chỉ cho phép chọn các ngày trùng với dayOfWeek
    private void openDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        // Tạo DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);

            if (isCorrectDayOfWeek(selectedDate)) {
                // Định dạng lại ngày để hiển thị cho người dùng
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDate.setText(sdf.format(selectedDate.getTime()));
            } else {
                Toast.makeText(this, "Please select a valid " + selectedDayOfWeek, Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Giới hạn phạm vi của DatePicker để chỉ chọn được các ngày trùng với dayOfWeek
        datePickerDialog.getDatePicker().setMinDate(getFirstCorrectDay(calendar).getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(getLastCorrectDay(calendar).getTimeInMillis());

        datePickerDialog.show();
    }

    // Tính ngày đầu tiên đúng với dayOfWeek kể từ ngày hiện tại
    private Calendar getFirstCorrectDay(Calendar calendar) {
        while (!isCorrectDayOfWeek(calendar)) {
            calendar.add(Calendar.DATE, 1);
        }
        return calendar;
    }

    // Tính ngày cuối cùng đúng với dayOfWeek trong tương lai (giả sử trong vòng 1 năm)
    private Calendar getLastCorrectDay(Calendar calendar) {
        Calendar lastDate = (Calendar) calendar.clone();
        lastDate.add(Calendar.YEAR, 1); // Giới hạn trong 1 năm
        while (!isCorrectDayOfWeek(lastDate)) {
            lastDate.add(Calendar.DATE, -1);
        }
        return lastDate;
    }

    // Hàm kiểm tra xem ngày chọn có trùng với dayOfWeek của lớp học hay không
    private boolean isCorrectDayOfWeek(Calendar selectedDate) {
        int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        return mapDayOfWeek(dayOfWeek).equalsIgnoreCase(selectedDayOfWeek);
    }

    // Hàm chuyển đổi giá trị từ Calendar.DAY_OF_WEEK thành dạng chuỗi (ví dụ: "Monday", "Tuesday")
    private String mapDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return "";
        }
    }

    private void saveClassInstance() {
        String date = etDate.getText().toString();
        String teacher = spTeacher.getSelectedItem().toString();
        String comments = etComments.getText().toString();
        String priceText = etPrice.getText().toString();

        if (date.isEmpty() || teacher.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Please fill in all information", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceText);

        // Tạo ID mới cho Firebase
        String instanceId = classRef.push().getKey();

        // Lưu buổi học mới vào SQLite và đồng bộ với Firebase
        long result = dbHelper.insertClassInstance(instanceId, yogaClassId, date, teacher, comments, price); // Lưu vào SQLite

        if (result != -1) {
            // Đồng bộ dữ liệu với Firebase
            syncDataToFirebase(instanceId, yogaClassId, date, teacher, comments, price);

            Toast.makeText(this, "Class instance added successfully!", Toast.LENGTH_SHORT).show();
            // Chuyển hướng sau khi thêm thành công
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
        String priceText = etPrice.getText().toString();

        if (date.isEmpty() || teacher.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Please fill in all information", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceText);

        // Lấy ID của instance hiện tại (instance mà bạn đang muốn cập nhật)
        String instanceId = getIntent().getStringExtra("INSTANCE_ID");
        if (instanceId == null || instanceId.isEmpty()) {
            Toast.makeText(this, "No instance ID provided", Toast.LENGTH_SHORT).show();
            return;
        }

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
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Tạo một ArrayList để lưu tên giáo viên
        ArrayList<String> teacherList = new ArrayList<>();

        // Query Firebase để lấy danh sách người dùng có vai trò là "Teacher"
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Xóa danh sách hiện tại
                teacherList.clear();

                // Duyệt qua tất cả các user trong Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String role = snapshot.child("role").getValue(String.class);

                    // Chỉ thêm những user có vai trò là "Teacher"
                    if ("Teacher".equals(role)) {
                        String teacherName = snapshot.child("name").getValue(String.class);
                        teacherList.add(teacherName);
                    }
                }

                // Nếu không có giáo viên nào, thêm tùy chọn trống
                if (teacherList.isEmpty()) {
                    teacherList.add("No Teachers Available");
                }

                // Gán danh sách giáo viên vào Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ManageInstancesActivity.this, android.R.layout.simple_spinner_item, teacherList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTeacher.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageInstancesActivity.this, "Failed to load teachers: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}