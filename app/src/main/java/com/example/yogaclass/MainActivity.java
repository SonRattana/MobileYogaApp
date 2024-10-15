package com.example.yogaclass;

import android.content.Intent;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference yogaClassesRef;
    ListView lvmain;
    EditText etDayOfWeek, etTime, etQuantity, etDuration, etDescription;
    Spinner spinnerType;
    Button btnSave, btnViewClasses, btnaddteacher;
    DBHelper dbHelper;
    Button btnManageInstances;
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
        etDayOfWeek = findViewById(R.id.etDayOfWeek);
        etTime = findViewById(R.id.etTime);
        etQuantity = findViewById(R.id.etQuantity);
        etDuration = findViewById(R.id.etDuration);
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType); // Spinner for Type
        btnSave = findViewById(R.id.btnSave);
        btnViewClasses = findViewById(R.id.btnViewClasses);
        lvmain = findViewById(R.id.lvmain);
        btnaddteacher = findViewById(R.id.btnaddteacher);

        // Set up Spinner with yoga types from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.yoga_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        // Firebase database initialization
        database = FirebaseDatabase.getInstance();
        yogaClassesRef = database.getReference("yogaclasses");

        // Save class when clicking on "Save Class" button
        btnSave.setOnClickListener(v -> saveYogaClass());

        // Chuyển sang trang hiển thị danh sách lớp học khi nhấn nút "View Class List"
        btnViewClasses.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DisplayClassesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", selectedYogaClassId);
            startActivity(intent);
        });
        btnaddteacher.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    private void saveYogaClass() {
        String dayOfWeek = etDayOfWeek.getText().toString();
        String time = etTime.getText().toString();
        int quantity = Integer.parseInt(etQuantity.getText().toString());
        int duration = Integer.parseInt(etDuration.getText().toString());
        String type = spinnerType.getSelectedItem().toString(); // Get selected type from Spinner
        String description = etDescription.getText().toString();

        if (dayOfWeek.isEmpty() || time.isEmpty() || quantity <= 0 || duration <= 0 || type.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please fill all required fields!", Toast.LENGTH_SHORT).show();
        } else {
            // Tạo một lớp Yoga mới (Bỏ Teacher và Price)
            YogaClass yogaClass = new YogaClass(null, dayOfWeek, time, quantity, duration, type, description);

            // Lưu lớp học vào SQLite trước
            long result = dbHelper.insertYogaClass(yogaClass);

            if (result != -1) {
                // Nếu lưu vào SQLite thành công, tiếp tục lưu vào Firebase
                String key = yogaClassesRef.push().getKey(); // Tạo khóa mới từ Firebase
                if (key != null) {
                    yogaClass.setId(key); // Đặt ID của lớp học

                    yogaClassesRef.child(key).setValue(yogaClass)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(MainActivity.this, "Yoga class saved to SQLite and Firebase!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(MainActivity.this, "Failed to save to Firebase", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(MainActivity.this, "Failed to save to SQLite", Toast.LENGTH_SHORT).show();
            }
        }
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
