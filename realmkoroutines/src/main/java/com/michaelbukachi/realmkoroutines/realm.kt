package com.michaelbukachi.realmkoroutines

import io.realm.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FrozenException : Exception("Only non frozen realm objects can be awaited")

private suspend fun <T : RealmObject, S : RealmQuery<T>> findAllAwait(query: S): RealmResults<T> =
        suspendCancellableCoroutine { continuation ->
            val listener = RealmChangeListener<RealmResults<T>> { t ->
                if (continuation.isActive) {
                    continuation.resume(t)
                }
            }
            val results = query.findAllAsync()
            if (results.isFrozen) {
                throw FrozenException()
            }
            results.addChangeListener(listener)
            continuation.invokeOnCancellation {
                results.removeChangeListener(listener)
            }
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findFirstAwait(query: S): T? =
        suspendCancellableCoroutine { continuation ->
            val listener = RealmChangeListener { t: T? ->
                if (continuation.isActive) {
                    continuation.resume(t)
                }
            }
            val result = query.findFirstAsync()
            if (result.isFrozen) {
                throw FrozenException()
            }
            result.addChangeListener(listener)
            continuation.invokeOnCancellation {
                result.removeChangeListener(listener)
            }
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findAllAwaitSafe(query: S): List<T> =
        suspendCancellableCoroutine { continuation ->
            val realm = query.realm
            val listener = RealmChangeListener<RealmResults<T>> { t ->
                if (continuation.isActive) {
                    continuation.resume(realm.copyFromRealm(t))
                }
            }
            val results = query.findAllAsync()
            if (results.isFrozen) {
                throw FrozenException()
            }
            results.addChangeListener(listener)
            continuation.invokeOnCancellation {
                results.removeChangeListener(listener)
            }
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findFirstAwaitSafe(query: S): T? =
        suspendCancellableCoroutine { continuation ->
            val realm = query.realm
            val listener = RealmChangeListener { t: T? ->
                if (continuation.isActive) {
                    t?.let {
                        continuation.resume(realm.copyFromRealm(it))
                    } ?: run {
                        continuation.resume(t)
                    }
                }

            }
            val result = query.findFirstAsync()
            if (result.isFrozen) {
                throw FrozenException()
            }
            result.addChangeListener(listener)
            continuation.invokeOnCancellation {
                result.removeChangeListener(listener)
            }
        }

private fun <T : RealmObject, S : RealmQuery<T>> findFirstSafe(query: S): T? {
    val realm = query.realm
    val t = query.findFirst()
    return t?.let { realm.copyFromRealm(it) } ?: t
}

private fun <T : RealmObject, S : RealmQuery<T>> findAllSafe(query: S): List<T> {
    val realm = query.realm
    val t = query.findAll()
    return realm.copyFromRealm(t)
}

private suspend fun executeAsync(realm: Realm, block: (Realm) -> Unit): Unit =
        suspendCancellableCoroutine { continuation ->
            realm.executeTransactionAsync(
                    { block(it) },
                    { continuation.resume(Unit) },
                    { continuation.resumeWithException(it) })
        }


@ExperimentalCoroutinesApi
fun <S : RealmObject> RealmQuery<S>.flowAll(): Flow<List<S>> = callbackFlow {
    val listener = RealmChangeListener<RealmResults<S>> { t ->
        offer(t)
    }
    val results = findAllAsync()
    if (results.isFrozen) {
        throw FrozenException()
    }
    results.addChangeListener(listener)
    awaitClose { results.removeChangeListener(listener) }
}

@ExperimentalCoroutinesApi
fun <S : RealmObject> RealmQuery<S>.flowAllSafe(): Flow<List<S>> = callbackFlow {
    val listener = RealmChangeListener<RealmResults<S>> { t ->

        offer(realm.copyFromRealm(t))
    }
    val results = findAllAsync()
    if (results.isFrozen) {
        throw FrozenException()
    }
    results.addChangeListener(listener)
    awaitClose { results.removeChangeListener(listener) }
}

suspend fun <S : RealmObject> RealmQuery<S>.await() = findAllAwait(this)

suspend fun <S : RealmObject> RealmQuery<S>.awaitFirst() = findFirstAwait(this)

suspend fun <S : RealmObject> RealmQuery<S>.safelyAwaitAll() = findAllAwaitSafe(this)

suspend fun <S : RealmObject> RealmQuery<S>.safleyAwaitFirst() = findFirstAwaitSafe(this)

fun <S : RealmObject> RealmQuery<S>.allSafe() = findAllSafe(this)

fun <S : RealmObject> RealmQuery<S>.firstSafe() = findFirstSafe(this)

suspend fun Realm.transactAwait(block: (Realm) -> Unit) = executeAsync(this, block)