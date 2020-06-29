package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = "MainActivity"

        val OK_RAPDU        = Utils.hexStringToByteArray("9000")
        val FAILED_RAPDU    = Utils.hexStringToByteArray("6F00")
        val UNKNOWN_RAPDU   = Utils.hexStringToByteArray("0000")
        val SELECT_CAPDU    = Utils.hexStringToByteArray("00A4040C07A0000002471001")
        val CA1_CAPDU       = Utils.hexStringToByteArray("00A4020C02011C")
        val CA2_CAPDU       = Utils.hexStringToByteArray("00B0000008")
        val CA2_RAPDU       = Utils.hexStringToByteArray("31143012060A04009000")
        val CA3_CAPDU       = Utils.hexStringToByteArray("00B000080E")
        val CA3_RAPDU       = Utils.hexStringToByteArray("7F000702020402040201020201109000")
        val PACE0_CAPDU     = Utils.hexStringToByteArray("0022C1A412800A04007F00070202040204830102840110")
        val PACE0_RAPDU     = Utils.hexStringToByteArray("9000")
        val PACE1_CAPDU     = Utils.hexStringToByteArray("10860000027C0000")
        val PACE1_RAPDU     = Utils.hexStringToByteArray("7C228020C6E82CE4A0F80BAB907A95E573090DAA94BF4227FAB66DD4C46E6154E28992659000")
        val PACE2_CAPDU     = Utils.hexStringToByteArray("10860000657C63816104")
        val PACE2_RAPDU     = Utils.hexStringToByteArray("7C638261046D756A258E35BE0A4ED2C03FAA6EC4C814D6B35922B94B1DD755A4F86E50B3DA3B7F3BACD7B64B7312F537335E3089D6260248E2D21B45E1D143441FA6B94500F10CAAE948C002C68DDE4545236E2B15349C50DD1BAC40B0FC72BECE3277A1889000")
        val PACE3_CAPDU     = Utils.hexStringToByteArray("10860000657C63836104")
        val PACE3_RAPDU     = Utils.hexStringToByteArray("7C6384610407DCE7549D3057CDEED8FF8E46B0C286768D79AE949B2D49AC7B55981F3D5711E693BBCD0C6EC65DA3716DEA394AFA323854691E3EC50E2D514B108C6057D13F6FD916765889B6D534A916AB943719B3956E7A66FE7EC6A350FAC5B663F5F2569000")
        val PACE4_CAPDU     = Utils.hexStringToByteArray("008600000C7C0A8508")
        val PACE4_RAPDU     = Utils.hexStringToByteArray("7C0A860859088653F6DC213F9000")
    }
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
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
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        val isoDep = IsoDep.get(tag)
        isoDep.connect()

        updateLog("\nSending: " + Utils.toHex(CA1_CAPDU))
        var response = isoDep.transceive(CA1_CAPDU)
        updateLog("\nResponse: " + Utils.toHex(response))

        if (!Arrays.equals(OK_RAPDU, response)) {
            updateLog("response should be: " + Utils.toHex(OK_RAPDU))
            return
        }

        updateLog("\nSending: " + Utils.toHex(CA2_CAPDU))
        response = isoDep.transceive(CA2_CAPDU)
        updateLog("\nResponse: " + Utils.toHex(response))

        updateLog("\nSending: " + Utils.toHex(CA3_CAPDU))
        response = isoDep.transceive(CA3_CAPDU)
        updateLog("\nResponse: " + Utils.toHex(response))

        isoDep.close()
    }

    private fun updateLog(msg: String) {
        Log.i(TAG, msg)
        runOnUiThread {textView.append(msg + "\n") }
    }
}
