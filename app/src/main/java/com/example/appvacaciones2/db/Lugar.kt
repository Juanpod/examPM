package com.example.appvacaciones2.db

data class Lugar(
    val id:Int,
    var nombre:String,
    var imagen:String,
    var latitude:Double,
    var longitude:Double,
    var orden:Int,
    var costoAlojamiento:Double,
    var costoTransporte:Double,
    var comentarios:String
)
