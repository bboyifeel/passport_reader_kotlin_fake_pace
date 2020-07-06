package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Parcelable
import android.widget.TextView
import android.widget.Toast


class WiFiDirectBroadcastReceiver(
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
    activity: Activity,
    peerListListener: PeerListListener,
    connectionInfoListener: ConnectionInfoListener,
    connectionStatus: TextView
) : BroadcastReceiver() {

    companion object {
        private var mManager: WifiP2pManager? = null
        private var mChannel: WifiP2pManager.Channel? = null
    }

    private val mActivity: Activity
    private var peerListListener: PeerListListener
    private var connectionInfoListener: ConnectionInfoListener
    private var connectionStatus: TextView


    override fun onReceive(context: Context, intent: Intent) {

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == intent.action) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(mActivity, "Wifi p2p is ON", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mActivity, "Wifi p2p is OFF", Toast.LENGTH_SHORT).show()
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == intent.action) {
            mManager?.requestPeers(
                mChannel, peerListListener)

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == intent.action) {

            if (mManager == null)
                return

            val netwinfo = intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo
            if (netwinfo.isConnected) {
                Toast.makeText(mActivity, "Device is Connected", Toast.LENGTH_SHORT).show()
                mManager?.requestConnectionInfo(
                    mChannel, connectionInfoListener)
            } else {
                Toast.makeText(mActivity, "Device is Disconnected", Toast.LENGTH_SHORT).show()
                connectionStatus.text = "Device Disconnected"
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == intent.action) {
            // DO something
        }
    }


    init {
        mManager = manager
        mChannel = channel
        mActivity = activity
        this.peerListListener = peerListListener
        this.connectionInfoListener = connectionInfoListener
        this.connectionStatus = connectionStatus
    }
}
