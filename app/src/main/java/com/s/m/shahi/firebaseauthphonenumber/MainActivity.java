package com.s.m.shahi.firebaseauthphonenumber;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout mainLayout, verify_layout, profile_layout;
    private Button next, confirm_code, finishBtn;
    private CountryCodePicker ccp;
    private EditText number_id, otp_code, name_id;
    private CircleImageView profile_image;
    private TextView resend_code;

    private String phone, verifyId, imageLink;

    private boolean mVerificationInProcess = false;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private FirebaseAuth auth;
    private DatabaseReference user;

    private ProgressDialog progressDialog;
    private static final int PICK_IMAGE_REQUEST = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fieldInitialize();

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(number_id.getText().toString().trim())) {
                    Toast.makeText(MainActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                } else {
                    phone = ccp.getFullNumberWithPlus();
                    sendCode(ccp.getFullNumberWithPlus());
                }
            }
        });

        confirm_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(otp_code.getText().toString().trim())) {
                    Toast.makeText(MainActivity.this, "Please enter your OTP code !", Toast.LENGTH_SHORT).show();
                } else {
                    signInWithPhoneAuthCredential(otp_code.getText().toString());
                }
            }
        });

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(name_id.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog.show();
                    saveDataBaseInfo(name_id.getText().toString().trim(), imageLink, phone);
                }
            }
        });

        resend_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phone,
                        60,
                        java.util.concurrent.TimeUnit.SECONDS,
                        MainActivity.this,
                        changedCallbacks,
                        forceResendingToken);
            }
        });

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });
    }

    private void saveDataBaseInfo(String name, String imageLink, String phone) {
        User userInfo = new User(name, phone, imageLink);

        user.child(auth.getCurrentUser().getPhoneNumber()).setValue(userInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    }
                });
    }

    private void fieldInitialize() {
        auth = FirebaseAuth.getInstance();
        user = FirebaseDatabase.getInstance().getReference("User");

        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
        verify_layout = (RelativeLayout) findViewById(R.id.verify_layout);
        profile_layout = (RelativeLayout) findViewById(R.id.profile_layout);

        next = (Button) findViewById(R.id.next);
        confirm_code = (Button) findViewById(R.id.confirm_code);
        finishBtn = (Button) findViewById(R.id.finishBtn);

        ccp = (CountryCodePicker) findViewById(R.id.ccp);

        number_id = (EditText) findViewById(R.id.number_id);
        otp_code = (EditText) findViewById(R.id.otp_code);
        name_id = (EditText) findViewById(R.id.name_id);

        profile_image = (CircleImageView) findViewById(R.id.profile_image);

        resend_code = (TextView) findViewById(R.id.resend_code);

        ccp.registerCarrierNumberEditText(number_id);
        ccp.setAutoDetectedCountry(true);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait.....");

    }

    private void sendCode(String phone) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phone,
                60,
                java.util.concurrent.TimeUnit.SECONDS,
                MainActivity.this,
                changedCallbacks);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks changedCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            mVerificationInProcess = false;
            String code = phoneAuthCredential.getSmsCode();
            if (code != null) {
                otp_code.setText(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(MainActivity.this, "Verify is Failed" + e, Toast.LENGTH_SHORT).show();
            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(MainActivity.this, "Invalid Phone number", Toast.LENGTH_SHORT).show();
            } else if (e instanceof FirebaseTooManyRequestsException) {

            }
        }

        @Override
        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
            Toast.makeText(MainActivity.this, "Verification code has been send", Toast.LENGTH_SHORT).show();
            verifyId = verificationId;
            forceResendingToken = token;
            mainLayout.setVisibility(View.GONE);
            verify_layout.setVisibility(View.VISIBLE);
        }
    };


    private void signInWithPhoneAuthCredential(String code) {
        if (code != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verifyId, code);
            signInWithPhoneAuth(credential);
        }
    }

    private void signInWithPhoneAuth(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            verify_layout.setVisibility(View.GONE);
                            profile_layout.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MainActivity.this, "Error : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageLink = data.getData().toString();
        }
    }

}
