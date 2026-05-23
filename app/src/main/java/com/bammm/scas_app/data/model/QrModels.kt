package com.bammm.scas_app.data.model

import com.google.gson.annotations.SerializedName

data class GenerateQrResponse(
    val status: String,
    @SerializedName("qr_batch") val qrBatch: List<String>
)
