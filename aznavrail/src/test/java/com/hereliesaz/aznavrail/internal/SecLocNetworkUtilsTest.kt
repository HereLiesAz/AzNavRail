package com.hereliesaz.aznavrail.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
class SecLocNetworkUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Write a mock log file
        val file = SecLocLogManager.getLogFile(context)
        file.writeText("123456789|1.0|2.0|gps\n")
    }

    @After
    fun teardown() {
        SecLocNetworkUtils.stopServer()
        val file = SecLocLogManager.getLogFile(context)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun fetchLogs_withCorrectSecret_returnsLogs() = runBlocking {
        val secret = "my_secret_key"

        // Start server in background
        val job = launch(Dispatchers.IO) {
            SecLocNetworkUtils.startServer(context, secret)
        }

        delay(500) // Wait for server to start

        val logs = SecLocNetworkUtils.fetchLogs("127.0.0.1", secret)

        assertEquals(1, logs.size)
        assertEquals(1.0, logs[0].lat, 0.0)
        assertEquals(2.0, logs[0].lng, 0.0)
        assertEquals("gps", logs[0].provider)

        SecLocNetworkUtils.stopServer()
        job.cancel()
    }

    @Test
    fun server_rejectsConnection_withIncorrectSecret() = runBlocking {
        val secret = "my_secret_key"

        // Start server in background
        val job = launch(Dispatchers.IO) {
            SecLocNetworkUtils.startServer(context, secret)
        }

        delay(500) // Wait for server to start

        // Manually connect with wrong secret
        var response = ""
        try {
            val socket = Socket("127.0.0.1", 10203)
            val writer = PrintWriter(socket.getOutputStream(), true)
            writer.println("wrong_secret")
            writer.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val line = reader.readLine()
            if (line != null) {
                response = line
            }
            socket.close()
        } catch (e: Exception) {
            // Expected to be closed or fail
        }

        // Server should have closed connection without sending data
        assertTrue("Expected empty response but got: \$response", response.isEmpty())

        SecLocNetworkUtils.stopServer()
        job.cancel()
    }
}
