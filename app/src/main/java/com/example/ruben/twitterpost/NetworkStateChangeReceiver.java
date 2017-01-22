package com.example.ruben.twitterpost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NetworkStateChangeReceiver extends BroadcastReceiver {

	private NetworkStateChangeListener networkStateChangeListener;

	public NetworkStateChangeReceiver setNetworkStateChangeListener(NetworkStateChangeListener networkStateChangeListener) {
		this.networkStateChangeListener = networkStateChangeListener;
		return this;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (NetUtils.CONNECTIVITY_CHANGE_ACTION.equals(intent.getAction())) {
			if (networkStateChangeListener != null){
				networkStateChangeListener.onConnectionChange(NetUtils.isConnected(context));
			}
		}
	}


	public interface NetworkStateChangeListener{
		void onConnectionChange(boolean hasConnection);
	}
}
