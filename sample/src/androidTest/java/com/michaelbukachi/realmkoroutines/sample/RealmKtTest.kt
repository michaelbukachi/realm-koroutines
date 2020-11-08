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
    fun testSyncSafe() {
        val realm = testRealm()
        val result = realm.where<TestObject>().firstSafe()
        assertThat(result?.name, `is`("Some Test"))

        val resultList = realm.where<TestObject>().allSafe()
        Assert.assertTrue(resultList.isNotEmpty())
        Assert.assertFalse(resultList is RealmResults)
    }

    @Test
    fun testAsyncSafe(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val result = realm.where<TestObject>().safleyAwaitFirst()
        assertThat(result?.name, `is`("Some Test"))

        val resultList = realm.where<TestObject>().safelyAwaitAll()
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
    fun testFlowSafe(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        val resultsFlow = realm.where<TestObject>().flowAllSafe()
        val testObjects = resultsFlow.take(1).first()
        val testObject = TestObject(name = "Some Test")
        assertThat(testObjects.first().name, `is`(testObject.name))
    }

    @Test
    fun testBulkSafe(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm()
        realm.transactAwait {
            it.delete(TestObject::class.java)
            val objects = mutableListOf<TestObject>()
            for (i in 1..1000) {
                objects.add(TestObject(name = "Some Test $i"))
            }
            it.copyToRealm(objects)
        }
        val resultsFlow = realm.where<TestObject>().flowAllSafe()
        val testObjects = resultsFlow.take(1)
        assertThat(testObjects.first().size, `is`(1000))
    }

    @Test(expected = FrozenException::class)
    fun testExceptionIsThrownIfObjectIsFrozen(): Unit = runBlocking(Dispatchers.Main) {
        val realm = testRealm().freeze()
        val resultsFlow = realm.where<TestObject>().flowAllSafe()
        val testObjects = resultsFlow.take(1).first()
    }
}