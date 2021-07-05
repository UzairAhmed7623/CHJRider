package com.inkhornsolutions.chjrider.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.inkhornsolutions.chjrider.Common.Common;
import com.inkhornsolutions.chjrider.EventBus.DriverRequestReceived;
import com.inkhornsolutions.chjrider.Utils.UserUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;

public class MyFirebaseService extends FirebaseMessagingService {

    private static final String FCM_CHANNEL_ID = "1001";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        UserUtils.updateToken(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("TAG", "Message recieved from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0){
            Log.d("TAG", "Data received: " + remoteMessage.getData().toString());

            String title = remoteMessage.getData().get("title");
            String riderKey = remoteMessage.getData().get("RiderKey");
            String PickupLocation = remoteMessage.getData().get("PickupLocation");
            String PickupLocationString = remoteMessage.getData().get("PickupLocationString");
            String DestinationLocation = remoteMessage.getData().get("DestinationLocation");
            String DestinationLocationString = remoteMessage.getData().get("DestinationLocationString");
            String dropOffUserId = remoteMessage.getData().get("dropOffUserId");
            String orderId = remoteMessage.getData().get("orderRefNumber");

            Log.d("TAG", "Title: "+title+"riderKey: "+riderKey+"PickupLocation: "+PickupLocation
                    +"PickupLocationString: "+PickupLocationString+"DestinationLocation: "+DestinationLocation
                    +"DestinationLocationString: "+DestinationLocationString);

            if (title.equals("RequestDriver")) {
                Log.d("TAG", "Title: " + title);

                DriverRequestReceived driverRequestReceived = new DriverRequestReceived();
                driverRequestReceived.setKey(riderKey);
                driverRequestReceived.setPickupLocation(PickupLocation);
                driverRequestReceived.setPickupLocationString(PickupLocationString);
                driverRequestReceived.setDestinationLocation(DestinationLocation);
                driverRequestReceived.setDestinationLocationString(DestinationLocationString);
                driverRequestReceived.setDropOffUserId(dropOffUserId);
                driverRequestReceived.setOrderId(orderId);

                SharedPreferences sharedPref = getSharedPreferences("driverRequestReceived", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                Gson gson = new Gson();
                String json = gson.toJson(driverRequestReceived);
                editor.putString("driverRequestReceived", json);
                editor.apply();

                EventBus.getDefault().postSticky(driverRequestReceived);
                Log.d("TAG", "Chala");

            }
            else {
                Intent intent = new Intent(this, MyFirebaseService.class);
                Log.d("TAG", "Chala");

                Common.showNotification(this, new Random().nextInt(), title, riderKey, intent);
            }


        }
    }

}
