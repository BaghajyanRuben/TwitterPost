package com.example.ruben.twitterpost;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.Media;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.User;
import com.twitter.sdk.android.core.services.MediaService;
import com.twitter.sdk.android.core.services.StatusesService;

import io.fabric.sdk.android.Fabric;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NetworkStateChangeReceiver.NetworkStateChangeListener {

	private static final String TWITTER_KEY = "94CS76PAnfLCs2j5otwGsr9np";
	private static final String TWITTER_SECRET = "u1QtjA2SdC9UXMWJMVfZhEN34MMskogT6uaL7nkdXcDBUBJhnp";
	private static int REQUEST_TAKE_PHOTO = 123;
	private static int REQUEST_CHOOSE_IMAGE = 124;

	private Uri imageUri;
	private FrameLayout loginContainer;
	private TwitterLoginButton loginButton;
	private TwitterSession session;
	private ImageView avatar;
	private AlertDialog noNetworkDialog;
	private NetworkStateChangeReceiver networkStateChangeReceiver;
	private Twitter twitter;
	private LinearLayout postView;
	private TextView nikname;
	private TextView userName;
	private File destination;
	private String screenName;

	private Callback<TwitterSession> twitterSessionCallback = new Callback<TwitterSession>() {
		@Override
		public void success(Result<TwitterSession> result) {
			loginContainer.setVisibility(View.GONE);
			session = result.data;

			Twitter.getApiClient(session).getAccountService().verifyCredentials(true, false).enqueue(new Callback<User>() {
				@Override
				public void success(Result<User> result) {
					User user = result.data;
					Glide.with(MainActivity.this).load(user.profileImageUrl).into(avatar);
					screenName = user.screenName;
					nikname.setText(String.format("@%s", screenName));
					userName.setText(user.name);
				}

				@Override
				public void failure(TwitterException exception) {
					exception.printStackTrace();
					if (!NetUtils.isConnected(MainActivity.this)) {
						noNetworkDialog = NetUtils.showNoNetworkDialog(MainActivity.this);
					}
				}
			});
		}

		@Override
		public void failure(TwitterException exception) {
			exception.printStackTrace();
			checkNetwork();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
		twitter = new Twitter(authConfig);
		Fabric.with(this, twitter);

		setContentView(R.layout.activity_main);

		initViews();

		networkStateChangeReceiver = new NetworkStateChangeReceiver();
		networkStateChangeReceiver.setNetworkStateChangeListener(this);

		IntentFilter filter = new IntentFilter();
		filter.addAction(NetUtils.CONNECTIVITY_CHANGE_ACTION);
		registerReceiver(networkStateChangeReceiver, filter);

	}

	private void initViews(){
		loginContainer = (FrameLayout) findViewById(R.id.login_container);

		avatar = (ImageView) findViewById(R.id.user_avatar);

		postView = (LinearLayout) findViewById(R.id.post_view);
		userName = (TextView) findViewById(R.id.tv_user_name);
		nikname = (TextView) findViewById(R.id.tv_nickname);

		findViewById(R.id.btn_choose_image).setOnClickListener(this);
		findViewById(R.id.btn_take_photo).setOnClickListener(this);
		findViewById(R.id.btn_show_history).setOnClickListener(this);

		loginButton = (TwitterLoginButton) findViewById(R.id.login_button);
		loginButton.setCallback(twitterSessionCallback);
	}

	private void initPostView(final String path){
		postView.setVisibility(View.VISIBLE);
		ImageView imageToPost = (ImageView) findViewById(R.id.image_to_post);

		final EditText description = (EditText) findViewById(R.id.et_description);


		Glide.with(this).load(path).into(imageToPost);


		findViewById(R.id.btn_post).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!NetUtils.isConnected(MainActivity.this)) {
					noNetworkDialog = NetUtils.showNoNetworkDialog(MainActivity.this);
					return;
				}
				String text = description.getText().toString();
				uploadPhoto(text, path);

			}
		});
	}

	@Override
	public void onClick(View v) {
		if (!NetUtils.isConnected(MainActivity.this)) {
			noNetworkDialog = NetUtils.showNoNetworkDialog(MainActivity.this);
			return;
		}
		switch (v.getId()) {
			case R.id.btn_choose_image:
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
				break;
			case R.id.btn_take_photo:
				destination = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
				imageUri = Uri.fromFile(destination);
				Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intentCamera, REQUEST_TAKE_PHOTO);
				break;
		}
	}

	@Override
	protected void onDestroy() {
		if (networkStateChangeReceiver != null) {
			unregisterReceiver(networkStateChangeReceiver);
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_TAKE_PHOTO) {
				galleryAddPic();

				initPostView(destination.getAbsolutePath());

			}
			if (requestCode == REQUEST_CHOOSE_IMAGE) {
				Uri uri = data.getData();

				String path = null;
				String[] projection = {MediaStore.Images.Media.DATA};

				Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
				if (cursor != null) {
					cursor.moveToFirst();
					int columnIndex = cursor.getColumnIndex(projection[0]);
					path = cursor.getString(columnIndex);
					cursor.close();
				}
				initPostView(path);
			}

			loginButton.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(imageUri);
		this.sendBroadcast(mediaScanIntent);
	}

	private void publishTweet(String text, String mediaID) {

		StatusesService statusesService = twitter.core.getApiClient(session).getStatusesService();
		statusesService.update(text, 0L, false, 0D, 0D, null, false, false, mediaID).enqueue(new Callback<Tweet>() {
			@Override
			public void success(Result<Tweet> tweetResult) {
				postView.setVisibility(View.GONE);
				Toast.makeText(MainActivity.this, "Post successful",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void failure(TwitterException e) {
				checkNetwork();
			}
		});

	}

	@Override
	public void onConnectionChange(boolean hasConnection) {
		if (hasConnection && noNetworkDialog != null && noNetworkDialog.isShowing()) {
			noNetworkDialog.dismiss();
		}
	}

	private void checkNetwork(){
		if (!NetUtils.isConnected(MainActivity.this)) {
			noNetworkDialog = NetUtils.showNoNetworkDialog(MainActivity.this);
		} else {
			Toast.makeText(MainActivity.this, "Something went wrong ", Toast.LENGTH_SHORT).show();
		}
	}

	private void uploadPhoto(final String text, String path){

		if (TextUtils.isEmpty(path)){
			Toast.makeText(MainActivity.this, "No image found", Toast.LENGTH_SHORT).show();
			return;
		}

		File file = new File(path);

		TwitterApiClient authClient = twitter.core.getApiClient(session);
		MediaService ms = authClient.getMediaService();

		RequestBody requestFile =  RequestBody.create(MediaType.parse("image/*"), file);
		ms.upload(requestFile, null, null).enqueue(new Callback<Media>() {
			@Override
			public void success(Result<Media> result) {
				publishTweet(text, String.valueOf(result.data.mediaId));
			}

			@Override
			public void failure(TwitterException e) {
				e.printStackTrace();

			}
		});
	}
}
