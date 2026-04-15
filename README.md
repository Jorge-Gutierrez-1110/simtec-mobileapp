# 📚 SIMTEC - Documentación del Proyecto

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Equipo:** 4 personas (2 Mobile + 2 Web)  
**Estado:** Completo

---

## 📄 Documentos

La documentación del proyecto está dividida en **3 documentos principales**:

### 1️⃣ **01_DEFINICION.md** (11.7 KB)
**¿Qué estamos construyendo?**

- Identificación del proyecto
- Visión general y propósito
- Alcance: 12 módulos backend + 10 módulos frontend + 8 en mobile
- Requisitos funcionales (RF-001 a RF-007)
- Requisitos no-funcionales
- Equipo y responsabilidades
- Dependencias externas
- Restricciones técnicas
- Riesgos identificados
- Cronograma

**Léelo primero si:** Necesitas entender QUÉ es SIMTEC

---

### 2️⃣ **02_MODELO.md** (13.7 KB)
**¿Cómo se estructuran los datos?**

- 10 Entidades principales con esquema SQL completo
  - Usuario, Empleado, Asistencia, Período Nómina
  - Nómina, Solicitud Nómina, Gasto, Chat
  - Empresa/Cliente, Rol-Permisos
- Flujos API (Login, Guardar Asistencia, Pre-Cálculo Nómina, Chat Socket.io)
- Estructura local (LocalStorage en web, SharedPreferences en mobile)
- Relaciones entre entidades
- Enumeraciones (Estatus, Roles)
- Índices de BD (optimización)
- Validaciones de datos

**Léelo primero si:** Necesitas entender CÓMO se almacenan los datos

---

### 3️⃣ **03_DISENO.md** (27.5 KB)
**¿Cómo está implementado técnicamente?**

- Arquitectura general (5 capas)
- Estructura de carpetas: Backend (Express), Frontend (React), Mobile (Kotlin)
- Patrones de diseño implementados
  - Backend: MVC + Repository
  - Frontend: Hooks + Context
  - Mobile: Manager + Coroutines
- Flujo de autenticación paso a paso
- Flujo de Chat con Socket.io
- Seguridad (JWT, Bcrypt, Validaciones)
- Decisiones arquitectónicas justificadas
  - Por qué Express vs Nest.js
  - Por qué Socket.io vs WebSockets puros
  - Por qué JavaScript vs TypeScript (por ahora)
  - Por qué Kotlin en Mobile
- Problemas identificados y soluciones
  - Sin TypeScript → Solución propuesta
  - Sin Tests → Solución propuesta
  - Chat no implementado en Mobile → Solución propuesta
- Mejoras futuras (TypeScript, Tests, Clean Architecture, etc.)
- Checklist de implementación

**Léelo primero si:** Eres developer y necesitas entender la implementación

---

## 🎯 Por Dónde Empezar (según tu rol)

### 👨‍💻 Equipo Mobile (Jorge + López)
```
1. Lee: 01_DEFINICION.md (20 min)
   → Entiende: qué es SIMTEC, módulos mobile
   
2. Lee: 02_MODELO.md - Secciones 1-4 (30 min)
   → Entiende: Entidades principales, DTOs
   
3. Lee: 03_DISENO.md - Secciones 4 + 6 + 8-9 (40 min)
   → Entiende: Estructura mobile, flujos, decisiones

TOTAL: 1.5 horas
```

### 👨‍💻 Equipo Web (Olivares + Sánchez)
```
1. Lee: 01_DEFINICION.md (20 min)
   → Entiende: qué es SIMTEC, módulos web
   
2. Lee: 02_MODELO.md - Completo (45 min)
   → Entiende: Todas las entidades, DTOs, APIs
   
3. Lee: 03_DISENO.md - Secciones 2-3 + 5-11 (60 min)
   → Entiende: Backend, Frontend, flujos, decisiones

TOTAL: 2 horas
```

### 🎯 Product Manager / Stakeholder
```
1. Lee: 01_DEFINICION.md (30 min)
   → Entiende: alcance, requisitos, cronograma
   
2. Lee: 02_MODELO.md - Sección 1 (20 min)
   → Entiende: entidades principales
   
3. Revisa: 03_DISENO.md - Secciones 1-2 (15 min)
   → Entiende: arquitectura visual

TOTAL: 1 hora
```

---

## 📖 Cómo Usar Esta Documentación

### Caso 1: "¿Cómo funciona el Chat en Mobile?"
1. Abre **01_DEFINICION.md** → Busca "Chat" → Sec 3.1
2. Abre **02_MODELO.md** → Sección 1.8 (entidad Chat)
3. Abre **03_DISENO.md** → Sección 7 (flujo Socket.io)
4. Ve la implementación en Sección 10 (ChatManager y ChatActivity)

### Caso 2: "¿Cuál es la estructura del endpoint /nomina/pre-calculo?"
1. Abre **02_MODELO.md** → Sección 2.3
2. Abre **03_DISENO.md** → Sección 5.1 (MVC pattern)

### Caso 3: "Necesito entender la autenticación completa"
1. Abre **03_DISENO.md** → Sección 6 (diagrama flujo)
2. Abre **03_DISENO.md** → Sección 8.1 (implementación)

### Caso 4: "¿Por qué usamos Express en lugar de Nest.js?"
1. Abre **03_DISENO.md** → Sección 9 (decisiones justificadas)

---

## 🔑 Puntos Clave

