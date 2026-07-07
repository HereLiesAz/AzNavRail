package com.hereliesaz.aznavrail.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

internal actual fun createAzHttpClient(): HttpClient = HttpClient(Android)
