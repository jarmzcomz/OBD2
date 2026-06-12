package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import com.autodiag.data.DtcParser
import com.autodiag.data.Mode09Decoder
import com.autodiag.model.DtcCode
import com.autodiag.model.dtcDescriptions
import com.autodiag.model.inferSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VehicleInfo(
    val vin: String? = null,
    val calId: String? = null,
    val elmVersion: String? = null,
    val protocol: String? = null,
    val voltage: String? = null,
    val pendingDtcs: List<DtcCode> = emptyList(),
    val permanentDtcs: List<DtcCode> = emptyList()
)

/**
 * Diagnóstico de ingeniero: VIN, IDs de calibración, info del adaptador
 * (versión ELM, protocolo, voltaje) y DTCs pendientes (07) / permanentes (0A).
 * Todo vía comandos crudos al adaptador, válido también en modo Demo.
 */
class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val _info = MutableStateFlow(VehicleInfo())
    val info: StateFlow<VehicleInfo> = _info.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun refresh() {
        val repo = getApplication<AutoDiagApplication>().activeRepository ?: return
        if (_loading.value) return
        _loading.value = true

        viewModelScope.launch {
            suspend fun ask(cmd: String) = runCatching { repo.sendRaw(cmd) }.getOrDefault("")

            val vin = Mode09Decoder.decodeVin(ask("0902"))
            val cal = Mode09Decoder.decodeCalibrationId(ask("0904"))
            val elm = ask("ATI").ifBlank { null }
            val proto = ask("ATDP").ifBlank { null }
            val volt = ask("ATRV").ifBlank { null }
            val pending = DtcParser.parse(ask("07"), "07").map { toDtc(it) }
            val permanent = DtcParser.parse(ask("0A"), "0A").map { toDtc(it) }

            _info.value = VehicleInfo(
                vin = vin,
                calId = cal,
                elmVersion = elm,
                protocol = proto,
                voltage = volt,
                pendingDtcs = pending,
                permanentDtcs = permanent
            )
            _loading.value = false
        }
    }

    private fun toDtc(code: String): DtcCode {
        val dtc = DtcCode(code = code, description = dtcDescriptions[code] ?: "Código de falla")
        return dtc.copy(severity = dtc.inferSeverity())
    }
}
