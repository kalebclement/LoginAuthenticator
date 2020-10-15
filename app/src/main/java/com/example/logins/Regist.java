package com.example.logins;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Regist extends AppCompatActivity {
EditText mFullName, mEmail, mPassword, mPhone, mOTPCode;
Button mRegisterBtn;
TextView mLoginBtn, mResendOtp;
FirebaseAuth fAuth;
FirebaseFirestore fstore;
ProgressBar progressBar;
CountryCodePicker countryCodePicker;
String verificationID;
PhoneAuthProvider.ForceResendingToken token;
Boolean verificationinprogress = false;
String userOTP, email, password, fullname, phoneNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regist);

        mFullName = findViewById(R.id.FullNameTxt);
        mEmail = findViewById(R.id.EmailTxt);
        mPassword = findViewById(R.id.PasswordTxt);
        mPhone = findViewById(R.id.PhoneTxt);
        mRegisterBtn = findViewById(R.id.RegisterBtn);
        mLoginBtn = findViewById(R.id.LoginBtn);
        mOTPCode = findViewById(R.id.OtpTxt);
        mResendOtp = findViewById(R.id.ResendOtpBtn);
        countryCodePicker = findViewById(R.id.ccp);

        fAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.ProgressBar);


//        if(fAuth.getCurrentUser() == null){
//            startActivity(new Intent(getApplicationContext(), MainActivity.class));
//            finish();
//        }

        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!verificationinprogress){
                    email = mEmail.getText().toString().trim();
                    password = mPassword.getText().toString().trim();


                    if(TextUtils.isEmpty(email)){
                        mEmail.setError("Email is required");
                        return;
                    }
                    if(TextUtils.isEmpty(password)){
                        mPassword.setError("Password is required");
                        return;
                    }
                    if(password.length() <= 6){
                        mPassword.setError("Password must be more than 6 characther");
                        return;
                    }
                    // progressBar.setVisibility(View.VISIBLE);
                    if(!mPhone.getText().toString().isEmpty()){
                        phoneNum = "+" + countryCodePicker.getSelectedCountryCode() + mPhone.getText().toString();
                        Log.d("tag", "Phone No -> " + phoneNum);
                        progressBar.setVisibility(View.VISIBLE);
                        requestOTP(phoneNum);
                    }else{
                        mPhone.setError("Phone number is not valid !");
                    }
                }
                else{
                    userOTP = mOTPCode.getText().toString();
                    if(!userOTP.isEmpty() && userOTP.length() == 6){
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationID, userOTP);
                        verifyAuth(credential, email, password);

                    }else{
                        mOTPCode.setError("INVALID OTP");
                    }
                }



            }

        });

    }

//    protected void onStart() {
//        super.onStart();
//
//        if (fAuth.getCurrentUser() != null){
//            progressBar.setVisibility(View.VISIBLE);
//            AutomateLogin();
//        }
//        else{
//            startActivity(new Intent(getApplicationContext(), Logins.class));
//        }
//    }

    private void AutomateLogin() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    private void verifyAuth(PhoneAuthCredential credential, String email, String password) {
        fAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    LoginCredential();
                }else{
                    Toast.makeText(Regist.this, "Invalid OTP Code", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void LoginCredential(){
        fullname = mFullName.getText().toString().trim();
        email = mEmail.getText().toString().trim();
        password = mPassword.getText().toString().trim();
        //Register user in firebase
        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(Regist.this, "User Succesfully Created", Toast.LENGTH_SHORT).show();
                    DocumentReference docref = fstore.collection("users").document(fAuth.getCurrentUser().getUid());
                    Map<String, Object> user = new HashMap<>();
                    user.put("FullName", fullname);
                    user.put("Email", email);
                    user.put("PhoneNumber", phoneNum);
                    docref.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(Regist.this, "Data Registered", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(Regist.this, "Data Didnt registered", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                }
                else{
                    Toast.makeText(Regist.this, "Error is occured " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void requestOTP(String phoneNum) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNum,
                60L,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationinprogress = true;
                progressBar.setVisibility(View.GONE);
                mOTPCode.setVisibility(View.VISIBLE);
                verificationID = s;
                token = forceResendingToken;
                mRegisterBtn.setText("VERIFY");
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                //mResendOtp.setVisibility(View.VISIBLE);
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(Regist.this, "Cannot Create Account, " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void RedirectLogin(View view){
        startActivity(new Intent(getApplicationContext(), Logins.class));
    }
}