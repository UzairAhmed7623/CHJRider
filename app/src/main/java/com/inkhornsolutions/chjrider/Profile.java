package com.inkhornsolutions.chjrider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.shivtechs.maplocationpicker.LocationPickerActivity;
import com.shivtechs.maplocationpicker.MapUtility;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {

    private static final int ADDRESS_PICKER_REQUEST = 1001;
    public static final int REQUEST_IMAGE = 1002;
    private TextView tvFirstName, tvLastName, tvEmailAddress;
    private TextView tvName, tvMobile, tvEmail, tvAddress, tvDateOfBirth;
    private Button btnSave;
    private ImageButton ivAddImage;
    private CircleImageView ivProfile;
    private Uri imageUri;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private Toolbar toolbar;
    private CoordinatorLayout rootLayout;
    private DatePickerDialog datePickerDialog;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        tvFirstName = (TextView) findViewById(R.id.tvFirstName);
        tvLastName = (TextView) findViewById(R.id.tvLastName);
        tvEmailAddress = (TextView) findViewById(R.id.tvEmailAddress);

        tvName = (TextView) findViewById(R.id.tvName);
        tvMobile = (TextView) findViewById(R.id.tvMobile);
        tvEmail = (TextView) findViewById(R.id.tvEmail);
        tvAddress = (TextView) findViewById(R.id.tvAddress);
        tvDateOfBirth = (TextView) findViewById(R.id.tvDateOfBirth);
        ivProfile = (CircleImageView) findViewById(R.id.ivProfile);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rootLayout = (CoordinatorLayout) findViewById(R.id.rootLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.myColorSecond));

        dialog = new ProgressDialog(this);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.exists()){
                    if (documentSnapshot.getString("firstName") != null && documentSnapshot.getString("lastName") != null){
                        String fName = documentSnapshot.getString("firstName");
                        String lName = documentSnapshot.getString("lastName");
                        tvFirstName.setText(fName);
                        tvLastName.setText(lName);
                        tvName.setText(fName+" "+lName);
                    }
                    if (documentSnapshot.getString("emailAddress") != null){
                        String email = documentSnapshot.getString("emailAddress");
                        tvEmailAddress.setText(email);
                        tvEmail.setText(email);
                    }
                    if (documentSnapshot.getString("phoneNumber") != null){
                        String mobile = documentSnapshot.getString("phoneNumber");
                        tvMobile.setText(mobile);
                    }
                    if (documentSnapshot.getString("address") != null){
                        String address = documentSnapshot.getString("address");
                        tvAddress.setText(address);
                    }
                    if (documentSnapshot.getString("DOB") != null){
                        String dob = documentSnapshot.getString("DOB");
                        tvDateOfBirth.setText(dob);
                    }
                    if (documentSnapshot.getString("driverImageProfile") != null){
                        String dob = documentSnapshot.getString("driverImageProfile");
                        Glide.with(Profile.this).load(dob).placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.account_circle)).into(ivProfile);
                    }

                }
                dialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        View view = LayoutInflater.from(this).inflate(R.layout.edit_details, null);
        EditText editText = (EditText) view.findViewById(R.id.editText);
        EditText editText2 = (EditText) view.findViewById(R.id.editText2);
        TextInputLayout TextInputLayout2 = (TextInputLayout) view.findViewById(R.id.TextInputLayout2);
        Button btnAdd = (Button) view.findViewById(R.id.btnAdd);

        AlertDialog.Builder builder = new AlertDialog.Builder(Profile.this);
        builder.setView(view);

        AlertDialog alertDialog = builder.create();


        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                alertDialog.show();

                TextInputLayout2.setVisibility(View.VISIBLE);
                editText2.setVisibility(View.VISIBLE);

                editText.setHint("Write your First Name");
                editText2.setHint("Write your Last Name");

                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                editText2.setInputType(InputType.TYPE_CLASS_TEXT);

                editText.setText(tvFirstName.getText().toString());
                editText2.setText(tvLastName.getText().toString());

                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String firstName = editText.getText().toString();
                        String lastName = editText2.getText().toString();

                        if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)){
                            editText.setError("Please write your first name");
                            editText2.setError("Please write your last name");
                        }
                        else {

                            tvFirstName.setText(firstName);
                            tvLastName.setText(lastName);
                            tvName.setText(firstName+" "+lastName);

                            Map<String, Object> addData = new HashMap<>();
                            addData.put("firstName", firstName);
                            addData.put("lastName", lastName);

                            firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).set(addData, SetOptions.merge())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(rootLayout, "Your name is updated successfully!", Snackbar.LENGTH_LONG).show();
                                            editText.clearComposingText();
                                            editText2.clearComposingText();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });

                            alertDialog.dismiss();
                        }

                    }
                });
            }
        });

        tvEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                alertDialog.show();

                editText.setHint("Write your email");

                editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                TextInputLayout2.setVisibility(View.GONE);

                editText.setText(tvEmailAddress.getText().toString());

                btnAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String emailAddress = editText.getText().toString();

                        if (TextUtils.isEmpty(emailAddress)){
                            editText.setError("Please write your email");
                        }
                        else {

                            tvEmail.setText(emailAddress);
                            tvEmailAddress.setText(emailAddress);

                            Map<String, Object> addData = new HashMap<>();
                            addData.put("emailAddress", emailAddress);

                            firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).set(addData, SetOptions.merge())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(rootLayout, "Your email is updated successfully!", Snackbar.LENGTH_LONG).show();
                                            editText.clearComposingText();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });

                            alertDialog.dismiss();
                        }
                    }
                });
            }
        });

