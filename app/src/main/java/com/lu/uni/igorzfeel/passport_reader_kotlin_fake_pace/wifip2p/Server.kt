package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class Server : Thread() {
    val serverPort = 9999
    var isStopped = true

    var clientSocket: Socket? = null
    var serverSocket: ServerSocket? = null


    override fun run() {
        openServerSocket()

        try {
            clientSocket = serverSocket!!.accept()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun closeServerSocket() {
        this.isStopped = true
        try {
            serverSocket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun openServerSocket() {
        try {
            serverSocket = ServerSocket(serverPort)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}