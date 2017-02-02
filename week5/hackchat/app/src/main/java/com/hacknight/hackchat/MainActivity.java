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

package com.hacknight.hackchat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE = 111;

    private ImageView mProfilePic;
    private User mUser;
    private TextView mNameText;
    private TextView mBioText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the UI of our application
        setContentView(R.layout.activity_main);

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

        // Get references to all of the views we needd
        mNameText = (TextView) findViewById(R.id.myFullName);
        mProfilePic = (ImageView) findViewById(R.id.profilePic);
        mBioText = (TextView) findViewById(R.id.myBio);

        // Change the profile pic when the picture is clicked
        mProfilePic.setOnClickListener(this);

        // Get notified whenever the edit bio button is clicked
        Button editBio = (Button) findViewById(R.id.editBio);
        editBio.setOnClickListener(this);

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

        // Read the user's data and get notified of any changes
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        User.getDatabaseRef(firebaseUser).addValueEventListener(
                new ValueEventListener() {
                    /**
                     * Called whenever the user's data changes
                     * Note: this gets called once when the ValueEventListener is first made
                     */
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // If the user's data changes, update the UI to show the changes
                        mUser = dataSnapshot.getValue(User.class);
                        showUser();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                }
        );
    }

    /**
     * Show or update the user's information on the UI
     */
    public void showUser() {
        mUser.loadProfilePic(this, mProfilePic);
        mNameText.setText(mUser.name);
        mBioText.setText(mUser.bio);
    }

    /**
     * Open the image picker to pick a new profile pic
     */
    public void pickProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // User just picked an image
        if (requestCode == PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }

            try {
                // Upload the selected profile picture, if possible
                mUser.setProfilePicture(this, data.getData(),
                        new OnSuccessListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                                showToast("Upload completed!");
                            }
                        }, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                showToast("Upload failed!");
                            }
                        });
            } catch (FileNotFoundException | URISyntaxException e) {
                e.printStackTrace();
                showToast("Can't open image");
            }
        }
    }

    /**
     * Show a popup box to change the user's biography
     */
    public void showEditBioDialog() {
        final EditText editText = new EditText(this);
        editText.setText(mUser.bio);

        AlertDialog.Builder alert =
                new AlertDialog.Builder(this)
                        .setTitle("Enter your new bio")
                        .setMessage(null)
                        .setView(editText);

        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mUser.updateBio(editText.getText().toString());
            }
        });
        alert.setNegativeButton("Cancel", null);

        alert.show();
    }

    public void showToast(CharSequence message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Triggered when a view we setOnClickListener on is clicked
     *
     * @param view the view that was clicked on
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.profilePic:
                pickProfilePicture();
                break;
            case R.id.editBio:
                showEditBioDialog();
                break;
        }
    }
}
