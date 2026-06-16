package com.example.colorblend.ui.gacha

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R
import java.util.Locale

class EqualizadorActivity : AppCompatActivity() {

    private var musicaService: MusicaService? = null
    private var serviceConectado = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicaService.MusicaBinder
            musicaService = binder.getService()
            serviceConectado = true
            setupEqualizador()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConectado = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizador)

        val intent = Intent(this, MusicaService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun setupEqualizador() {
        val service = musicaService ?: return
        val eq = service.equalizer ?: return
        val container = findViewById<LinearLayout>(R.id.containerBandas)
        container.removeAllViews()

        val minEQLevel = eq.bandLevelRange[0]
        val maxEQLevel = eq.bandLevelRange[1]

        for (i in 0 until eq.numberOfBands) {
            val freq = eq.getCenterFreq(i.toShort()) / 1000
            
            val layoutBanda = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 32) }
            }

            val tvFreq = TextView(this).apply {
                text = if (freq < 1000) "${freq} Hz" else "${freq / 1000} kHz"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
            }

            val seek = SeekBar(this).apply {
                max = maxEQLevel - minEQLevel
                progress = eq.getBandLevel(i.toShort()).toInt() - minEQLevel
                progressTintList = ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
                thumbTintList = ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
                
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            eq.setBandLevel(i.toShort(), (progress + minEQLevel).toShort())
                        }
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }

            layoutBanda.addView(tvFreq)
            layoutBanda.addView(seek)
            container.addView(layoutBanda)
        }

        // Bass Boost
        findViewById<SeekBar>(R.id.seekBass).apply {
            progress = service.bassBoost?.roundedStrength?.toInt() ?: 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) service.bassBoost?.setStrength(progress.toShort())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        // Virtualizer
        findViewById<SeekBar>(R.id.seekVirtualizer).apply {
            progress = service.virtualizer?.roundedStrength?.toInt() ?: 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) service.virtualizer?.setStrength(progress.toShort())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        // Speed and Pitch
        val tvSpeed = findViewById<TextView>(R.id.tvSpeed)
        val tvPitch = findViewById<TextView>(R.id.tvPitch)

        findViewById<SeekBar>(R.id.seekSpeed).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress / 100f
                tvSpeed.text = "Velocidad: ${String.format(Locale.US, "%.1f", speed)}x"
                if (fromUser) service.setPlaybackParams(speed, (findViewById<SeekBar>(R.id.seekPitch).progress / 100f))
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        findViewById<SeekBar>(R.id.seekPitch).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val pitch = progress / 100f
                tvPitch.text = "Tono (Pitch): ${String.format(Locale.US, "%.1f", pitch)}x"
                if (fromUser) service.setPlaybackParams((findViewById<SeekBar>(R.id.seekSpeed).progress / 100f), pitch)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        if (serviceConectado) unbindService(connection)
        super.onDestroy()
    }
}
