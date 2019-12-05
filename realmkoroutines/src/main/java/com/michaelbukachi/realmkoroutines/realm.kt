package com.michaelbukachi.realmkoroutines

import io.realm.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T : RealmObject, S : RealmQuery<T>> findAllAwait(query: S): RealmResults<T> =
        suspendCancellableCoroutine { continuation ->
            val listener = RealmChangeListener<RealmResults<T>> { t ->
                if (continuation.isActive) {
                    continuation.resume(t)
                }
            }
            query.findAllAsync().addChangeListener(listener)
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findFirstAwait(query: S): T? =
        suspendCancellableCoroutine { continuation ->
            val listener = RealmChangeListener { t: T? ->
                if (continuation.isActive) {
                    continuation.resume(t)
                }
            }
            query.findFirstAsync().addChangeListener(listener)
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findAllAwaitOffline(query: S): List<T> =
        suspendCancellableCoroutine { continuation ->
            val realm = query.realm
            val listener = RealmChangeListener<RealmResults<T>> { t ->
                if (continuation.isActive) {
                    continuation.resume(realm.copyFromRealm(t))
                }
            }
            query.findAllAsync().addChangeListener(listener)
        }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findFirstAwaitOffline(query: S): T? =
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
            query.findFirstAsync().addChangeListener(listener)
        }

private fun <T : RealmObject, S : RealmQuery<T>> findFirstOffline(query: S): T? {
    val realm = query.realm
    val t = query.findFirst()
    return t?.let { realm.copyFromRealm(it) } ?: t
}

private fun <T : RealmObject, S : RealmQuery<T>> findAllOffline(query: S): List<T> {
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

suspend fun <S : RealmObject> RealmQuery<S>.await() = findAllAwait(this)

suspend fun <S : RealmObject> RealmQuery<S>.awaitFirst() = findFirstAwait(this)

suspend fun <S : RealmObject> RealmQuery<S>.awaitAllOffline() = findAllAwaitOffline(this)

suspend fun <S : RealmObject> RealmQuery<S>.awaitFirstOffline() = findFirstAwaitOffline(this)

fun <S : RealmObject> RealmQuery<S>.allOffline() = findAllOffline(this)

fun <S : RealmObject> RealmQuery<S>.firstOffline() = findFirstOffline(this)

suspend fun Realm.transactAwait(block: (Realm) -> Unit) = executeAsync(this, block)