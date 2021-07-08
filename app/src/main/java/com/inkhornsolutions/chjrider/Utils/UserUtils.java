package com.inkhornsolutions.chjrider.Utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inkhornsolutions.chjrider.EventBus.NotifyToRiderEvent;
import com.inkhornsolutions.chjrider.Models.FCMSendData;
import com.inkhornsolutions.chjrider.Models.TokenModel;
import com.inkhornsolutions.chjrider.Remote.IFCMService;
import com.inkhornsolutions.chjrider.Remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UserUtils {

    private static final String id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

    public static void updateToken(Context context, String token) {


        TokenModel tokenModel = new TokenModel(token);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");

        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(tokenModel)
                    .addOnSuccessListener(aVoid -> {

//                        Toast.makeText(context, "Token successfully submitted to database!", Toast.LENGTH_SHORT).show();


                    })
                    .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    public static void sendDeclineRequest(View view, Context context, String key) {

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                    Map<String, String> notificationdata = new HashMap<>();
                    notificationdata.put("title", "Decline");
                    notificationdata.put("body", "This message represent action driver decline");
                    notificationdata.put("DriverKey", id);

                    FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                    compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {
                                if (fcmResponse.getSuccess() == 0){
                                    compositeDisposable.clear();
                                    Snackbar.make(view, "Decline Failed", Snackbar.LENGTH_LONG).show();
                                }
                                else {
                                    Snackbar.make(view, "You have decline this request", Snackbar.LENGTH_LONG).show();

                                }

                            }, throwable -> {
                                compositeDisposable.clear();
                                Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                            }));
                }
                else {
                    compositeDisposable.clear();
                    Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                compositeDisposable.clear();
                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });

    }

    public static void sendAcceptRequestToRider(View view, Context context, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                    Map<String, String> notificationdata = new HashMap<>();
                    notificationdata.put("title", "Accept");
                    notificationdata.put("body", "This message represent action driver accept");
                    notificationdata.put("DriverKey", id);
                    notificationdata.put("TripKey", tripNumberId);

                    FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                    compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {
                                if (fcmResponse.getSuccess() == 0){
                                    compositeDisposable.clear();
                                    Snackbar.make(view, "Accept Failed", Snackbar.LENGTH_LONG).show();
                                }

                            }, throwable -> {
                                compositeDisposable.clear();
                                Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                            }));
                }
                else {
                    compositeDisposable.clear();
                    Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                compositeDisposable.clear();
                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public static void sendNotifyToRider(Context context, View view, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                    Map<String, String> notificationdata = new HashMap<>();
                    notificationdata.put("title", "DriverArrived");
                    notificationdata.put("body", "Your driver arrived!");
                    notificationdata.put("DriverKey", id);
                    notificationdata.put("TripKey", tripNumberId);

                    FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                    compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {
                                if (fcmResponse.getSuccess() == 0){
                                    compositeDisposable.clear();
                                    Snackbar.make(view, "Accept Failed", Snackbar.LENGTH_LONG).show();
                                }
                                else {
                                    EventBus.getDefault().postSticky(new NotifyToRiderEvent());
                                }

                            }, throwable -> {
                                compositeDisposable.clear();
                                Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                            }));
                }
                else {
                    compositeDisposable.clear();
                    Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                compositeDisposable.clear();
                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public static void sendDeclineAndRemoveRiderRequest(View view, Context context, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance().getReference("Trips")
                .child(tripNumberId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {

                    FirebaseDatabase.getInstance()
                            .getReference("Tokens")
                            .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()){
                                TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                                Map<String, String> notificationdata = new HashMap<>();
                                notificationdata.put("title", "DeclineAndRemoveTrip");
                                notificationdata.put("body", "Your driver declined trip!");
                                notificationdata.put("DriverKey", id);

                                notificationdata.put("RiderKey", key);

                                FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                                compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(fcmResponse -> {
                                            if (fcmResponse.getSuccess() == 0){
                                                compositeDisposable.clear();
                                                Snackbar.make(view, "Accept Failed", Snackbar.LENGTH_LONG).show();
                                            }
                                            else {
                                                EventBus.getDefault().postSticky(new NotifyToRiderEvent());
                                            }

                                        }, throwable -> {
                                            compositeDisposable.clear();
                                            Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                                        }));
                            }
                            else {
                                compositeDisposable.clear();
                                Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            compositeDisposable.clear();
                            Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });

                }).addOnFailureListener(e -> {
            Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
        });
    }

    public static void sendCompleteTripToRider(View view, Context context, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                            .getReference("Tokens")
                            .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()){
                                TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                                Map<String, String> notificationdata = new HashMap<>();
                                notificationdata.put("title", "DriverCompleteTrip");
                                notificationdata.put("body", "Ride Completed!");
                                notificationdata.put("TripKey", tripNumberId);


                                notificationdata.put("RiderKey", key);

                                FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                                compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(fcmResponse -> {
                                            if (fcmResponse.getSuccess() == 0){
                                                compositeDisposable.clear();
                                                Snackbar.make(view, "Complete trip message send failed!", Snackbar.LENGTH_LONG).show();
                                            }
                                            else {
                                                Snackbar.make(view, "Order has been delivered!", Snackbar.LENGTH_LONG).show();
                                            }

                                        }, throwable -> {
                                            compositeDisposable.clear();
                                            Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                                        }));
                            }
                            else {
                                compositeDisposable.clear();
                                Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            compositeDisposable.clear();
                            Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });

    }

    public static void sendTimeOverNotification(View view, Context context, String key) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                    Map<String, String> notificationdata = new HashMap<>();
                    notificationdata.put("title", "TimeOver");
                    notificationdata.put("body", "Please HarryUp!");


                    notificationdata.put("RiderKey", key);

                    FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                    compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {
                                if (fcmResponse.getSuccess() == 0){
                                    compositeDisposable.clear();
                                    Snackbar.make(view, "timeUp message send failed!", Snackbar.LENGTH_LONG).show();
                                }
                                else {
                                    Snackbar.make(view, "TimeUp!", Snackbar.LENGTH_LONG).show();
                                }

                            }, throwable -> {
                                compositeDisposable.clear();
                                Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                            }));
                }
                else {
                    compositeDisposable.clear();
                    Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                compositeDisposable.clear();
                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public static void sendOrderCompletedNotification(View view, Context context, String key) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);

                    Map<String, String> notificationdata = new HashMap<>();
                    notificationdata.put("title", "OrderCompleted");
                    notificationdata.put("body", "Congrats. Your order has been completed successfully!");
                    notificationdata.put("RiderKey", key);

                    FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(), notificationdata);
                    compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {
                                if (fcmResponse.getSuccess() == 0){
                                    compositeDisposable.clear();
                                    Snackbar.make(view, "Order completed message send failed!", Snackbar.LENGTH_LONG).show();
                                }
                                else {
                                    Snackbar.make(view, "Order completed!", Snackbar.LENGTH_LONG).show();
                                }

                            }, throwable -> {
                                compositeDisposable.clear();
                                Snackbar.make(view, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                            }));
                }
                else {
                    compositeDisposable.clear();
                    Snackbar.make(view, "Token not found!", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                compositeDisposable.clear();
                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
