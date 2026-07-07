package com.hereliesaz.aznavrail.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createAzHttpClient(): HttpClient = HttpClient(CIO)
