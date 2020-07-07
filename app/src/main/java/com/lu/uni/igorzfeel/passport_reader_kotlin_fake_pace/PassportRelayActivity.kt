package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_passport_relay.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*


class PassportRelayActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = "PassportRelayActivity"
        const val MESSAGE_READ = 1
        var server: Server? = null
        var client: Client? = null

        val OK_RAPDU        = Utils.hexStringToByteArray("9000")
        val CA1_CAPDU       = Utils.hexStringToByteArray("00A4020C02011C")
    }
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var isoDep: IsoDep
    private var status: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passport_relay)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        status = intent.getStringExtra("connectionStatus")
        updateLog(status)

        try {

            if (status == WifiConnectionActivity.CLIENT)
                initializeClient()
            else if (status == WifiConnectionActivity.SERVER)
                initializeServer()

        } catch(e: Exception) {
            updateError(e.toString())
        }

        btnSend.setOnClickListener {
            sendReceive.sendMessage("This is a test message")
        }
    }


    private fun initializeServer() {
        server = Server()
        server!!.start()

        updateLog("Connected as $status")
    }


    private fun initializeClient() {
        client = Client(WifiConnectionActivity.groupOwnerAddress)
        client!!.start()

        updateLog("Connected as $status")
    }


    public override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null)
    }


    public override fun onPause() {
        super.onPause()
        if (this::isoDep.isInitialized) {
            isoDep.close()
        }
        nfcAdapter?.disableReaderMode(this)
    }


    override fun onTagDiscovered(tag: Tag?) {
        if (this::isoDep.isInitialized) {
            isoDep.close()
        }

        isoDep = IsoDep.get(tag)
        isoDep.connect()

        startReadingCardAccessFile()
    }

    private fun startReadingCardAccessFile() {
        updateLog("capdu: " + Utils.toHex(CA1_CAPDU))
        var CA1_RAPDU = isoDep.transceive(CA1_CAPDU)
        updateLog("rapdu: " + Utils.toHex(CA1_RAPDU))

        if (!Arrays.equals(OK_RAPDU, CA1_RAPDU)) {
            updateLog("This card does not implement PACE" + Utils.toHex(OK_RAPDU))
            return
        }

        updateLog("[->] ${Utils.toHex(CA1_RAPDU)}")
        Thread() {
            sendReceive.sendMessage(Utils.toHex(CA1_RAPDU))
        }.start()
    }


    private fun updateLog(msg: String) {
        Log.i(TAG, msg)
        runOnUiThread {textView.append(msg + "\n") }
    }


    private fun updateError(msg: String) {
        Log.e(TAG, msg)
        runOnUiThread {textView.append("[ERROR] " + msg + "\n") }
    }


    private lateinit var sendReceive: SendReceive

    private var handler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val buffer = msg.obj as ByteArray
                val msgString = String(buffer, 0, msg.arg1)

                updateLog("[<-] ${msgString}")
                var response = isoDep.transceive(Utils.hexStringToByteArray(msgString))
                updateLog("[->] ${Utils.toHex(response)}")

                sendReceive.sendMessage(Utils.toHex(response))
            }
        }
        true
    })


    inner class SendReceive(private val socket: Socket?) : Thread() {

        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        override fun run() {
            updateLog("sendReceive has been initialized and started")
            try {
                inputStream = socket!!.getInputStream()
                outputStream = socket.getOutputStream()

                val buffer = ByteArray(1024)
                var bytes: Int
                while (true) {
                    try {
                        bytes = inputStream.read(buffer)

                        if (bytes == -1) {
                            break;
                        }

                        handler.obtainMessage(WifiConnectionActivity.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget()

                    } catch (e: IOException) {
                        updateError(e.toString())
                    }
                }
            } catch (e: IOException) {
                updateError(e.toString())
            } finally {
                try {
                    socket?.close();
                } catch (e: IOException) {
                    updateError(e.toString())
                }
            }
        }


        fun sendMessage(msg: String) {
            write(msg.toByteArray())
        }


        fun write(bytes: ByteArray?) {
            try {
                outputStream.write(bytes)
            } catch (e: Exception) {
                updateError(e.toString())
            }
        }


    }

    inner class Client(hostAddr: InetAddress) : Thread() {
        val serverPort = 9999
        var socket: Socket = Socket()

        private var hostAddr: String = hostAddr.hostAddress

        override fun run() {
            try {
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddr, serverPort), 5000)
                sendReceive = SendReceive(socket)
                sendReceive.start()
            } catch (e: IOException) {
                updateError(e.toString())
                try {
                    socket.close()
                } catch (e: IOException) {
                    updateError(e.toString())
                }
                return
            }
        }
    }

    inner class Server : Thread() {
        val serverPort = 9999
        var socket: ServerSocket? = null

        override fun run() {
            openServerSocket()

            while (true) {
                try {
                    sendReceive = SendReceive(socket!!.accept())
                    sendReceive.start()
                } catch (e: IOException) {
                    try {
                        if (socket != null && !socket!!.isClosed)
                            socket!!.close()
                    } catch (ioe: IOException) {
                    }
                    updateError(e.toString())
                    break
                }
            }

        }


        private fun openServerSocket() {
            try {
                socket = ServerSocket(serverPort)
            } catch (e: IOException) {
                updateError(e.toString())
            }
        }
    }
}
