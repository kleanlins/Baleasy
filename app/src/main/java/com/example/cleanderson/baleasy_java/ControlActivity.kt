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
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.control_layout.*
import org.jetbrains.anko.Android
import rx.observables.StringObservable
import rx.Observer
import rx.schedulers.Schedulers
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

        control_led_on.setOnClickListener { sendCommand("a") }
        control_led_off.setOnClickListener { sendCommand("b") }
        control_led_disconnect.setOnClickListener { disconnect() }

    }

    private fun sendCommand(input: String) {
        val inputSt = m_bluetoothSocket!!.inputStream
        val scanner = Scanner(inputSt)
        val inputAsStream: String
        val reader = InputStreamReader(inputSt)

        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())

                val array = ByteArray(15)
                var arrayString: String? = ""

                /*val reading = List(4) {
                    scanner.nextInt()
                }*/

                /*val reading = scanner.nextLine()

                val numbers = reading.split(' ').map {
                    it.toInt()
                }

                Log.i("device", numbers.joinToString(" "))*/

                /*do {
                    val reading = m_bluetoothSocket!!.inputStream.read(array)
                } while (array[3].toInt() != 32)

                for (i in 0..14) {
                    arrayString += array[i].toChar()
                }
                Log.i("bytesToString", "" + arrayString)

                arrayString = ""*/


                /*val reading = scanner.nextLine()

                val numbers = reading.split(' ').map {
                    it.toInt()
                }

                Log.i("device", numbers.joinToString(" "))*/


            } catch (e: IOException) {
                e.printStackTrace()
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
                createObservable(m_bluetoothSocket!!.inputStream)
            }
            m_progress.dismiss()
        }
    }

}

private fun createObservable(inputStream: InputStream) {
    val reader = InputStreamReader(inputStream)
    val scanner = Scanner(inputStream)
    val array = ByteArray(15)
    var arrayString: String? = ""

    StringObservable.byLine(StringObservable.from(reader))
            .map { it.split(" ") }
            .subscribeOn(rx.schedulers.Schedulers.computation())
//            .observeOn(rx.Scheduler)
            .subscribe{numbers->
                Log.i("Device", numbers.joinToString(" "))
            }

    /*Strings.from(reader)
            .map { it.split(" ") }
            .delay(17, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if(it.size == 15)
                    Log.i("device", it.joinToString(" "))
            }*/
}
