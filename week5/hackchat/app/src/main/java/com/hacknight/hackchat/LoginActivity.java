package com.hacknight.hackchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * LoginActivity is the first activity shown when the app is opened through the icon
 * If the user has not logged in yet, they are prompted to log in and their info is saved
 * Otherwise, continue to MainActivity
 */
public class LoginActivity extends AppCompatActivity implements OnSuccessListener<Object>,
        ValueEventListener {

    private static final String TAG = "LoginActivity";
    private static final int FIREBASE_LOGIN = 123;

    private User mUser;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Ask the user to make a new account or login
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())
                            ).build(), FIREBASE_LOGIN);
        } else {
            // User already logged in
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    /**
     * Runs when the user logs in
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FIREBASE_LOGIN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == ResultCodes.OK) {
                // User logged in!
                mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                // Let's see if we have any data on the user
                // Code continues in onDataChange
                User.getDatabaseRef(mFirebaseUser).addListenerForSingleValueEvent(this);
            } else {
                if (response == null) {
                    // User pressed back button
                    showToast("Sign in cancelled");
                } else if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showToast("No internet");
                } else {
                    showToast("Unknown error");
                }
            }
        }
    }

    /**
     * Upload all of the information about a new user
     */
    public void uploadUser(FirebaseUser firebaseUser) {
        mUser = new User();
        mUser.id = firebaseUser.getUid();
        mUser.name = firebaseUser.getDisplayName();
        mUser.token = FirebaseInstanceId.getInstance().getToken();
        mUser.bio = "New in town";
        try {
            mUser.setProfilePicture(this, firebaseUser.getPhotoUrl(),
                    this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showToast("User not created");
                        }
                    });
        } catch (FileNotFoundException | URISyntaxException e) {
            e.printStackTrace();
            showToast("User not created");
        }
    }

    /**
     * Runs once we retrieve information about the user that just logged in
     */
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) {
            // We don't have ANY info on this user, which means this is a new account
            // Save some information about them into our database
            Log.i(TAG, "New user!");
            uploadUser(mFirebaseUser);
        } else {
            // We already know about this user, continue to MainActivity
            Log.i(TAG, "Old user");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * Runs if we successfully uploaded a new user's profile picture
     */
    @Override
    public void onSuccess(Object o) {
        // Upload info on the new user to our database
        mUser.getDatabaseRef().setValue(mUser.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "User uploaded");
                        // All done, go to MainActivity!
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "User not uploaded");
                        e.printStackTrace();
                    }
                });
    }

    public void showToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
