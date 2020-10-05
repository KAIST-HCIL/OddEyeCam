package com.daehwa.wificomm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ScenarioList : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val LIST_MENU = arrayOf(getString(R.string.scenario1),getString(R.string.scenario2),getString(R.string.scenario3),
            getString(R.string.scenario4),getString(R.string.scenario5),getString(R.string.scenario6),"Peephole Pointing","Peephole 2D")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scenario_list)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, LIST_MENU)

        val listview = findViewById(R.id.listview1) as ListView
        listview.setAdapter(adapter)

        listview.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, v: View, position: Int, id: Long) {

                // get TextView's Text.
                val strText = parent.getItemAtPosition(position) as String

                val nextIntent = Intent(v.context, Scenario::class.java)
                nextIntent.action = Intent.ACTION_SEND
                nextIntent.type="text/plain"
                nextIntent.putExtra(Intent.EXTRA_TEXT, strText)
                var p_number = findViewById<EditText>(R.id.p_number).text.toString()
                if (p_number != "")
                    nextIntent.putExtra("P_NUMBER",p_number)
                startActivity(nextIntent)

                // TODO : use strText
            }
        }
    }
}