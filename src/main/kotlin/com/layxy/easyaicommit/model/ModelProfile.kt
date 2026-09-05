package com.layxy.easyaicommit.model

data class ModelProfile(
    val id: String,
    val name: String,
    val preset: ProviderPreset,
    val protocol: ModelProtocol,
    val baseUrl: String,
    val model: String,
    val outputLanguage: String,
    val promptTemplate: String,
    val proxy: ProxyConfiguration = ProxyConfiguration(),
    val requestTimeoutSeconds: Int = 90,
)
