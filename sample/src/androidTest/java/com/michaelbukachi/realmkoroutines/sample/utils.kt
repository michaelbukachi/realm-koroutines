package com.michaelbukachi.realmkoroutines.sample

import io.realm.Realm
import io.realm.RealmConfiguration

fun testRealm(): Realm {
    val testConfig = RealmConfiguration.Builder()
        .inMemory()
        .name("test-realm")
        .build()
    return Realm.getInstance(testConfig)
}