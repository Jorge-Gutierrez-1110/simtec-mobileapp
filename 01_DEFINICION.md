# 📋 SIMTEC - Definición del Proyecto

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Estado:** Completo  
**Equipo:** 4 personas (2 Mobile, 2 Web)

---

## 1. Identificación del Proyecto

| Aspecto | Valor |
|--------|-------|
| **Nombre** | SIMTEC - Sistema de Gestión Integral de Recursos Humanos |
| **Alcance** | Plataforma web y mobile para RRHH empresarial |
| **Equipo Mobile** | Jorge Elías Gutiérrez Santos, López Placencia Axel Marco Antonio |
| **Equipo Web** | Olivares Castro Héctor Adolfo, Sánchez Aguilar José Isaac |
| **Stack Frontend** | React 19.2.3, React Router DOM 7.11.0, Socket.io, Leaflet |
| **Stack Backend** | Express 5.2.1, Node.js, MySQL2, Socket.io, JWT |
| **Stack Mobile** | Kotlin, Android (API 26+), CameraX, ML Kit, OkHttp3 |

---

## 2. Visión General

SIMTEC es una plataforma empresarial integral que centraliza la gestión de recursos humanos, abarcando:

- **Autenticación y Control de Acceso** (JWT, permisos por rol)
- **Control de Asistencia** (web/mobile con biometría en mobile)
- **Gestión de Nómina** (cálculos, pre-cierre, exportación)
- **Gestión de Solicitudes** (anticipos, ajustes, aprobaciones)
- **Control de Gastos** (solicitud, validación, pago)
- **Mensajería en Tiempo Real** (Chat Socket.io implementado)
- **Reportes y Análisis** (Excel, PDF, gráficos)
- **Gestión de Documentos** (legales, expedientes, generación Word)
- **Sistema ATS** (Applicant Tracking System para reclutamiento)
- **Configuración del Sistema** (catálogos, empresa, permisos)

---

## 3. Alcance del Proyecto

### 3.1 Módulos Implementados

#### Backend (Express)
| Módulo | Ruta | Responsabilidad |
|--------|------|-----------------|
| **Autenticación** | `/auth` | Login, recuperación, gestión usuarios |
| **Empleados** | `/empleados` | CRUD, perfil, expedientes |
| **Asistencia** | `/asistencia` | Registros entrada/salida, turnos |
| **Nómina** | `/nomina` | Cálculos, períodos, cierre |
| **Gastos** | `/gastos` | Solicitud y validación de gastos |
| **Reportes** | `/reportes` | Generación Excel, PDF, gráficos |
| **Chat** | `/chat` | Conversaciones en tiempo real (Socket.io) ✅ IMPLEMENTADO |
| **Documentos** | `/documentos` | Gestión documentos legales |
| **Catálogos** | `/catalogos` | Datos dinámicos (depts, puestos, etc.) |
| **Configuración** | `/config` | Settings del sistema |
| **ATS** | `/ats` | Reclutamiento y selección |
| **Sistema** | `/sistema` | Configuración general |

#### Frontend React
| Módulo | Descripción | Features |
|--------|-----------|----------|
| **Auth** | Autenticación | Login, recuperación contraseña |
| **Dashboard** | Panel principal | Overview del sistema |
| **Empleados** | Gestión empleados | Directorio, expedientes, perfiles |
| **Asistencia** | Control asistencias | Registro, historial, reportes |
| **Nómina** | Gestión nómina | Cálculos, pre-cierre, PDFs |
| **Gastos** | Gestión gastos | Solicitud, validación, seguimiento |
| **Reportes** | Generación reportes | Excel, PDF, gráficos interactivos |
| **Chat** | Mensajería | Conversaciones en tiempo real ✅ IMPLEMENTADO |
| **Configuración** | Ajustes | Roles, permisos, catálogos |
| **Reclutamiento** | ATS | Candidatos, aplicaciones, selección |

#### Mobile (Kotlin/Android)
| Módulo | Descripción | Features |
|--------|-----------|----------|
| **Autenticación** | Login y sesión | Email/password, huella dactilar, JWT |
| **Asistencia** | Registro entrada/salida | Detección facial (ML Kit), GPS, biometría |
| **Perfil** | Datos empleado | Información personal, laboral, bancaria |
| **Nómina** | Consulta nóminas | Historial, detalles, descargas |
| **Solicitudes** | Solicitudes nómina | Consulta, estado, comentarios |
| **Gastos** | Gestión gastos | Solicitud, validación |
| **Mapa** | Geofencing | Ubicación trabajo, validación entrada |
| **Historial** | Historial asistencia | Últimos registros, detalles |

