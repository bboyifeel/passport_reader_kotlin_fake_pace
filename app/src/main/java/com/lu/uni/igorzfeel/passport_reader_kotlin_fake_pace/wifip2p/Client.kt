package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class Client(hostAddr: InetAddress) : Thread() {
    val serverPort = 9999
    var clientSocket: Socket = Socket()

    private var hostAddr: String = hostAddr.hostAddress

    override fun run() {
        try {
            clientSocket.connect(InetSocketAddress(hostAddr, serverPort), 500)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}