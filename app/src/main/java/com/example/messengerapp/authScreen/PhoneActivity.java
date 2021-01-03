package com.example.messengerapp.authScreen;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.messengerapp.R;
import com.example.messengerapp.homeScreen.HomeActivity;
import com.example.messengerapp.mainScreen.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mPhoneNumberField, mVerificationField;
    private Button mStartButton, mVerifyButton;
    private TextView textForEditTextCode;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;

    private static final String TAG = "SignUp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mPhoneNumberField = (EditText) findViewById(R.id.editPhone);
        mVerificationField = (EditText) findViewById(R.id.editTextPassword);

        mStartButton = (Button) findViewById(R.id.buttongetcode);
        mVerifyButton = (Button) findViewById(R.id.buttonSingIn);
        textForEditTextCode = (TextView) findViewById(R.id.textViewForCode);

        mStartButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumberField.setError("Неккоректный номер.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), "Квота превышена.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                mVerificationId = s;
                mResendToken = forceResendingToken;
                textForEditTextCode.setVisibility(View.VISIBLE);
                mVerificationField.setVisibility(View.VISIBLE);
                mStartButton.setVisibility(View.GONE);
                mVerifyButton.setVisibility(View.VISIBLE);
            }
        };

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String userId = FirebaseAuth.getInstance().getUid();
                    FirebaseFirestore.getInstance().collection("users")
                            .document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                if (documentSnapshot.get("name") != null) {
                                    startActivity(new Intent(PhoneActivity.this, HomeActivity.class));
                                    finish();
                                }
                            } else {
                                showAlertDialogButtonClicked();
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        mVerificationField.setError("Неверный код.");
                    }
                }
            }
        });
    }

    public void showAlertDialogButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.user);
        builder.setTitle("Введите имя");
        builder.setMessage("Чтобы ваши друзья заметили вас");
        final View customLayout = getLayoutInflater().inflate(R.layout.custom_alert2, null);
        builder.setView(customLayout);

        builder.setPositiveButton("Сохранить меня", null);
        AlertDialog dialog = builder.show();
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setOnClickListener(view -> {
            EditText editText = customLayout.findViewById(R.id.editTextTextPersonName);
            if (editText.getText().toString().trim().isEmpty()) {
                editText.setError("Пожалуйста, введите имя.");
            } else {
                sendName(editText);
                dialog.dismiss();
            }
        });
    }

    public void sendName(EditText editName) {
        String name = editName.getText().toString().trim();
        String userId = FirebaseAuth.getInstance().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("id", userId);
        map.put("name", name);
        FirebaseFirestore.getInstance().collection("users")
                .document(name)
                .set(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(PhoneActivity.this, HomeActivity.class));
                            finish();
                        }
                    }
                });
    }



    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks
        );
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks,
                token);
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(PhoneActivity.this, HomeActivity.class));
            finish();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttongetcode:
                if (!validatePhoneNumber()) {
                    return;
                }
                startPhoneNumberVerification(mPhoneNumberField.getText().toString());
                break;

            case R.id.buttonSingIn:
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;
        }
    }
}