package com.hereliesaz.aznavrail.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

internal actual fun createAzHttpClient(): HttpClient = HttpClient(Js)
