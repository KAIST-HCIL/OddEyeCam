package com.daehwa.wificomm

import android.content.*
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.view.setPadding
import android.os.Vibrator
import android.view.LayoutInflater
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.util.Log
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import kotlinx.android.synthetic.main.peephole_1d_left.*
import java.io.File
import java.lang.Math.toRadians
import kotlin.math.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.R.anim
import android.graphics.ColorFilter
import android.view.animation.LinearInterpolator






class Scenario: AppCompatActivity() {
    companion object {
        lateinit var instance: Context
            private set
        var isLatentSet = false
        var gravityVector: FloatArray? = null
        var mode_walk = false
        var check_walk = false
    }

    /********
    Constants
     ********/
    // Constant
    val ZOOM_PARAM = 2.5
    var SCREEN_WIDTH = 1080
    var SCREEN_HEIGHT = 1920
    val RIGHT_SHOULDER = floatArrayOf(100f,0f,0f)
    val CHEST = floatArrayOf(0f,0f,0f)

    // Sensor
    var vibrator: Vibrator? = null

    // Type of Scenario
    var type = ""
    var p_number = ""
    var obj: FloatArray? = null
    var objs: Array<FloatArray>? = null

    // UI
    var background: FrameLayout? = null
    var fragment: LinearLayout? = null
    var fragment1: LinearLayout? = null
    var fragment2: LinearLayout? = null
    var fcmin_bar: SeekBar? = null
    var beta_bar: SeekBar? = null
    var fcmin_bar_tv: TextView? = null
    var beta_bar_tv: TextView? = null

    //Filter
    var freq = 15.0; var mincutoff = 0.01; var beta = 0.001
    var f_x = OneEuroFilter(freq,mincutoff,beta)
    var f_y = OneEuroFilter(freq,mincutoff,beta)
    var f_z =  OneEuroFilter(freq,mincutoff,beta)
    var f_size = OneEuroFilter(freq,mincutoff,beta)

    /******
      Main
     ******/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        isLatentSet = true
        getSupportActionBar()!!.hide()

        // vibration
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setContentView(R.layout.scenario_basic)
        background = findViewById(R.id.background)
        fragment = findViewById<LinearLayout?>(R.id.fragment)
        fragment1 = findViewById<LinearLayout?>(R.id.fragment1)
        fragment2 = findViewById<LinearLayout?>(R.id.fragment2)
        fcmin_bar_tv = findViewById<TextView?>(R.id.fcmin_value)
        fcmin_bar = findViewById<SeekBar?>(R.id.fcmin)
        fcmin_bar?.setProgress(100)
        fcmin_bar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                var div = 100.0/findViewById<TextView>(R.id.fcmin_max_value).text.toString().toDouble()
                var value = i/div
                if (value <=0)
                    value = 0.005
                fcmin_bar_tv?.text = "$value"
                updateFilter(value, null)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })
        beta_bar_tv = findViewById<TextView?>(R.id.beta_value)
        beta_bar = findViewById<SeekBar?>(R.id.beta)
        beta_bar?.setProgress(0)
        beta_bar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                var div = 100.0/findViewById<TextView>(R.id.beta_max_value).text.toString().toDouble()
                var value = i/div
                beta_bar_tv?.text = "$value"
                updateFilter(null, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })
        findViewById<Button>(R.id.fcmin_max_btn).setOnClickListener {
            var et = findViewById<EditText>(R.id.fcmin_max)
            if (et.text.isEmpty())
                Toast.makeText(applicationContext,"값을 입력해주세요",Toast.LENGTH_SHORT)
            else{
                var value = et.text.toString()
                et.text.clear()
                findViewById<TextView>(R.id.fcmin_max_value).text = value
            }
        }
        findViewById<Button>(R.id.beta_max_btn).setOnClickListener {
            var et =  findViewById<EditText>(R.id.beta_max)
            if (et.text.isEmpty())
                Toast.makeText(applicationContext,"값을 입력해주세요",Toast.LENGTH_SHORT)
            else{
                var value = et.text.toString()
                et.text.clear()
                findViewById<TextView>(R.id.beta_max_value).text = value
            }
        }
        // Scenario Intent
        val prevIntent = intent
        if(prevIntent.extras!!.containsKey("P_NUMBER"))
            p_number = prevIntent.getStringExtra("P_NUMBER")
        type = prevIntent.getStringExtra(Intent.EXTRA_TEXT)

        when (type){
            getString(R.string.scenario1) -> setDragAndDropApps()
            getString(R.string.scenario2) -> setApplicationStack()
            getString(R.string.scenario3) -> setLargeImageViewer()
            getString(R.string.scenario4) -> setBookmarking()
            getString(R.string.scenario5) -> setMap()
            getString(R.string.scenario6) -> setVirtualShelves()
            "Peephole Pointing" -> setPeepholePointing()
            "Peephole 2D" -> setPeephole2DRandom()
        }
    }

    override fun onStop() {
        check_walk = false
        this.finish()
        super.onStop()
    }

    /**********************
    Scenario Setting Methods
     **********************/

    var recent_drag_item: String? = null
    fun setDragAndDropApps(){
//        updateFilter(0.093,3.91e-4)
        updateFilter(0.16 ,3.00e-4 )

//        obj = floatArrayOf(50f,50f,220f)
        obj = floatArrayOf(50f,30f,280f)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.messenger, null
        )
        fragment1_vg.addView(child1)

        val fragment2_vg = fragment2 as ViewGroup
        val child2 = LayoutInflater.from(this).inflate(
            R.layout.album, null
        )
        fragment2_vg.addView(child2)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        fragment1?.layoutParams = param1

        val param2 = fragment2?.layoutParams as LinearLayout.LayoutParams
        fragment2?.layoutParams = param2

        for (i in 1..21) {
            var name: Int = getResources().getIdentifier("sample" + i, "id", getPackageName())
            findViewById<ImageView>(name).setOnLongClickListener { v: View ->
                val item = ClipData.Item(v.tag as? CharSequence)
                recent_drag_item = v.context.resources.getResourceEntryName(v.id)

                val dragData = ClipData(
                    v.tag as? CharSequence,
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item
                )
                val myShadow = View.DragShadowBuilder(v)
                v.startDrag(
                    dragData,   // the data to be dragged
                    myShadow,
                    null,       // no need to use local data
                    0           // flags (not currently used, set to 0)
                )
            }
        }
        findViewById<LinearLayout>(R.id.message_board).setOnDragListener { v, dragEvent ->
            when(dragEvent.action){
                DragEvent.ACTION_DROP -> {
                    recent_drag_item?.let {
                        var resourceId = resources.getIdentifier(recent_drag_item, "drawable", getPackageName())
                        val imagev = ImageView(findViewById<LinearLayout>(R.id.message_board).context)
                        imagev.setImageDrawable(imagev.getResources().getDrawable(resourceId))
                        imagev.setPadding(20)
                        val param = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        param.gravity = Gravity.RIGHT
                        imagev?.layoutParams = param
                        findViewById<LinearLayout>(R.id.message_board).addView(imagev)
                        var sv = findViewById<ScrollView>(R.id.message_scroll)
                        sv.post(Runnable { sv.fullScroll(View.FOCUS_DOWN) })
                    }
                }
            }
            true
        }

        findViewById<Button>(R.id.fix_btn).setOnClickListener {
            obj = floatArrayOf(temp_coord[0],temp_coord[1],temp_coord[2])
        }
    }

    fun setDragAndDropApps_(){
//        updateFilter(0.093,3.91e-4)
        updateFilter(0.16 ,3.53e-4 )

        obj = floatArrayOf(50f,50f,220f)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.messenger, null
        )
        fragment1_vg.addView(child1)

        val fragment2_vg = fragment2 as ViewGroup
        val child2 = LayoutInflater.from(this).inflate(
            R.layout.album, null
        )
        fragment2_vg.addView(child2)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.width = 900; param1.height = 1600
        fragment1?.layoutParams = param1

        val param2 = fragment2?.layoutParams as LinearLayout.LayoutParams
        param2.width = 1000; param2.height = 1600
        fragment2?.layoutParams = param2

        for (i in 1..21) {
            var name: Int = getResources().getIdentifier("sample" + i, "id", getPackageName())
            findViewById<ImageView>(name).setOnTouchListener { v, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val item = ClipData.Item(v.tag as? CharSequence)
                        recent_drag_item = v.context.resources.getResourceEntryName(v.id)

                        val dragData = ClipData(
                            v.tag as? CharSequence,
                            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item
                        )
                        val myShadow = View.DragShadowBuilder(v)
                        v.startDrag(
                            dragData,   // the data to be dragged
                            myShadow,
                            null,       // no need to use local data
                            0           // flags (not currently used, set to 0)
                        )
                    }
                }
                true
            }
        }
        findViewById<LinearLayout>(R.id.message_board).setOnDragListener { v, dragEvent ->
            when(dragEvent.action){
                DragEvent.ACTION_DROP -> {
                    recent_drag_item?.let {
                        var resourceId = resources.getIdentifier(recent_drag_item, "drawable", getPackageName())
                        val imagev = ImageView(findViewById<LinearLayout>(R.id.message_board).context)
                        imagev.setImageDrawable(imagev.getResources().getDrawable(resourceId))
                        imagev.setPadding(20)
                        val param = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        param.gravity = Gravity.RIGHT
                        imagev?.layoutParams = param
                        findViewById<LinearLayout>(R.id.message_board).addView(imagev)
                        var sv = findViewById<ScrollView>(R.id.message_scroll)
                        sv.post(Runnable { sv.fullScroll(View.FOCUS_DOWN) })
                    }
                }
            }
            true
        }

        findViewById<Button>(R.id.fix_btn).setOnClickListener {
            obj = floatArrayOf(temp_coord[0],temp_coord[1],temp_coord[2])
        }
    }

    var activate_float = false
    fun setApplicationStack(){
        updateFilter(0.2 ,0.226 )

        objs = arrayOf( //floatArrayOf(-120f,0f,300f),
                        floatArrayOf(-40f,0f,300f),
                        floatArrayOf(40f,0f,300f),
                        floatArrayOf(120f,0f,300f),
                        floatArrayOf(200f,0f,300f))

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.application_stack, null
        )
        fragment1_vg.addView(child1)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1

