package com.example.yogaclass;

import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Spinner;
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
        TextView tvPrice = rowView.findViewById(R.id.tvPrice);
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
        tvPrice.setText("Price: $" + yogaClass.getPrice());
        tvDescription.setText("Description: " + yogaClass.getDescription());

        // Xử lý nút Manage Instances
        btnManage.setOnClickListener(view -> {
            Intent intent = new Intent(context, ManageInstancesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", yogaClass.getId());
            intent.putExtra("DAY_OF_WEEK", yogaClass.getDayOfWeek());
            intent.putExtra("courseId", yogaClass.getId()); // Pass the Firebase key as String
            intent.putExtra("PRICE", yogaClass.getPrice());
            context.startActivity(intent);
        });

        // Xử lý nút Edit
        btnEdit.setOnClickListener(view -> showEditDialog(yogaClass));

        // Xử lý nút Delete
        btnDelete.setOnClickListener(view -> deleteClass(yogaClass));

        return rowView;
    }

    // Phương thức để cập nhật danh sách yoga class
    public void updateData(ArrayList<YogaClass> newYogaClasses) {
        originalValues.clear();
        originalValues.addAll(newYogaClasses);
        filteredValues.clear();
        filteredValues.addAll(newYogaClasses);
        notifyDataSetChanged();
    }

    // Phương thức filter theo từ khóa (tìm kiếm)
    public void filter(String keyword) {
        filteredValues.clear();
        if (keyword.isEmpty()) {
            filteredValues.addAll(originalValues);
        } else {
            for (YogaClass yogaClass : originalValues) {
                if (yogaClass.getDayOfWeek().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredValues.add(yogaClass);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void showEditDialog(YogaClass yogaClass) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_class, null);

        // Get the correct views based on your layout
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Spinner spDayOfWeek = dialogView.findViewById(R.id.spDayOfWeek); // Spinner for day of the week
        EditText etTime = dialogView.findViewById(R.id.etTime);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etDuration = dialogView.findViewById(R.id.etDuration);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etPrice = dialogView.findViewById(R.id.etPrice); // Add the price field

        // Set initial values for all fields based on the yogaClass object
        etDescription.setText(yogaClass.getDescription());
        etTime.setText(yogaClass.getTime());
        etQuantity.setText(String.valueOf(yogaClass.getQuantity()));
        etDuration.setText(String.valueOf(yogaClass.getDuration()));
        etType.setText(yogaClass.getType());
        etPrice.setText(String.valueOf(yogaClass.getPrice())); // Set initial value for price

        // Set the initial value of the Spinner for DayOfWeek
        String[] daysOfWeek = context.getResources().getStringArray(R.array.days_of_week);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, daysOfWeek);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(adapter);

        // Set the spinner's selected item based on the yogaClass's dayOfWeek
        int position = adapter.getPosition(yogaClass.getDayOfWeek());
        spDayOfWeek.setSelection(position);

        // Determine if the class has instances
        boolean hasInstances = dbHelper.hasClassInstances(yogaClass.getId());


        if (hasInstances) {
            spDayOfWeek.setEnabled(false);
            etTime.setEnabled(false);
            etQuantity.setEnabled(false);
            etDuration.setEnabled(false);
            etType.setEnabled(false);
            etPrice.setEnabled(false);
        }

        new AlertDialog.Builder(context)
                .setTitle("Edit Class")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Update the description field
                    String updatedDescription = etDescription.getText().toString();
                    yogaClass.setDescription(updatedDescription);

                    // Only update other fields if the class has no instances
                    if (!hasInstances) {
                        String updatedDayOfWeek = spDayOfWeek.getSelectedItem().toString();
                        String updatedTime = etTime.getText().toString();
                        int updatedQuantity = Integer.parseInt(etQuantity.getText().toString());
                        int updatedDuration = Integer.parseInt(etDuration.getText().toString());
                        String updatedType = etType.getText().toString();
                        double updatedPrice = Double.parseDouble(etPrice.getText().toString());

                        // Kiểm tra giá trị âm cho price
                        if (updatedPrice < 0) {
                            Toast.makeText(context, "Price cannot be negative", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        yogaClass.setDayOfWeek(updatedDayOfWeek);
                        yogaClass.setTime(updatedTime);
                        yogaClass.setQuantity(updatedQuantity);
                        yogaClass.setDuration(updatedDuration);
                        yogaClass.setType(updatedType);
                        yogaClass.setPrice(updatedPrice);
                    }

                    // Save the updated yoga class to the database
                    updateClassInDatabase(yogaClass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    // Cập nhật lớp học trong cơ sở dữ liệu
    // Cập nhật lớp học trong cơ sở dữ liệu
    private void updateClassInDatabase(YogaClass yogaClass) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Cập nhật các giá trị khác nếu class không có instance
        if (!dbHelper.hasClassInstances(yogaClass.getId())) {
            values.put("dayOfWeek", yogaClass.getDayOfWeek());
            values.put("time", yogaClass.getTime());
            values.put("quantity", yogaClass.getQuantity());
            values.put("duration", yogaClass.getDuration());
            values.put("type", yogaClass.getType());
        }

        // Luôn cập nhật description
        values.put("description", yogaClass.getDescription());

        // Cập nhật lớp học trong SQLite
        try {
            db.update("YogaClass", values, "id = ?", new String[]{yogaClass.getId()});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Không đóng cơ sở dữ liệu ở đây
        }

        // Cập nhật lớp học trong Firebase
        firebaseRef.child(yogaClass.getId()).setValue(yogaClass)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Class updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update on Firebase", Toast.LENGTH_SHORT).show());

        notifyDataSetChanged();
    }

    private void deleteClass(YogaClass yogaClass) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this class?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    if (!db.isOpen()) {
                        db = dbHelper.getWritableDatabase();
                    }
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
                .setNegativeButton("No", null)
                .show();
    }

}