### 3.2 Características Principales

✅ **Autenticación Multi-Factor**
- JWT con expiración 8h
- Login web: email/password
- Login mobile: email/password + huella + detección facial
- Tokens seguros, refrescables

✅ **Control de Asistencia**
- Web: registro manual con timestamp
- Mobile: registro con detección facial (ML Kit), validación GPS, biometría
- Cálculo automático de horas extras
- Validación de geofencing (si está activo)

✅ **Gestión de Nómina**
- Pre-cálculo automático
- Validación de asistencias
- Cierre paso a paso (web)
- Exportación a PDF/Excel
- Envío por email

✅ **Chat en Tiempo Real**
- Socket.io bidireccional
- Mensajería entre empleados
- Notificaciones en tiempo real
- Historial de conversaciones (BD)

✅ **Reportes Avanzados**
- Gráficos interactivos (Recharts)
- Exportación a Excel (XLSX)
- Exportación a PDF (jsPDF)
- Filtros y búsquedas

✅ **Soporte Offline**
- Mobile: almacenamiento local de registros
- Web: caché de datos
- Sincronización al recuperar conexión

---

## 4. Requisitos Funcionales

### RF-001: Autenticación de Usuario
- El sistema debe permitir login con email/password
- Mobile debe soportar huella dactilar (opcional)
- Backend genera JWT válido por 8 horas
- Token debe incluir: id, rol, rol_id, empleado_id, permisos
- Logout debe limpiar sesión local

### RF-002: Registro de Asistencia
- Web: timestamp manual + ubicación opcional
- Mobile: detección facial + GPS + biometría + foto
- Sistema calcula automáticamente horas extras
- Validación de geofencing si está activo
- Almacenamiento local en mobile si no hay conexión

### RF-003: Gestión de Nómina
- Administrador puede iniciar pre-cálculo de período
- Sistema valida asistencias completas
- Permite editar datos antes de cerrar
- Cierre paso a paso con confirmación
- Exportación a PDF/Excel
- Envío por email

### RF-004: Chat en Tiempo Real
- Usuarios pueden iniciar conversaciones
- Mensajes sincronizados en tiempo real (Socket.io)
- Historial persistente en BD
- Notificaciones de nuevos mensajes
- Soporte en web y future mobile

### RF-005: Gestión de Gastos
- Solicitud de gasto con detalles (monto, categoría, proveedor)
- Cálculo automático IVA + retenciones
- Aprobación/rechazo con comentarios
- Seguimiento de pago
- Reportes de gastos

### RF-006: Reportes y Análisis
- Generación de reportes en Excel
- Exportación de datos en PDF
- Gráficos interactivos (asistencia, nómina)
- Filtros por fecha, empleado, departamento
- Descarga de documentos

### RF-007: ATS (Reclutamiento)
- Carga de candidatos
- Seguimiento de aplicaciones
- Entrevistas y evaluaciones
- Estados de candidato
- Reportes de reclutamiento

---

## 5. Requisitos No-Funcionales

### RNF-001: Seguridad
- Encriptación en tránsito (HTTPS)
- Validación de JWT en cada endpoint
- Hash bcrypt para passwords
- Sanitización de inputs
- Validación de permisos por rol

### RNF-002: Rendimiento
- Carga de páginas < 3 segundos
- Detección facial en tiempo real (mobile)
- Chat con latencia < 500ms
- Reportes generados < 5 segundos
- Caché de datos frecuentes

### RNF-003: Disponibilidad
- Uptime > 99% en web
- Sincronización offline en mobile
- Retry automático de fallos de red
- Backup diario de BD MySQL

### RNF-004: Usabilidad
- Interfaz intuitiva en web y mobile
- Responsivo en desktop/tablet/mobile
- Feedback visual en acciones
- Mensajes de error claros
- Accesibilidad WCAG

### RNF-005: Mantenibilidad
- Código documentado
- Estructura modular por features
- Logging centralizado
- Monitoreo de errores
- Testing (en progreso)

---

## 6. Equipo y Responsabilidades

### Equipo Mobile (2 personas)
**Responsables:** Jorge Elías Gutiérrez Santos, López Placencia Axel Marco Antonio

Responsabilidades:
- Desarrollo app Android en Kotlin
- Integración con API backend
- Implementación de:
  - Autenticación (email/password, huella, JWT)
  - Detección facial (ML Kit)
  - Geolocalización (GPS)
  - Biometría
  - Offline mode
  - Chat (future)

