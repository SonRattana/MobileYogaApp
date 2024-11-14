package com.example.yogaclass;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ClassInstanceAdapter extends ArrayAdapter<ClassInstance> {

    private List<ClassInstance> instanceList;
    private Context context;
    private DBHelper dbHelper;
    private DatabaseReference classRef;
    private String currentUserRole;

    public ClassInstanceAdapter(Context context, List<ClassInstance> instances, DBHelper dbHelper, DatabaseReference classRef, String currentUserRole) {
        super(context, 0, instances);
        this.context = context;
        this.instanceList = instances;
        this.dbHelper = dbHelper;
        this.classRef = classRef;
        this.currentUserRole = currentUserRole;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ClassInstance instance = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_instance, parent, false);
        }


        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTeacher = convertView.findViewById(R.id.tvTeacher);
        TextView tvPrice = convertView.findViewById(R.id.tvPrice);
        TextView tvComments = convertView.findViewById(R.id.tvComments);


        tvDate.setText(instance.getDate());
        tvTeacher.setText(instance.getTeacher());
        tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", instance.getPrice()));
        tvComments.setText(instance.getAdditionalComments());


        Button btnEdit = convertView.findViewById(R.id.btnEdit);
        Button btnDelete = convertView.findViewById(R.id.btnDelete);


        if ("Teacher".equals(currentUserRole)) {

            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        } else {

            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);


            btnEdit.setOnClickListener(v -> showEditDialog(instance));
            btnDelete.setOnClickListener(v -> deleteClassInstance(instance));
        }

        return convertView;
    }
    public void updateList(ArrayList<ClassInstance> newList) {
        instanceList.clear();
        instanceList.addAll(newList);
        notifyDataSetChanged();
    }

    private void showEditDialog(ClassInstance classInstance) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_instance, null);
        EditText etComments = dialogView.findViewById(R.id.etComments);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        Spinner spTeacher = dialogView.findViewById(R.id.spTeacher);
        tvDate.setText(classInstance.getDate());
        etComments.setText(classInstance.getAdditionalComments());
        loadTeachersIntoSpinner(spTeacher, classInstance.getTeacher());
        tvDate.setOnClickListener(v -> openDatePicker(tvDate));
        new AlertDialog.Builder(context)
                .setTitle("Edit Class Instance")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedDate = tvDate.getText().toString();
                    String updatedTeacher = spTeacher.getSelectedItem().toString();
                    String updatedComments = etComments.getText().toString();
                    classInstance.setDate(updatedDate);
                    classInstance.setTeacher(updatedTeacher);
                    classInstance.setAdditionalComments(updatedComments);
                    updateClassInstanceInDatabase(classInstance);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private void openDatePicker(TextView tvDate) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(selectedDate.getTime());

            tvDate.setText(formattedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    private boolean isCorrectDayOfWeek(String dayOfWeek, Calendar selectedDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        String selectedDayName = sdf.format(selectedDate.getTime());

        return dayOfWeek.equalsIgnoreCase(selectedDayName);
    }



    private void updateClassInstanceInDatabase(ClassInstance classInstance) {
        String instanceId = classInstance.getInstanceId();
        String updatedDate = classInstance.getDate();
        String updatedTeacher = classInstance.getTeacher();
        String updatedComments = classInstance.getAdditionalComments();
        double updatedPrice = classInstance.getPrice();
        int result = dbHelper.updateClassInstance(instanceId, updatedDate, updatedTeacher, updatedComments, updatedPrice);
        if (result != -1) {
            DatabaseReference instanceRef = classRef.child(classInstance.getYogaClassId()).child(instanceId);
            instanceRef.child("date").setValue(updatedDate);
            instanceRef.child("teacher").setValue(updatedTeacher);
            instanceRef.child("additionalComments").setValue(updatedComments);
            instanceRef.child("price").setValue(updatedPrice);
            Toast.makeText(context, "Class instance updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to update class instance", Toast.LENGTH_SHORT).show();
        }

        notifyDataSetChanged();
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(getContext(), ConnectivityManager.class);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void deleteClassInstance(ClassInstance classInstance) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this class instance?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (isNetworkAvailable()) {

                        deleteFromFirebaseAndSQLite(classInstance);
                    } else {

                        deleteFromSQLiteOnly(classInstance);
                        Toast.makeText(context, "No network. Only deleted locally. Will sync with Firebase when online.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFromFirebaseAndSQLite(ClassInstance classInstance) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ClassInstance", "id = ?", new String[]{classInstance.getInstanceId()});

        if (rowsDeleted > 0) {

            DatabaseReference instanceRef = classRef.child(classInstance.getYogaClassId()).child(classInstance.getInstanceId());
            instanceRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Class Instance deleted successfully from Firebase and SQLite!", Toast.LENGTH_SHORT).show();
                        instanceList.remove(classInstance);
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete from Firebase", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(context, "Failed to delete from SQLite", Toast.LENGTH_SHORT).show();
        }
        db.close();
    }


    private void deleteFromSQLiteOnly(ClassInstance classInstance) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ClassInstance", "id = ?", new String[]{classInstance.getInstanceId()});

        if (rowsDeleted > 0) {
            Toast.makeText(context, "Class Instance deleted successfully from SQLite!", Toast.LENGTH_SHORT).show();
            instanceList.remove(classInstance);
            notifyDataSetChanged();
        } else {
            Toast.makeText(context, "Failed to delete from SQLite", Toast.LENGTH_SHORT).show();
        }
        db.close();
    }




    private void loadTeachersIntoSpinner(Spinner spTeacher, String currentTeacher) {
        ArrayList<String> teacherList = dbHelper.getAllTeachersFromSQLite();


        if (teacherList.isEmpty()) {
            teacherList.add("No teachers available");
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, teacherList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTeacher.setAdapter(adapter);


        int position = teacherList.indexOf(currentTeacher);
        if (position >= 0) {
            spTeacher.setSelection(position);
        }
    }

    public void updateData(ArrayList<ClassInstance> newInstances) {
        instanceList.clear();
        instanceList.addAll(newInstances);
        notifyDataSetChanged();
    }


}