package com.example.appvacaciones2.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.appvacaciones2.db.DBContrato.TablaLugares

class DBHelper(contexto: Context) : SQLiteOpenHelper(contexto, DB_NOMBRE, null, DB_VERSION) {

    companion object {
        const val DB_NOMBRE ="lugares.gb"
        const val DB_VERSION = 1
        const val SQL_CREACION_TABLAS = """
            CREATE TABLE ${TablaLugares.TABLA_NOMBRE} (
                ${TablaLugares.COLUMNA_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${TablaLugares.COLUMNA_NOMBRE} TEXT, 
                ${TablaLugares.COLUMNA_IMAGEN} TEXT,
                ${TablaLugares.COLUMNA_LATITUD} DOUBLE,
                ${TablaLugares.COLUMNA_LONGITUD} DOUBLE,
                ${TablaLugares.COLUMNA_ORDEN} INTEGER,
                ${TablaLugares.COLUMNA_CALOJAMIENTO} DOUBLE,
                ${TablaLugares.COLUMNA_CTRANSPORTE} DOUBLE,
                ${TablaLugares.COLUMNA_COMENTARIOS} TEXT
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREACION_TABLAS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }


}