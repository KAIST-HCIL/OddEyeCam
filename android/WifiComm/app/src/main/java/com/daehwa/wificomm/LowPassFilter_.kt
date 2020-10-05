package com.daehwa.wificomm

class LowPassFilter_ {

    var y: Double = 0.toDouble()
    var a: Double = 0.toDouble()
    var s: Double = 0.toDouble()
    var initialized: Boolean = false

    @Throws(Exception::class)
    fun setAlpha(alpha: Double) {
        if (alpha <= 0.0 || alpha > 1.0) {
            throw Exception("alpha should be in (0.0., 1.0]")
        }
        a = alpha
    }

    @Throws(Exception::class)
    constructor(alpha: Double) {
        init(alpha, 0.0)
    }

    @Throws(Exception::class)
    constructor(alpha: Double, initval: Double) {
        init(alpha, initval)
    }

    @Throws(Exception::class)
    private fun init(alpha: Double, initval: Double) {
        s = initval
        y = s
        setAlpha(alpha)
        initialized = false
    }

    fun filter(value: Double): Double {
        val result: Double
        if (initialized) {
            result = a * value + (1.0 - a) * s
        } else {
            result = value
            initialized = true
        }
        y = value
        s = result
        return result
    }

    @Throws(Exception::class)
    fun filterWithAlpha(value: Double, alpha: Double): Double {
        setAlpha(alpha)
        return filter(value)
    }

    fun hasLastRawValue(): Boolean {
        return initialized
    }

    fun lastRawValue(): Double {
        return y
    }
}