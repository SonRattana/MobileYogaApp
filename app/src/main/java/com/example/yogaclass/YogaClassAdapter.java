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
        this.originalValues = new ArrayList<>(objects);
        this.filteredValues = new ArrayList<>(objects);
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
        ImageButton btnManage = rowView.findViewById(R.id.btnManage);

        YogaClass yogaClass = filteredValues.get(position);

        tvDayOfWeek.setText("Day of Week: " + yogaClass.getDayOfWeek());
        tvTime.setText("Time: " + yogaClass.getTime());
        tvQuantity.setText("Quantity: " + yogaClass.getQuantity());
        tvDuration.setText("Duration: " + yogaClass.getDuration() + " mins");
        tvType.setText("Type: " + yogaClass.getType());
        tvPrice.setText("Price: $" + yogaClass.getPrice());
        tvDescription.setText("Description: " + yogaClass.getDescription());


        btnManage.setOnClickListener(view -> {
            Intent intent = new Intent(context, ManageInstancesActivity.class);
            intent.putExtra("YOGA_CLASS_ID", yogaClass.getId());
            intent.putExtra("DAY_OF_WEEK", yogaClass.getDayOfWeek());
            intent.putExtra("courseId", yogaClass.getId());
            intent.putExtra("PRICE", yogaClass.getPrice());
            context.startActivity(intent);
        });


        btnEdit.setOnClickListener(view -> showEditDialog(yogaClass));


        btnDelete.setOnClickListener(view -> deleteClass(yogaClass));

        return rowView;
    }


    public void updateData(ArrayList<YogaClass> newYogaClasses) {
        originalValues.clear();
        originalValues.addAll(newYogaClasses);
        filteredValues.clear();
        filteredValues.addAll(newYogaClasses);
        notifyDataSetChanged();
    }


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

        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Spinner spDayOfWeek = dialogView.findViewById(R.id.spDayOfWeek);
        EditText etTime = dialogView.findViewById(R.id.etTime);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        Spinner spType = dialogView.findViewById(R.id.spType);
        EditText etDuration = dialogView.findViewById(R.id.etDuration);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);


        etDescription.setText(yogaClass.getDescription());
        etTime.setText(yogaClass.getTime());
        etQuantity.setText(String.valueOf(yogaClass.getQuantity()));
        etDuration.setText(String.valueOf(yogaClass.getDuration()));
        etPrice.setText(String.valueOf(yogaClass.getPrice()));


        String[] daysOfWeek = context.getResources().getStringArray(R.array.days_of_week);
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, daysOfWeek);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(dayAdapter);
        int dayPosition = dayAdapter.getPosition(yogaClass.getDayOfWeek());
        spDayOfWeek.setSelection(dayPosition);


        ArrayList<String> uniqueTypes = new ArrayList<>();
        for (YogaClass yc : originalValues) {
            if (!uniqueTypes.contains(yc.getType())) {
                uniqueTypes.add(yc.getType());
            }
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, uniqueTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);


        int typePosition = uniqueTypes.indexOf(yogaClass.getType());
        if (typePosition >= 0) {
            spType.setSelection(typePosition);
        }

        boolean hasInstances = dbHelper.hasClassInstances(yogaClass.getId());

        if (hasInstances) {
            spDayOfWeek.setEnabled(false);
            etTime.setEnabled(false);
            etQuantity.setEnabled(false);
            spType.setEnabled(false);
            etDuration.setEnabled(false);
            etPrice.setEnabled(false);
        }

        new AlertDialog.Builder(context)
                .setTitle("Edit Class")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {

                    String updatedDescription = etDescription.getText().toString();
                    yogaClass.setDescription(updatedDescription);

                    if (!hasInstances) {
                        String updatedDayOfWeek = spDayOfWeek.getSelectedItem().toString();
                        String updatedTime = etTime.getText().toString();
                        int updatedQuantity = Integer.parseInt(etQuantity.getText().toString());
                        int updatedDuration = Integer.parseInt(etDuration.getText().toString());
                        String updatedType = spType.getSelectedItem().toString();
                        double updatedPrice = Double.parseDouble(etPrice.getText().toString());

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

                    updateClassInDatabase(yogaClass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }






    private void updateClassInDatabase(YogaClass yogaClass) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();


        if (!dbHelper.hasClassInstances(yogaClass.getId())) {
            values.put("dayOfWeek", yogaClass.getDayOfWeek());
            values.put("time", yogaClass.getTime());
            values.put("quantity", yogaClass.getQuantity());
            values.put("duration", yogaClass.getDuration());
            values.put("type", yogaClass.getType());
        }


        values.put("description", yogaClass.getDescription());


        try {
            db.update("YogaClass", values, "id = ?", new String[]{yogaClass.getId()});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }


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
