package com.autodiag

import android.app.Application
import com.autodiag.data.ObdRepository

class AutoDiagApplication : Application() {

    var activeRepository: ObdRepository? = null
        private set

    fun setRepository(repo: ObdRepository) {
        activeRepository?.disconnect()
        activeRepository = repo
    }

    fun clearRepository() {
        activeRepository?.disconnect()
        activeRepository = null
    }

    override fun onTerminate() {
        super.onTerminate()
        clearRepository()
    }
}
