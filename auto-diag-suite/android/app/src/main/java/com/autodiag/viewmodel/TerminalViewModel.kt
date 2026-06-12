package com.autodiag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autodiag.AutoDiagApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TerminalLine(val command: String, val response: String)

/**
 * Terminal de ingeniero: envía comandos AT/OBD crudos al adaptador activo y
 * muestra la respuesta. Indispensable para diagnóstico avanzado.
 */
class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val supported: Boolean
        get() = getApplication<AutoDiagApplication>().activeRepository?.capabilities?.rawTerminal == true

    fun send(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty() || _busy.value) return
        val repo = getApplication<AutoDiagApplication>().activeRepository ?: return

        _busy.value = true
        viewModelScope.launch {
            val response = runCatching { repo.sendRaw(cmd) }
                .getOrElse { "ERROR: ${it.message}" }
            _lines.value = _lines.value + TerminalLine(cmd, response.ifBlank { "(sin respuesta)" })
            _busy.value = false
        }
    }

    fun clear() { _lines.value = emptyList() }
}