//        tvMobile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                alertDialog.show();
//
//                editText.setHint("Write your phone with +92");
//
//                editText.setInputType(InputType.TYPE_CLASS_PHONE);
//
//                TextInputLayout2.setVisibility(View.GONE);
//
//                editText.setText(tvMobile.getText().toString());
//
//                btnAdd.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        String phoneNumber = editText.getText().toString();
//                        if (TextUtils.isEmpty(phoneNumber)){
//                            editText.setError("Please write your phone");
//                        }
//                        else {
//                            tvMobile.setText(phoneNumber);
//
//                            Map<String, Object> addData = new HashMap<>();
//                            addData.put("phoneNumber", phoneNumber);
//
//                            firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).set(addData, SetOptions.merge())
//                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                        @Override
//                                        public void onSuccess(Void aVoid) {
//                                            Snackbar.make(rootLayout, "Your phone number is updated successfully!", Snackbar.LENGTH_LONG).show();
//                                            editText.clearComposingText();
//                                        }
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
//                                }
//                            });
//
//                            alertDialog.dismiss();
//                        }
//                    }
//                });
//            }
//        });

        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MapUtility.apiKey = getResources().getString(R.string.google_maps_key);
                Intent i = new Intent(Profile.this, LocationPickerActivity.class);
                startActivityForResult(i, ADDRESS_PICKER_REQUEST);

            }
        });

        tvDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                datePickerDialog = new DatePickerDialog(Profile.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        tvDateOfBirth.setText(dayOfMonth + "/" + (month + 1) + "/" + year);

                        String DOB = tvDateOfBirth.getText().toString();

                        Map<String, Object> addData = new HashMap<>();
                        addData.put("DOB", DOB);

                        firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).set(addData, SetOptions.merge())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(rootLayout, "Your DOB is updated successfully!", Snackbar.LENGTH_LONG).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                },year, month, day);

                datePickerDialog.show();

            }
        });

        ivProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withContext(Profile.this)
                        .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report.areAllPermissionsGranted()) {
                                    ImagePicker.Companion.with(Profile.this)
                                            .cropSquare()
                                            .compress(1024)			//Final image size will be less than 1 MB(Optional)
                                            .start(REQUEST_IMAGE);
                                }
                                else {
                                    Snackbar.make(rootLayout, report.getDeniedPermissionResponses().toString(), Snackbar.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADDRESS_PICKER_REQUEST) {
            try {
                if (data != null && data.getStringExtra(MapUtility.ADDRESS) != null) {

                    Bundle completeAddress =data.getBundleExtra("fullAddress");

                    String address = new StringBuilder().append(completeAddress.getString("addressline2").replace(", Punjab,","").replace("Pakistan","")).toString();
                    tvAddress.setText(address);

                    Map<String, Object> addData = new HashMap<>();
                    addData.put("address", address);

                    firebaseFirestore.collection("Users").document(firebaseAuth.getCurrentUser().getUid()).set(addData, SetOptions.merge())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Snackbar.make(rootLayout, "Your address is updated successfully!", Snackbar.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(rootLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });
                }
            }
            catch (Exception ex) {
                Snackbar.make(rootLayout, ex.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        }

        else if (requestCode == REQUEST_IMAGE ) {

            if (data!= null && data.getData() != null){

                //Image Uri will not be null for RESULT_OK
                Uri uri = data.getData();

                Glide.with(this)
                        .load(uri)
                        .placeholder(ContextCompat.getDrawable(getApplicationContext(),R.drawable.account_circle))
                        .transform(new CircleCrop())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                dialog.dismiss();
                                return false; // important to return false so the error placeholder can be placed
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                dialog.dismiss();
                                return false;
                            }
                        })
                        .into(ivProfile);

                uploadImage(uri);

            }
            else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ""+ImagePicker.RESULT_ERROR, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void uploadImage(Uri imageUri) {

        dialog.show();

        String someFilepath = String.valueOf(imageUri);
        String extension = someFilepath.substring(someFilepath.lastIndexOf("."));

        String userId = firebaseAuth.getCurrentUser().getUid();

        StorageReference riversRef = storageReference.child("images/Restaurants/Profile" + extension+ " " + tvFirstName.getText().toString() +" "+ tvLastName.getText().toString());

        riversRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!urlTask.isSuccessful());
                Uri downloadUrl = urlTask.getResult();

                final String sdownload_url = String.valueOf(downloadUrl);

                HashMap<String, Object> img = new HashMap<>();
                img.put("driverImageProfile", sdownload_url);

                firebaseFirestore.collection("Users").document(userId).set(img, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                dialog.dismiss();
                                Snackbar.make(findViewById(android.R.id.content), "Image Uploaded", Snackbar.LENGTH_LONG).setBackgroundTint(ContextCompat.getColor(getApplicationContext(),R.color.myColor)).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).setBackgroundTint(ContextCompat.getColor(getApplicationContext(),R.color.myColor)).show();
                            }
                        });
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        dialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "Failed to Upload", Snackbar.LENGTH_LONG).setBackgroundTint(ContextCompat.getColor(getApplicationContext(),R.color.myColor)).show();
                    }
                });
    }

}