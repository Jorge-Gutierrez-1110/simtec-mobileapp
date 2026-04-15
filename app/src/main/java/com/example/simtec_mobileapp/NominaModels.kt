package com.example.simtec_mobileapp

data class Periodo(
    val id: Int = 0,
    val nombre: String = "",
    val fecha_inicio: String = "",
    val fecha_fin: String = "",
    val tipo: String = "",
    val status_cierre: String = "Abierto"
)

data class EmpleadoNomina(
    val id: Int = 0,
    val nombre: String = "",
    val apellido_paterno: String = "",
    val numero_empleado: String = "",
    val imagen_perfil: String? = null,
    val rfc: String = "",
    val tipo_nomina: String = "",
    val salario_diario: Double = 0.0,
    val salario_diario_integrado: Double = 0.0,
    val turno_id: Int? = null,
    val email: String = "",
    val banco: String? = null,
    val cuenta_bancaria: String? = null,
    val clabe_interbancaria: String? = null,
    val nombre_turno: String? = null,
    val dias_trabajo: String? = null,
    val cliente_nombre: String? = null,
    val diasTrab: Int = 0,
    val diasDescanso: Int = 0,
    val diasVacaciones: Int = 0,
    val diasFalta: Int = 0,
    val totalPercepciones: Double = 0.0,
    val totalDeducciones: Double = 0.0,
    val neto: Double = 0.0,
    val listaPercepciones: List<Concepto> = emptyList(),
    val listaDeducciones: List<Concepto> = emptyList()
)

data class Concepto(
    val id: Int = 0,
    val nombre: String = "",
    val tipo: String = ""
)

data class NominaResponse(
    val success: Boolean = false,
    val message: String? = null,
    val nomina_id: Int? = null
)

data class NominaResumen(
    val total_empleados: Int = 0,
    val total_percepciones: Double = 0.0,
    val total_deducciones: Double = 0.0,
    val total_neto: Double = 0.0
)

data class NominaCierreRequest(
    val periodo_id: Int,
    val resumen: NominaResumen,
    val detalles: List<DetalleNomina>
)

data class DetalleNomina(
    val empleado_id: Int,
    val dias_pagados: Int,
    val sueldo_diario: Double,
    val total_percepciones: Double,
    val total_deducciones: Double,
    val neto: Double,
    val desglose: Desglose
)

data class Desglose(
    val percepciones: List<Concepto> = emptyList(),
    val deducciones: List<Concepto> = emptyList()
)
