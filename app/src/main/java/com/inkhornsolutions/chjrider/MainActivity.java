package com.inkhornsolutions.chjrider;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
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
    private TextView tvPickUp, tvDropOff, tvTotalDistance, tvEstFare;
    private TextView tvPickupAddress, tvOrderRefNo, tvDistanceFromPickup, tvEstimatedFare, tvETA;
    private TextView tvDropOffAddress, tvDropOffOrderRefNo, tvDistanceFromDropOff, tvEstimatedFareDropOff;
    private ConstraintLayout yesterdayLayout, newOrderLayout, gotoPickupLayout, gotoDropOffLayout;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    GeoFire geoFire;
    GeoQuery geoQuery;
    private GeoFire pickupGeoFire, destinationGeoFire;
    private GeoQuery pickupGeoQuery, destinationGeoQuery;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private DatabaseReference driverLocationRef,currentUserRef;
    private String id;
    private String tripNumberId;
    private boolean isTripStart = false;

    private DriverRequestRecieved driverRequestReceived;
    private Polyline blackPolyLine, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;

    private GeoQueryEventListener pickupGeoQueryEventListner = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
//            btnStartRide.setEnabled(true);

            UserUtils.sendNotifyToRider(MainActivity.this, gotoDropOffLayout, key);

            if (pickupGeoQuery != null) {
                if (pickupGeoFire != null){
                    pickupGeoFire.removeLocation(key);
                }
                pickupGeoFire = null;
                pickupGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {
//            btnStartRide.setEnabled(false);
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
//            btnCompleteRide.setEnabled(true);
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
//        id = firebaseAuth.getCurrentUser().getUid();
        id = "WhaMIBVZyxUWtN9EPrx16d9Dzoo2";
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
        tvTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        tvEstFare = (TextView) findViewById(R.id.tvEstFare);
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

        btnAcceptJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yesterdayLayout.setVisibility(View.GONE);
                newOrderLayout.setVisibility(View.GONE);
                gotoPickupLayout.setVisibility(View.VISIBLE);
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

                            }
                            else {
                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {

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
                if (driverRequestReceived != null) {
                    LatLng destinationLatLng = new LatLng(
                            Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[0]),
                            Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[1]));

                    mgoogleMap.addMarker(new MarkerOptions()
                            .position(destinationLatLng)
                            .title(driverRequestReceived.getDestinationLocationString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                    //draw path
                    drawPathFromCurrentLocation(driverRequestReceived.getPickupLocation());
                }

                gotoPickupLayout.setVisibility(View.GONE);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

    }

    private void drawPathFromCurrentLocation(String PickupLocation) {
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

                                    createGeoFireDestinationLocation(driverRequestReceived.getKey(), pickUpLatLng);

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
                    LatLng destinationLatLng = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                            Double.parseDouble(event.getPickupLocation().split(",")[1]));

                    Log.d("address: ", "Chala1");

                    Log.d("address: ", "" + destinationLatLng);

                    GoogleDirection.withServerKey(DIRECTION_API_KEY)
                            .from(originLatLng)
                            .to(destinationLatLng)
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
                                            .include(destinationLatLng)
                                            .build();

                                    //Add car icon for origin

                                    Info distanceInfo = leg.getDistance();
                                    Info durationInfo = leg.getDuration();
                                    String distance = distanceInfo.getText();
                                    String duration = durationInfo.getText();

                                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    List<Address> pickUp;
                                    List<Address> dropOff;
                                    try {
                                        pickUp = geocoder.getFromLocation(originLatLng.latitude, originLatLng.longitude, 1);
                                        String pickUpString = pickUp.get(0).getAddressLine(0);
                                        tvPickUp.setText(pickUpString);

                                        dropOff = geocoder.getFromLocation(originLatLng.latitude, originLatLng.longitude, 1);
                                        String dropOffString = dropOff.get(0).getAddressLine(0);
                                        tvDropOff.setText(dropOffString);
}
                                    catch (Exception e){
                                        Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }

                                    tvTotalDistance.setText(distance);
                                    //Fare dalna ha abi
                                    tvEstFare.setText(duration);



                                    mgoogleMap.addMarker(new MarkerOptions()
                                            .position(destinationLatLng)
                                            .icon(BitmapDescriptorFactory.defaultMarker())
                                            .title("Pickup Location"));

                                    createGeoFirePickupLocation(event.getKey(), destinationLatLng);

                                    mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mgoogleMap.moveCamera(CameraUpdateFactory.zoomTo(mgoogleMap.getCameraPosition().zoom - 1));

                                    newOrderLayout.setVisibility(View.VISIBLE);

                                    //Timer lagana ha.

                                    btnAcceptJob.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            createTripPlan(event, duration, distance);
                                        }
                                    });
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

    private void createTripPlan(DriverRequestRecieved event, String duration, String distance) {
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

                                                TripPlanModel tripPlanModel = new TripPlanModel();
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
                                                        tripPlanModel.setDistancePickup(distance);
                                                        tripPlanModel.setDurationPickup(duration);
                                                        tripPlanModel.setCurrentLat(location.getLatitude());
                                                        tripPlanModel.setCurrentLng(location.getLongitude());

                                                        tripNumberId = Common.createUniqueTripIdNumber(timeOffset);

                                                        FirebaseDatabase.getInstance().getReference("Trips")
                                                                .child(tripNumberId)
                                                                .setValue(tripPlanModel)
                                                                .addOnSuccessListener(aVoid -> {

//                                                                    tvRiderName.setText(riderModel.getFirstName());
//                                                                    tvStartRiderEstimateTime.setText(duration);
//                                                                    tvStartRiderEstimateDistance.setText(distance);

                                                                    setOfflineModeForDriver(event, duration, distance);

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

    private void setOfflineModeForDriver(DriverRequestRecieved event, String duration, String distance) {

        UserUtils.sendAcceptRequestToRider(findViewById(android.R.id.content), this, event.getKey(), tripNumberId);

        if (currentUserRef != null){
            currentUserRef.removeValue();
        }
        newOrderLayout.setVisibility(View.GONE);

        gotoPickupLayout.setVisibility(View.VISIBLE);

        isTripStart = true;
    }

    private void createGeoFirePickupLocation(String key, LatLng destinationLatLng) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TripPickupLocation");

        pickupGeoFire = new GeoFire(ref);
        pickupGeoFire.setLocation(key, new GeoLocation(destinationLatLng.latitude, destinationLatLng.longitude),
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
                    if (tripNumberId != null){
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
                                    Toast.makeText(MainActivity.this, "Chala", Toast.LENGTH_SHORT).show();
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