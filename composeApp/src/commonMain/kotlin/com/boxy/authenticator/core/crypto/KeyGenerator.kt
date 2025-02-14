package com.boxy.authenticator.core.crypto

import diglol.crypto.Argon2

object KeyGenerator {
    suspend fun deriveKey(
        password: ByteArray,
        salt: ByteArray = ByteArray(32),
    ): ByteArray {
        val kdf = Argon2(
            version = Argon2.Version.V13,
            type = Argon2.Type.ID,
            iterations = 3,
            memory = 65536,
            parallelism = 2,
            hashSize = 32 // (256-bit)
        )
        return kdf.deriveKey(password, salt)
    }
}