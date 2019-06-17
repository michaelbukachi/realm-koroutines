package com.michaelbukachi.realmkoroutines.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelbukachi.realmkoroutines.allOffline
import com.michaelbukachi.realmkoroutines.awaitAllOffline
import com.michaelbukachi.realmkoroutines.awaitFirstOffline
import com.michaelbukachi.realmkoroutines.firstOffline
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealmKtTest {

    @Before
    fun setup() {
        val realm = testRealm()
        realm.executeTransaction {
            val testObject = TestObject(name = "Some Test")
            it.copyToRealm(testObject)
        }
    }

    @After
    fun tearDown() {
        val realm = testRealm()
        realm.executeTransaction {
            it.deleteAll()
        }
        realm.refresh()
    }

    @Test
    fun testSyncOffline() {
        val realm = testRealm()
        val result = realm.where<TestObject>().firstOffline()
        Assert.assertEquals("Some Test", result!!.name)

        val resultList = realm.where<TestObject>().allOffline()
        Assert.assertTrue(resultList.isNotEmpty())
        Assert.assertFalse(resultList is RealmResults)
    }

    @Test
    fun testAsyncOffline() = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val result = realm.where<TestObject>().awaitFirstOffline()
        Assert.assertEquals("Some Test", result!!.name)

        val resultList = realm.where<TestObject>().awaitAllOffline()
        Assert.assertTrue(resultList.isNotEmpty())
        Assert.assertFalse(resultList is RealmResults)
    }


}