package com.example.ruben.twitterpost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

/**
 * Created by ruben on 1/22/17.
 */

public class NetUtils {

	public static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

	public static boolean isConnected(Context context) {

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();

		if (netinfo != null && netinfo.isConnectedOrConnecting()) {
			android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

			return (mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting());

		} else
			return false;
	}

	public static AlertDialog showNoNetworkDialog(final Activity activity) {
		if (activity == null) {
			return null;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
				.setMessage(R.string.no_network_massage)
				.setTitle(R.string.network_failed)
				.setPositiveButton(R.string.network_settings, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});

		final AlertDialog alertDialog = builder.create();
		if (!activity.isFinishing() && alertDialog != null && !alertDialog.isShowing()) {
			alertDialog.show();
		}
		return alertDialog;


	}

}
