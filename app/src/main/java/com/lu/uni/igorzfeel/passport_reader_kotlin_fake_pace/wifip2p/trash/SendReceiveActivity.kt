package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p.trash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.R
import com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p.WifiConnectionActivity
import kotlinx.android.synthetic.main.activity_send_receive.*

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
//                msgTextView.text = msgString
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
            sendReceive =
                SendReceive(
                    Client.socket,
                    handler
                )
            sendReceive.start()
            updateLog("Connected as $status")

        }
        else if (status == WifiConnectionActivity.SERVER) {
            sendReceive = Server.socket?.let {
                SendReceive(
                    it,
                    handler
                )
            }!!
            sendReceive.start()
            updateLog("Connected as $status")
//            sendReceive.write("This message is from $status" as ByteArray)
        }

//
    }


    private fun updateLog(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
