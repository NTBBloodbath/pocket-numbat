package io.github.ntbbloodbath.pocketnumbat.calc

import java.io.Closeable

class NumbatEngine private constructor(private var handle: Long) : Closeable {
    fun eval(input: String): String {
        check(handle != 0L) { "engine is closed" }
        return NumbatNative.nativeInterpret(handle, input)
    }

    override fun close() {
        if (handle != 0L) {
            NumbatNative.nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        fun create(): NumbatEngine {
            val handle = NumbatNative.nativeCreate()
            check(handle != 0L) { "native engine creation failed" }
            return NumbatEngine(handle)
        }
    }
}
