package com.example.yogaclass;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference yogaClassesRef, categoriesRef;
    ListView lvmain;
    EditText etTime, etQuantity, etDuration, etDescription, etPrice;
    Spinner spinnerType, spDayOfWeek; // Giữ lại Spinner Day of Week
    Button btnSave, btnViewClasses, btnAddTeacher, btnManageUsers, btnViewCustomerBookings, btnDeleteAll, btnManageCategory, btnLogout;
    DBHelper dbHelper;
    int selectedYogaClassId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this);

        dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Initialize UI components
        spDayOfWeek = findViewById(R.id.spDayOfWeek); // Spinner Day of Week
        etTime = findViewById(R.id.etTime);
        etQuantity = findViewById(R.id.etQuantity);
        etDuration = findViewById(R.id.etDuration);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        btnSave = findViewById(R.id.btnSave);
        btnViewClasses = findViewById(R.id.btnViewClasses);
        lvmain = findViewById(R.id.lvmain);
        btnAddTeacher = findViewById(R.id.btnaddteacher);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnViewCustomerBookings = findViewById(R.id.btnViewCustomerBookings);
        btnDeleteAll = findViewById(R.id.btnDeleteAll); // Nút xóa toàn bộ lớp học
        btnManageCategory = findViewById(R.id.btnManageCategory); // Nút quản lý Category
        btnLogout = findViewById(R.id.btnLogout);

        // Firebase database initialization
        database = FirebaseDatabase.getInstance();
        yogaClassesRef = database.getReference("yogaclasses");
        categoriesRef = database.getReference("categories"); // Thêm tham chiếu đến "categories" trong Firebase

        // Load category từ Firebase và gán vào Spinner
        loadCategoryTypes();

        // Load dayOfWeek vào Spinner
        loadDayOfWeek();

        // Save class when clicking on "Save Class" button
        btnSave.setOnClickListener(v -> saveYogaClass());

        // Chuyển sang trang hiển thị danh sách lớp học khi nhấn nút "View Class List"
        btnViewClasses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DisplayClassesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", selectedYogaClassId);
            startActivity(intent);
        });

        btnAddTeacher.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        });

        btnViewCustomerBookings.setOnClickListener(v -> {
            // Open the CustomerBookingsActivity when the button is clicked
            Intent intent = new Intent(MainActivity.this, CustomerBookingsActivity.class);
            startActivity(intent);
        });

        btnDeleteAll.setOnClickListener(v -> confirmAndDeleteAllClasses()); // Gọi hàm xóa toàn bộ khi nhấn nút

        btnManageCategory.setOnClickListener(v -> {
            // Mở CategoryManagementActivity khi nhấn nút "Quản lý Category"
            Intent intent = new Intent(MainActivity.this, CategoryManagementActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        // Xóa thông tin người dùng từ SharedPreferences
        SharedPreferences preferences = getSharedPreferences("your_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // Xóa tất cả dữ liệu
        editor.apply();

        // Quay về màn hình đăng nhập
        Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Thay bằng Activity đăng nhập của bạn
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa tất cả các Activity trước đó
        startActivity(intent);
        finish(); // Kết thúc Activity hiện tại
    }

    // Hàm xác nhận xóa toàn bộ dữ liệu
    private void confirmAndDeleteAllClasses() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete all classes and seasons?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteAllClasses())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Hàm xóa toàn bộ dữ liệu từ Firebase và SQLite (bao gồm cả classes, class instances, categories và bookings)
    private void deleteAllClasses() {
        // Xóa tất cả các lớp học trên Firebase
        yogaClassesRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Sau khi xóa classes thành công trên Firebase, tiếp tục xóa class instances
                DatabaseReference classInstancesRef = FirebaseDatabase.getInstance().getReference("classinstances");
                classInstancesRef.removeValue().addOnCompleteListener(instanceTask -> {
                    if (instanceTask.isSuccessful()) {
                        // Xóa categories sau khi đã xóa class instances
                        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
                        categoriesRef.removeValue().addOnCompleteListener(categoriesTask -> {
                            if (categoriesTask.isSuccessful()) {
                                // Xóa bookings sau khi đã xóa categories
                                DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
                                bookingsRef.removeValue().addOnCompleteListener(bookingsTask -> {
                                    if (bookingsTask.isSuccessful()) {
                                        // Xóa dữ liệu trong SQLite sau khi xóa trên Firebase thành công
                                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                                        try {
                                            // Xóa toàn bộ lớp học và class instances
                                            db.execSQL("DELETE FROM YogaClass"); // Xóa toàn bộ lớp học
                                            db.execSQL("DELETE FROM ClassInstance"); // Xóa toàn bộ class instance
                                            db.execSQL("DELETE FROM Categories"); // Xóa toàn bộ categories
                                            db.execSQL("DELETE FROM Bookings"); // Xóa toàn bộ bookings

                                            Toast.makeText(MainActivity.this, "All classes, seasons (instances), categories, and bookings deleted successfully from Firebase and SQLite!", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Toast.makeText(MainActivity.this, "Failed to delete data from SQLite", Toast.LENGTH_SHORT).show();
                                        } finally {
                                            db.close();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to delete bookings from Firebase", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to delete categories from Firebase", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to delete class instances from Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "Failed to delete classes from Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }



    // Hàm load danh sách các Category Type từ Firebase và gán vào Spinner
    private void loadCategoryTypes() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> categories = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    categories.add(category);
                }
                // Cập nhật Spinner với dữ liệu mới
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerType.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm load danh sách các ngày trong tuần vào Spinner
    private void loadDayOfWeek() {
        ArrayAdapter<CharSequence> dayOfWeekAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(dayOfWeekAdapter);
    }

    private void saveYogaClass() {
        String dayOfWeek = spDayOfWeek.getSelectedItem().toString();
        String time = etTime.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();
        String durationText = etDuration.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();

        if (dayOfWeek.isEmpty() || time.isEmpty() || quantityText.isEmpty() || durationText.isEmpty() || type.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please fill all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity;
        int duration;
        double price;

        try {
            quantity = Integer.parseInt(quantityText);
            duration = Integer.parseInt(durationText);
            price = Double.parseDouble(priceText);

            if (quantity <= 0 || duration <= 0 || price <= 0) {
                Toast.makeText(MainActivity.this, "Quantity, Duration, and Price must be greater than 0!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter valid numbers for Quantity, Duration, and Price!", Toast.LENGTH_SHORT).show();
            return;
        }

        YogaClass yogaClass = new YogaClass(null, dayOfWeek, time, quantity, duration, type, price, description);


        String classDetails = "Day of Week: " + dayOfWeek +
                "\nTime: " + time +
                "\nQuantity: " + quantity +
                "\nDuration: " + duration +
                "\nType: " + type +
                "\nPrice: " + price +
                "\nDescription: " + description;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Yoga Class Details")
                .setMessage(classDetails)
                .setPositiveButton("Confirm", (dialog, which) -> {

                    long result = dbHelper.insertYogaClass(yogaClass);

                    if (result != -1) {

                        String key = yogaClassesRef.push().getKey();
                        if (key != null) {
                            yogaClass.setId(key);
                            yogaClassesRef.child(key).setValue(yogaClass)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Yoga class saved to SQLite and Firebase!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to save to Firebase", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to save to SQLite", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }



    // Hàm load danh sách lớp Yoga từ Firebase
    private void loadYogaClasses() {
        ArrayList<YogaClass> yogaClasses = new ArrayList<>();
        yogaClassesRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    for (DataSnapshot data : task.getResult().getChildren()) {
                        String key = data.getKey();
                        HashMap r = (HashMap) data.getValue();

                        YogaClass yogaClass = new YogaClass(
                                key,
                                r.get("dayOfWeek").toString(),
                                r.get("time").toString(),
                                Integer.parseInt(r.get("quantity").toString()),
                                Integer.parseInt(r.get("duration").toString()),
                                r.get("type").toString(),
                                Double.parseDouble(r.get("price").toString()),
                                r.get("description").toString()
                        );

                        yogaClasses.add(yogaClass);
                    }

                    // Truyền thêm dbHelper và yogaClassesRef vào YogaClassAdapter
                    YogaClassAdapter adapter = new YogaClassAdapter(MainActivity.this, R.layout.list_item_yoga_class, yogaClasses, dbHelper, yogaClassesRef);
                    lvmain.setAdapter(adapter);
                }
            }
        });
    }
}
