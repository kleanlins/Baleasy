package com.example.cleanderson.baleasy_java

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import kotlinx.android.synthetic.main.control_layout.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.Main
import java.io.IOException
import java.util.*

class ControlActivity : AppCompatActivity() {

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String

        var tip_stage: Int = -1
        val tip_list: List<String> = listOf(
                "Inicio da Baliza",
                "Esterce totalmente o volante para o lado indicado",
                "Gire o volante totalmente ao contrário",
                "Ajuste o volante o necessário para finalizar")

        var rotateLeft: Animation? = null
        var rotateRight: Animation? = null
        var blink: Animation? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContentView(R.layout.control_layout)

        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)

        ConnectToDevice(this).execute()

        //imageView modifications
        car_img.alpha = 0.2f

        switch_button.setOnClickListener { startStream(front_left, front_right, rear_left, rear_right) }
        control_led_disconnect.setOnClickListener { disconnect() }
        next_step_btn.setOnClickListener{ changeState() }

        rotateLeft = AnimationUtils.loadAnimation(this, R.anim.rotate)
        rotateRight = AnimationUtils.loadAnimation(this, R.anim.rotate_right)
        blink = AnimationUtils.loadAnimation(this, R.anim.blink)

    }

    private fun changeState(){
        if(tip_stage == 3)
            tip_stage = 0
        else
            tip_stage++

        when(tip_stage){
            0 -> enterLeft()
            1 -> enterRight()
        }
    }

    private fun enterLeft(){
        Log.i("STATE", tip_stage.toString())

        left_arrow.alpha = 1.0f
        right_arrow.alpha = 0.0f
        left_arrow.startAnimation(blink)

        steering_img.clearAnimation()
        steering_img.startAnimation(rotateLeft)
    }

    private fun enterRight(){
        Log.i("STATE", tip_stage.toString())

        right_arrow.alpha = 1.0f
        left_arrow.alpha = 0.0f
        right_arrow.startAnimation(blink)

        steering_img.clearAnimation()
        steering_img.startAnimation(rotateRight)
    }

    private fun startStream(fl: TextView, fr: TextView, rl: TextView, rr: TextView) {
        if (m_bluetoothSocket != null) {
            GlobalScope.async(Dispatchers.Main){

                val scanner = Scanner(m_bluetoothSocket!!.inputStream)
                var line: String
                var sensorValues: List<String>

                while(true) {
                    line = scanner.nextLine()
//                    Log.i("device", line)

                    sensorValues = line.split(" ")

                    fl.text = sensorValues[0]
                    fr.text = sensorValues[1]
                    rl.text = sensorValues[2]
                    rr.text = sensorValues[3]

                    delay(100)

                }
            }
        }
    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {

        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "Please wait.")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("DATA", "Coudn't connect.")
            } else {
                m_isConnected = true
//                createObservable(m_bluetoothSocket!!.inputStream)
            }
            m_progress.dismiss()
        }
    }

}