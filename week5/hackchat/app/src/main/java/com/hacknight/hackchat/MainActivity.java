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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, Response.Listener<JSONObject> {

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE = 111;

    private ImageView mProfilePic;
    private User mUser;
    private TextView mNameText;
    private TextView mBioText;
    private ListView mUserList;

    private RequestQueue mRequestQueue;
    private FirebaseListAdapter<User> mUserAdapter;
    private DatabaseReference mDbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the UI of our application
        setContentView(R.layout.activity_main);

        notDoneYo();

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

        mRequestQueue = Volley.newRequestQueue(this);

        // Get references to all of the views we need
        mNameText = (TextView) findViewById(R.id.myFullName);
        mProfilePic = (ImageView) findViewById(R.id.profilePic);
        mBioText = (TextView) findViewById(R.id.myBio);
        mUserList = (ListView) findViewById(R.id.userList);

        // Change the profile pic when the picture is clicked
        mProfilePic.setOnClickListener(this);

        mDbRef = FirebaseDatabase.getInstance().getReference();

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

        mUserAdapter = new FirebaseListAdapter<User>(this, User.class, R.layout.user_list_item,
                FirebaseDatabase.getInstance().getReference().child("users")) {
            @Override
            protected void populateView(View v, User model, int position) {
                model.loadProfilePic(MainActivity.this, ((ImageView) v.findViewById(R.id.picture)));
                ((TextView) v.findViewById(R.id.name)).setText(model.name);
                ((TextView) v.findViewById(R.id.bio)).setText(model.bio);
            }
        };
        mUserList.setAdapter(mUserAdapter);
        mUserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showMessagesDialog(mUserAdapter.getItem(i));
            }
        });

        subscribeUserInfo();
    }

    private void notDoneYo() {
        throw new IllegalStateException("Hey! Did you go to Tools -> Firebase -> click on any of "
                + "the items and connect the app to your Firebase account? You can't use my "
                + "google-services.json file, which is in hackchat\\app. You'll want to delete it,"
                + "maybe restart Android Studio, then");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserAdapter.cleanup();
    }

    /**
     * Show a popup box with a list of messages
     * This code is a little deficient. It'll crash if you haven't messaged before
     * To fix, borrow some code from sendMessage!
     */
    public void showMessagesDialog(final User other) {
        View messageView = View.inflate(this, R.layout.message_list, null);

        // Oh god this is so terrible don't do this in real life
        // Use a ListView instead!
        final LinearLayout messageList = (LinearLayout) messageView.findViewById(R.id.message_list);
        mDbRef.child("conversations").child(mUser.id + other.id)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        messageList.removeAllViews();
                        Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                        int messageCount = ((Long) data.get("message_count")).intValue();
                        for (int i = 1; i <= messageCount; i++) {
                            TextView textView = new TextView(MainActivity.this);
                            textView.setText((String) data.get(String.valueOf(i)));
                            messageList.addView(textView);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        final EditText messageText = (EditText) messageView.findViewById(R.id.message_text);
        messageView.findViewById(R.id.send_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(mUser, other, messageText.getText().toString());
                messageText.setText("");
            }
        });

        AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Messages")
                .setMessage(null)
                .setView(messageView);

        alert.setNegativeButton("Done", null);
        alert.show();
    }

    /**
     * Just to be clear, you should never structure a real messaging ap this way
     * First big issues is that having the key for the conversation be user1.id+user2.id
     * means that when the other user initiates conversationg with you, it's user2.id+user2.id,
     * which is different!
     */
    public void sendMessage(final User user1, final User user2, final String messageText) {
        mDbRef.child("conversations").child(user1.id + user2.id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int messageCount = 0;
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map == null) {
                            map = new HashMap<>();
                            map.put("message_count", 0);
                            mDbRef.child("conversations").child(user1.id + user2.id)
                                    .setValue(map);
                        } else {
                            messageCount = ((Long) dataSnapshot.child("message_count").getValue())
                                    .intValue();
                        }

                        map.put(String.valueOf(messageCount + 1), messageText);
                        map.put("message_count", messageCount + 1);

                        mDbRef.child("conversations").child(user1.id + user2.id)
                                .setValue(map);

                        try {
                            mRequestQueue.add(makeMessageRequest(user2.token, "Yooo", "Yo"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Listen to changes to the user's data and update the UI accordingly
     */
    private void subscribeUserInfo() {
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
     * Convencience method to make a request for sending a message
     */
    @NonNull
    private JsonObjectRequest makeMessageRequest(String to, String title, String body)
            throws JSONException {
        JSONObject notification = new JSONObject()
                .put("title", title)
                .put("body", body);
        JSONObject request = new JSONObject()
                .put("to", to)
                .put("notification", notification);
        return (new JsonObjectRequest(Request.Method.POST,
                "https://fcm.googleapis.com/fcm/send", request,
                MainActivity.this,
                null) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                stillNotDoneYo();
                params.put("Authorization", "key=");
                params.put("Content-Type", "application/json");
                return params;
            }
        });
    }

    private Map<String, String> stillNotDoneYo() {
        throw new IllegalStateException("Hey! You can't use my server key! Get your own"
                + "from console.firebase.google.com/project/[Your project name]/settings/cloudmessaging");
    }

    /**
     * Show or update the user's information on the UI
     */
    public void showUser() {
        mUser.loadProfilePic(this, mProfilePic);
        mNameText.setText(mUser.name);
        mBioText.setText(mUser.bio);

        if (mUser.name == null || mUser.name.isEmpty()) {
            showNameDialog();
        }
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

    /**
     * Show a popup box to set the user's display name
     */
    public void showNameDialog() {
        final EditText editText = new EditText(this);

        AlertDialog.Builder alert =
                new AlertDialog.Builder(this)
                        .setTitle("What's your name?")
                        .setMessage(null)
                        .setView(editText);

        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mUser.updateName(editText.getText().toString());
            }
        });

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

    @Override
    public void onResponse(JSONObject response) {
        Log.i("News", response.toString());
    }
}
