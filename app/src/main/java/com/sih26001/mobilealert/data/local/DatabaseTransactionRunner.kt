package com.sih26001.mobilealert.data.local

/**
 * Abstraction for executing database operations inside an atomic transaction.
 *
 * This abstraction allows repository transaction behavior to be tested in JVM unit tests
 * without requiring native Android SQLite bindings or Robolectric.
 */
interface DatabaseTransactionRunner {
    suspend operator fun <T> invoke(
        block: suspend () -> T
    ): T
}
