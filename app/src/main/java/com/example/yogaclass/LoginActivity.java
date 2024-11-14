package com.example.yogaclass;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseApp;

public class LoginActivity extends AppCompatActivity {
    private EditText eEmail, ePassword;
    private TextView lblMessage;
    private Button btnLogin;
    private DatabaseReference usersRef;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        dbHelper = new DBHelper(this);

        eEmail = findViewById(R.id.eEmail);
        ePassword = findViewById(R.id.ePassword);
        lblMessage = findViewById(R.id.lblMessage);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = eEmail.getText().toString().trim();
                String password = ePassword.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()) {
                    lblMessage.setText("Please fill in all fields!");
                } else {
                    if (isNetworkAvailable()) {
                        loginUser(email, password);
                    } else {
                        Toast.makeText(LoginActivity.this, "Check your connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loginUser(String email, String password) {
        if (dbHelper.checkUser(email, password)) {
            String role = dbHelper.getUserRole(email);

            if ("Admin".equals(role)) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else if ("Teacher".equals(role)) {
                Intent intent = new Intent(LoginActivity.this, TeacherActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            }
        } else {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean userFound = false;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dbEmail = snapshot.child("email").getValue(String.class);
                        String dbPassword = snapshot.child("password").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);

                        if (dbEmail != null && dbPassword != null && dbEmail.equals(email) && dbPassword.equals(password)) {
                            userFound = true;

                            String name = snapshot.child("name").getValue(String.class);
                            dbHelper.insertUser(name, email, password, role);

                            if ("Admin".equals(role)) {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else if ("Teacher".equals(role)) {
                                Intent intent = new Intent(LoginActivity.this, TeacherActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            }
                            break;
                        }
                    }

                    if (!userFound) {
                        lblMessage.setText("Login failed. Please check your credentials.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    lblMessage.setText("Database error: " + databaseError.getMessage());
                }
            });
        }
    }


}