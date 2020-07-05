package com.lu.uni.igorzfeel.passport_reader_kotlin_fake_pace.wifip2p

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class Client(hostAddr: InetAddress) : Thread() {
    companion object {
        var socket: Socket = Socket()
    }

    private var hostAddr: String = hostAddr.hostAddress

    override fun run() {
        try {
            socket.connect(InetSocketAddress(hostAddr, 8888), 500)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}