package com.vonbraunlabs.mindfocus.core.util

import java.util.UUID

/** Injected so tests get deterministic ids instead of random UUIDs. */
fun interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
