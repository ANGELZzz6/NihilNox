package com.example.colorblend.ui.gacha.metas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.colorblend.domain.model.Meta
import java.util.Calendar

object MetaRecordatorioScheduler {

    /**
     * Programa el próximo recordatorio para esta meta.
     * Busca el próximo día habilitado a partir de hoy/mañana con la hora configurada.
     */
    fun programar(context: Context, meta: Meta) {
        val hora = meta.horaRecordatorio ?: return
        val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
        if (partes.size < 2) return

        val horaInt   = partes[0]
        val minutoInt = partes[1]

        val ahora = Calendar.getInstance()

        // Buscar el próximo trigger: puede ser hoy (si la hora no pasó) o los próximos días
        var diasBuscados = 0
        var trigger: Calendar? = null

        while (diasBuscados < 8) {
            val candidato = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, diasBuscados)
                set(Calendar.HOUR_OF_DAY, horaInt)
                set(Calendar.MINUTE, minutoInt)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Si el candidato ya pasó hoy, saltar a mañana
            if (diasBuscados == 0 && candidato.timeInMillis <= ahora.timeInMillis) {
                diasBuscados++
                continue
            }

            // Verificar si ese día está habilitado en la meta
            val diaSemana = candidato.get(Calendar.DAY_OF_WEEK)
            val diaFormato = when (diaSemana) {
                Calendar.MONDAY    -> 1
                Calendar.TUESDAY   -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY  -> 4
                Calendar.FRIDAY    -> 5
                Calendar.SATURDAY  -> 6
                Calendar.SUNDAY    -> 7
                else -> 1
            }

            val diasLista = meta.diasSemana
                ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                ?: listOf(1, 2, 3, 4, 5, 6, 7) // null = todos los días

            if (diaFormato in diasLista) {
                trigger = candidato
                break
            }

            diasBuscados++
        }

        trigger ?: return

        val intent = Intent(context, MetaRecordatorioReceiver::class.java).apply {
            putExtra(MetaRecordatorioReceiver.EXTRA_META_ID,     meta.id)
            putExtra(MetaRecordatorioReceiver.EXTRA_TITULO,      meta.titulo)
            putExtra(MetaRecordatorioReceiver.EXTRA_DESCRIPCION, meta.descripcion ?: "")
        }

        val pending = PendingIntent.getBroadcast(
            context, meta.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pending
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pending
            )
        }
    }

    /** Cancela el recordatorio de esta meta */
    fun cancelar(context: Context, metaId: Int) {
        val intent = Intent(context, MetaRecordatorioReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, metaId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
    }

    /** Reprograma automáticamente al día siguiente habilitado después de dispararse */
    fun reprogramarSiguiente(context: Context, meta: Meta) {
        programar(context, meta)
    }
}