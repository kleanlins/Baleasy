package com.example.cleanderson.baleasy_java

import android.animation.ObjectAnimator
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.control_layout.*
import rx.observables.StringObservable
import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.Main
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class ControlActivity : AppCompatActivity() {

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)
        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)

        ConnectToDevice(this).execute()

        switch_button.setOnClickListener { startStream(front_left, front_right, rear_left, rear_right) }
        control_led_disconnect.setOnClickListener { disconnect() }

    }

    fun startStream(fl: TextView, fr: TextView, rl: TextView, rr: TextView) {
        if (m_bluetoothSocket != null) {
            GlobalScope.async(Dispatchers.Main){
//                readFromArduino(m_bluetoothSocket!!.inputStream)

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

private fun createObservable(inputStream: InputStream) {
    val reader = InputStreamReader(inputStream)

    RxJavaInterop.toV2Observable(StringObservable.byLine(StringObservable.from(reader)))
            .map { it.split(" ")
                    .map { it.toInt() }
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{numbers->
                Log.i("Device", numbers.joinToString(" "))
            }

    /*Strings.from(reader, 15)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                    Log.i("device", it.toString())
            }*/

}
