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

        const val sensor_limit = 30
        var tip_stage: Int = 0
        val tip_list: List<String> = listOf(
                "Inicio da Baliza",
                "Esterça totalmente o volante para o lado indicado",
                "Agora gire o volante totalmente ao contrário",
                "Ajuste o volante o necessário para finalizar",
                "Baliza finalizada :)")

        var rotateLeft: Animation? = null
        var rotateRight: Animation? = null
        var blink: Animation? = null

        var lv_fl: Int = 0
        var lv_fr: Int = 0
        var lv_rl: Int = 0
        var lv_rr: Int = 0

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

        sensor_fl.alpha = 1f
        sensor_fr.alpha = 1f
        sensor_rl.alpha = 1f
        sensor_rr.alpha = 1f


    }

    private fun changeState(){

        Log.i("STATE", tip_stage.toString())

        if(tip_stage == 5) {

            next_step_btn.text = "START"
            tip_stage = 0

            steering_img.alpha = 1f
            sensor_fl.alpha = 1f
            sensor_fr.alpha = 1f
            sensor_rl.alpha = 1f
            sensor_rr.alpha = 1f

            done_img.alpha = 0f
        }
        else
            next_step_btn.text = "NEXT"


        when(tip_stage){
            0 -> {
                tips_text.text = tip_list[tip_stage]
            }
            1 -> {
                turnRight()
                tips_text.text = tip_list[tip_stage]
            }
            2 -> {
                turnLeft()
                tips_text.text = tip_list[tip_stage]
                goAhead()
            }
            3 -> {
                turnRight()
                tips_text.text = tip_list[tip_stage]
                goBack()
            }
            4 -> {
                tips_text.text = tip_list[tip_stage]
                endBaleasy()
                next_step_btn.text = "END"
            }
            else -> {
                TODO("WHAT HAPPENED?")
            }
        }

        tip_stage++

    }

    private fun turnLeft(){

        left_arrow.alpha = 1f
        right_arrow.alpha = 0f
        left_arrow.startAnimation(blink)

        steering_img.clearAnimation()
        steering_img.startAnimation(rotateLeft)
    }

    private fun turnRight(){

        right_arrow.alpha = 1f
        left_arrow.alpha = 0f
        right_arrow.startAnimation(blink)

        steering_img.clearAnimation()
        steering_img.startAnimation(rotateRight)
    }

    private fun goAhead(){
        rear_arrow.alpha = 0f
        rear_arrow.clearAnimation()

        front_arrow.alpha = 1f
        front_arrow.startAnimation(blink)
    }

    private fun goBack(){
        front_arrow.alpha = 0f
        front_arrow.clearAnimation()

        rear_arrow.alpha = 1f
        rear_arrow.startAnimation(blink)
    }

    private fun endBaleasy(){
        steering_img.alpha = 0f
        front_arrow.alpha = 0f
        rear_arrow.alpha = 0f
        left_arrow.alpha = 0f
        right_arrow.alpha = 0f

        sensor_fl.alpha = 0f
        sensor_fr.alpha = 0f
        sensor_rl.alpha = 0f
        sensor_rr.alpha = 0f

        done_img.alpha = 1f
        done_img.startAnimation(blink)
    }

    private fun scaleValue(value: Int): Float{
        var lValue = value

        if(lValue > sensor_limit)
            lValue = sensor_limit

        return ((lValue/0.3f)/100) * 2.3f
    }

    private suspend fun readFromArduino(scn: Scanner): List<String> = withContext(Dispatchers.IO){
        val line = scn.nextLine()
        Log.i("device", line)

        line.split(" ")
    }


    private fun startStream(fl: TextView, fr: TextView, rl: TextView, rr: TextView) {
        val scanner = Scanner(m_bluetoothSocket!!.inputStream)
        var sensorValues: List<String>

        if (m_bluetoothSocket != null) {
            GlobalScope.launch(Dispatchers.Main) {

                while (true) {

                    sensorValues = readFromArduino(scanner)

                    fl.text = sensorValues[0] + "cm"
                    fr.text = sensorValues[1] + "cm"
                    rl.text = sensorValues[2] + "cm"
                    rr.text = sensorValues[3] + "cm"

                    if (sensorValues[0].toInt() != 0)
                        lv_fl = sensorValues[0].toInt()

                    if (sensorValues[1].toInt() != 0)
                        lv_fr = sensorValues[1].toInt()

                    if (sensorValues[2].toInt() != 0)
                        lv_rl = sensorValues[2].toInt()

                    if (sensorValues[3].toInt() != 0)
                        lv_rr = sensorValues[3].toInt()


                    sensor_fl.scaleY = scaleValue(lv_fl)
                    sensor_fr.scaleY = scaleValue(lv_fr)
                    sensor_rl.scaleY = scaleValue(lv_rl)
                    sensor_rr.scaleY = scaleValue(lv_rr)

                    delay(17)

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