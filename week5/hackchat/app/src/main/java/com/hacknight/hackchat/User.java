package com.hacknight.hackchat;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A class representing all of the information we have about a user
 */
public class User {
    public static DatabaseReference getDatabaseRef(FirebaseUser user) {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(user.getUid());
    }

    Uri picName;
    String id;
    String name;
    String token;
    String bio;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("picName", picName.toString());
        map.put("id", id);
        map.put("name", name);
        map.put("token", token);
        map.put("bio", bio);
        return map;
    }

    public void setPicName(String picName) {
        this.picName = Uri.parse(picName);
    }

    /**
     * Update the URL of the user's profile picture in our database
     */
    public void updatePicName(String picName) {
        this.picName = Uri.parse(picName);
        getDatabaseRef().child("picName").setValue(picName);
    }

    /**
     * Update the user's biography in our database
     */
    public void updateBio(String bio) {
        this.bio = bio;
        getDatabaseRef().child("bio").setValue(bio);
    }

    public DatabaseReference getDatabaseRef() {
        return FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(id);
    }

    /**
     * Load this user's profile picture into an imageView
     */
    public void loadProfilePic(Context context, ImageView imageView) {
        if (picName.toString().isEmpty()) {
            return;
        }

        if (picName.getScheme().equals("http") || picName.getScheme().equals("https")) {
            Glide.with(context)
                    .load(picName)
                    .into(imageView);
        } else if (picName.getScheme().equals("gs")) {
            StorageReference storageReference = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(picName.toString());
            Glide.with(context)
                    .using(new FirebaseImageLoader())
                    .load(storageReference)
                    .into(imageView);
        }
    }

    /**
     * Change the profile picture of this user
     *
     * @param context
     * @param uri               If this is set to null, the profile picture is deleted
     * @param onSuccessListener
     * @param onFailureListener
     */
    public void setProfilePicture(Context context, Uri uri,
                                  final OnSuccessListener<Object> onSuccessListener,
                                  OnFailureListener onFailureListener)
            throws FileNotFoundException, URISyntaxException {
        deleteProfilePic();

        if (uri == null) {
            onSuccessListener.onSuccess(null);
            return;
        }

        if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
            picName = uri;
            onSuccessListener.onSuccess(null);
            return;
        }

        InputStream inputStream = null;
        if (uri.getScheme().equals("content")) {
            inputStream = context.getContentResolver().openInputStream(uri);
        } else if (uri.getScheme().equals("file")) {
            inputStream = new FileInputStream(new File(new URI(uri.toString())));
        }

        picName = Uri.parse(String.format("gs://hack-night-messaging.appspot.com/%s/%s.png",
                id, UUID.randomUUID()));

        StorageReference newPic = FirebaseStorage.getInstance()
                .getReferenceFromUrl(picName.toString());
        newPic.putStream(inputStream)
                .addOnFailureListener(onFailureListener)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        updatePicName(picName.toString());
                        onSuccessListener.onSuccess(taskSnapshot);
                    }
                });
    }

    /**
     * Delete the user's profile picture from our database and storage
     */
    private void deleteProfilePic() {
        if (picName.toString().isEmpty()) {
            return;
        }

        if (picName.getScheme().equals("http") || picName.getScheme().equals("https")) {
            updatePicName("");
        } else if (picName.getScheme().equals("gs")) {
            StorageReference storageReference = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(picName.toString());
            storageReference.delete();
        }
    }
}
