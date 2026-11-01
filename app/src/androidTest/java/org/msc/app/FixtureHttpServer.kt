package org.msc.app

import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Test-only loopback server serving original audio or synthetic JSON, including byte ranges. */
internal class FixtureHttpServer(private val bytes: ByteArray, private val contentType: String, private val status: Int = 200) : Closeable {
    private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val worker = Executors.newSingleThreadExecutor()
    val requests = AtomicInteger()
    val url = "http://127.0.0.1:${socket.localPort}/fixture"
    init {
        worker.submit {
            while (!socket.isClosed) {
                val client = try { socket.accept() } catch (_: java.io.IOException) { break }
                try { client.use {
                    client.soTimeout = 5000
                    val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
                    reader.readLine() ?: return@use
                    var start = 0
                    var partial = false
                    var headers = 0
                    while (headers++ < 50) {
                        val line = reader.readLine().orEmpty()
                        if (line.isBlank()) break
                        if (line.startsWith("Range:", ignoreCase = true)) {
                            start = line.substringAfter("bytes=", "0").substringBefore('-').toIntOrNull() ?: 0
                            partial = true
                        }
                    }
                    requests.incrementAndGet()
                    start = start.coerceIn(0, bytes.size)
                    val code = if (status == 200 && partial) 206 else status
                    val header = buildString {
                        append("HTTP/1.1 $code Fixture\r\nContent-Type: $contentType\r\n")
                        append("Content-Length: ${bytes.size - start}\r\nAccept-Ranges: bytes\r\nConnection: close\r\n")
                        if (partial && bytes.isNotEmpty()) append("Content-Range: bytes $start-${bytes.lastIndex}/${bytes.size}\r\n")
                        append("\r\n")
                    }
                    client.getOutputStream().apply { write(header.toByteArray(Charsets.US_ASCII)); write(bytes, start, bytes.size - start); flush() }
                } } catch (_: java.io.IOException) { /* Client cancellation closes the socket. */ }
            }
        }
    }
    override fun close() { socket.close(); worker.shutdownNow(); worker.awaitTermination(6, java.util.concurrent.TimeUnit.SECONDS) }
}