### Módulos Implementados
✅ Autenticación (JWT 8h)  
✅ Asistencia (web + mobile con detección facial)  
✅ Nómina (pre-cálculo, cierre paso a paso)  
✅ Solicitudes (de nómina)  
✅ Gastos (solicitud y validación)  
✅ Reportes (Excel, PDF, gráficos)  
✅ Chat (Socket.io - Backend + Mobile con supervisores/RH)
✅ ATS (reclutamiento)  
✅ Configuración (roles, catálogos)  

### Stack Técnico
- **Backend:** Express 5.2.1, Node.js, MySQL2, Socket.io
- **Frontend:** React 19.2.3, React Router, Leaflet, Recharts
- **Mobile:** Kotlin, Android (API 26+), ML Kit, CameraX, OkHttp

### Equipo Responsable
- **Mobile:** Jorge Elías Gutiérrez Santos, López Placencia Axel Marco Antonio
- **Web:** Olivares Castro Héctor Adolfo, Sánchez Aguilar José Isaac

---

## 🚨 Problemas Conocidos y Soluciones

| Problema | Solución | Ubicación |
|----------|----------|-----------|
| Sin TypeScript | Migrar a TS + generar DTOs | 03_DISENO.md Sec 10 |
| Sin Tests | Implementar Jest + Testing Lib | 03_DISENO.md Sec 10 |

---

## 📋 Checklist Antes de Empezar

- [ ] Todos leyeron 01_DEFINICION.md
- [ ] Cada equipo leyó su sección en 03_DISENO.md
- [ ] Se comprende la autenticación JWT
- [ ] Se entiende el flujo de Chat con Socket.io
- [ ] Se saben qué son las 10 entidades principales
- [ ] Se comprende por qué cada decisión arquitectónica

---

## 🔗 Enlaces Rápidos

### Por Módulo
- **Autenticación:** 01_DEFINICION.md Sec 3.1 → 02_MODELO.md Sec 1.1 → 03_DISENO.md Sec 6
- **Asistencia:** 01_DEFINICION.md Sec 3.1 → 02_MODELO.md Sec 1.3 → 03_DISENO.md Sec 5.2
- **Nómina:** 01_DEFINICION.md Sec 3.1 → 02_MODELO.md Sec 1.4-5 → 03_DISENO.md Sec 5.1
- **Chat:** 01_DEFINICION.md Sec 3.1 → 02_MODELO.md Sec 1.8 → 03_DISENO.md Sec 7

### Por Tecnología
- **Express Backend:** 03_DISENO.md Sección 2.1
- **React Frontend:** 03_DISENO.md Sección 3.1
- **Android Mobile:** 03_DISENO.md Sección 4.1
- **Socket.io Chat:** 03_DISENO.md Sección 7
- **Seguridad JWT:** 03_DISENO.md Sección 8

---

## 💡 Recomendaciones Importantes

### Ahora (MVP)
1. Sigue los patrones descritos en 03_DISENO.md
2. Usa las DTOs definidas en 02_MODELO.md
3. Implementa las validaciones en Sección 7 (02_MODELO.md)
4. Sigue el checklist de implementación (03_DISENO.md Sec 12)

### Próximas Semanas
1. **TypeScript:** Migra React frontend (Sección 10, 03_DISENO.md)
2. **Tests:** Implementa Jest + Testing Library (Sección 10, 03_DISENO.md)
3. **Optimizaciones Chat:** Mejorar UX y performance en mobile

### Después del MVP
1. **Clean Architecture:** Refactor a DDD/Clean
2. **Swagger API:** Documenta endpoints automáticamente
3. **Micro-servicios:** Si escala mucho

---

## ❓ Preguntas Frecuentes

**P: ¿Dónde están los endpoints del API?**
A: Definidos en 01_DEFINICION.md Sección 3.1 (tabla de rutas)

**P: ¿Cómo se hace login?**
A: Ver 02_MODELO.md Sección 2.1 (DTO) y 03_DISENO.md Sección 6 (flujo)

**P: ¿Por qué no TypeScript?**
A: MVP rápido. Recomendado después. Ver 03_DISENO.md Sección 9

**P: ¿Cómo se comunican web y mobile?**
A: Mismo API REST + Socket.io. Ver 02_MODELO.md Sección 2

**P: ¿Chat está implementado?**
A: Sí, en backend y mobile. Ver 03_DISENO.md Secciones 7 (flujo) y 10 (implementación)

**P: ¿Cómo valido datos?**
A: Ver 02_MODELO.md Sección 7 (tabla de validaciones)

---

## 📞 Contacto

- **Problemas con documentación:** Jorge (Mobile Lead)
- **Backend:** Equipo Web
- **Frontend:** Equipo Web
- **Mobile:** Equipo Mobile

---

## 📈 Historial de Versiones

| Versión | Fecha      | Cambios                                                                                                                     |
|---------|------------|-----------------------------------------------------------------------------------------------------------------------------|
| 1.0     | Marzo 2026 | Primera versión estable de la app SIMTEC Mobile.                                                                            |
| 2.0     | Marzo 2026 | Nueva funcionalidad: toma de asistencias y uso de cámara.                                                                   |
| 3.0     | Abril 2026 | Login funcional con API, Registro de ubicacion y deteccion facial.                                                          |
| 4.0     | Abril 2026 | Usuarios, apartado descarga de nomina, chat con supervisores o RH, control de ajustes, control de pagos, monitor en vivo, . |

---

**Documentación Definitiva v4.0**  
**SIMTEC - Equipo de 4 personas**  
**Abril 2026**
