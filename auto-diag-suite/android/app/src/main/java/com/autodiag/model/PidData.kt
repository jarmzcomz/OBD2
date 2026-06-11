package com.autodiag.model

data class PidData(val name: String, val value: Double, val unit: String)
data class DtcCode(val code: String, val status: String = "stored")