### Equipo Web (2 personas)
**Responsables:** Olivares Castro Héctor Adolfo, Sánchez Aguilar José Isaac

Responsabilidades:
- Desarrollo frontend en React 19
- Desarrollo backend en Express
- Implementación de:
  - Autenticación (JWT)
  - Chat en tiempo real (Socket.io)
  - Generación de reportes
  - Gestión de nómina
  - ATS y documentos
  - APIs RESTful

---

## 7. Dependencias Externas

### Backend (Node.js)
- Express 5.2.1
- MySQL2
- Socket.io
- JWT (jsonwebtoken)
- Bcrypt
- Nodemailer
- Swagger
- Multer
- docxtemplater + PizzIP

### Frontend (React)
- React 19.2.3
- React Router DOM 7.11.0
- Leaflet + React Leaflet (mapas)
- Recharts (gráficos)
- Socket.io-client
- jsPDF + jsPDF-autotable
- XLSX (ExcelJS)
- Lucide React + React Icons
- @hello-pangea/dnd (drag & drop)
- Testing Library

### Mobile (Android)
- Kotlin 1.9+
- AndroidX
- CameraX
- ML Kit (face detection)
- OkHttp3
- Gson
- Google Play Services (location)
- OSMDroid (mapas)
- Biometric API

---

## 8. Restricciones Técnicas

### Mobile
- Mínimo API 26 (Android 8.0)
- Target API 36 (Android 15)
- Cámara requerida
- GPS requerida
- Sensor de huella (opcional)

### Web
- Navegadores modernos (Chrome, Firefox, Edge)
- Soporte responsive (mobile/tablet/desktop)
- JavaScript habilitado

### Backend
- Node.js LTS
- MySQL 5.7+
- 15 segundos timeout para operaciones
- Max 10MB por file upload

---

## 9. Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|--------|-----------|
| Falta de TypeScript | Alta | Alto | Agregar tipos formales (DTO pattern) |
| Sin tests implementados | Alta | Alto | Implementar testing framework |
| Chat requiere real-time | Media | Medio | Socket.io estable, fallback HTTP |
| Sincronización offline | Media | Medio | Queue local + retry automático |
| Generación de reportes lenta | Media | Medio | Caché + worker threads |
| Cambios de DB sin migración | Alta | Alto | Script de migración versionado |

---

## 10. Consideraciones de Diseño

### Arquitectura
- Backend: Express con rutas modularizadas por feature
- Frontend: React con estructura de features/shared
- Mobile: Activity-based con coroutines
- Todos: REST + Socket.io para real-time

### Autenticación
- JWT con expiración 8h (refrescable)
- LocalStorage en web, SharedPreferences en mobile
- Validación en cada request
- Logout limpia token

### Persistencia
- Web: LocalStorage (datos sesión), caché API
- Mobile: SharedPreferences (datos sesión), caché local
- Backend: MySQL (BD relacional)

### Comunicación Real-Time
- Socket.io para chat y notificaciones
- Fallback a polling si Socket.io falla
- Reconexión automática

---

## 11. Cronograma Estimado

| Fase | Duración | Deliverables |
|------|----------|--------------|
| **Fase 1** | 2 semanas | Autenticación, asistencia (web/mobile) |
| **Fase 2** | 2 semanas | Nómina, gastos |
| **Fase 3** | 1 semana | Chat, reportes |
| **Fase 4** | 1 semana | ATS, documentos |
| **Fase 5** | 1 semana | Testing, optimización |

---

## 12. Convenciones de Codificación

### Backend (JavaScript/Node.js)
- Camel case: `getEmpleadoData()`
- Arrow functions cuando sea posible
- Const por defecto, let cuando sea necesario
- Async/await en lugar de .then()
- Logging: `console.log()`

### Frontend (React)
- Componentes con PascalCase: `LoginPage.jsx`
- Funciones con camel case
- Hooks para state management
- PropTypes o TypeScript (futuro)
- CSS modules o Tailwind

### Mobile (Kotlin)
- PascalCase para clases: `LoginActivity`
- camelCase para funciones
- Coroutines para async
- LiveData para observables
- Documentación en Kotlin doc

---

## 13. Métricas de Éxito

- ✅ 100% de funcionalidades implementadas
- ✅ 0 bugs críticos en producción
- ✅ Tiempo de respuesta < 2 segundos
- ✅ Uptime > 99%
- ✅ Satisfacción usuario > 4/5 estrellas

---

**Documento Definitivo v1.0**  
**Abril 2026**  
**Equipo SIMTEC - 4 personas (2 Mobile + 2 Web)**
