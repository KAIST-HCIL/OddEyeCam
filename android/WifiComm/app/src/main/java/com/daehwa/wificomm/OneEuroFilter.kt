package com.daehwa.wificomm

import android.util.Log

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author s. conversy from n. roussel c++ version
 *
 * Copyright 2019
 *
 * BSD License https://opensource.org/licenses/BSD-3-Clause
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
internal class LowPassFilter {

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

class OneEuroFilter {

    internal var freq: Double = 0.toDouble()
    internal var mincutoff: Double = 0.toDouble()
    internal var beta_: Double = 0.toDouble()
    internal var dcutoff: Double = 0.toDouble()
    internal var x: LowPassFilter? = null
    internal var dx: LowPassFilter? = null
    internal var lasttime: Double = 0.toDouble()

    internal fun alpha(cutoff: Double): Double {
        val te = 1.0 / freq
        val tau = 1.0 / (2.0 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / te)
    }

    @Throws(Exception::class)
    internal fun setFrequency(f: Double) {
        if (f <= 0) {
            throw Exception("freq should be >0")
        }
        freq = f
    }

    @Throws(Exception::class)
    internal fun setMinCutoff(mc: Double) {
        if (mc <= 0) {
            throw Exception("mincutoff should be >0")
        }
        mincutoff = mc
    }

    internal fun setBeta(b: Double) {
        beta_ = b
    }

    @Throws(Exception::class)
    internal fun setDerivateCutoff(dc: Double) {
        if (dc <= 0) {
            throw Exception("dcutoff should be >0")
        }
        dcutoff = dc
    }

    @Throws(Exception::class)
    constructor(freq: Double) {
        init(freq, 1.0, 0.0, 1.0)
    }

    @Throws(Exception::class)
    constructor(freq: Double, mincutoff: Double) {
        init(freq, mincutoff, 0.0, 1.0)
    }

    @Throws(Exception::class)
    constructor(freq: Double, mincutoff: Double, beta_: Double) {
        init(freq, mincutoff, beta_, 1.0)
    }

    @Throws(Exception::class)
    constructor(freq: Double, mincutoff: Double, beta_: Double, dcutoff: Double) {
        init(freq, mincutoff, beta_, dcutoff)
    }

    @Throws(Exception::class)
    private fun init(
        freq: Double,
        mincutoff: Double, beta_: Double, dcutoff: Double
    ) {
        setFrequency(freq)
        setMinCutoff(mincutoff)
        setBeta(beta_)
        setDerivateCutoff(dcutoff)
        x = LowPassFilter(alpha(mincutoff))
        dx = LowPassFilter(alpha(dcutoff))
        lasttime = UndefinedTime
    }

    @Throws(Exception::class)
    @JvmOverloads
    internal fun filter(value: Double, timestamp: Double = UndefinedTime): Double {
        // update the sampling frequency based on timestamps
        if (lasttime != UndefinedTime && timestamp != UndefinedTime) {
            freq = 1.0 / (timestamp - lasttime)
        }

        lasttime = timestamp
        // estimate the current variation per second
        val dvalue = if (x!!.hasLastRawValue()) (value - x!!.lastRawValue()) * freq else 0.0 // FIXME: 0.0 or value?
        val edvalue = dx?.filterWithAlpha(dvalue, alpha(dcutoff))
        // use it to update the cutoff frequency
        val cutoff = mincutoff + beta_ * Math.abs(edvalue!!)
        // filter the given value
        return x!!.filterWithAlpha(value, alpha(cutoff))
    }

    companion object {
        internal var UndefinedTime = -1.0

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val duration = 10.0 // seconds
            val frequency = 15.0 // Hz
            val mincutoff = 1.0 // FIXME
            val beta = 1.0      // FIXME
            val dcutoff = 1.0   // this one should be ok

            print(
                "#SRC OneEuroFilter.java" + "\n"
                        + "#CFG {'beta': " + beta + ", 'freq': " + frequency + ", 'dcutoff': " + dcutoff + ", 'mincutoff': " + mincutoff + "}" + "\n"
                        + "#LOG timestamp, signal, noisy, filtered" + "\n"
            )

            val f = OneEuroFilter(
                frequency,
                mincutoff,
                beta,
                dcutoff
            )
            var timestamp = 0.0
            while (timestamp < duration) {
                val signal = Math.sin(timestamp)
                val noisy = signal + (Math.random() - 0.5) / 5.0
                val filtered = f.filter(noisy, timestamp)
                println(
                    ("" + timestamp + ", "
                            + signal + ", "
                            + noisy + ", "
                            + filtered)
                )
                timestamp += 1.0 / frequency
            }
        }
    }
}