package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_wifi_connection.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*


open class WifiConnectionActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WifiConnectionActivity"
        const val MESSAGE_READ = 1
        const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
        var serverClass: ServerClass? = null
        var clientClass: ClientClass? = null
        var sendReceive: SendReceive? = null
    }

    var wifiManager: WifiManager? = null
    var p2pManager: WifiP2pManager? = null
    var p2pChannel: WifiP2pManager.Channel? = null
    var broadcastReceiver: BroadcastReceiver? = null
    var intentFilter: IntentFilter? = null
    var peers: MutableList<WifiP2pDevice> = ArrayList()

    private lateinit var deviceNameArray: Array<String?>
    private lateinit var deviceArray: Array<WifiP2pDevice?>

    private var handler = Handler(Handler.Callback { msg ->

        when (msg.what) {
            MESSAGE_READ -> {
                val challengeBytes = msg.obj as ByteArray
                val challenge = String(challengeBytes, 0, 8)
                updateLog("Nonce has been updated with ")
//
//                CardService.NONCE =
//                    CardService.ByteArrayToHexString(challenge.toByteArray()) // has to have format "4141414141414141"
//                      updateLog("Nonce has been updated with ")
            }
        }
        true
    })

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
        val groupOwnerAddress = info.groupOwnerAddress
        if (info.groupFormed && info.isGroupOwner) {
            connectionStatus!!.text = "Host"
            serverClass = ServerClass()
            serverClass!!.start()
        } else if (info.groupFormed) {
            connectionStatus!!.text = "Client"
            clientClass = ClientClass(groupOwnerAddress)
            clientClass!!.start()
        }
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
        Toast.makeText(this.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun init() {
        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        p2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        p2pChannel = p2pManager!!.initialize(this, mainLooper, null)

        broadcastReceiver = WiFiDirectBroadcastReceiver(
            p2pManager!!,
            p2pChannel!!,
            this,
            peerListListener,
            connectionInfoListener,
            connectionStatus
        )

        intentFilter = IntentFilter()
        intentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)


        btnDiscover!!.setOnClickListener {
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


    class ServerClass : Thread() {
        var socket: Socket? = null
        var serverSocket: ServerSocket? = null

        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                socket = serverSocket!!.accept()
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    class SendReceive(private val socket: Socket?) : Thread() {

        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (socket != null) {
                try {
                    bytes = inputStream!!.read(buffer)
                    if (bytes > 0) {
//                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            try {
                inputStream = socket!!.getInputStream()
                outputStream = socket.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    class ClientClass(hostAddr: InetAddress) : Thread() {
        var socket: Socket = Socket()
        var hostAddr: String = hostAddr.hostAddress

        override fun run() {
            try {
                socket.connect(InetSocketAddress(hostAddr, 8888), 500)
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}