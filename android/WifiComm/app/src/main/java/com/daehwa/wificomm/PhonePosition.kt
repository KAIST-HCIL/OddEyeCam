package com.daehwa.wificomm

class PhonePosition(x_: Float,y_: Float,z_: Float, d_: Float, theta_: Float, phi_: Float,xaxis_: FloatArray,yaxis_: FloatArray,zaxis_: FloatArray){
    var x: Float
    var y: Float
    var z: Float
    var d: Float
    var theta: Float
    var phi: Float
    var xaxis: FloatArray
    var yaxis: FloatArray
    var zaxis: FloatArray

    init {
        x = x_
        y = y_
        z = z_
        d = d_
        theta = theta_
        phi = phi_
        xaxis = xaxis_
        yaxis = yaxis_
        zaxis = zaxis_
    }

    fun getPolarCoord(): String{
        var res = "d:"+d.toString()+"\ntheta:"+theta.toString()+"\nphi:"+phi.toString()
        return res
    }

    fun getXYZCoord(): String{
        var res = "x:"+x.toString()+"\ny:"+y.toString()+"\nz:"+z.toString()
        return res
    }
}