package com.example.yogaclass;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class CustomerBookingsActivity extends AppCompatActivity {

    ListView lvBookings;
    FirebaseDatabase database;
    DatabaseReference bookingsRef;
    ArrayList<Booking> bookingsList;
    BookingAdapter bookingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_bookings);

        // Liên kết với ListView trong giao diện
        lvBookings = findViewById(R.id.lvBookings);

        // Khởi tạo Firebase và tham chiếu đến "bookings"
        database = FirebaseDatabase.getInstance();
        bookingsRef = database.getReference("bookings");

        // Khởi tạo danh sách và adapter
        bookingsList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(this, R.layout.list_item_booking, bookingsList);
        lvBookings.setAdapter(bookingAdapter);

        // Load dữ liệu từ Firebase
        loadBookings();
    }

    private void loadBookings() {
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                bookingsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Booking booking = snapshot.getValue(Booking.class);
                    bookingsList.add(booking);
                }
                bookingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
