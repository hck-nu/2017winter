/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.fcm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int FIREBASE_LOGIN = 123;
    private static final int PICK_IMAGE = 111;

    public FirebaseUser mUser;
    public FirebaseStorage mStorage;
    public StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser == null) {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())
                            ).build(), FIREBASE_LOGIN);
            return;
        }

        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReferenceFromUrl("gs://hack-night-messaging.appspot.com");

        loadUi();
    }

    private void loadUi() {
        // TODO: Move the login logic to another activity so we don't need this!
        setContentView(R.layout.activity_main);

        Button subscribeButton = (Button) findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseMessaging.getInstance().subscribeToTopic("news");

                String msg = getString(R.string.msg_subscribed);
                Log.d(TAG, msg);
                showToast(msg);
            }
        });

        Button logTokenButton = (Button) findViewById(R.id.logTokenButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = FirebaseInstanceId.getInstance().getToken();

                String msg = getString(R.string.msg_token_fmt, token);
                Log.d(TAG, msg);
                showToast(msg);
            }
        });

        // TODO: Insert code to get the user's name, email, profile picture, etc and display it
    }

    public void pickProfilePicture() {
        // TODO: make an intent to pick a profile picture
    }

    public void showProfilePicture(Bitmap picture) {
        // TODO: Update the imageView we have to show the new profile picture
    }

    /**
     * Load the user's profile picture into an ImageView
     */
    public void loadProfilePicture(ImageView imageView) {
        // TODO: Use this to load the user's image once we make an imageView
        StorageReference imageRef = getUserProfPicRef(mUser);
        Glide.with(this).using(new FirebaseImageLoader()).load(imageRef).into(imageView);
    }

    public void uploadProfilePicture(Uri imageUri) throws FileNotFoundException,
            URISyntaxException {
        StorageReference profileRef = getUserProfPicRef(mUser);
        UploadTask uploadTask = profileRef.putStream(uriToInputStream(imageUri));
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Image upload failed");
            }
        });
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                UserProfileChangeRequest imageChange = new UserProfileChangeRequest
                        .Builder()
                        .setPhotoUri(taskSnapshot.getDownloadUrl())
                        .build();
                mUser.updateProfile(imageChange)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                showToast("Image uploaded!");
                            }
                        });
            }
        });
    }

    public StorageReference getUserProfPicRef(FirebaseUser user) {
        return mStorageRef.child("profile_pics/" + user.getUid() + ".jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FIREBASE_LOGIN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == ResultCodes.OK) {
                loadUi();
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
        } else if (requestCode == PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }

            Uri imageUri = data.getData();

            try {
                Bitmap image = BitmapFactory.decodeStream(uriToInputStream(imageUri));
                showProfilePicture(image);
                uploadProfilePicture(imageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                showToast("Can't open image");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                showToast("Can't open image");
            }
        }
    }

    public InputStream uriToInputStream(Uri uri) throws FileNotFoundException, URISyntaxException {
        InputStream inputStream = null;
        if (uri.getScheme().equals("content")) {
            inputStream = getContentResolver().openInputStream(uri);
        } else if (uri.getScheme().equals("file")) {
            inputStream = new FileInputStream(new File(new URI(uri.toString())));
        }
        return inputStream;
    }

    public void showToast(CharSequence message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