//        findViewById<LinearLayout>(R.id.folder_background).setOnTouchListener { view, motionEvent ->
//            activate_float = true
//            when(motionEvent.action){
//                MotionEvent.ACTION_UP -> {
//                    activate_float = false
//                }
//            }
//            true
//        }
        activate_float = true
    }

    fun setLargeImageViewer(){
        updateFilter(0.16 ,3.53e-4 )

        obj = floatArrayOf(0f,0f,350f)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.large_image, null
        )
        fragment1_vg.addView(child1)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1
    }

    var recent_popup: View? = null
    var bookmarked = arrayOf("","","","","","","","","")
    var current_bookmarked = ""
    fun setBookmarking(){
        updateFilter(0.2 ,0.175 )

//        objs = arrayOf(
//            floatArrayOf(-100f,-150f,350f), floatArrayOf(50f,-150f,350f), floatArrayOf(200f,-150f,350f),
//            floatArrayOf(-100f,0f,350f), floatArrayOf(50f,0f,350f), floatArrayOf(200f,0f,350f),
//            floatArrayOf(-100f,150f,350f), floatArrayOf(50f,200f,350f), floatArrayOf(200f,150f,350f))
        objs = arrayOf(
            floatArrayOf(330f,-30f,10f),floatArrayOf(330f,0f,10f),floatArrayOf(330f,30f,10f),
            floatArrayOf(330f,-30f,-5f),floatArrayOf(330f,0f,-5f),floatArrayOf(330f,30f,-5f),
            floatArrayOf(330f,-30f,-20f),floatArrayOf(330f,0f,-20f),floatArrayOf(330f,30f,-20f)
        )

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.bookmarking, null
        )
        fragment1_vg.addView(child1)

        // Add Notification
        val vi = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Popup 1
        val popup1 = vi.inflate(R.layout.notification_fragment, null)
        popup1.findViewById<TextView>(R.id.noti_app_name).setText("Gmail")
        popup1.findViewById<TextView>(R.id.noti_app_info).setText("yourbusinesspartner@gmail.com")
        popup1.findViewById<TextView>(R.id.noti_content).setText("[URGENT REQUEST] A reply at your earliest convenience would be much appreciated")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup1)
        // Popup 2
        val popup2 = vi.inflate(R.layout.notification_fragment, null)
        popup2.findViewById<TextView>(R.id.noti_app_name).setText("Youtube")
        popup2.findViewById<TextView>(R.id.noti_app_info).setText("PM 12:58")
        popup2.findViewById<TextView>(R.id.noti_content).setText("Suggested: \"Parasite\" wins Best Picture")
        popup2.findViewById<ImageView>(R.id.noti_content_fig).setImageDrawable(getDrawable(R.drawable.parasite))
        popup2.findViewById<ImageView>(R.id.noti_content_fig).visibility = ImageView.VISIBLE
        findViewById<LinearLayout>(R.id.popup_list).addView(popup2)
        // Popup 3
        val popup3 = vi.inflate(R.layout.notification_fragment, null)
        popup3.findViewById<TextView>(R.id.noti_app_name).setText("TODO LIST")
        popup3.findViewById<TextView>(R.id.noti_app_info).setText("AM 09:00")
        popup3.findViewById<TextView>(R.id.noti_content).setText("visit dentist")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup3)
        // Popup 4
        val popup4 = vi.inflate(R.layout.notification_fragment, null)
        popup4.findViewById<TextView>(R.id.noti_app_name).setText("TODO LIST")
        popup4.findViewById<TextView>(R.id.noti_app_info).setText("AM 09:00")
        popup4.findViewById<TextView>(R.id.noti_content).setText("lunch date: pizza restaurant")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup4)
        // Popup 5
        val popup5 = vi.inflate(R.layout.notification_fragment, null)
        popup5.findViewById<TextView>(R.id.noti_app_name).setText("CNN")
        popup5.findViewById<TextView>(R.id.noti_app_info).setText("AM 07:25")
        popup5.findViewById<TextView>(R.id.noti_content).setText("Larry Tesler, creator of copy, cut and paste function, dies at 74")
        popup5.findViewById<ImageView>(R.id.noti_content_fig).setImageDrawable(getDrawable(R.drawable.rarry_tesler))
        popup5.findViewById<ImageView>(R.id.noti_content_fig).visibility = ImageView.VISIBLE
        findViewById<LinearLayout>(R.id.popup_list).addView(popup5)
        // Popup 6
        val popup6 = vi.inflate(R.layout.notification_fragment, null)
        popup6.findViewById<TextView>(R.id.noti_app_name).setText("Messenger")
        popup6.findViewById<TextView>(R.id.noti_app_info).setText("Yesterday")
        popup6.findViewById<TextView>(R.id.noti_content).setText("Mary: What will you prepare for my mother's day?")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup6)
        // Popup 7
        val popup7 = vi.inflate(R.layout.notification_fragment, null)
        popup7.findViewById<TextView>(R.id.noti_app_name).setText("Stack Overflow")
        popup7.findViewById<TextView>(R.id.noti_app_info).setText("Yesterday")
        popup7.findViewById<TextView>(R.id.noti_content).setText("You got a new answer!: Hi, I think ..")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup7)
        // Popup 8
        val popup8 = vi.inflate(R.layout.notification_fragment, null)
        popup8.findViewById<TextView>(R.id.noti_app_name).setText("Facebook")
        popup8.findViewById<TextView>(R.id.noti_app_info).setText("Yesterday")
        popup8.findViewById<TextView>(R.id.noti_content).setText("Kierra likes your post!")
        findViewById<LinearLayout>(R.id.popup_list).addView(popup8)

        var notifications = arrayOf(popup1,popup2,popup3,popup4,popup5,popup6,popup7,popup8)
        for (n in notifications){
            n.setOnLongClickListener {v: View ->
                val item = ClipData.Item(v.tag as? CharSequence)
                recent_drag_item = v.context.resources.getResourceEntryName(v.id)

                val dragData = ClipData(
                    v.tag as? CharSequence,
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
                val myShadow = View.DragShadowBuilder(v)
                v.startDrag(
                    dragData,   // the data to be dragged
                    myShadow,
                    null,       // no need to use local data
                    0           // flags (not currently used, set to 0)
                )
                recent_popup = v
                findViewById<LinearLayout>(R.id.status_bar).visibility = LinearLayout.GONE
                findViewById<LinearLayout>(R.id.zone_box).visibility = LinearLayout.VISIBLE
                current_bookmarked = "- " + v.findViewById<TextView>(R.id.noti_app_name).text.toString() + ": " +v.findViewById<TextView>(R.id.noti_content).text.toString()
                true
            }
        }

        findViewById<LinearLayout>(R.id.zone_box).setOnDragListener { v, dragEvent ->
            when(dragEvent.action){
                DragEvent.ACTION_DROP -> {
                    if(selected_zone_num != null){
                        recent_popup!!.visibility = LinearLayout.GONE
                        bookmarked[selected_zone_num!!] += ("\n"+current_bookmarked)
                    }
                    findViewById<LinearLayout>(R.id.zone_box).visibility = LinearLayout.GONE
                    findViewById<LinearLayout>(R.id.status_bar).visibility = LinearLayout.VISIBLE
                    findViewById<LinearLayout>(R.id.popup_background).setBackgroundResource(R.drawable.background_quebec)
                    recent_popup = null
                    selected_zone_num = null
                    current_bookmarked = ""
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    findViewById<LinearLayout>(R.id.status_bar).visibility = LinearLayout.VISIBLE
                }
            }
            true
        }
    }

    fun setMap(){
        updateFilter(0.2 ,0.165 )

        objs = arrayOf(
            floatArrayOf(300f,-40f,0f), floatArrayOf(300f,0f,0f), floatArrayOf(300f,40f,0f))
        check_walk = true

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.map, null
        )
        fragment1_vg.addView(child1)
    }

    var file_tj: File? = null
    fun setVirtualShelves(){
        updateFilter(0.2 ,0.555 )

        objs = arrayOf(
            floatArrayOf(-200f,-200f,300f), floatArrayOf(-100f,-200f,300f), floatArrayOf(0f,-200f,300f), floatArrayOf(100f,-200f,300f),floatArrayOf(200f,-200f,300f),
            floatArrayOf(-200f,-100f,300f), floatArrayOf(-100f,-100f,300f), floatArrayOf(0f,-100f,300f), floatArrayOf(100f,-100f,300f),floatArrayOf(200f,-100f,300f),
            floatArrayOf(-200f,0f,300f), floatArrayOf(-100f,0f,300f), floatArrayOf(0f,0f,300f), floatArrayOf(100f,0f,300f),floatArrayOf(200f,0f,300f),
            floatArrayOf(-200f,100f,300f), floatArrayOf(-100f,100f,300f), floatArrayOf(0f,100f,300f), floatArrayOf(100f,100f,300f),floatArrayOf(200f,100f,300f),
            floatArrayOf(-200f,200f,300f), floatArrayOf(-100f,200f,300f), floatArrayOf(0f,200f,300f), floatArrayOf(100f,200f,300f),floatArrayOf(200f,200f,300f))

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.virtual_shelves, null
        )
        fragment1_vg.addView(child1)

        findViewById<LinearLayout>(R.id.virtual_shelves).setOnTouchListener { view, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN -> shelve_activated = true
                MotionEvent.ACTION_UP -> shelve_activated = false
            }
            true
        }

    }

    var finding_target = false
    var timer = System.currentTimeMillis()
    fun setPeepholePointing(){
        updateFilter(0.22,8.0e-4)

        var selection_num = 16
        var selection_cnt = 1
//        val A = listOf(1200,1400,1600,1800,2000)
//        val W = listOf(100,200,300)
//        var Target:MutableList<IntArray> = mutableListOf()
//        A.forEach{
//            a->
//            W.forEach {
//                w->
//                Target.add(intArrayOf(a,w))
//            }
//        }
        var Target:MutableList<IntArray> = mutableListOf(
            intArrayOf(1200,158), intArrayOf(1200,116),
            intArrayOf(1400,357), intArrayOf(1400,255), intArrayOf(1400,185), intArrayOf(1400,136), intArrayOf(1400,100),
            intArrayOf(1600,586), intArrayOf(1600,408), intArrayOf(1600,291), intArrayOf(1600,211), intArrayOf(1600,155),
            intArrayOf(1800,659), intArrayOf(1800,459), intArrayOf(1800,328), intArrayOf(1800,238), intArrayOf(1800,175),
            intArrayOf(2000,264), intArrayOf(2000,194), intArrayOf(2000,144), intArrayOf(2000,107)
        )
        Target.shuffle()

        obj = floatArrayOf(0f,50f,200f)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.peephole_1d_left, null
        )
        fragment1_vg.addView(child1)
        val fragment2_vg = fragment2 as ViewGroup
        val child2 = LayoutInflater.from(this).inflate(
            R.layout.peephole_1d_right, null
        )
        fragment2_vg.addView(child2)

        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        param.setMargins(1080,1920,0,0)
        fragment?.layoutParams = param
        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1
        val param2 = fragment2?.layoutParams as LinearLayout.LayoutParams
        param2.setMargins(0,0,0,0)
        fragment2?.layoutParams = param2
        val button = Button(this)
        button.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        button.setBackgroundColor(getColor(R.color.colorTransparent))
        fragment2?.addView(button)

        val path = "/sdcard/OddEyedCam/"
        Log.d("pathpath",path)
        val file = File(path, "p"+p_number+".txt")
        file_tj = File(path, "p"+p_number+"_trajectory.txt")
        file.createNewFile()
        file_tj!!.createNewFile()

        var peephole_target_left = findViewById<Button>(R.id.peephole_target_left)
        var peephole_target_right = findViewById<Button>(R.id.peephole_target_right)

        var now_left_turn = true

        var AW = intArrayOf(0,0)
        var next_round_btn = findViewById<Button>(R.id.next_round)
        (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
        peephole_target_right.visibility = Button.GONE
        peephole_target_left.visibility = Button.GONE
        next_round_btn.setOnClickListener {
            if (Target.size <=0){
                finding_target = false
                (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
                next_round_btn.text = "END"
            }
            else{
                AW = Target.removeAt(0)
                peephole_target_right.visibility = Button.VISIBLE
                peephole_target_left.visibility = Button.VISIBLE
                peephole_target_left.layoutParams.width = AW[1]
                peephole_target_right.layoutParams.width = AW[1]
                var btw_margin = AW[0]/2-AW[1]/2
                var rest_margin = 1080 - btw_margin - AW[1]
                findViewById<Button>(R.id.peephole_left_margin_left).layoutParams.width = rest_margin
                findViewById<Button>(R.id.peephole_right_margin_left).layoutParams.width = btw_margin
                findViewById<Button>(R.id.peephole_left_margin_right).layoutParams.width = btw_margin
                findViewById<Button>(R.id.peephole_right_margin_right).layoutParams.width = rest_margin
                peephole_target_right.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                peephole_target_left.setBackgroundColor(getColor(R.color.colorPrimary))
                (next_round_btn.parent as LinearLayout).visibility = LinearLayout.GONE
                now_left_turn = true
            }
        }

        peephole_target_left.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    var time = System.currentTimeMillis() - timer
                    if(finding_target && now_left_turn){
                        vibrator !!. vibrate (10)
                        file.appendText(AW[0].toString()+","+AW[1].toString()+","+time.toString()+"\n")
                        peephole_target_left.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                        peephole_target_right.setBackgroundColor(getColor(R.color.colorPrimary))
                        selection_cnt += 1
                        file_tj!!.appendText("-999,-999,"+(System.currentTimeMillis()-timer).toString()+"\n")
                        now_left_turn = false
                        timer = System.currentTimeMillis()
                    }
                    if(selection_cnt == 1 && now_left_turn){
                        vibrator !!. vibrate (10)
                        selection_cnt += 1
                        finding_target = true
                        now_left_turn = false
                        peephole_target_left.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                        peephole_target_right.setBackgroundColor(getColor(R.color.colorPrimary))
                        file_tj!!.appendText("-999,-999,"+(System.currentTimeMillis()-timer).toString()+"\n")
                        timer = System.currentTimeMillis()
                    }
                }
            }
            true
        }
        peephole_target_right.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    var time = System.currentTimeMillis() - timer
                    if(selection_cnt >= selection_num && !now_left_turn){
                        vibrator !!. vibrate (10)
                        file.appendText(AW[0].toString()+","+AW[1].toString()+","+time.toString()+"\n")
                        file_tj!!.appendText("-999,-999,"+(System.currentTimeMillis()-timer).toString()+"\n")
                        selection_cnt = 1
                        (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
                        peephole_target_right.visibility = Button.GONE
                        peephole_target_left.visibility = Button.GONE
                        finding_target = false
                        now_left_turn = true
                    }
                    if(finding_target && !now_left_turn){
                        vibrator !!. vibrate (10)
                        file.appendText(AW[0].toString()+","+AW[1].toString()+","+time.toString()+"\n")
                        peephole_target_right.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                        peephole_target_left.setBackgroundColor(getColor(R.color.colorPrimary))
                        selection_cnt += 1
                        file_tj!!.appendText("-999,-999,"+(System.currentTimeMillis()-timer).toString()+"\n")
                        now_left_turn = true
                        timer = System.currentTimeMillis()
                    }
                }
            }
            true
        }
    }

    fun setPeephole2DTarget(W: Int, r: Double, Target:MutableList<Int>) {
        var x_margins = doubleArrayOf(r* sin(toRadians(80.0)), -r* sin(toRadians(80.0)), r* cos(toRadians(30.0)),
            -r* sin(toRadians(40.0)), r* sin(toRadians(20.0)), 0.0,
            -r* sin(toRadians(20.0)), r*sin(toRadians(40.0)), -r*cos(toRadians(30.0)))
        var y_margins = doubleArrayOf(-r* cos(toRadians(80.0)), -r* cos(toRadians(80.0)), r* sin(toRadians(30.0)),
            -r* cos(toRadians(40.0)), r* cos(toRadians(20.0)), -r,
            r* cos(toRadians(20.0)), -r*cos(toRadians(40.0)), r*sin(toRadians(30.0)))

        for (i in Target.indices){
            var ll = findViewById<LinearLayout>(Target[i])
            ll.setBackgroundColor(getColor(R.color.colorGrayTransparent))
            val param = ll.layoutParams as FrameLayout.LayoutParams
            param.width = W; param.height = W
            param.setMargins((SCREEN_WIDTH/2 + x_margins[i]-W/2).toInt(),(SCREEN_HEIGHT/2 + y_margins[i]-W/2).toInt(),0,0)
            ll?.layoutParams = param
        }
    }
    fun setPeephole2D(){
        updateFilter(0.3 ,8.00e-4 )
        obj = floatArrayOf(30f,50f,300f)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.peephole_2d, null
        )
        fragment1_vg.addView(child1)

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        fragment1?.layoutParams = param1

        var Target:MutableList<Int> = mutableListOf(R.id.target1,R.id.target2,R.id.target3,R.id.target4,R.id.target5,R.id.target6,R.id.target7,R.id.target8,R.id.target9)
        var rW:MutableList<DoubleArray> = mutableListOf(doubleArrayOf(500.0,179.0),doubleArrayOf(500.0,141.0),doubleArrayOf(500.0,111.0),doubleArrayOf(500.0,89.0),
                                                        doubleArrayOf(400.0,184.0),doubleArrayOf(400.0,143.0),doubleArrayOf(400.0,113.0),doubleArrayOf(400.0,89.0),
                                                        doubleArrayOf(300.0,180.0),doubleArrayOf(300.0,138.0),doubleArrayOf(300.0,107.0),doubleArrayOf(300.0,84.0),
                                                        doubleArrayOf(200.0,215.0),doubleArrayOf(200.0,159.0),doubleArrayOf(200.0,120.0),doubleArrayOf(200.0,92.0),
                                                        doubleArrayOf(100.0,107.0),doubleArrayOf(100.0,79.0))
