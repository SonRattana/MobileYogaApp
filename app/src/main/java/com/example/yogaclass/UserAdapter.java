package com.example.yogaclass;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class UserAdapter extends ArrayAdapter<User> {

    private Context context;
    private ArrayList<User> users;
    private DBHelper dbHelper;
    private DatabaseReference usersRef;
    private ArrayList<String> userIds;  // Thêm danh sách userIds để lưu ID Firebase

    public UserAdapter(Context context, ArrayList<User> users, ArrayList<String> userIds, DBHelper dbHelper) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
        this.userIds = userIds;
        this.dbHelper = dbHelper;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_user, parent, false);
        }

        // Lấy người dùng và ID hiện tại
        User user = users.get(position);
        String userId = userIds.get(position);  // Lấy ID của người dùng tại vị trí này

        // Hiển thị tên và email của người dùng
        TextView tvUserName = convertView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = convertView.findViewById(R.id.tvUserEmail);
        TextView tvUserRole = convertView.findViewById(R.id.tvUserRole);
        tvUserName.setText(user.getName());
        tvUserEmail.setText(user.getEmail());
        tvUserRole.setText(user.getRole());
        // Nút chỉnh sửa
        ImageButton btnEdit = convertView.findViewById(R.id.btnEditUser);
        btnEdit.setOnClickListener(v -> {
            // Hiển thị dialog chỉnh sửa tên
            showEditDialog(user, userId);  // Truyền userId vào để cập nhật chính xác
        });

        return convertView;
    }

    // Hiển thị dialog để chỉnh sửa tên người dùng
    private void showEditDialog(User user, String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit User Name");

        final EditText input = new EditText(context);
        input.setText(user.getName());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                updateUser(user, newName, userId);
            } else {
                Toast.makeText(context, "Please enter a valid name!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void updateUser(User user, String newName, String userId) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE Users SET name = ? WHERE id = ?", new Object[]{newName, user.getId()});

        usersRef.child(userId).child("name").setValue(newName)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(context, "User name updated successfully!", Toast.LENGTH_SHORT).show();
                    user.setName(newName);
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(context, "Failed to update user name.", Toast.LENGTH_SHORT).show();
                });
    }
}





