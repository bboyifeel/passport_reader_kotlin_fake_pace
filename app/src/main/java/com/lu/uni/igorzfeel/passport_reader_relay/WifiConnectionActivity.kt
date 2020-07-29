package com.lu.uni.igorzfeel.passport_reader_relay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_wifi_connection.*
import java.net.InetAddress
import java.util.*


open class WifiConnectionActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WifiConnectionActivity"
        const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
        const val SERVER = "Server"
        const val CLIENT = "Client"
        const val MESSAGE_READ = 1
        lateinit var groupOwnerAddress: InetAddress
    }

//    var wifiManager: WifiManager? = null
    var p2pManager: WifiP2pManager? = null
    var p2pChannel: WifiP2pManager.Channel? = null
    var broadcastReceiver: BroadcastReceiver? = null
    var intentFilter: IntentFilter = IntentFilter()
    var peers: MutableList<WifiP2pDevice> = ArrayList()

    private lateinit var deviceNameArray: Array<String?>
    private lateinit var deviceArray: Array<WifiP2pDevice?>


    private val peerListListener = PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList

        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            deviceNameArray = arrayOfNulls(refreshedPeers.size)
            deviceArray     = arrayOfNulls(refreshedPeers.size)

            var index = 0

            for (device in refreshedPeers) {
                deviceNameArray[index]  = device.deviceName
                deviceArray[index]      = device
                index++
            }

            val adapter = ArrayAdapter(
                applicationContext,
                android.R.layout.simple_list_item_1,
                deviceNameArray
            )
            peerListView!!.adapter = adapter
        }
        if (peers.size == 0) {
            updateLog("No device found")
        }
    }


    private var connectionInfoListener = ConnectionInfoListener { info ->

        updateLog("connectionInfoListener")
        groupOwnerAddress = info.groupOwnerAddress
        if (info.groupFormed && info.isGroupOwner) {
            connectionStatus!!.text =
                SERVER
        } else if (info.groupFormed) {
            connectionStatus!!.text =
                CLIENT
        } else {
            return@ConnectionInfoListener
        }

        val intent = Intent(this, PassportRelayActivity::class.java)
        intent.putExtra("connectionStatus", connectionStatus.text)
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_connection)

        grandPermission()
        sdkValidation()
        init()

        peerListView!!.onItemClickListener =
            OnItemClickListener {
                    adapterView, view, i, id ->
                val device = deviceArray[i]
                val config = WifiP2pConfig()
                config.deviceAddress = device!!.deviceAddress

                p2pManager!!.connect(p2pChannel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        updateLog("Connected to " + device.deviceName)
                    }

                    override fun onFailure(reason: Int) {
                        updateLog("Unable to connect to " + device.deviceName)
                    }
                })
            }
    }


    private fun grandPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            !== PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION
            )
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
    }


    private fun sdkValidation() {
        val SDK_INT = Build.VERSION.SDK_INT
        if (SDK_INT > 8) {
            val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
    }


    private fun updateLog(msg: String) {
//        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        Log.i(TAG, msg)
    }


    private fun init() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

//        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        p2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        p2pChannel = p2pManager!!.initialize(this, mainLooper, null)

        broadcastReceiver =
            WiFiDirectBroadcastReceiver(
                p2pManager!!,
                p2pChannel!!,
                this,
                peerListListener,
                connectionInfoListener,
                connectionStatus
            )


        btnDiscover.setOnClickListener {
            peerListView!!.adapter = null
            p2pManager!!.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    connectionStatus!!.text = "Discovery Started"
                }

                override fun onFailure(reason: Int) {
                    connectionStatus!!.text = "Discovery Starting Failure"
                }
            })
        }
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, intentFilter)
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }
}
