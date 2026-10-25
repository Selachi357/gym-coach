package com.gymcoach

import android.app.Application
import com.gymcoach.data.GymDatabase
import com.gymcoach.data.GymRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymCoachApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: GymRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GymRepository(GymDatabase.create(this))
        appScope.launch { repository.seedIfEmpty() }
    }
}
