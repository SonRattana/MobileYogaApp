package com.example.yogaclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    private EditText eName, eEmail, ePassword;
    private RadioButton rbAdmin, rbTeacher;
    private TextView lblMessage;
    private Button btnaddteacher;
    private DatabaseReference usersRef;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase và DBHelper
        FirebaseApp.initializeApp(this);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        dbHelper = new DBHelper(this);

        eName = findViewById(R.id.eName); // Thêm trường tên người dùng
        eEmail = findViewById(R.id.eEmail);
        ePassword = findViewById(R.id.ePassword);
//        rbAdmin = findViewById(R.id.rbAdmin);
        rbTeacher = findViewById(R.id.rbTeacher);
        lblMessage = findViewById(R.id.lblMessage);
        btnaddteacher = findViewById(R.id.btnaddteacher);

        btnaddteacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = eName.getText().toString().trim(); // Lấy tên người dùng
                String email = eEmail.getText().toString().trim();
                String password = ePassword.getText().toString().trim();
                String role = rbAdmin.isChecked() ? "Admin" : "Teacher";

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    lblMessage.setText("Please fill in all fields!");
                } else {
                    registerUser(name, email, password, role);
                }
            }
        });
    }

    private void registerUser(String name, String email, String password, String role) {
        String userId = usersRef.push().getKey();
        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("name", name); // Lưu tên người dùng vào Firebase
        userMap.put("email", email);
        userMap.put("password", password);
        userMap.put("role", role);

        if (userId != null) {
            usersRef.child(userId).setValue(userMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Lưu vào SQLite
                    boolean isInserted = dbHelper.insertUser(name, email, password, role);
                    if (isInserted) {
                        Toast.makeText(RegisterActivity.this, "Add Teacher successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        lblMessage.setText("Add Teacher failed in SQLite.");
                    }
                } else {
                    lblMessage.setText("Add Teacher failed. User might already exist.");
                }
            });
        }
    }
}