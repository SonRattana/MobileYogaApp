package com.example.yogaclass;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CategoryManagementActivity extends AppCompatActivity {

    private EditText etCategoryName;
    private Button btnAddCategory, btnUpdateCategory, btnDeleteCategory;
    private ListView lvCategories;

    private FirebaseDatabase database;
    private DatabaseReference categoriesRef;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayList<String> categoryList;
    private String selectedCategory = null; // Giữ track danh mục được chọn để sửa hoặc xóa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        // Liên kết giao diện người dùng
        etCategoryName = findViewById(R.id.etCategoryName);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnUpdateCategory = findViewById(R.id.btnUpdateCategory);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        lvCategories = findViewById(R.id.lvCategories);

        // Firebase reference
        database = FirebaseDatabase.getInstance();
        categoriesRef = database.getReference("categories");

        // Khởi tạo danh sách và adapter
        categoryList = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryList);
        lvCategories.setAdapter(categoryAdapter);

        // Load danh sách category từ Firebase
        loadCategories();

        // Xử lý sự kiện khi click vào một category từ ListView
        lvCategories.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
            etCategoryName.setText(selectedCategory);
        });

        // Nút thêm danh mục
        btnAddCategory.setOnClickListener(v -> addCategory());

        // Nút sửa danh mục
        btnUpdateCategory.setOnClickListener(v -> updateCategory());

        // Nút xóa danh mục
        btnDeleteCategory.setOnClickListener(v -> deleteCategory());
    }

    // Thêm danh mục mới vào Firebase
    private void addCategory() {
        String categoryName = etCategoryName.getText().toString().trim();
        if (!categoryName.isEmpty()) {
            categoriesRef.push().setValue(categoryName)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CategoryManagementActivity.this, "Category added!", Toast.LENGTH_SHORT).show();
                        etCategoryName.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(CategoryManagementActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show();
        }
    }

    // Cập nhật danh mục hiện tại trong Firebase
    private void updateCategory() {
        String newCategoryName = etCategoryName.getText().toString().trim();
        if (selectedCategory != null && !newCategoryName.isEmpty()) {
            categoriesRef.orderByValue().equalTo(selectedCategory)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().setValue(newCategoryName)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CategoryManagementActivity.this, "Category updated!", Toast.LENGTH_SHORT).show();
                                            etCategoryName.setText("");
                                            selectedCategory = null; // Clear selection
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(CategoryManagementActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please select and update a category", Toast.LENGTH_SHORT).show();
        }
    }

    // Xóa danh mục từ Firebase
    private void deleteCategory() {
        if (selectedCategory != null) {
            categoriesRef.orderByValue().equalTo(selectedCategory)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CategoryManagementActivity.this, "Category deleted!", Toast.LENGTH_SHORT).show();
                                            etCategoryName.setText("");
                                            selectedCategory = null; // Clear selection
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(CategoryManagementActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please select a category to delete", Toast.LENGTH_SHORT).show();
        }
    }

    // Load danh sách các loại từ Firebase
    private void loadCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoryList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String category = snapshot.getValue(String.class);
                    categoryList.add(category);
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CategoryManagementActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
