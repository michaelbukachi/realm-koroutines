package com.michaelbukachi.realmkoroutines.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelbukachi.realmkoroutines.*
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
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
        assertThat(result?.name, `is`("Some Test"))

        val resultList = realm.where<TestObject>().allOffline()
        Assert.assertTrue(resultList.isNotEmpty())
        Assert.assertFalse(resultList is RealmResults)
    }

    @Test
    fun testAsyncOffline(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val result = realm.where<TestObject>().awaitFirstOffline()
        assertThat(result?.name, `is`("Some Test"))

        val resultList = realm.where<TestObject>().awaitAllOffline()
        assertThat(resultList.isNotEmpty(), `is`(true))

    }

    @Test
    fun testFlow(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val resultsFlow = realm.where<TestObject>().flowAll()
        val testObjects = resultsFlow.take(1).first()
        val testObject = TestObject(name = "Some Test")
        assertThat(testObjects.first().name, `is`(testObject.name))
    }

    @Test
    fun testFlowOffline(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val resultsFlow = realm.where<TestObject>().flowAllOffline()
        val testObjects = resultsFlow.take(1).first()
        val testObject = TestObject(name = "Some Test")
        assertThat(testObjects.first().name, `is`(testObject.name))
    }

    @Test
    fun testBulkOffline(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        realm.transactAwait {
            it.delete(TestObject::class.java)
            val objects = mutableListOf<TestObject>()
            for (i in 1..1000) {
                objects.add(TestObject(name = "Some Test $i"))
            }
            it.copyToRealm(objects)
        }
        val resultsFlow = realm.where<TestObject>().flowAllOffline()
        val testObjects = resultsFlow.take(1)
        assertThat(testObjects.first().size, `is`(1000))
    }
}