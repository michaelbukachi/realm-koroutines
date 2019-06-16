package com.michaelbukachi.realmkoroutines

import io.realm.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T : RealmObject, S : RealmQuery<T>> findAllAwait(query: S): RealmResults<T> =
    suspendCancellableCoroutine { continuation ->
        val listener = RealmChangeListener<RealmResults<T>> { t -> continuation.resume(t) }
        query.findAllAsync().addChangeListener(listener)
    }

private suspend fun <T : RealmObject, S : RealmQuery<T>> findFirstAwait(query: S): T? =
    suspendCancellableCoroutine { continuation ->
        val listener = RealmChangeListener { t: T? -> continuation.resume(t) }
        query.findFirstAsync().addChangeListener(listener)
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

suspend fun Realm.transactAwait(block: (Realm) -> Unit) = executeAsync(this, block)