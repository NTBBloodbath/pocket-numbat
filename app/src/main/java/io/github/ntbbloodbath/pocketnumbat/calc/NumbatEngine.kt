package io.github.ntbbloodbath.pocketnumbat.calc

import java.io.Closeable

enum class IdentifierKind {
    UNIT,
    FUNCTION,
    VARIABLE,
}

data class Identifier(
    val kind: IdentifierKind,
    val name: String,
)

class NumbatEngine private constructor(private var handle: Long) : Closeable {
    fun eval(input: String): String {
        check(handle != 0L) { "engine is closed" }
        return NumbatNative.nativeInterpret(handle, input)
    }

    fun identifiers(): List<Identifier> {
        check(handle != 0L) { "engine is closed" }
        return NumbatNative.nativeIdentifiers(handle)
            .lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('|')
                if (separator <= 0) return@mapNotNull null
                val kind = when (line.substring(0, separator)) {
                    "unit" -> IdentifierKind.UNIT
                    "function" -> IdentifierKind.FUNCTION
                    "variable" -> IdentifierKind.VARIABLE
                    else -> return@mapNotNull null
                }
                Identifier(kind, line.substring(separator + 1))
            }
            .filter { it.name.isNotBlank() }
            .toList()
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
