package com.example.colorblend.domain.model

enum class Rareza {
    COMUN,
    RARO,
    EPICO,
    LEGENDARIO;

    companion object {
        fun desde(favoritos: Int): Rareza {
            return when {
                favoritos >= 13000 -> LEGENDARIO
                favoritos >= 5000  -> EPICO
                favoritos >= 1000  -> RARO
                else               -> COMUN
            }
        }
    }
}