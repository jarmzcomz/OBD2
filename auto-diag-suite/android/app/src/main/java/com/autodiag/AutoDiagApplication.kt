package com.autodiag

import android.app.Application
import com.autodiag.data.ObdRepository

class AutoDiagApplication : Application() {

    var activeRepository: ObdRepository? = null
        private set

    /** Etiqueta legible del modo de conexión activo (para la pantalla Info). */
    var connectionLabel: String = "—"
        private set

    fun setRepository(repo: ObdRepository, label: String = "—") {
        activeRepository?.disconnect()
        activeRepository = repo
        connectionLabel = label
    }

    fun clearRepository() {
        activeRepository?.disconnect()
        activeRepository = null
        connectionLabel = "—"
    }

    override fun onTerminate() {
        super.onTerminate()
        clearRepository()
    }
}
