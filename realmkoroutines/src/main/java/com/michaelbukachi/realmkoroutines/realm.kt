package com.michaelbukachi.realmkoroutines

import io.realm.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend inline fun <reified T : RealmObject, reified S : RealmQuery<T>> findAllAwait(query: S): RealmResults<T> =
    suspendCancellableCoroutine { continuation ->
        val listener = RealmChangeListener<RealmResults<T>> { t ->
            if (continuation.isActive) {
                continuation.resume(t)
            }
        }
        val results = query.findAllAsync()
        results.addChangeListener(listener)
        continuation.invokeOnCancellation {
            results.removeChangeListener(listener)
        }
    }

suspend inline fun <reified T : RealmObject, reified S : RealmQuery<T>> findFirstAwait(query: S): T? =
    suspendCancellableCoroutine { continuation ->
        val listener = RealmChangeListener { t: T? ->
            if (continuation.isActive) {
                continuation.resume(t)
            }
        }
        val result = query.findFirstAsync()
        result.addChangeListener(listener)
        continuation.invokeOnCancellation {
            result.removeChangeListener(listener)
        }
    }

suspend inline fun <reified T : RealmObject, reified S : RealmQuery<T>> findAllAwaitOffline(query: S): List<T> =
    suspendCancellableCoroutine { continuation ->
        val realm = query.realm
        val listener = RealmChangeListener<RealmResults<T>> { t ->
            if (continuation.isActive) {
                continuation.resume(realm.copyFromRealm(t))
            }
        }
        val results = query.findAllAsync()
        results.addChangeListener(listener)
        continuation.invokeOnCancellation {
            results.removeChangeListener(listener)
        }
    }

suspend inline fun <reified T : RealmObject, reified S : RealmQuery<T>> findFirstAwaitOffline(query: S): T? =
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
        result.addChangeListener(listener)
        continuation.invokeOnCancellation {
            result.removeChangeListener(listener)
        }
    }

inline fun <reified T : RealmObject, S : RealmQuery<T>> findFirstOffline(query: S): T? {
    val realm = query.realm
    val t = query.findFirst()
    return t?.let { realm.copyFromRealm(it) } ?: t
}

inline fun <reified T : RealmObject, S : RealmQuery<T>> findAllOffline(query: S): List<T> {
    val realm = query.realm
    val t = query.findAll()
    return realm.copyFromRealm(t)
}

suspend fun Realm.executeAsync(block: (Realm) -> Unit): Unit =
    suspendCancellableCoroutine { continuation ->
        executeTransactionAsync(
            { block(it) },
            { continuation.resume(Unit) },
            { continuation.resumeWithException(it) })
    }

@ExperimentalCoroutinesApi
inline fun <reified S : RealmObject> RealmQuery<S>.flowAll(): Flow<List<S>> = callbackFlow {
    val listener = RealmChangeListener<RealmResults<S>> { t ->
        offer(t)
    }
    val results = findAllAsync()
    results.addChangeListener(listener)
    awaitClose { results.removeAllChangeListeners() }
}

@ExperimentalCoroutinesApi
inline fun <reified S : RealmObject> RealmQuery<S>.flowAllOffline(): Flow<List<S>> = callbackFlow {
    val listener = RealmChangeListener<RealmResults<S>> { t ->
        offer(realm.copyFromRealm(t))
    }
    val results = findAllAsync()
    results.addChangeListener(listener)
    awaitClose { results.removeAllChangeListeners() }
}

suspend inline fun <reified S : RealmObject> RealmQuery<S>.await() = findAllAwait(this)

suspend inline fun <reified S : RealmObject> RealmQuery<S>.awaitFirst() = findFirstAwait(this)

suspend inline fun <reified S : RealmObject> RealmQuery<S>.awaitAllOffline() = findAllAwaitOffline(this)

suspend inline fun <reified S : RealmObject> RealmQuery<S>.awaitFirstOffline() = findFirstAwaitOffline(this)

inline fun <reified S : RealmObject> RealmQuery<S>.allOffline() = findAllOffline(this)

inline fun <reified S : RealmObject> RealmQuery<S>.firstOffline() = findFirstOffline(this)

@Deprecated("redundant", ReplaceWith("executeAsync(block)"))
suspend fun Realm.transactAwait(block: (Realm) -> Unit) = executeAsync(block)
