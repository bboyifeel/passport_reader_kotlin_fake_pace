package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class Server : Thread() {
    companion object {
        var socket: Socket? = null
    }

    var serverSocket: ServerSocket? = null

    override fun run() {
        try {
            serverSocket = ServerSocket(8888)
            socket = serverSocket!!.accept()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}