//        var rW:MutableList<DoubleArray> = mutableListOf()
//        var r = doubleArrayOf(135.0,270.0,540.0)
//        var W = doubleArrayOf(83.0,166.0,332.0)
//        r.forEach{
//                radius->
//            W.forEach {
//                    w->
//                rW.add(doubleArrayOf(radius,w))
//            }
//        }
        rW.shuffle()

        val path = "/sdcard/OddEyedCam/"
        Log.d("pathpath",path)
        val file = File(path, "2d_serial_p"+p_number+".txt")
        file_tj = File(path, "2d_serial_p"+p_number+"_trajectory.txt")
        file.createNewFile()
        file_tj!!.createNewFile()

        var t = Target.removeAt(0)
        findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorYellow))


        var rw = doubleArrayOf(0.0,0.0)
        var A = 0
        var next_round_btn = findViewById<Button>(R.id.next_round)
        (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
        next_round_btn.setOnClickListener {
            if (rW.size <=0){
                next_round_btn.text = "END"
            }
            else{
                Target = mutableListOf(R.id.target1,R.id.target2,R.id.target3,R.id.target4,R.id.target5,R.id.target6,R.id.target7,R.id.target8,R.id.target9)
                rw = rW.removeAt(0)
                A = round(2 * rw[0] * sin(toRadians(80.0))).toInt()
                setPeephole2DTarget(rw[1].toInt(),rw[0],Target)
                (next_round_btn.parent as LinearLayout).visibility = LinearLayout.GONE
                Target.add(R.id.target1)
                t = Target.removeAt(0)
                findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorYellow))
            }
        }

        var recticle = findViewById<FrameLayout>(R.id.reticle)
        recticle.visibility = FrameLayout.VISIBLE
        recticle.setOnTouchListener { view, motionEvent ->
            if(Target.size <= 0){
                finding_target = false
                (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
            } else if(t == R.id.target1){
                var target_ll = findViewById<LinearLayout>(t)
                var x = (recent_px_pos!![0] + target_ll.marginLeft + target_ll.width/2 -  SCREEN_WIDTH/2) - SCREEN_WIDTH/2
                var y = (recent_px_pos!![1] + target_ll.marginTop + target_ll.height/2 -  SCREEN_HEIGHT/2) - SCREEN_HEIGHT/2
                if (abs(x) > rw[1]/2 || abs(y) > rw[1]/2) {
                    findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorAccent))
                }
                else{
                    findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorPrimary))
                    t = Target.removeAt(0)
                    findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorYellow))
                    finding_target = true
                    file_tj!!.appendText("-999,-999\n")
                }
                vibrator !!. vibrate (10)
                timer = System.currentTimeMillis()
            } else{
                when(motionEvent.action){
                    MotionEvent.ACTION_DOWN ->{
                        // Moving Time
                        var time = System.currentTimeMillis() - timer
                        //
                        finding_target = true
                        vibrator !!. vibrate (10)
                        // Error Rate
                        var target_ll = findViewById<LinearLayout>(t)
                        var x = SCREEN_WIDTH/2 - (recent_px_pos!![0] + target_ll.marginLeft + target_ll.width/2 -  SCREEN_WIDTH/2)
                        var y = SCREEN_HEIGHT/2 - (recent_px_pos!![1] + target_ll.marginTop + target_ll.height/2 -  SCREEN_HEIGHT/2)
                        if (abs(x) > rw[1]/2 || abs(y) > rw[1]/2) {
                            file.appendText(A.toString()+","+rw[1].toString()+","+time.toString()+","+"1\n")
                            findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorAccent))
                        }
                        else {
                            file.appendText(A.toString()+","+rw[1].toString()+","+time.toString()+","+"0\n")
                            findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorPrimary))
                        }
                        t = Target.removeAt(0)
                        if(t != R.id.target1)
                            findViewById<LinearLayout>(t).setBackgroundColor(getColor(R.color.colorYellow))
                        file_tj!!.appendText("-999,-999\n")
                        timer = System.currentTimeMillis()
                    }
                }
            }
            true
        }
    }

    fun rotateTargetPathHint(degree: Float){
        var line = findViewById<LinearLayout>(R.id.hint_path)
        line.visibility = LinearLayout.VISIBLE
        val anim = RotateAnimation(degree, degree, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f)
        anim.interpolator = LinearInterpolator()
        anim.repeatCount = Animation.INFINITE
        line.setAnimation(anim)
        line.startAnimation(anim)
    }
    fun setPeephole2DRandomTarget(AWd: IntArray?){
        rotateTargetPathHint(AWd!![2].toFloat())
        var A = AWd!![0]; var W = AWd!![1]; var direction = AWd[2].toDouble()
        var upper = intArrayOf((SCREEN_WIDTH/2 + A/2* sin(toRadians(direction))-W/2).toInt(),(SCREEN_HEIGHT/2 - A/2* cos(toRadians(direction))-W/2).toInt())
        var lower = intArrayOf((SCREEN_WIDTH/2 - A/2* sin(toRadians(direction))-W/2).toInt(),(SCREEN_HEIGHT/2 + A/2* cos(toRadians(direction))-W/2).toInt())
        var target_known = findViewById<Button>(R.id.target_known)
        target_known.layoutParams.width = W; target_known.layoutParams.height = W
        var param_known = target_known.layoutParams as FrameLayout.LayoutParams
        var position_known = upper; var position_unkown = lower
        param_known.setMargins(position_known[0],position_known[1],0,0)
        target_known.layoutParams = param_known
        var target_unknown = findViewById<Button>(R.id.target_unknown)
        target_unknown.layoutParams.width = W; target_unknown.layoutParams.height = W
        var param_unknown = target_unknown.layoutParams as FrameLayout.LayoutParams
        param_unknown.setMargins(position_unkown[0],position_unkown[1],0,0)
        target_known.setBackgroundColor(getColor(R.color.colorPrimary))
        target_unknown.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
        target_unknown.layoutParams = param_unknown
    }
    fun setPeephole2DRandom(){
        updateFilter(0.25 ,8.00e-4 )
        obj = floatArrayOf(50f,-10f,250f)

        val path = "/sdcard/OddEyedCam/"
        val file_task = File(path, "p"+p_number+"_task.txt")

        var Target:MutableList<IntArray> = mutableListOf(
            intArrayOf(1200,100), intArrayOf(1615,100), intArrayOf(1870,100),
            intArrayOf(1327,150), intArrayOf(1547,150), intArrayOf(2089,150),
            intArrayOf(1293,200), intArrayOf(1515,200), intArrayOf(2063,200),
            intArrayOf(1375,250), intArrayOf(1616,250), intArrayOf(1894,250),
            intArrayOf(1397,300), intArrayOf(1649,300), intArrayOf(1939,300),
            intArrayOf(1374,350), intArrayOf(1630,350), intArrayOf(1924,350),
            intArrayOf(1315,400), intArrayOf(1570,400), intArrayOf(1863,400),
            intArrayOf(1479,450), intArrayOf(1766,450), intArrayOf(2096,450),
            intArrayOf(1366,500), intArrayOf(1644,500), intArrayOf(1962,500),
            intArrayOf(1503,550), intArrayOf(1808,550), intArrayOf(2159,550)

        )
        var directions:MutableList<Int> = mutableListOf(0, 30, 60, 90, -60, -30)

        var AWd:MutableList<IntArray> = mutableListOf()
        if (file_task.exists()){
            file_task.readLines().forEach{
                val a = it.split(',')
                AWd.add(intArrayOf(a[0].toInt(),a[1].toInt(),a[2].toInt()))
            }
        }else{
            Target.forEach{
                    t->
                directions.forEach {
                        d->
                    AWd.add(intArrayOf(t[0],t[1],d))
                }
            }
            AWd.shuffle()
            file_task.createNewFile()
            AWd.forEach {
                file_task.appendText(it[0].toString()+","+it[1].toString()+","+it[2].toString()+"\n")
            }
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val fragment1_vg = fragment1 as ViewGroup
        val child1 = LayoutInflater.from(this).inflate(
            R.layout.peephole_2d_random, null
        )
        fragment1_vg.addView(child1)

        SCREEN_WIDTH = 3000; SCREEN_HEIGHT = 3000

        val param1 = fragment1?.layoutParams as LinearLayout.LayoutParams
        param1.setMargins(0,0,0,0)
        param1.width = SCREEN_WIDTH; param1.height = SCREEN_HEIGHT
        fragment1?.layoutParams = param1
        val param2 = fragment2?.layoutParams as LinearLayout.LayoutParams
        param2.setMargins(0,0,0,0)
        param2.width = 0; param2.height = 0
        fragment2?.layoutParams = param2

        findViewById<LinearLayout>(R.id.mask).visibility = LinearLayout.VISIBLE

        var target_known = findViewById<Button>(R.id.target_known)
        var target_unknown = findViewById<Button>(R.id.target_unknown)

        var file: File? = null
        var task_step = 6
        for(i in 1..6){
            file = File(path, "p"+p_number+"-"+i.toString()+".txt")
            file_tj = File(path, "p"+p_number+"-"+i.toString()+"_trajectory.txt")
            if (!file.exists()){
                task_step = i -1
                for(j in 1..30*(i-1)) AWd.removeAt(0)
                break
            }
        }
        if (task_step == 6){
            for(j in 1..30*6) AWd.removeAt(0)
        }

        var file_err: File? = null
        var awd: IntArray? = null
        var now_turn_known = true
        var next_round_btn = findViewById<Button>(R.id.next_round)
        (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE

        next_round_btn.setOnClickListener {
            if (AWd.size <=0){
                next_round_btn.text = "END"
            }
            else{
                awd = AWd.removeAt(0)
                if (AWd.size%30 == 29){
                    task_step += 1
                    file = File(path, "p"+p_number+"-"+task_step.toString()+".txt")
                    file_tj = File(path, "p"+p_number+"-"+task_step.toString()+"_trajectory.txt")
                    file_err = File(path, "p"+p_number+"-"+task_step.toString()+"_error.txt")
                    file!!.createNewFile()
                    file_tj!!.createNewFile()
                }
                setPeephole2DRandomTarget(awd)
                now_turn_known = true
                target_known.setBackgroundColor(getColor(R.color.colorPrimary))
                target_unknown.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                target_known.visibility = Button.VISIBLE
                target_unknown.visibility = Button.VISIBLE
                (next_round_btn.parent as LinearLayout).visibility = LinearLayout.GONE
                findViewById<TextView>(R.id.remain_task).text = task_step.toString()+"/6 단계\n남은 과업:"+(AWd.size%30).toString()
                if(AWd.size%30 == 0){
                    next_round_btn.setBackgroundColor(getColor(R.color.error_color_material_light))
                    next_round_btn.text = "Take a break"
                    next_round_btn.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    next_round_btn.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                else{
                    next_round_btn.setBackgroundColor(getColor(R.color.colorPrimary))
                    next_round_btn.text = "Next Round"
                    next_round_btn.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    next_round_btn.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }


        var record_mark = findViewById<LinearLayout>(R.id.record)
        var step = 0
        var selection_num = 3
        target_known.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN ->{
                    if(now_turn_known){
                        var line = findViewById<LinearLayout>(R.id.hint_path)
                        line.clearAnimation()
                        line.visibility = LinearLayout.GONE
                        now_turn_known = false
                        var time = System.currentTimeMillis() - timer
                        view.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                        target_unknown.setBackgroundColor(getColor(R.color.colorPrimary))
                        step += 1
                        if(finding_target) {
                            file!!.appendText(awd!![0].toString() + "," + awd!![1].toString() + "," + time.toString() + "," + awd!![2].toString() + "," + step.toString() +",0\n")
                        }
                        file_tj!!.appendText("-999,-999,-999\n")
                        finding_target = true
                        record_mark.background = getDrawable(R.drawable.circle_shape)
                        if(step >= selection_num){
                            step = 0
                            finding_target = false
                            record_mark.background = getDrawable(R.drawable.circle_shape_blue)
                            target_known.visibility = Button.GONE
                            target_unknown.visibility = Button.GONE
                            (next_round_btn.parent as LinearLayout).visibility = LinearLayout.VISIBLE
                        }
                        vibrator!!.vibrate(10)
                        timer = System.currentTimeMillis()
                    }
                }
            }
            true
        }

        target_unknown.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN ->{
                    if(!now_turn_known){
                        var time = System.currentTimeMillis() - timer
                        view.setBackgroundColor(getColor(R.color.dim_foreground_material_dark))
                        target_known.setBackgroundColor(getColor(R.color.colorPrimary))
                        step += 1
                        now_turn_known = true
                        if(finding_target) {
                            file!!.appendText(awd!![0].toString() + "," + awd!![1].toString() + "," + time.toString() + "," + awd!![2].toString() + "," + step.toString() + ",0\n")
                            file_tj!!.appendText("-999,-999,-999\n")
                        }
                        finding_target = true
                        record_mark.background = getDrawable(R.drawable.circle_shape)
                        vibrator!!.vibrate(10)
                        timer = System.currentTimeMillis()
                    }
                }
            }
            true
        }

        findViewById<Button>(R.id.buffer_btn).setOnTouchListener { view, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN ->{
                    var time = System.currentTimeMillis() - timer
                    var x = motionEvent.x - 4500 + 1080/2
                    var y = motionEvent.y - 4500 + 1920/2
                    if(finding_target && y > 420){
                        vibrator!!.vibrate(50)
                        step += 1
                        file!!.appendText(awd!![0].toString() + "," + awd!![1].toString() + "," + time.toString() + "," + awd!![2].toString() + "," + step.toString() + ",1\n")
                        file_err!!.appendText(x.toString()+","+y.toString()+","+awd!![0].toString() + "," + awd!![1].toString() + "," + awd!![2].toString()+ ","+step.toString()+"\n")
                        if(step % 2 == 0){
                            now_turn_known = true
                            target_known.setBackgroundColor(getColor(R.color.colorPrimary))
                            target_unknown.setBackgroundColor(getColor(R.color.colorAccent))
                        }
                        else{
                            now_turn_known = false
                            target_known.setBackgroundColor(getColor(R.color.colorAccent))
                            target_unknown.setBackgroundColor(getColor(R.color.colorPrimary))
                        }
                        step -= 2
                        finding_target = false
                        record_mark.background = getDrawable(R.drawable.circle_shape_blue)
                    }
                }
            }
            true
        }
    }


    /************************
    Position Scenario Methods
     ************************/

    fun setPosition(phone_obj: PhonePosition){
        when (type){
            getString(R.string.scenario1) -> setPosition_DragAndDrop(obj, phone_obj, fragment)
            getString(R.string.scenario2) -> setPosition_ApplicationStack(objs, phone_obj, fragment)
            getString(R.string.scenario3) -> setPosition_LargeImageViewer(obj, phone_obj, fragment1)
            getString(R.string.scenario4) -> setPosition_Bookmarking(objs, phone_obj)
            getString(R.string.scenario5) -> setPosition_Map(objs, phone_obj)
            getString(R.string.scenario6) -> setPosition_VirtualShelves(objs, phone_obj)
            "Peephole Pointing" -> setPosition_PeepholePointing(obj, phone_obj)
            "Peephole 2D" -> setPosition_Peephole2DRandom(obj,phone_obj)
        }

    }
    var temp_coord = floatArrayOf(0f,0f,0f)
    fun setPosition_DragAndDrop(obj: FloatArray?, phone_obj: PhonePosition, fragment: LinearLayout?){
        val margin = toScreenCoordinate(obj,phone_obj, fragment)
        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        param.setMargins(margin[0],margin[1],0,0)
        fragment?.layoutParams = param
    }
    fun setPosition_DragAndDrop_(obj: FloatArray?, phone_obj: PhonePosition, fragment: LinearLayout?){
        temp_coord = floatArrayOf(phone_obj.x,phone_obj.y,phone_obj.z)
        val margin = toScreenCoordinate(obj,phone_obj, fragment)
        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        val bound_left = 1080; val bound_right = 150
        if(margin[0] > bound_left + 900 || margin[0] < bound_right - 900){
            fragment1!!.alpha = 0.5f
            fragment2!!.alpha = 0.5f
        }else{
            fragment1!!.alpha = 1f
            fragment2!!.alpha = 1f
        }
        if (margin[0] > bound_left) margin[0] =  bound_left
        if (margin[0] < bound_right) margin[0] =  bound_right
        param.setMargins(margin[0],2000,0,0)
        fragment?.layoutParams = param
    }

    var selected_folder_num = 1
    fun setPosition_ApplicationStack(objs: Array<FloatArray>?, phone_obj: PhonePosition, fragment: LinearLayout?){
        val folder_name = arrayOf(//"Search",
            "Entertainment", "Camera", "Work",  "Basic")
        val app_icons = arrayOf(
            //intArrayOf(R.drawable.safari_icon, R.drawable.chrome_icon, R.drawable.aliexpress_icon, R.drawable.amazon_icon, R.drawable.ebay_icon),
            intArrayOf(R.drawable.music_icon,R.drawable.gplay_icon,R.drawable.facebook_icon,R.drawable.twitter_icon,R.drawable.instagram_icon,R.drawable.duolingo_icon, R.drawable.angrybirds_icon, R.drawable.applemusic_icon),
            intArrayOf(R.drawable.camera_icon, R.drawable.video_icon,R.drawable.photo_icon, R.drawable.photoeditorpro_icon,R.drawable.tiktok_icon,R.drawable.googlephoto_icon),
            intArrayOf(R.drawable.email_icon, R.drawable.calendar_icon, R.drawable.timer_icon,R.drawable.slack_icon,R.drawable.document_icon,R.drawable.dropbox_icon),
            intArrayOf(R.drawable.call_icon,R.drawable.contact_icon,R.drawable.message_icon,R.drawable.memo_icon,R.drawable.map_icon))
        val app_names = arrayOf(
            //arrayOf("Safari", "Chrome", "Aliexpress", "Amazon", "ebay"),
            arrayOf("Music","Play","Facebook","Twitter", "Instagram", "Duolingo", "Angry Birds","Apple Music"),
            arrayOf("Camera", "Video", "Album", "Photo Editor", "Tik Tok", "Photo"),
            arrayOf("Email", "Calendar", "Timer","Slack","Document","Dropbox"),
            arrayOf("Call","Contact","Message", "Memo", "Map"))

        // Deciding App
        val apps = arrayOf(R.id.app1,R.id.app2,R.id.app3,R.id.app4,R.id.app5,R.id.app6,R.id.app7,R.id.app8,R.id.app9)
        val app_icon_ids = arrayOf(R.id.app1_icon,R.id.app2_icon,R.id.app3_icon,R.id.app4_icon,R.id.app5_icon,R.id.app6_icon,R.id.app7_icon,R.id.app8_icon,R.id.app9_icon)
        val app_name_ids = arrayOf(R.id.app1_name,R.id.app2_name,R.id.app3_name,R.id.app4_name,R.id.app5_name,R.id.app6_name,R.id.app7_name,R.id.app8_name,R.id.app9_name)

        apps.forEach {
            findViewById<LinearLayout>(it).setBackgroundColor(getColor(R.color.colorTransparent))
        }
//        if (activate_float){
            var r = 1; var c = 1
            Log.d("gravity_vector","("+gravityVector!![0].toString()+","+gravityVector!![1].toString()+","+gravityVector!![2].toString()+")")
            if (gravityVector!![0] < 0-0.8) c = 2
            else if (gravityVector!![0] > 0+0.8) c = 0
            if (gravityVector!![2] < 7.2-0.8) r = 2
            else if (gravityVector!![2] > 7.2+0.8) r = 0
            var app_idx = r*3 + c
            findViewById<LinearLayout>(apps[app_idx]).setBackgroundColor(getColor(R.color.colorYellow))
//        }
//        else{
            // Deciding Folder
            var p = getFilteredCoordinate(phone_obj)
            var x = p[0]
            var y = p[1]
            for (i in objs!!.indices) {
                var distance = floatArrayOf(x - objs[i][0], y - objs[i][1], 0f)
                if (abs(distance[0]) < 30) {
                    if (selected_folder_num != i) {
                        vibrator!!.vibrate(10)
                        findViewById<TextView>(R.id.folder_name).setText(folder_name[i])
                        selected_folder_num = i
                        for (j in 0..8){
                            if(j < app_icons[i].size){
                                findViewById<LinearLayout>(apps[j]).visibility = LinearLayout.VISIBLE
                                findViewById<ImageView>(app_icon_ids[j]).setImageDrawable(getDrawable(app_icons[i][j]))
                                findViewById<TextView>(app_name_ids[j]).text = app_names[i][j]
                            }else{
                                findViewById<LinearLayout>(apps[j]).visibility = LinearLayout.INVISIBLE
                            }
                        }
                    }
                    if (i == 0) findViewById<LinearLayout>(R.id.previous_folder).visibility = LinearLayout.INVISIBLE
                    else if (i == objs!!.indices.last) findViewById<LinearLayout>(R.id.next_folder).visibility = LinearLayout.INVISIBLE
                    else{
                        findViewById<LinearLayout>(R.id.previous_folder).visibility = LinearLayout.VISIBLE
                        findViewById<LinearLayout>(R.id.next_folder).visibility = LinearLayout.VISIBLE
                    }
                    break
                }
            }
//        }
    }

    fun setPosition_LargeImageViewer(obj: FloatArray?, phone_obj: PhonePosition, fragment: LinearLayout?){
        val r = obj!![2]
        val min_d = 250f
        val p = floatArrayOf(phone_obj.x,phone_obj.y,phone_obj.z)

        // Size
        var length_mm = get_d(p,CHEST)
        if(length_mm < min_d)
            length_mm = min_d
        else if (length_mm > r)
            length_mm = r
//        length_mm = length_mm - min_d
        length_mm = r - length_mm
        val length_px = getPixelsOfMM(length_mm)
        var size = (SCREEN_WIDTH + length_px* ZOOM_PARAM).toDouble()

        size = f_size.filter(size, System.currentTimeMillis()/1000.0)
        val cropWidth = size.toFloat()
        val cropHeight = (size*1.8f).toFloat()

        // Position
        var x_mm = r * get_theta(p,CHEST) * PI / 180f
        var y_mm = r * (-get_phi(p,CHEST)) * PI / 180f

        var x_px = getPixelsOfMM(x_mm.toFloat())
        var y_px = getPixelsOfMM(y_mm.toFloat())

        var myimage_drawable = R.drawable.world_map
        var myimage_imagev = findViewById<ImageView>(R.id.largeimage)
        val drawableWidth = getDrawable(myimage_drawable)!!.intrinsicWidth
        val drawableHeight = getDrawable(myimage_drawable)!!.intrinsicHeight
        val offsetW = drawableWidth/2f - cropWidth/2f
        val offsetH = drawableHeight/2f - cropHeight/2f
        val viewPositionX = f_x.filter((x_px + offsetW).toDouble(),System.currentTimeMillis()/1000.0).toFloat()
        val viewPositionY = f_y.filter((y_px + offsetH).toDouble(),System.currentTimeMillis()/1000.0).toFloat()
        val drawableRect = RectF(viewPositionX, viewPositionY, viewPositionX + cropWidth, viewPositionY + cropHeight)
        val viewRect = RectF(0f, 0f, SCREEN_WIDTH.toFloat(), SCREEN_HEIGHT.toFloat())
        val matrix: Matrix = findViewById<ImageView>(R.id.largeimage).imageMatrix
        myimage_imagev.imageMatrix = matrix
        matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.START)
        myimage_imagev.setImageMatrix(matrix)
        myimage_imagev.invalidate()
    }

    var selected_zone_num: Int? = null
    fun setPosition_Bookmarking(objs: Array<FloatArray>?, phone_obj: PhonePosition){
        val zone_name_list = arrayOf("\uD83C\uDF06 \nRemind me \ntomorrow night","TODO\n \uD83D\uDEA8\uD83D\uDEA8\uD83D\uDEA8","Fun\n \uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F",
                                     "\uD83C\uDF05 \nRemind me \ntomorrow morning","TODO\n \uD83D\uDEA8\uD83D\uDEA8","Fun\n \uD83C\uDF1F\uD83C\uDF1F",
                                     "\uD83D\uDD50 \nRemind me \nin 1 hour","TODO\n \uD83D\uDEA8","Fun\n \uD83C\uDF1F")

        var p = getFilteredCoordinate(phone_obj)
//        var x = p[0]
//        var y = p[1]
        var theta = get_theta(p,RIGHT_SHOULDER)
        var phi = get_phi(p,RIGHT_SHOULDER)
        var d = get_d(p,RIGHT_SHOULDER)
        val ll = findViewById<LinearLayout>(R.id.zone_box)

        recent_popup?.let {
            for (i in objs!!.indices)  {
                var distance = floatArrayOf(theta-objs[i][1],phi-objs[i][2])
                if (abs(distance[0]) < 14 && abs(distance[1]) < 7 && d > 300f){
                    if(selected_zone_num != i){
                        vibrator!!.vibrate(10)
                    }
                    ll.visibility = TextView.VISIBLE
                    ll.setBackgroundColor(getColor(R.color.colorPrimaryTransparent))
                    findViewById<TextView>(R.id.zone_name).setText(zone_name_list[i])
                    findViewById<TextView>(R.id.bookmarked).setText(bookmarked[i])
                    selected_zone_num = i
                    break
                }
                else if (i == objs.size - 1 && d < 280f){
                    findViewById<TextView>(R.id.zone_name).setText("")
                    findViewById<TextView>(R.id.bookmarked).setText("")
                    ll.setBackgroundColor(getColor(R.color.colorTransparent))
                    selected_zone_num = null
                }
            }
        }
    }

    var selected_map_prev: Int? = null
    var selected_map_d_prev: Int? = null
    var walk_delay = 0.0
    fun setPosition_Map(objs: Array<FloatArray>?, phone_obj: PhonePosition){
//        var p = floatArrayOf(phone_obj.x, phone_obj.y, phone_obj.z)
        var p = getFilteredCoordinate(phone_obj)
        var d = get_d(p,RIGHT_SHOULDER)
        var theta = get_theta(p,RIGHT_SHOULDER)
        var phi = get_phi(p,RIGHT_SHOULDER)

        var linearlayouts = intArrayOf(R.id.hotel,R.id.map,R.id.restaurant,
                                        R.id.destination, R.id.map, R.id.bus)

        var current_t = System.currentTimeMillis()/1000.0

        var selected_map = selected_map_prev
        for (i in objs!!.indices) {
            var diff = floatArrayOf(theta - (objs[i][1]), phi  - (objs[i][1]))
            var btn = findViewById<Button>(R.id.walking_mode)
            if (abs(diff[0]) < 17f) {
                if (mode_walk || current_t - walk_delay < 5){
                    selected_map = i + 3
                    btn.setText("WALKING")
                    btn.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent))
                }else{
                    selected_map = i
                    btn.setText("STILL")
                    walk_delay = 0.0
                    btn.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary))
                }
                break
            }
        }
        if(mode_walk)
            walk_delay = current_t

        linearlayouts.forEach {
            findViewById<LinearLayout>(it).visibility = LinearLayout.GONE
        }
        if(selected_map!=null){
            findViewById<LinearLayout>(linearlayouts[selected_map!!]).visibility = LinearLayout.VISIBLE
        }

        var selected_map_d = selected_map_d_prev
        if (selected_map == 1){
            findViewById<LinearLayout>(R.id.arrow).visibility = LinearLayout.GONE
            if (d < 280f) {
                findViewById<ImageView>(R.id.map_content).setImageDrawable(getDrawable(R.drawable.map_close))
                selected_map_d = 0
            }
            else if(d >= 300f){
                findViewById<ImageView>(R.id.map_content).setImageDrawable(getDrawable(R.drawable.map_overall))
                selected_map_d = 1
            }
        }
        else if(selected_map == 4){
//            if(d < 230f) {
//                findViewById<LinearLayout>(R.id.arrow).visibility = LinearLayout.GONE
//                findViewById<ImageView>(R.id.map_content).setImageDrawable(getDrawable(R.drawable.map_close))
//                selected_map_d = 0
//            }
            if(d < 280f) {
                findViewById<LinearLayout>(R.id.arrow).visibility = LinearLayout.GONE
                findViewById<ImageView>(R.id.map_content).setImageDrawable(getDrawable(R.drawable.map_overall))
                selected_map_d = 1
            }
            else if(d >= 300f){
                findViewById<LinearLayout>(R.id.arrow).visibility = LinearLayout.VISIBLE
                selected_map_d = 2
            }
        }

        if (selected_map_prev != selected_map || selected_map_d_prev != selected_map_d) {
            vibrator!!.vibrate(10)
        }
        selected_map_prev = selected_map
        selected_map_d_prev = selected_map_d
    }

    var selected_shelve_num = intArrayOf(0)
    var shelve_center_row = 2; var shelve_center_col = 2
    var shelve_activated: Boolean = false
    var shelve_start_position: FloatArray? = null
    fun setPosition_VirtualShelves(objs: Array<FloatArray>?, phone_obj: PhonePosition){
        val iv_id = arrayOf(R.id.center_app_icon,R.id.upper_app_icon,R.id.right_app_icon,R.id.lower_app_icon,R.id.left_app_icon)
        val tv_id = arrayOf(R.id.center_app_text,R.id.upper_app_text,R.id.right_app_text,R.id.lower_app_text,R.id.left_app_text)

        val icon = arrayOf(R.drawable.map_icon,R.drawable.map_icon, R.drawable.call_icon, R.drawable.music_icon,R.drawable.calendar_icon,
                                R.drawable.map_icon,R.drawable.map_icon, R.drawable.map_icon, R.drawable.map_icon, R.drawable.map_icon,
                                R.drawable.call_icon,R.drawable.call_icon, R.drawable.call_icon, R.drawable.call_icon, R.drawable.call_icon,
                                R.drawable.music_icon,R.drawable.volume_up, R.drawable.next, R.drawable.volume_down, R.drawable.previous,
                                R.drawable.calendar_icon,R.drawable.next_day, R.drawable.morning, R.drawable.afternoon, R.drawable.night)
        val icon_text = arrayOf(
            "Menu","Map","Call","Music","Calendar",
            "Map","Thai restaurant","Workplace","Chinatown","Home",
            "Call","Vivien Trevino","Kim","Hibah Busby","Mom",
            "Music","Volume Up","Next","Volume Down","Previous",
            "Calendar","Tomorrow","Morning","Afternoon","Evening")
        val suffix_text = arrayOf(
            "Menu","Loading ","Calling ","","Loading ")

        if (selected_shelve_num.size == 1) {
            findViewById<LinearLayout>(R.id.marking_menu).visibility = LinearLayout.VISIBLE
            findViewById<ImageView>(iv_id[0]).visibility = ImageView.GONE
            findViewById<TextView>(tv_id[0]).visibility = TextView.VISIBLE
        }
        else if(selected_shelve_num.size == 2){
            findViewById<LinearLayout>(R.id.marking_menu).visibility = LinearLayout.VISIBLE
            findViewById<ImageView>(iv_id[0]).visibility = ImageView.VISIBLE
            findViewById<TextView>(tv_id[0]).visibility = TextView.GONE
        }
        else if (selected_shelve_num.size == 3){
            val idx = selected_shelve_num[1]*5 + selected_shelve_num[2]
            findViewById<LinearLayout>(R.id.marking_menu).visibility = LinearLayout.GONE
            findViewById<ImageView>(R.id.running_app_icon).setBackgroundResource(icon[idx])
            findViewById<TextView>(R.id.running_app_text).setText(suffix_text[selected_shelve_num[1]]+icon_text[idx]+" ..")

        }

        for (i in 0..4){
            findViewById<ImageView>(iv_id[i]).setImageDrawable(getDrawable(icon[selected_shelve_num.last()*5+i]))
            findViewById<TextView>(tv_id[i]).setText(icon_text[selected_shelve_num.last()*5+i])
        }

        var p = getFilteredCoordinate(phone_obj)
        var x = p[0]; var y = p[1]; var z = p[2]
        var d = get_d(p,RIGHT_SHOULDER)
//        var x = phone_obj.x; var y = phone_obj.y; var z = phone_obj.z
//        if(d > 300f){
//            if(abs(x-RIGHT_SHOULDER[0]) < 40 && abs(y-(RIGHT_SHOULDER[1])) < 40 && !shelve_activated)
//                shelve_activated = true
//        }
//        else{
//            shelve_activated = false
//        }

        if(!shelve_activated) {
            shelve_start_position = null
            selected_shelve_num = intArrayOf(0)
            shelve_center_row = 2
            shelve_center_col = 2
            findViewById<LinearLayout>(R.id.marking_menu).visibility = LinearLayout.INVISIBLE
        }
        else {
            if (shelve_start_position == null){
                shelve_start_position = floatArrayOf(x,y,z)
                vibrator!!.vibrate(10)
            }
            for (i in objs!!.indices) {
                var distance = floatArrayOf(x - (objs[i][0]+shelve_start_position!![0]), y - (objs[i][1]+shelve_start_position!![1]))
                if (abs(distance[0]) < 40 && abs(distance[1]) < 40) {
                    var r = i / 5 - shelve_center_row;
                    var c = i % 5 - shelve_center_col
                    var direction = 0
                    if (r == -1 && c == 0) direction = 1
                    else if (r == 0 && c == 1) direction = 2
                    else if (r == 1 && c == 0) direction = 3
                    else if (r == 0 && c == -1) direction = 4
                    if (direction != 0 && i != shelve_center_row * 5 + shelve_center_col && selected_shelve_num.size < 3) {
                        vibrator!!.vibrate(10)
                        shelve_center_row = i / 5; shelve_center_col = i % 5
                        selected_shelve_num += direction
                    }
                    break
                }
            }
        }
    }

    var recent_phone: PhonePosition? = null
    fun setPosition_PeepholePointing(obj: FloatArray?, phone_obj: PhonePosition){
        recent_phone = phone_obj
        val margin = toScreenCoordinate(obj,phone_obj, fragment)
        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        param.setMargins(margin[0]+SCREEN_WIDTH/2,SCREEN_HEIGHT,0,0)
        fragment?.layoutParams = param
        if(finding_target) {
            var x = SCREEN_WIDTH - margin[0]
            var y = margin[1]
            file_tj!!.appendText(x.toString() + "," + y.toString() + ","+ (System.currentTimeMillis()-timer).toString() + "\n")
        }
    }

    var recent_px_pos: IntArray? = null
    fun setPosition_Peephole2D(obj: FloatArray?, phone_obj: PhonePosition){
        val margin = toScreenCoordinate(obj,phone_obj, fragment)
        recent_px_pos = margin
        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        param.setMargins(margin[0]+SCREEN_WIDTH/2,margin[1]+SCREEN_HEIGHT/2,0,0)
        fragment?.layoutParams = param
        if(finding_target) {
            var x = SCREEN_WIDTH - margin[0]
            var y = margin[1]
            file_tj!!.appendText(x.toString() + "," + y.toString() + "\n")
        }
    }

    fun setPosition_Peephole2DRandom(obj: FloatArray?, phone_obj: PhonePosition){
        val margin = toScreenCoordinate(obj,phone_obj, fragment)
        recent_px_pos = margin
        val param = fragment?.layoutParams as FrameLayout.LayoutParams
        param.setMargins(margin[0],margin[1],0,0)
        fragment?.layoutParams = param

//        var x = SCREEN_WIDTH/2 - (margin[0] - background!!.width/2.0 + fragment!!.width/2.0)
//        var y = SCREEN_HEIGHT/2 - (margin[1] - background!!.height/2.0 + fragment!!.height/2.0)
//        Log.d("Position",x.toString() + "," + y.toString() + "\n")
        if(finding_target) {
            var x = SCREEN_WIDTH/2 - (margin[0] - background!!.width/2.0 + fragment!!.width/2.0)
            var y = SCREEN_HEIGHT/2 - (margin[1] - background!!.height/2.0 + fragment!!.height/2.0)
            file_tj!!.appendText(x.toString() + "," + y.toString() + ","+ (System.currentTimeMillis()-timer).toString() + "\n")
        }
    }


    /***********************
    Position General Methods
     ***********************/

    private fun getPixelsOfMM(value: Float): Float{
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_MM, value,
            resources.displayMetrics
        )
        return px
    }

    private fun dotproduct(a : FloatArray,  b: FloatArray): Float{
        return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]
    }

    private fun toScreenCoordinate(obj: FloatArray?, phone_obj: PhonePosition, fragment: LinearLayout?) : IntArray{
//        var p = getFilteredCoordinate(phone_obj)
        var p = floatArrayOf(phone_obj.x,phone_obj.y,phone_obj.z)
        val v = floatArrayOf(obj!![0] - p[0], obj!![1] - p[1], obj!![2] - p[2])
        val xdotMM : Float = dotproduct(phone_obj.xaxis, v)
        val ydotMM : Float = dotproduct(phone_obj.yaxis, v)
        val zdotMM : Float = dotproduct(phone_obj.zaxis, v)
//        if (zdotMM < 0)
////            intArrayOf(0,0)
        var x = getPixelsOfMM(xdotMM) + background!!.width/2.0 - fragment!!.width/2.0
        var y = -getPixelsOfMM(ydotMM) + background!!.height/2.0 - fragment!!.height/2.0
        x = f_x.filter(x, System.currentTimeMillis()/1000.0)
        y = f_y.filter(y, System.currentTimeMillis()/1000.0)
        return intArrayOf(x.toInt(),y.toInt())
    }

    private fun toScreenCoordinate_wo_filter(obj: FloatArray?, phone_obj: PhonePosition, fragment: LinearLayout?) : IntArray{
//        var p = getFilteredCoordinate(phone_obj)
        var p = floatArrayOf(phone_obj.x,phone_obj.y,phone_obj.z)
        val v = floatArrayOf(obj!![0] - p[0], obj!![1] - p[1], obj!![2] - p[2])
        val xdotMM : Float = dotproduct(phone_obj.xaxis, v)
        val ydotMM : Float = dotproduct(phone_obj.yaxis, v)
        val zdotMM : Float = dotproduct(phone_obj.zaxis, v)
//        if (zdotMM < 0)
////            intArrayOf(0,0)
        var x = getPixelsOfMM(xdotMM) + background!!.width/2.0 - fragment!!.width/2.0
        var y = -getPixelsOfMM(ydotMM) + background!!.height/2.0 - fragment!!.height/2.0
        return intArrayOf(x.toInt(),y.toInt())
    }


    /**********************
    One Euro Filter Methods
     **********************/

    fun updateFilter(mincutoff: Double?, beta: Double?){
        if(mincutoff != null){
            f_x.setMinCutoff(mincutoff)
            f_y.setMinCutoff(mincutoff)
            f_z.setMinCutoff(mincutoff)
            f_size.setMinCutoff(mincutoff)
        }
        if(beta != null){
            f_x.setBeta(beta)
            f_y.setBeta(beta)
            f_z.setBeta(beta)
            f_size.setBeta(beta)
        }
    }

    fun getFilteredCoordinate(phone_obj: PhonePosition): FloatArray{
        val timestamp: Double = System.currentTimeMillis()/1000.0
        var x_filtered: Float = f_x.filter(phone_obj.x.toDouble(),timestamp).toFloat()
        var y_filtered: Float = f_y.filter(phone_obj.y.toDouble(),timestamp).toFloat()
        var z_filtered: Float = f_z.filter(phone_obj.z.toDouble(),timestamp).toFloat()
        return floatArrayOf(x_filtered,y_filtered,z_filtered)
    }

    /*************************
    Coordinate in Polar System
     *************************/

    fun get_d(v: FloatArray, c: FloatArray): Float{
        var p = floatArrayOf(v[0]-c[0],v[1]-c[1],v[2]-c[2])
        var d = sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2])
        return d
    }

    fun get_theta(v: FloatArray, c: FloatArray): Float{
        var p = floatArrayOf(v[0]-c[0],v[1]-c[1],v[2]-c[2])
        var x = p[0]
        var z = p[2]
        var theta = atan(x/z) * 180f/ PI.toFloat()
        return theta
    }

    fun get_phi(v: FloatArray, c: FloatArray): Float{
        var xyz = get_d(v,c)
        var x_z = get_d(floatArrayOf(v[0],0f,v[2]),c)
        var phi = acos(x_z/xyz) * 180f/ PI.toFloat()
        if (v[1] >= 0)
            phi = -phi
        return phi
    }
}