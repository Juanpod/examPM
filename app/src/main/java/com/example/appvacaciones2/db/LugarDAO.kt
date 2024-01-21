package com.example.appvacaciones2.db

import android.content.ContentValues

import com.example.appvacaciones2.db.DBContrato.TablaLugares

class LugarDAO(val db:DBHelper) {

    fun findAll():List<Lugar>{

        val cursor = db.readableDatabase.query(
            TablaLugares.TABLA_NOMBRE,
            null,
            null,
            null,
            null,
            null,
            "${DBContrato.TablaLugares.COLUMNA_ID} ASC"
        )

        val lista = mutableListOf<Lugar>()
        while (cursor.moveToNext() ){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_ID))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_NOMBRE))
            val imagen = cursor.getString(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_IMAGEN))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_LATITUD))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_LONGITUD))
            val orden = cursor.getInt(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_ORDEN))
            val costoAlojamiento = cursor.getDouble(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_CALOJAMIENTO))
            val costoTransporte = cursor.getDouble(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_CTRANSPORTE))
            val comentarios = cursor.getString(cursor.getColumnIndexOrThrow(TablaLugares.COLUMNA_COMENTARIOS))

            val lugar = Lugar(id,nombre,imagen,latitude,longitude,orden,costoAlojamiento,costoTransporte,comentarios )
            lista.add(lugar)
        }
        cursor.close()
        return lista
    }


    fun insertar(lugar:Lugar):Long {
        val valores = ContentValues().apply {
            put(DBContrato.TablaLugares.COLUMNA_NOMBRE, lugar.nombre)
            put(DBContrato.TablaLugares.COLUMNA_IMAGEN, lugar.imagen)
            put(DBContrato.TablaLugares.COLUMNA_LATITUD, lugar.latitude)
            put(DBContrato.TablaLugares.COLUMNA_LONGITUD, lugar.longitude)
            put(DBContrato.TablaLugares.COLUMNA_ORDEN, lugar.orden)
            put(DBContrato.TablaLugares.COLUMNA_CALOJAMIENTO, lugar.costoAlojamiento)
            put(DBContrato.TablaLugares.COLUMNA_CTRANSPORTE, lugar.costoTransporte)
            put(DBContrato.TablaLugares.COLUMNA_COMENTARIOS, lugar.comentarios)
        }
        return db.writableDatabase.insert(DBContrato.TablaLugares.TABLA_NOMBRE,null,valores)
    }

    fun actualizar(lugar:Lugar):Unit{
        val valores = ContentValues().apply {
            put(DBContrato.TablaLugares.COLUMNA_NOMBRE, lugar.nombre)
            put(DBContrato.TablaLugares.COLUMNA_IMAGEN, lugar.imagen)
            put(DBContrato.TablaLugares.COLUMNA_LATITUD, lugar.latitude)
            put(DBContrato.TablaLugares.COLUMNA_LONGITUD, lugar.longitude)
            put(DBContrato.TablaLugares.COLUMNA_ORDEN, lugar.orden)
            put(DBContrato.TablaLugares.COLUMNA_CALOJAMIENTO, lugar.costoAlojamiento)
            put(DBContrato.TablaLugares.COLUMNA_CTRANSPORTE, lugar.costoTransporte)
            put(DBContrato.TablaLugares.COLUMNA_COMENTARIOS, lugar.comentarios)
        }
        // Obtiene el ID del lugar
        val id = lugar.id

        // Crea la cláusula WHERE de la consulta SQL
        val where = "${DBContrato.TablaLugares.COLUMNA_ID} = ?"

        // Crea un array de parámetros para la consulta SQL
        val whereArgs = arrayOf(id.toString())
        db.writableDatabase.update(
            DBContrato.TablaLugares.TABLA_NOMBRE,
            valores,
            where,
            whereArgs
        )
    }

    fun borrar(lugar:Lugar):Unit {
        val id = lugar.id
        val where =  "${DBContrato.TablaLugares.COLUMNA_ID} = ?"
        val whereArgs = arrayOf(id.toString())
        val deletedRows = db.writableDatabase.delete(TablaLugares.TABLA_NOMBRE, where,whereArgs)
    }

    fun getCantidadRegistros():Int {
        val cursor = db.readableDatabase.rawQuery("SELECT COUNT(*) FROM ${TablaLugares.TABLA_NOMBRE}",null)
        cursor.moveToFirst()
        val cantidadRegistros = cursor.getInt(0)
        return cantidadRegistros
    }





}