package com.specknet.pdiotapp.demo

import java.time.Instant

data class HistoricalDataModel(
    val id: String,
    val type: String,
    val movement: String,
    val timestamp: Instant,
) {
}