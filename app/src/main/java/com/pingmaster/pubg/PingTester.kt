package com.pingmaster.pubg

import java.net.InetSocketAddress
import java.net.Socket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ThreadLocalRandom

object PingTester {
    data class Endpoint(val name: String, val host: String)

    // TCP connect time to port 443. This is not PUBG game-server ping.
    fun measureTcp(host: String, timeoutMs: Int = 1800): Long? {
        val socket = Socket()
        return try {
            val started = System.nanoTime()
            socket.connect(InetSocketAddress(host, 443), timeoutMs)
            ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally { try { socket.close() } catch (_: Exception) { } }
    }

    // UDP DNS query/response RTT. This measures the DNS resolver path, not arbitrary UDP/game traffic.
    fun measureUdpDns(host: String, timeoutMs: Int = 1800): Long? {
        val id = ThreadLocalRandom.current().nextInt(0, 65536)
        val query = buildDnsQuery(id, "example.com")
        val socket = DatagramSocket()
        return try {
            socket.soTimeout = timeoutMs
            val address = java.net.InetAddress.getByName(host)
            val request = DatagramPacket(query, query.size, address, 53)
            val started = System.nanoTime()
            socket.send(request)
            val buffer = ByteArray(512)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val elapsed = ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(1)
            if (response.length >= 12 &&
                ((buffer[0].toInt() and 0xff) shl 8 or (buffer[1].toInt() and 0xff)) == id &&
                (buffer[2].toInt() and 0x80) != 0) elapsed else null
        } catch (_: Exception) {
            null
        } finally { socket.close() }
    }

    private fun buildDnsQuery(id: Int, domain: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write((id shr 8) and 0xff); out.write(id and 0xff)
        out.write(0x01); out.write(0x00) // recursion desired
        out.write(0x00); out.write(0x01) // QDCOUNT
        out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00)
        out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00)
        domain.split(".").forEach { part ->
            val bytes = part.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size); out.write(bytes)
        }
        out.write(0x00)
        out.write(0x00); out.write(0x01) // A
        out.write(0x00); out.write(0x01) // IN
        return out.toByteArray()
    }
}
