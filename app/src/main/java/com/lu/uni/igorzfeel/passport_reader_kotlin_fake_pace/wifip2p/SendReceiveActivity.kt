package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.R
import kotlinx.android.synthetic.main.activity_send_receive.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset

class SendReceiveActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SendReceiveActivity"
        const val MESSAGE_READ = 1
    }

    private lateinit var sendReceive: SendReceive

    private var handler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val buffer = msg.obj as ByteArray
                val msgString = String(buffer, 0, msg.arg1)
                msgTextView.text = msgString
                updateLog(msgString)
            }
        }
        true
    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_receive)

        val status = intent.getStringExtra("connectionStatus")
        connectionStatus.text = status

        if (status == WifiConnectionActivity.CLIENT) {
            sendReceive = SendReceive(Client.socket)
            sendReceive.start()
            updateLog("Connected as $status")
        }
        else if (status == WifiConnectionActivity.SERVER) {
            sendReceive = Server.socket?.let {
                SendReceive(it)
            }!!
            sendReceive.start()
            updateLog("Connected as $status")
        }

        btnSend.setOnClickListener {
            sendReceive.SendMessage("This is from Server".toByteArray())
        }
    }

    private fun updateLog(msg: String) {
//        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        Log.i(TAG, msg);
    }


    inner class SendReceive(private val socket: Socket?) : Thread() {

        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        override fun run() {

            val buffer = ByteArray(1024)
            var bytes: Int
            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer)

                    if (bytes > 0) {
                        handler
                            .obtainMessage(WifiConnectionActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun SendMessage(bytes: ByteArray?) {
            try {
                outputStream.write(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            updateLog("sendReceive has been initialized")
            try {
                inputStream = socket!!.getInputStream()
                outputStream = socket.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
