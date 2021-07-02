package com.inkhornsolutions.chjrider;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.evernote.android.state.StateSaver;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.inkhornsolutions.chjrider.Common.Common;
import com.inkhornsolutions.chjrider.EventBus.DriverRequestRecieved;
import com.inkhornsolutions.chjrider.Models.DriverInfoModel;
import com.inkhornsolutions.chjrider.Models.RiderModel;
import com.inkhornsolutions.chjrider.Models.TripPlanModel;
import com.inkhornsolutions.chjrider.Utils.UserUtils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String DIRECTION_API_KEY = "AIzaSyDl7YXtTZQNBkthV3PjFS0fQOKvL8SIR7k";
    private GoogleMap mgoogleMap;
    private MaterialButton btnGoOnline, btnIgnoreJob, btnAcceptJob, btnCancelJob, btnCall, btnPickup, btnDropOff, btnCancelDropOffJob, btnDropOffCall;
    private TextView tvMoneyEarned, tvHoursSpentOnline, tvTotalDistanceCovered, tvTotalJobs;
    private TextView tvPickUp, tvDropOff, tvPickUpDistance, tvEstTime;
    private TextView tvPickupAddress, tvOrderRefNo, tvDistanceFromPickup, tvEstimatedFare, tvETA;
    private TextView tvDropOffAddress, tvDropOffOrderRefNo, tvDistanceFromDropOff, tvEstimatedFareDropOff;
    private ImageButton btnCallToolbar;
    private TextView pickupPlace, dropoffPlace, tvAddress, tvArrivalTimeToolbar;
    private TextView tvPickupAddressRideSummary, tvDropOffAddressRideSummary, tvOrderRefNoRideSummary, tvDistanceRideSummary, tvEstimatedFareRideSummary;
    private MaterialButton btnContinueRide, btnStopRiding;
    private ConstraintLayout yesterdayLayout, newOrderLayout, gotoPickupLayout, gotoDropOffLayout, toolbar_pickup_and_dropoff_layout, rideSummaryLayout;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    GeoFire geoFire;
    GeoQuery geoQuery;
    private GeoFire pickupGeoFire, destinationGeoFire;
    private GeoQuery pickupGeoQuery, destinationGeoQuery;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private DatabaseReference driverLocationRef, currentUserRef;
    private String id;
    private String tripNumberId = "";
    private boolean isTripStart = false;

    private DriverRequestRecieved driverRequestReceived;
    private TripPlanModel tripPlanModel;
    private Polyline blackPolyLine, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;

    private GeoQueryEventListener pickupGeoQueryEventListner = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {

            toolbar_pickup_and_dropoff_layout.setVisibility(View.GONE);
            gotoDropOffLayout.setVisibility(View.VISIBLE);
            tvDropOffAddress.setText(tvDropOff.getText().toString());
            tvDropOffOrderRefNo.setText(driverRequestReceived.getOrderRefNumber());

            LatLng pickupLatLng = new LatLng(Double.parseDouble(driverRequestReceived.getPickupLocation().split(",")[0]),
                    Double.parseDouble(driverRequestReceived.getPickupLocation().split(",")[1]));
            LatLng destinationLatLng = new LatLng(Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[0]),
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[1]));

            GoogleDirection.withServerKey(DIRECTION_API_KEY)
                    .from(pickupLatLng)
                    .to(destinationLatLng)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(@Nullable Direction direction) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            Info distanceInfo = leg.getDistance();
                            Info durationInfo = leg.getDuration();
                            String distance = distanceInfo.getText();
                            String duration = durationInfo.getText();

                            tvDistanceFromDropOff.setText(distance);
                            tvETA.setText(duration);
                        }

                        @Override
                        public void onDirectionFailure(@NonNull Throwable t) {
                            Log.d("address: ", "Chala2");

                            Snackbar.make(findViewById(android.R.id.content), t.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });

            tvEstimatedFareDropOff.setText(tvEstimatedFare.getText().toString());

            UserUtils.sendNotifyToRider(MainActivity.this, gotoPickupLayout, key, tripNumberId);

            if (pickupGeoQuery != null) {
                if (pickupGeoFire != null) {
                    pickupGeoFire.removeLocation(key);
                }
                pickupGeoFire = null;
                pickupGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {
        }

        @Override
        public void onGeoQueryReady() {
        }

        @Override
        public void onGeoQueryError(DatabaseError error) {
        }
    };

    private GeoQueryEventListener destinationGeoQueryEventListner = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {

            toolbar_pickup_and_dropoff_layout.setVisibility(View.GONE);
            rideSummaryLayout.setVisibility(View.VISIBLE);

            UserUtils.sendAcceptRequestToRider(rideSummaryLayout, MainActivity.this, driverRequestReceived.getKey(), tripNumberId);

            LatLng pickUpLatLng = new LatLng(
                    Double.parseDouble(tripPlanModel.getOrigin().split(",")[0]),
                    Double.parseDouble(tripPlanModel.getOrigin().split(",")[1]));

            LatLng dropOffLatLng = new LatLng(
                    Double.parseDouble(tripPlanModel.getDestination().split(",")[0]),
                    Double.parseDouble(tripPlanModel.getDestination().split(",")[1]));

            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> pickUp;
            List<Address> dropOff;
            try {
                pickUp = geocoder.getFromLocation(pickUpLatLng.latitude, pickUpLatLng.longitude, 1);
                String pickUpString = pickUp.get(0).getAddressLine(0);
                tvPickupAddressRideSummary.setText(pickUpString);

                dropOff = geocoder.getFromLocation(dropOffLatLng.latitude, dropOffLatLng.longitude, 1);
                String dropOffString = dropOff.get(0).getAddressLine(0);
                tvDropOffAddressRideSummary.setText(dropOffString);
            }
            catch (Exception e){
                Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
            }

            tvOrderRefNoRideSummary.setText(driverRequestReceived.getOrderRefNumber());
            FirebaseDatabase.getInstance().getReference("Trips").child(tripNumberId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        String distancePickup = snapshot.child("distancePickup").getValue().toString().replace("km","");
                        String dropOffDistance = snapshot.child("dropOffDistance").getValue().toString().replace("km","");;

                        Log.d("address1" ,""+distancePickup+"");
                        Log.d("address1" ,""+dropOffDistance+"");


                        double totalDistance = Double.parseDouble(distancePickup) + Double.parseDouble(dropOffDistance);
                        tvDistanceRideSummary.setText("" + totalDistance + " km");
                    }
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });

            tvEstimatedFareRideSummary.setText(tvEstimatedFareDropOff.getText().toString());

            HashMap<String, Object> tripDone = new HashMap<>();
            tripDone.put("done", true);
            FirebaseDatabase.getInstance().getReference("Trips").child(tripNumberId).updateChildren(tripDone)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<Void> task) {

                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {

                        }
                    });

            tripNumberId = "";
            isTripStart = false;


            if (destinationGeoQuery != null) {
                destinationGeoFire.removeLocation(key);
                destinationGeoFire = null;
                destinationGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    @SuppressLint("NonMatchingStateSaverCalls")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StateSaver.restoreInstanceState(this, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
//        id = firebaseAuth.getCurrentUser().getUid();
        id = "WhaMIBVZyxUWtN9EPrx16d9Dzoo2";

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.myColorSecond));

        //yesterday layout
        btnGoOnline = (MaterialButton) findViewById(R.id.btnGoOnline);
        tvMoneyEarned = (TextView) findViewById(R.id.tvMoneyEarned);
        tvHoursSpentOnline = (TextView) findViewById(R.id.tvHoursSpentOnline);
        tvTotalDistanceCovered = (TextView) findViewById(R.id.tvTotalDistanceCovered);
        tvTotalJobs = (TextView) findViewById(R.id.tvTotalJobs);
        yesterdayLayout = (ConstraintLayout) findViewById(R.id.yesterdayLayout);

        //new order layout
        btnIgnoreJob = (MaterialButton) findViewById(R.id.btnIgnoreJob);
        btnAcceptJob = (MaterialButton) findViewById(R.id.btnAcceptJob);
        tvPickUp = (TextView) findViewById(R.id.tvPickUp);
        tvDropOff = (TextView) findViewById(R.id.tvDropOff);
        tvPickUpDistance = (TextView) findViewById(R.id.tvPickUpDistance);
        tvEstTime = (TextView) findViewById(R.id.tvEstTime);
        newOrderLayout = (ConstraintLayout) findViewById(R.id.newOrderLayout);

        //go to pickup layout
        btnCancelJob = (MaterialButton) findViewById(R.id.btnCancelJob);
        btnCall = (MaterialButton) findViewById(R.id.btnCall);
        btnPickup = (MaterialButton) findViewById(R.id.btnPickup);
        tvPickupAddress = (TextView) findViewById(R.id.tvPickupAddress);
        tvOrderRefNo = (TextView) findViewById(R.id.tvOrderRefNo);
        tvDistanceFromPickup = (TextView) findViewById(R.id.tvDistanceFromPickup);
        tvEstimatedFare = (TextView) findViewById(R.id.tvEstimatedFare);
        tvETA = (TextView) findViewById(R.id.tvETA);
        gotoPickupLayout = (ConstraintLayout) findViewById(R.id.gotoPickupLayout);

        //go to drop off layout
        btnDropOff = (MaterialButton) findViewById(R.id.btnDropOff);
        btnCancelDropOffJob = (MaterialButton) findViewById(R.id.btnCancelDropOffJob);
        btnDropOffCall = (MaterialButton) findViewById(R.id.btnDropOffCall);
        tvDropOffAddress = (TextView) findViewById(R.id.tvDropOffAddress);
        tvDropOffOrderRefNo = (TextView) findViewById(R.id.tvDropOffOrderRefNo);
        tvDistanceFromDropOff = (TextView) findViewById(R.id.tvDistanceFromDropOff);
        tvEstimatedFareDropOff = (TextView) findViewById(R.id.tvEstimatedFareDropOff);
        tvETA = (TextView) findViewById(R.id.tvETA);
        gotoDropOffLayout = (ConstraintLayout) findViewById(R.id.gotoDropOffLayout);

        //toolbar_pickup_layout
        toolbar_pickup_and_dropoff_layout = (ConstraintLayout) findViewById(R.id.toolbar_pickup_and_dropoff_layout);
        pickupPlace = (TextView) findViewById(R.id.pickupPlace);
        dropoffPlace = (TextView) findViewById(R.id.dropoffPlace);
        tvAddress = (TextView) findViewById(R.id.tvAddress);
        tvArrivalTimeToolbar = (TextView) findViewById(R.id.tvArrivalTimeToolbar);
        btnCallToolbar = (ImageButton) findViewById(R.id.btnCallToolbar);

        //Ride Summary layout
        rideSummaryLayout = (ConstraintLayout) findViewById(R.id.rideSummaryLayout);
        tvPickupAddressRideSummary = (TextView) findViewById(R.id.tvPickupAddressRideSummary);
        tvDropOffAddressRideSummary = (TextView) findViewById(R.id.tvDropOffAddressRideSummary);
        tvOrderRefNoRideSummary = (TextView) findViewById(R.id.tvOrderRefNoRideSummary);
        tvDistanceRideSummary = (TextView) findViewById(R.id.tvDistanceRideSummary);
        tvEstimatedFareRideSummary = (TextView) findViewById(R.id.tvEstimatedFareRideSummary);
        btnContinueRide = (MaterialButton) findViewById(R.id.btnContinueRide);
        btnStopRiding = (MaterialButton) findViewById(R.id.btnStopRiding);

        btnGoOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yesterdayLayout.setVisibility(View.GONE);

                updateFirebaseToken();

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mgoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));

                                buildLocationRequest();
                                buildLocationCallBack();
                                updateLocation();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "dosra wala" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnCancelJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yesterdayLayout.setVisibility(View.VISIBLE);
                newOrderLayout.setVisibility(View.GONE);
                gotoPickupLayout.setVisibility(View.GONE);
            }
        });

        btnIgnoreJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (driverRequestReceived != null) {
                    if (TextUtils.isEmpty(tripNumberId)) {

                        newOrderLayout.setVisibility(View.GONE);
                        mgoogleMap.clear();
                        UserUtils.sendDeclineRequest(newOrderLayout, MainActivity.this, driverRequestReceived.getKey());
                        driverRequestReceived = null;
                        HashMap<String, Object> tripCancel = new HashMap<>();
                        tripCancel.put("cancel", true);
                        FirebaseDatabase.getInstance().getReference("Trips").child(tripNumberId).updateChildren(tripCancel).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull @NotNull Task<Void> task) {

                            }
                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {

                                    }
                                });

                    } else {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {

                            HashMap<String, Object> tripCancel = new HashMap<>();
                            tripCancel.put("cancel", true);
                            FirebaseDatabase.getInstance().getReference("Trips").child(tripNumberId).updateChildren(tripCancel).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull @NotNull Task<Void> task) {

                                }
                            })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull @NotNull Exception e) {

                                        }
                                    });

                            newOrderLayout.setVisibility(View.GONE);
                            mgoogleMap.clear();
                            UserUtils.sendDeclineAndRemoveRiderRequest(newOrderLayout, MainActivity.this,
                                    driverRequestReceived.getKey(), tripNumberId);

                            tripNumberId = "";
                            driverRequestReceived = null;

                            makeDriverOnline(location);

                        }).addOnFailureListener(e -> Snackbar.make(newOrderLayout, e.getMessage(), Snackbar.LENGTH_LONG).show());
                    }
                }


            }
        });

        btnPickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blackPolyLine != null) blackPolyLine.remove();
                if (greyPolyline != null) greyPolyline.remove();

                gotoPickupLayout.setVisibility(View.GONE);
                toolbar_pickup_and_dropoff_layout.setVisibility(View.VISIBLE);
                pickupPlace.setVisibility(View.VISIBLE);
                dropoffPlace.setVisibility(View.GONE);

                if (driverRequestReceived != null) {
                    LatLng pickupLatLng = new LatLng(
                            Double.parseDouble(driverRequestReceived.getPickupLocation().split(",")[0]),
                            Double.parseDouble(driverRequestReceived.getPickupLocation().split(",")[1]));

                    mgoogleMap.addMarker(new MarkerOptions()
                            .position(pickupLatLng)
                            .title(driverRequestReceived.getPickupLocationString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                    //draw path
                    drawPathFromCurrentLocation(driverRequestReceived.getPickupLocation(), driverRequestReceived.getDestinationLocation());
                }

                gotoPickupLayout.setVisibility(View.GONE);
            }
        });

        btnDropOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blackPolyLine != null) blackPolyLine.remove();
                if (greyPolyline != null) greyPolyline.remove();

                gotoDropOffLayout.setVisibility(View.GONE);
                toolbar_pickup_and_dropoff_layout.setVisibility(View.VISIBLE);
                dropoffPlace.setVisibility(View.VISIBLE);
                pickupPlace.setVisibility(View.GONE);

                if (driverRequestReceived != null) {
                    LatLng destinationLatLng = new LatLng(
                            Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[0]),
                            Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[1]));

                    mgoogleMap.addMarker(new MarkerOptions()
                            .position(destinationLatLng)
                            .title(driverRequestReceived.getDestinationLocationString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    //draw path
                    drawPathFromCurrentLocation(driverRequestReceived.getDestinationLocation(), driverRequestReceived.getDestinationLocation());
                }

                gotoPickupLayout.setVisibility(View.GONE);
            }
        });

        btnContinueRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rideSummaryLayout.setVisibility(View.GONE);
                yesterdayLayout.setVisibility(View.GONE);

                mgoogleMap.clear();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            makeDriverOnline(location);
                        }
                    }
                });
            }
        });

        btnStopRiding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rideSummaryLayout.setVisibility(View.GONE);
                yesterdayLayout.setVisibility(View.VISIBLE);
                mgoogleMap.clear();
                if (currentUserRef != null){
                    currentUserRef.removeValue();
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

    }

    private void drawPathFromCurrentLocation(String PickupLocation, String destinationLocation) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                try {

                    LatLng originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    LatLng pickUpLatLng = new LatLng(
                            Double.parseDouble(PickupLocation.split(",")[0]),
                            Double.parseDouble(PickupLocation.split(",")[1]));

                    Log.d("address: ", "Chala1");

                    Log.d("address: ", "" + pickUpLatLng);

                    GoogleDirection.withServerKey(DIRECTION_API_KEY)
                            .from(originLatLng)
                            .to(pickUpLatLng)
                            .execute(new DirectionCallback() {
                                @Override
                                public void onDirectionSuccess(@Nullable Direction direction) {
                                    Route route = direction.getRouteList().get(0);
                                    Leg leg = route.getLegList().get(0);

                                    String time = leg.getDuration().getText();
                                    String address = leg.getEndAddress();
                                    tvArrivalTimeToolbar.setText(time);
                                    tvAddress.setText(address);

                                    polylineList = leg.getDirectionPoint();


                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    greyPolyline = mgoogleMap.addPolyline(polylineOptions);

                                    blackPolylineOptions = new PolylineOptions();
                                    blackPolylineOptions.color(Color.BLACK);
                                    blackPolylineOptions.width(5);
                                    blackPolylineOptions.startCap(new SquareCap());
                                    blackPolylineOptions.jointType(JointType.ROUND);
                                    blackPolylineOptions.addAll(polylineList);
                                    greyPolyline = mgoogleMap.addPolyline(polylineOptions);
                                    blackPolyLine = mgoogleMap.addPolyline(blackPolylineOptions);

                                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                            .include(originLatLng)
                                            .include(pickUpLatLng)
                                            .build();

                                    LatLng DestinationLatLng = new LatLng(
                                            Double.parseDouble(destinationLocation.split(",")[0]),
                                            Double.parseDouble(destinationLocation.split(",")[1]));

                                    createGeoFireDestinationLocation(driverRequestReceived.getKey(), DestinationLatLng);

                                    mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mgoogleMap.moveCamera(CameraUpdateFactory.zoomTo(mgoogleMap.getCameraPosition().zoom - 1));

                                }
                                @Override
                                public void onDirectionFailure(@NonNull Throwable t) {
                                    Log.d("address: ", "Chala2");

                                    Snackbar.make(findViewById(android.R.id.content), t.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });
                } catch (Exception e) {
                    Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                    Log.d("address: ", "Chala3");
                }
            }
        }).addOnFailureListener(e -> {
            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
            Log.d("address: ", "Chala4");

        });
    }

    private void createGeoFireDestinationLocation(String key, LatLng destinationLatLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TripDestinationLocation");
        destinationGeoFire = new GeoFire(ref);
        destinationGeoFire.setLocation(key, new GeoLocation(destinationLatLng.latitude, destinationLatLng.longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDriverRequestReceived(DriverRequestRecieved event) {

        driverRequestReceived = event;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                try {

                    LatLng originLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    LatLng pickUpLatLng = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                            Double.parseDouble(event.getPickupLocation().split(",")[1]));

                    LatLng dropOffLatLng = new LatLng(Double.parseDouble(event.getDestinationLocation().split(",")[0]),
                            Double.parseDouble(event.getDestinationLocation().split(",")[1]));
                    Log.d("address: ", "Chala1");

                    Log.d("address: ", "" + pickUpLatLng);

                    GoogleDirection.withServerKey(DIRECTION_API_KEY)
                            .from(originLatLng)
                            .to(pickUpLatLng)
                            .execute(new DirectionCallback() {
                                @Override
                                public void onDirectionSuccess(@Nullable Direction direction) {
                                    Route route = direction.getRouteList().get(0);
                                    Leg leg = route.getLegList().get(0);

                                    polylineList = leg.getDirectionPoint();


                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    greyPolyline = mgoogleMap.addPolyline(polylineOptions);

                                    blackPolylineOptions = new PolylineOptions();
                                    blackPolylineOptions.color(Color.BLACK);
                                    blackPolylineOptions.width(5);
                                    blackPolylineOptions.startCap(new SquareCap());
                                    blackPolylineOptions.jointType(JointType.ROUND);
                                    blackPolylineOptions.addAll(polylineList);
                                    greyPolyline = mgoogleMap.addPolyline(polylineOptions);
                                    blackPolyLine = mgoogleMap.addPolyline(blackPolylineOptions);

                                    ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 100);
                                    valueAnimator.setDuration(1000);
                                    valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                                    valueAnimator.setInterpolator(new LinearInterpolator());
                                    valueAnimator.addUpdateListener(animation -> {
                                        List<LatLng> points = greyPolyline.getPoints();
                                        int percentValue = (int) animation.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue / 100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyLine.setPoints(p);
                                    });

                                    valueAnimator.start();

                                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                            .include(originLatLng)
                                            .include(pickUpLatLng)
                                            .build();

                                    //Add car icon for origin

                                    Info distanceInfo = leg.getDistance();
                                    Info durationInfo = leg.getDuration();
                                    String pickupDistance = distanceInfo.getText();
                                    String pickupDuration = durationInfo.getText();

                                    LatLng pickupLatLng = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                                            Double.parseDouble(event.getPickupLocation().split(",")[1]));

                                    LatLng destinationLatLng = new LatLng(Double.parseDouble(event.getDestinationLocation().split(",")[0]),
                                            Double.parseDouble(event.getDestinationLocation().split(",")[1]));

                                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    List<Address> pickUp;
                                    List<Address> dropOff;
                                    try {
                                        pickUp = geocoder.getFromLocation(pickupLatLng.latitude, pickupLatLng.longitude, 1);
                                        String pickUpString = pickUp.get(0).getAddressLine(0);
                                        tvPickUp.setText(pickUpString);

                                        dropOff = geocoder.getFromLocation(destinationLatLng.latitude, destinationLatLng.longitude, 1);
                                        String dropOffString = dropOff.get(0).getAddressLine(0);
                                        tvDropOff.setText(dropOffString);
                                    }
                                    catch (Exception e){
                                        Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }

                                    tvPickUpDistance.setText(pickupDistance);
                                    //Fare dalna ha abi
                                    tvEstTime.setText(pickupDuration);

                                    mgoogleMap.addMarker(new MarkerOptions()
                                            .position(destinationLatLng)
                                            .icon(BitmapDescriptorFactory.defaultMarker())
                                            .title("Pickup Location"));

                                    createGeoFirePickupLocation(event.getKey(), pickupLatLng);

                                    mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mgoogleMap.moveCamera(CameraUpdateFactory.zoomTo(mgoogleMap.getCameraPosition().zoom - 1));

                                    newOrderLayout.setVisibility(View.VISIBLE);

                                    //Timer lagana ha.

                                    GoogleDirection.withServerKey(DIRECTION_API_KEY)
                                            .from(pickUpLatLng)
                                            .to(dropOffLatLng)
                                            .execute(new DirectionCallback() {
                                                @Override
                                                public void onDirectionSuccess(@Nullable @org.jetbrains.annotations.Nullable Direction direction) {
                                                    Route route = direction.getRouteList().get(0);
                                                    Leg leg = route.getLegList().get(0);

                                                    Info distanceInfo = leg.getDistance();
                                                    Info durationInfo = leg.getDuration();
                                                    String dropOffDistance = distanceInfo.getText();
                                                    String dropOffDuration = durationInfo.getText();

                                                    btnAcceptJob.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            yesterdayLayout.setVisibility(View.GONE);
                                                            newOrderLayout.setVisibility(View.GONE);
                                                            gotoPickupLayout.setVisibility(View.VISIBLE);
                                                            createTripPlan(event, pickupDuration, pickupDistance, dropOffDistance, dropOffDuration);
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onDirectionFailure(@NonNull @NotNull Throwable t) {
                                                    Snackbar.make(findViewById(android.R.id.content), t.getMessage(), Snackbar.LENGTH_LONG).show();
                                                }
                                            });


                                }
                                @Override
                                public void onDirectionFailure(@NonNull Throwable t) {

                                    Snackbar.make(findViewById(android.R.id.content), t.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });
                } catch (Exception e) {
                    Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                }

            }
        }).addOnFailureListener(e -> {
            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
        });
    }

    private void createTripPlan(DriverRequestRecieved event, String pickupDuration, String pickupDistance, String dropOffDistance, String dropOffDuration) {
//        setProcessLayout(true);

        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long timeOffset = snapshot.getValue(Long.class);

                firebaseFirestore.collection("Users").document(event.getKey())
                        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                                if (documentSnapshot.exists()) {
                                    RiderModel riderModel = documentSnapshot.toObject(RiderModel.class);

                                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        return;
                                    }
                                    fusedLocationProviderClient.getLastLocation()
                                            .addOnSuccessListener(location -> {

                                                tripPlanModel = new TripPlanModel();
                                                tripPlanModel.setDriver(id);
                                                tripPlanModel.setRider(event.getKey());

                                                firebaseFirestore.collection("Users").document(id)
                                                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                        DriverInfoModel driverInfoModel = documentSnapshot.toObject(DriverInfoModel.class);


                                                        tripPlanModel.setDriverInfoModel(driverInfoModel);
                                                        tripPlanModel.setRiderModel(riderModel);
                                                        tripPlanModel.setOrigin(event.getPickupLocation());
                                                        tripPlanModel.setOriginString(event.getPickupLocationString());
                                                        tripPlanModel.setDestination(event.getDestinationLocation());
                                                        tripPlanModel.setDestinationString(event.getDestinationLocationString());
                                                        tripPlanModel.setDistancePickup(pickupDistance);
                                                        tripPlanModel.setDurationPickup(pickupDuration);
                                                        tripPlanModel.setCurrentLat(location.getLatitude());
                                                        tripPlanModel.setCurrentLng(location.getLongitude());
                                                        tripPlanModel.setOrderId(event.getOrderRefNumber());
                                                        tripPlanModel.setDropOffDistance(dropOffDistance);
                                                        tripPlanModel.setDropOffDuration(dropOffDuration);


                                                        tripNumberId = Common.createUniqueTripIdNumber(timeOffset);

                                                        FirebaseDatabase.getInstance().getReference("Trips")
                                                                .child(tripNumberId)
                                                                .setValue(tripPlanModel)
                                                                .addOnSuccessListener(aVoid -> {

                                                                    tvPickupAddress.setText(tvPickUp.getText().toString());
                                                                    tvOrderRefNo.setText(event.getOrderRefNumber());
                                                                    tvDistanceFromPickup.setText(pickupDistance);
                                                                    tvETA.setText(pickupDuration);

                                                                    firebaseFirestore.collection("Users").document(event.getDropOffUserId())
                                                                            .collection("Cart")
                                                                            .whereEqualTo("ID", event.getOrderRefNumber())
                                                                            .get()
                                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                                                            if (task.isSuccessful()){
                                                                                for (DocumentSnapshot snapshots : task.getResult()){
                                                                                    if (snapshot.exists()){
                                                                                        String orderTotal = snapshots.getString("total");
                                                                                        tvEstimatedFare.setText(orderTotal);
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                                            .addOnFailureListener(e -> {
                                                                        Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                                                    });

                                                                    btnCall.setOnClickListener(new View.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(View v) {
                                                                            Dexter.withContext(getApplicationContext()).withPermission(Manifest.permission.CALL_PHONE).withListener(new PermissionListener() {
                                                                                @Override
                                                                                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                                                                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + riderModel.getPhoneNumber()));
                                                                                    startActivity(intent);
                                                                                }
                                                                                @Override
                                                                                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                                                                    Toast.makeText(MainActivity.this, "Please grant permission!", Toast.LENGTH_SHORT).show();
                                                                                }

                                                                                @Override
                                                                                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                                                                    permissionToken.continuePermissionRequest();
                                                                                }
                                                                            });
                                                                        }
                                                                    });

                                                                    setOfflineModeForDriver(event);

                                                                }).addOnFailureListener(e -> {
                                                            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                                        });
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                                    }
                                                });

                                            }).addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show());
                                }
                                else {
                                    Snackbar.make(findViewById(android.R.id.content), "Cannot find rider with key"+" "+event.getKey(), Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setOfflineModeForDriver(DriverRequestRecieved event) {

        UserUtils.sendAcceptRequestToRider(findViewById(android.R.id.content), this, event.getKey(), tripNumberId);

        if (currentUserRef != null){
            currentUserRef.removeValue();
        }
        newOrderLayout.setVisibility(View.GONE);

        gotoPickupLayout.setVisibility(View.VISIBLE);

        isTripStart = true;
    }

    private void createGeoFirePickupLocation(String key, LatLng pickupLatLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TripPickupLocation");

        pickupGeoFire = new GeoFire(ref);
        pickupGeoFire.setLocation(key, new GeoLocation(pickupLatLng.latitude, pickupLatLng.longitude),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null){
                            Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                        else {
                            Log.d("Success", key+"was create success on geofire");
                        }
                    }
                });

    }

    private void makeDriverOnline(Location location) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        List<Address> addressList;
        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String cityName = addressList.get(0).getLocality();

            driverLocationRef = FirebaseDatabase.getInstance().getReference("driversLocation").child(cityName);
            currentUserRef = driverLocationRef.child(id);
            geoFire = new GeoFire(driverLocationRef);

            geoFire.setLocation(id, new GeoLocation(location.getLatitude(), location.getLongitude()),
                    new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                            }

                        }
                    });


        }
        catch (IOException e) {
            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void buildLocationCallBack() {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                    Location location = locationResult.getLastLocation();

                    if (pickupGeoFire != null){
                        pickupGeoQuery = pickupGeoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 0.05);

                        pickupGeoQuery.addGeoQueryEventListener(pickupGeoQueryEventListner);
                    }

                    if (destinationGeoFire != null){
                        destinationGeoQuery = destinationGeoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 0.05);

                        destinationGeoQuery.addGeoQueryEventListener(destinationGeoQueryEventListner);
                    }

                    if (!isTripStart){
                        makeDriverOnline(location);
                    }
                    if (!tripNumberId.equals("")){
                        TripPlanModel tripPlanModel = new TripPlanModel();

                        tripPlanModel.setCurrentLat(location.getLatitude());
                        tripPlanModel.setCurrentLng(location.getLongitude());

                        HashMap<String, Object> newLocation = new HashMap<>();
                        newLocation.put("currentLat",location.getLatitude());
                        newLocation.put("currentLng",location.getLongitude());

                        FirebaseDatabase.getInstance().getReference("Trips")
                                .child(tripNumberId)
                                .updateChildren(newLocation)
                                .addOnSuccessListener(aVoid -> {

                                }).addOnFailureListener(e -> {
                            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
                    }
                }
            };
        }
    }

    private void buildLocationRequest() {
        if (locationRequest == null) {
            locationRequest = LocationRequest.create();
            locationRequest.setSmallestDisplacement(20f);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(15000);
            locationRequest.setFastestInterval(10000);
        }
    }

    private void updateLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mgoogleMap = googleMap;

        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mgoogleMap.setMyLocationEnabled(true);
                mgoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mgoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return false;
                        }
                        fusedLocationProviderClient.getLastLocation()
                                .addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        mgoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "dosra wala" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        return true;
                    }
                });

                View locationbutton = ((View)findViewById(Integer.parseInt("1")).getParent())
                        .findViewById(Integer.parseInt("2"));
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationbutton.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.CENTER_IN_PARENT);
                params.setMargins(0,100,0,0);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(MainActivity.this, "Permission "+permissionDeniedResponse.getPermissionName()+" "+
                        "was denied!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();

    }

    private void updateFirebaseToken() {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();

                        UserUtils.updateToken(MainActivity.this, token);

                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!EventBus.getDefault().isRegistered(this)) {

            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        geoFire.removeLocation(id);
        super.onDestroy();
    }
}