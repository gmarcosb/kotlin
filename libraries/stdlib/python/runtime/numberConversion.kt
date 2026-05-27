// TODO: to be converted to python
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal fun numberToByte(a: dynamic): Byte = toByte(numberToInt(a))

@UsedFromCompilerGeneratedCode
internal fun numberToDouble(@Suppress("UNUSED_PARAMETER") a: dynamic): Double = TODO("Number(a)").unsafeCast<Double>()

@UsedFromCompilerGeneratedCode
internal fun numberToInt(a: dynamic): Int = if (a is Long) a.toInt() else doubleToInt(a)

@UsedFromCompilerGeneratedCode
internal fun numberToShort(a: dynamic): Short = toShort(numberToInt(a))

// << and >> shifts are used to preserve sign of the number
@UsedFromCompilerGeneratedCode
internal fun toByte(@Suppress("UNUSED_PARAMETER") a: dynamic): Byte = TODO("a << 24 >> 24").unsafeCast<Byte>()

@UsedFromCompilerGeneratedCode
internal fun toShort(@Suppress("UNUSED_PARAMETER") a: dynamic): Short = TODO("a << 16 >> 16").unsafeCast<Short>()

internal fun doubleToInt(a: Double): Int = when {
    a > 2147483647 -> 2147483647
    a < -2147483648 -> -2147483648
    else -> jsBitwiseOr(a, 0)
}

@UsedFromCompilerGeneratedCode
internal fun numberToChar(a: dynamic) = Char(numberToInt(a).toUShort())
