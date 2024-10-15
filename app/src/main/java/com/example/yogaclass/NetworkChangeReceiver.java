//package com.example.yogaclass;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.net.ConnectivityManager;
//import android.net.NetworkCapabilities;
//import android.net.NetworkInfo;
//import android.os.Build;
//import android.widget.Toast;
//
//public class NetworkChangeReceiver extends BroadcastReceiver {
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        if (isNetworkAvailable(context)) {
//            Toast.makeText(context, "Network available. Syncing data...", Toast.LENGTH_SHORT).show();
//            ManageInstancesActivity.syncPendingData(context);  // Gọi phương thức đồng bộ khi có mạng
//        }
//    }
//
//    // Kiểm tra kết nối mạng
//    private boolean isNetworkAvailable(Context context) {
//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (cm != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
//                return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//            } else {
//                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//                return activeNetwork != null && activeNetwork.isConnected();
//            }
//        }
//        return false;
//    }
//}
