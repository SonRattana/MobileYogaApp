package com.example.yogaclass;

import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.EditText;

public class YogaClassAdapter extends ArrayAdapter<YogaClass> {
    private final Context context;
    private final ArrayList<YogaClass> originalValues;
    private final ArrayList<YogaClass> filteredValues;
    private final DBHelper dbHelper;
    private final DatabaseReference firebaseRef;


        public YogaClassAdapter(Context context, int resource, ArrayList<YogaClass> objects, DBHelper dbHelper, DatabaseReference yogaClassesRef) {
            super(context, resource, objects);
            this.context = context;
            this.originalValues = new ArrayList<>(objects); // Lưu trữ dữ liệu gốc
            this.filteredValues = new ArrayList<>(objects); // Khởi tạo danh sách ban đầu
            this.dbHelper = dbHelper;
            this.firebaseRef = yogaClassesRef;
        }

        @Override
        public int getCount() {
            return filteredValues.size();
        }

        @Override
        public YogaClass getItem(int position) {
            return filteredValues.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                rowView = inflater.inflate(R.layout.list_item_yoga_class, parent, false);
            }


            TextView tvDayOfWeek = rowView.findViewById(R.id.tvDayOfWeek);
            TextView tvTime = rowView.findViewById(R.id.tvTime);
            TextView tvQuantity = rowView.findViewById(R.id.tvQuantity);
            TextView tvDuration = rowView.findViewById(R.id.tvDuration);

            TextView tvType = rowView.findViewById(R.id.tvType);
            TextView tvDescription = rowView.findViewById(R.id.tvDescription);
            ImageButton btnEdit = rowView.findViewById(R.id.btnEdit);
            ImageButton btnDelete = rowView.findViewById(R.id.btnDelete);
            ImageButton btnManage = rowView.findViewById(R.id.btnManage); // Nút để quản lý các phiên

            YogaClass yogaClass = filteredValues.get(position);


            tvDayOfWeek.setText("Day of Week: " + yogaClass.getDayOfWeek());
            tvTime.setText("Time: " + yogaClass.getTime());
            tvQuantity.setText("Quantity: " + yogaClass.getQuantity());
            tvDuration.setText("Duration: " + yogaClass.getDuration() + " mins");

            tvType.setText("Type: " + yogaClass.getType());
            tvDescription.setText("Description: " + yogaClass.getDescription());

            // Xử lý nút Manage Instances
            btnManage.setOnClickListener(view -> {
                Intent intent = new Intent(context, ManageInstancesActivity.class);
                intent.putExtra("YOGA_CLASS_ID", yogaClass.getId());
                intent.putExtra("courseId", yogaClass.getId()); // Pass the Firebase key as String
                context.startActivity(intent);
            });

            // Xử lý nút Edit
            btnEdit.setOnClickListener(view -> showEditDialog(yogaClass));

            // Xử lý nút Delete
            btnDelete.setOnClickListener(view -> deleteClass(yogaClass));

            return rowView;
        }

    public void filter(String keyword) {
        filteredValues.clear();
        if (keyword.isEmpty()) {
            filteredValues.addAll(originalValues);
        } else {
            for (YogaClass yogaClass : originalValues) {
                if (
                        yogaClass.getDayOfWeek().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredValues.add(yogaClass);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void showEditDialog(YogaClass yogaClass) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_class, null);

        // Tham chiếu tới các trường nhập liệu trong layout

//        EditText etDayOfWeek = dialogView.findViewById(R.id.etDayOfWeek);
//        EditText etTime = dialogView.findViewById(R.id.etTime);
//        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
//        EditText etDuration = dialogView.findViewById(R.id.etDuration);
//
//        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        // Đặt giá trị hiện tại của lớp học vào các trường nhập liệu

//        etDayOfWeek.setText(yogaClass.getDayOfWeek());
//        etTime.setText(yogaClass.getTime());
//        etQuantity.setText(String.valueOf(yogaClass.getQuantity()));
//        etDuration.setText(String.valueOf(yogaClass.getDuration()));
//
//        etType.setText(yogaClass.getType());
        etDescription.setText(yogaClass.getDescription());

        new AlertDialog.Builder(context)
                .setTitle("Edit Class")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Lấy giá trị từ các trường nhập liệu

//                    String updatedDayOfWeek = etDayOfWeek.getText().toString();
//                    String updatedTime = etTime.getText().toString();
//                    int updatedQuantity = Integer.parseInt(etQuantity.getText().toString());
//                    int updatedDuration = Integer.parseInt(etDuration.getText().toString());
//
//                    String updatedType = etType.getText().toString();
                    String updatedDescription = etDescription.getText().toString();

                    // Cập nhật đối tượng yogaClass

//                    yogaClass.setDayOfWeek(updatedDayOfWeek);
//                    yogaClass.setTime(updatedTime);
//                    yogaClass.setQuantity(updatedQuantity);
//                    yogaClass.setDuration(updatedDuration);
//                    yogaClass.setType(updatedType);
                    yogaClass.setDescription(updatedDescription);

                    // Cập nhật vào cơ sở dữ liệu
                    updateClassInDatabase(yogaClass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateClassInDatabase(YogaClass yogaClass) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
//        values.put("dayOfWeek", yogaClass.getDayOfWeek());
//        values.put("time", yogaClass.getTime());
//
//        values.put("quantity", yogaClass.getQuantity());
//        values.put("duration", yogaClass.getDuration());
//
//        values.put("type", yogaClass.getType());
        values.put("description", yogaClass.getDescription());

        // Cập nhật lớp học trong SQLite
        db.update("YogaClass", values, "id = ?", new String[]{yogaClass.getId()});

        // Cập nhật lớp học trong Firebase
        firebaseRef.child(yogaClass.getId()).setValue(yogaClass)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Class updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update on Firebase", Toast.LENGTH_SHORT).show());

        // Thông báo rằng dữ liệu đã thay đổi để cập nhật ListView
        notifyDataSetChanged();
        db.close();
    }

    private void deleteClass(YogaClass yogaClass) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this class?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Người dùng nhấn "Yes", thực hiện việc xóa lớp học
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("YogaClass", "id = ?", new String[]{yogaClass.getId()});

                    // Xóa khỏi Firebase
                    firebaseRef.child(yogaClass.getId()).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "Class deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to delete from Firebase", Toast.LENGTH_SHORT).show());

                    originalValues.remove(yogaClass);
                    filteredValues.remove(yogaClass);
                    notifyDataSetChanged();
                })
                .setNegativeButton("No", null) // Người dùng nhấn "No", đóng dialog
                .show();
    }
}
