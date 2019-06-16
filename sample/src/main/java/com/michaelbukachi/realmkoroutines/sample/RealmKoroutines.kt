package com.michaelbukachi.realmkoroutines.sample

import android.app.Application
import io.realm.Realm

class RealmKoroutines : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}