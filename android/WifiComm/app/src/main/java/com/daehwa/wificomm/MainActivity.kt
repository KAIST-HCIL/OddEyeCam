package com.daehwa.wificomm

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daehwa.wificomm.Scenario.Companion.check_walk
import com.daehwa.wificomm.Scenario.Companion.gravityVector
import com.daehwa.wificomm.Scenario.Companion.instance
import com.daehwa.wificomm.Scenario.Companion.isLatentSet
import com.daehwa.wificomm.Scenario.Companion.mode_walk
import com.github.ybq.android.spinkit.SpinKitView
import kotlinx.android.synthetic.main.map.*

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var et1: EditText? = null
    private var et2: EditText? = null
    private var et3: EditText? = null
    private var tv4: TextView? = null
    private var tv5: TextView? = null
    private var socket: Socket? = null
    private var writeSocket: DataOutputStream? = null
    private var readSocket: DataInputStream? = null
    private val mHandler = Handler()
    private var cManager: ConnectivityManager? = null
    private var wifi: NetworkInfo? = null
    private var serverSocket: ServerSocket? = null
    var sensorDataQueue = ConcurrentLinkedDeque<ByteArray>()

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        et1 = findViewById(R.id.editText1) as EditText
        et2 = findViewById(R.id.editText2) as EditText
        et3 = findViewById(R.id.editText3) as EditText

        tv4 = findViewById(R.id.textView4) as TextView
        tv5 = findViewById(R.id.textView5) as TextView

        cManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    var v_z = 0.0
    var d_z = 0.0
    var prev_t: Double? = null
    var v_z_queue: Queue<Double> =  LinkedList()
    var d_z_queue: Queue<Double> =  LinkedList()
    fun resetWalkParams(){
        v_z = 0.0
        d_z = 0.0
        prev_t = null
        v_z_queue =  LinkedList()
        d_z_queue =  LinkedList()
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (!check_walk){
            resetWalkParams()
        }
        if (event!!.sensor.type == Sensor.TYPE_GRAVITY) {
            var x = event.values[0]
            var y = event.values[1]
            var z = event.values[2]
            gravityVector = floatArrayOf(x,y,z)
            var data = "START{\'x\' : "+x+", \'y\' : "+y+", \'z\' : " +z+"}END"
//            sensorData = data.toByteArray()
            sensorDataQueue.offer(data.toByteArray())
        }
        else if (event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION && check_walk){

            var a_z = event.values[2] + 0.54f //+ 0.57f

            var current_t = System.currentTimeMillis()/1000.0
            if (prev_t == null)
                prev_t = current_t
            var t = current_t - prev_t!!
            prev_t = current_t

            v_z += a_z * t

            var v_z_window = 0.0
            if (v_z_queue.size >= 100){
                v_z_window = v_z - v_z_queue.poll()
            }
            v_z_queue.add(v_z)

            d_z += v_z_window * t
            var d_z_window = 0.0
            if(d_z_queue.size >= 500){
                d_z_window = d_z - d_z_queue.poll()
            }
            d_z_queue.add(d_z)

            if(d_z_window > 1.2){
                mode_walk = true
            } else {
                mode_walk = false
            }
//            Log.d("d_z",d_z_window.toString())
            findViewById<TextView>(R.id.accdata).text = mode_walk.toString() + "\n" + d_z_window.toString()
        }
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }
    override fun onPause() {
        super.onPause()
        //sensorManager.unregisterListener(this)
    }


    fun scenario(){
        val nextIntent = Intent(this, ScenarioList::class.java)
        startActivity(nextIntent)
    }

    @Throws(Exception::class)
    fun OnClick(v: View) {
        when (v.id) {
            R.id.button1 -> Connect().start()
            /*R.id.button2 -> Disconnect().start()
            R.id.button3 -> SetServer().start()
            R.id.button4 -> CloseServer().start()
            R.id.button5 -> {
                wifi = cManager!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (wifi!!.isConnected) {
                    val wManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val info = wManager.connectionInfo
                    tv4!!.text = "IP Address : " + Formatter.formatIpAddress(info.ipAddress)
                } else {
                    tv4!!.text = "Disconnected"
                }
            }
            R.id.button6 -> sendMessage().start()*/
            R.id.button7 -> scenario()
        }
    }

    internal inner class Connect : Thread() {
        override fun run() {
            Log.d("Connect", "Run Connect")
            var ip: String? = null
            var port = 0

            try {
                ip = et1!!.text.toString()
                port = Integer.parseInt(et2!!.text.toString())
            } catch (e: Exception) {
                val recvInput = "Please fill the blank"
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }
            }

            try {
                socket = Socket(ip, port)
                socket!!.tcpNoDelay = true
                writeSocket = DataOutputStream(socket!!.getOutputStream())
                readSocket = DataInputStream(socket!!.getInputStream())

//                recvSocket().start()
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast("Connected")
                    findViewById<Button>(R.id.button1).visibility = Button.INVISIBLE
                    findViewById<SpinKitView>(R.id.spin_kit).visibility = SpinKitView.VISIBLE
                }
                sendMessage().start()
            } catch (e: Exception) {
                val recvInput = "Connection Fail"
                Log.d("Connect", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }

            }

        }
    }

    internal inner class Disconnect : Thread() {
        override fun run() {
            try {
                if (socket != null) {
                    writeSocket!!.write("exit".toByteArray())
                    socket!!.close()
                    mHandler.post {
                        // TODO Auto-generated method stub
                        setToast("Disconnected")
                    }

                }

            } catch (e: Exception) {
                val recvInput = "Connection Fail"
                Log.d("Connect", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }

            }

        }
    }

    internal inner class SetServer : Thread() {

        override fun run() {
            try {
                val port = Integer.parseInt(et2!!.text.toString())
                serverSocket = ServerSocket(port)
                val result = "Server port $port is ready"

                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(result)
                }

                socket = serverSocket!!.accept()
                writeSocket = DataOutputStream(socket!!.getOutputStream())
                readSocket = DataInputStream(socket!!.getInputStream())

                while (true) {
                    val b = ByteArray(100)
                    val ac = readSocket!!.read(b, 0, b.size)
                    val input = String(b, 0, b.size)
                    val recvInput = input.trim { it <= ' ' }

                    if (ac == -1)
                        break

                    mHandler.post {
                        // TODO Auto-generated method stub
                        setToast(recvInput)
                    }
                }
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast("Disconnected")
                }
                serverSocket!!.close()
                socket!!.close()
            } catch (e: Exception) {
                val recvInput = "Fail to connect"
                Log.d("SetServer", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }

            }

        }
    }

    internal inner class recvSocket : Thread() {

        override fun run() {
            try {
                readSocket = DataInputStream(socket!!.getInputStream())

                while (true) {
                    val b = ByteArray(2048)
                    val ac = readSocket!!.read(b, 0, b.size)
                    val input = String(b, 0, b.size)
                    val recvInput = input.trim { it <= ' ' }

                    if (ac == -1)
                        break

                    mHandler.post {
                        // TODO Auto-generated method stub
                        // received data is here
                        try {
                            var dataCandidates = recvInput.split("START")
                            var dataCandidate = dataCandidates[1].split("END")
                            var data = dataCandidate[0].split(",")
                            if( data.size == 15){
                                var phone_obj = PhonePosition(
                                    data[0].toFloat(),data[1].toFloat(),data[2].toFloat(),
                                    data[3].toFloat(),data[4].toFloat(),data[5].toFloat(),
                                    floatArrayOf(data[6].toFloat(),data[7].toFloat(),data[8].toFloat()),
                                    floatArrayOf(data[9].toFloat(),data[10].toFloat(),data[11].toFloat()),
                                    floatArrayOf(data[12].toFloat(),data[13].toFloat(),data[14].toFloat()))
                                tv4!!.setText(phone_obj.getPolarCoord())
                                tv5!!.setText(phone_obj.getXYZCoord())

                                if (isLatentSet){
                                    (instance as Scenario).setPosition(phone_obj)
                                }
                                //setToast(recvInput)
                            }
                        }catch (e: java.lang.Exception){
                            Log.d("invalid-type","Received Invalid Type of Data")
                        }
                    }
                }
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast("Successfully Ended")
                    findViewById<Button>(R.id.button1).visibility = Button.VISIBLE
                    findViewById<SpinKitView>(R.id.spin_kit).visibility = SpinKitView.INVISIBLE
                }
            } catch (e: Exception) {
                val recvInput = "Successfully Ended"
                Log.d("SetServer", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                    findViewById<Button>(R.id.button1).visibility = Button.VISIBLE
                    findViewById<SpinKitView>(R.id.spin_kit).visibility = SpinKitView.INVISIBLE
                }

            }

        }
    }

    internal inner class CloseServer : Thread() {
        override fun run() {
            try {
                if (serverSocket != null) {
                    serverSocket!!.close()
                    socket!!.close()

                    mHandler.post {
                        // TODO Auto-generated method stub
                        setToast("Disconnected")
                    }
                }
            } catch (e: Exception) {
                val recvInput = "서버 준비에 실패하였습니다."
                Log.d("SetServer", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }

            }

        }
    }

    internal inner class sendMessage : Thread() {
        override fun run() {
            try {
                recvSocket().start()
                while(true){
                    if(sensorDataQueue.isEmpty())
                        continue
                    var sensorData = sensorDataQueue.last()
                    sensorDataQueue.clear()
                    writeSocket!!.write(sensorData,0,sensorData.size)
//                    sleep(1)
//                    recvSocket()
                }
            } catch (e: Exception) {
                val recvInput = "Successfully Ended"
                Log.d("SetServer", e.message)
                mHandler.post {
                    // TODO Auto-generated method stub
                    setToast(recvInput)
                }

            }

        }
    }

    fun getGravityData(){

    }

    internal fun setToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }*/

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.string.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
}