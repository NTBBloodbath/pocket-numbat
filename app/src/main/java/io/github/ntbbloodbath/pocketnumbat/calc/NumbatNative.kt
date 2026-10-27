package io.github.ntbbloodbath.pocketnumbat.calc

object NumbatNative {
    init {
        System.loadLibrary("numbat_bridge")
    }

    @JvmStatic
    external fun nativeCreate(): Long

    @JvmStatic
    external fun nativeDestroy(handle: Long)

    @JvmStatic
    external fun nativeInterpret(handle: Long, input: String): String

    @JvmStatic
    external fun nativeIdentifiers(handle: Long): String

    @JvmStatic
    external fun nativeSetExchangeRates(xml: String)
}
