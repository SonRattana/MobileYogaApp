package com.example.yogaclass;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ClassInstanceAdapter extends ArrayAdapter<ClassInstance> {

    private List<ClassInstance> instanceList;
    private Context context;
    private DBHelper dbHelper;
    private DatabaseReference classRef;
    private String currentUserRole;  // "Admin" or "Teacher"

    public ClassInstanceAdapter(Context context, List<ClassInstance> instances, DBHelper dbHelper, DatabaseReference classRef, String currentUserRole) {
        super(context, 0, instances);
        this.context = context;
        this.instanceList = instances;
        this.dbHelper = dbHelper;
        this.classRef = classRef;
        this.currentUserRole = currentUserRole;  // Store the current user's role
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ClassInstance instance = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_instance, parent, false);
        }

        // Fetch views
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTeacher = convertView.findViewById(R.id.tvTeacher);
        TextView tvPrice = convertView.findViewById(R.id.tvPrice);
        TextView tvComments = convertView.findViewById(R.id.tvComments);

        // Bind data to views
        tvDate.setText(instance.getDate());
        tvTeacher.setText(instance.getTeacher());
        tvPrice.setText(String.valueOf(instance.getPrice()));
        tvComments.setText(instance.getAdditionalComments());

        // Fetch buttons
        Button btnEdit = convertView.findViewById(R.id.btnEdit);
        Button btnDelete = convertView.findViewById(R.id.btnDelete);

        // Show/Hide buttons based on the role
        if ("Teacher".equals(currentUserRole)) {
            // Teachers can only view, so hide Edit and Delete buttons
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        } else {
            // Admins can edit and delete
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            // Set up edit and delete button functionality for Admins
            btnEdit.setOnClickListener(v -> showEditDialog(instance));
            btnDelete.setOnClickListener(v -> deleteClassInstance(instance));
        }

        return convertView;
    }
    public void updateList(ArrayList<ClassInstance> newList) {
        instanceList.clear();               // Clear the existing list
        instanceList.addAll(newList);       // Add the new filtered list
        notifyDataSetChanged();             // Notify the adapter that the data has changed
    }

    // Hàm mở dialog chỉnh sửa ClassInstance
    private void showEditDialog(ClassInstance classInstance) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_instance, null);

        // Tham chiếu tới các trường nhập liệu trong layout
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etComments = dialogView.findViewById(R.id.etComments);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);

        // Lấy Spinner thay vì EditText cho teacher (vì bạn đã sử dụng Spinner để chọn teacher)
        Spinner spTeacher = dialogView.findViewById(R.id.spTeacher);

        // Đặt giá trị hiện tại của ClassInstance vào các trường nhập liệu
        etDate.setText(classInstance.getDate());
        etComments.setText(classInstance.getAdditionalComments());
        etPrice.setText(String.valueOf(classInstance.getPrice()));

        // Load teachers into spinner
        loadTeachersIntoSpinner(spTeacher, classInstance.getTeacher());

        new AlertDialog.Builder(context)
                .setTitle("Edit Class Instance")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Lấy giá trị từ các trường nhập liệu
                    String updatedDate = etDate.getText().toString();
                    String updatedTeacher = spTeacher.getSelectedItem().toString();  // Lấy từ spinner
                    String updatedComments = etComments.getText().toString();
                    double updatedPrice = Double.parseDouble(etPrice.getText().toString());

                    // Cập nhật đối tượng classInstance
                    classInstance.setDate(updatedDate);
                    classInstance.setTeacher(updatedTeacher);
                    classInstance.setAdditionalComments(updatedComments);
                    classInstance.setPrice(updatedPrice);

                    // Cập nhật vào cơ sở dữ liệu
                    updateClassInstanceInDatabase(classInstance);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Hàm cập nhật ClassInstance trong SQLite và Firebase
    private void updateClassInstanceInDatabase(ClassInstance classInstance) {
        String instanceId = classInstance.getInstanceId();
        String updatedDate = classInstance.getDate();
        String updatedTeacher = classInstance.getTeacher();
        String updatedComments = classInstance.getAdditionalComments();
        double updatedPrice = classInstance.getPrice();

        // Cập nhật buổi học trong SQLite
        int result = dbHelper.updateClassInstance(instanceId, updatedDate, updatedTeacher, updatedComments, updatedPrice);

        if (result != -1) {
            // Nếu cập nhật thành công trong SQLite, tiếp tục cập nhật Firebase
            DatabaseReference instanceRef = classRef.child(classInstance.getYogaClassId()).child(instanceId);
            instanceRef.child("date").setValue(updatedDate);
            instanceRef.child("teacher").setValue(updatedTeacher);
            instanceRef.child("additionalComments").setValue(updatedComments);
            instanceRef.child("price").setValue(updatedPrice);

            Toast.makeText(context, "Class instance updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to update class instance", Toast.LENGTH_SHORT).show();
        }

        notifyDataSetChanged();  // Cập nhật danh sách hiển thị
    }
    // Hàm kiểm tra kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(getContext(), ConnectivityManager.class);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Hàm xóa ClassInstance từ SQLite và Firebase
    private void deleteClassInstance(ClassInstance classInstance) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this class instance?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Người dùng nhấn "Yes", thực hiện việc xóa ClassInstance

                    // Xóa dữ liệu trong SQLite
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("ClassInstance", "id = ?", new String[]{classInstance.getInstanceId()});

                    // Tham chiếu Firebase với yogaClassId và instanceId
                    DatabaseReference instanceRef = classRef.child(classInstance.getYogaClassId()).child(classInstance.getInstanceId());

                    // Xóa dữ liệu khỏi Firebase
                    instanceRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Class Instance deleted successfully from Firebase!", Toast.LENGTH_SHORT).show();
                                instanceList.remove(classInstance);  // Xóa khỏi danh sách hiển thị
                                notifyDataSetChanged();  // Cập nhật lại giao diện
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to delete from Firebase", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Load teachers into the Spinner
    private void loadTeachersIntoSpinner(Spinner spTeacher, String currentTeacher) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        ArrayList<String> teacherList = new ArrayList<>();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                teacherList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if ("Teacher".equals(role)) {
                        String teacherName = snapshot.child("name").getValue(String.class);
                        teacherList.add(teacherName);
                    }
                }
                if (teacherList.isEmpty()) {
                    teacherList.add("No teachers available");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, teacherList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spTeacher.setAdapter(adapter);

                int position = teacherList.indexOf(currentTeacher);
                if (position >= 0) {
                    spTeacher.setSelection(position);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Failed to load teachers", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
