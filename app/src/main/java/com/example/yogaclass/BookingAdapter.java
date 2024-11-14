package com.example.yogaclass;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

public class BookingAdapter extends ArrayAdapter<Booking> {

    private Context context;
    private int resource;
    private List<Booking> bookings;

    public BookingAdapter(@NonNull Context context, int resource, @NonNull List<Booking> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.bookings = objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_booking, parent, false);
        }


        TextView tvUserEmail = convertView.findViewById(R.id.tvUserEmail);
        TextView tvYogaClassId = convertView.findViewById(R.id.tvYogaClassId);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTeacher = convertView.findViewById(R.id.tvTeacher);
        TextView tvPrice = convertView.findViewById(R.id.tvPrice);
        TextView tvAdditionalComments = convertView.findViewById(R.id.tvAdditionalComments);


        Booking booking = getItem(position);


        tvUserEmail.setText(booking.getUserEmail());


        SeasonDetails seasonDetails = booking.getSeasonDetails();
        if (seasonDetails != null) {
            tvYogaClassId.setText(seasonDetails.getId());
            tvDate.setText(seasonDetails.getDate());
            tvTeacher.setText(seasonDetails.getTeacher());


            tvPrice.setText("$" + String.valueOf(seasonDetails.getPrice()));

            tvAdditionalComments.setText(seasonDetails.getAdditionalComments());
        }

        return convertView;
    }
}



