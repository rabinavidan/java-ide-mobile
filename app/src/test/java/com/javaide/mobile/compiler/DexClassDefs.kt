package com.javaide.mobile.compiler

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Minimal reader for the class_def_item table of a .dex file (format per the Dalvik Executable
 * spec), used only by tests to prove which classes D8 actually merged into an output dex --
 * as opposed to classes merely *referenced* by it (e.g. android.jar types), which "dex succeeds
 * without error" alone can't distinguish since D8 doesn't require referenced-but-absent classes
 * to resolve the way ECJ compilation does.
 */
object DexClassDefs {

    fun definedClasses(dexFile: File): Set<String> {
        val bytes = dexFile.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val stringIdsSize = buf.getInt(0x38)
        val stringIdsOff = buf.getInt(0x3c)
        val typeIdsSize = buf.getInt(0x40)
        val typeIdsOff = buf.getInt(0x44)
        val classDefsSize = buf.getInt(0x60)
        val classDefsOff = buf.getInt(0x64)

        val stringDataOffs = IntArray(stringIdsSize) { i -> buf.getInt(stringIdsOff + i * 4) }
        val typeDescriptorIdx = IntArray(typeIdsSize) { i -> buf.getInt(typeIdsOff + i * 4) }

        return (0 until classDefsSize).map { i ->
            val base = classDefsOff + i * 32
            val classIdx = buf.getInt(base)
            val descriptorStringIdx = typeDescriptorIdx[classIdx]
            readMutf8(bytes, stringDataOffs[descriptorStringIdx])
        }.toSet()
    }

    /** string_data_item: uleb128 utf16_size, followed by MUTF-8 bytes, NUL-terminated. */
    private fun readMutf8(bytes: ByteArray, offset: Int): String {
        var pos = offset
        while ((bytes[pos].toInt() and 0x80) != 0) pos++
        pos++ // consume the final uleb128 byte
        val start = pos
        while (bytes[pos].toInt() != 0) pos++
        return String(bytes, start, pos - start, StandardCharsets.UTF_8)
    }
}
