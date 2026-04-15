# 📊 SIMTEC - Modelo de Datos

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Cobertura:** Backend (MySQL), Frontend (React), Mobile (Kotlin)

---

## 1. Entidades Principales

### 1.1 Usuario
```sql
CREATE TABLE usuarios (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,      -- bcrypt
    nombre VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
    empleado_id INT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rol_id) REFERENCES roles(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);
```

**DTO JSON (respuesta login):**
```json
{
    "id": 1,
    "nombre": "Juan Pérez",
    "email": "juan@empresa.com",
    "rol_id": 2,
    "rol": "supervisor",
    "empleado_id": 5,
    "permisos": ["registrar_asistencia", "ver_nómina"],
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 28800
}
```

---

### 1.2 Empleado
```sql
CREATE TABLE empleados (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT,
    numero_empleado VARCHAR(50) UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    apellido_paterno VARCHAR(255),
    apellido_materno VARCHAR(255),
    email VARCHAR(255),
    telefono VARCHAR(20),
    
    -- Documentos
    rfc VARCHAR(20),
    curp VARCHAR(30),
    nss VARCHAR(20),
    
    -- Dirección
    calle VARCHAR(255),
    numero_exterior VARCHAR(20),
    colonia VARCHAR(255),
    cp VARCHAR(10),
    ciudad VARCHAR(100),
    estado VARCHAR(100),
    
    -- Laboral
    puesto VARCHAR(100),
    departamento VARCHAR(100),
    turno_id INT,
    hora_entrada TIME,
    hora_salida TIME,
    cliente_id INT,          -- Empresa asignada
    manager_id INT,          -- Gerente/supervisor
    
    -- Nómina
    tipo_nomina VARCHAR(50),
    salario_diario DECIMAL(10,2),
    salario_diario_integrado DECIMAL(10,2),
    periodo_pago VARCHAR(50),
    fecha_ingreso DATE,
    status VARCHAR(50) DEFAULT 'activo',
    
    -- Banco
    banco VARCHAR(100),
    cuenta_bancaria VARCHAR(50),
    clabe VARCHAR(50),
    
    -- Biometría
    imagen_perfil LONGBLOB,
    
    -- Geofencing
    geocerca_activa BOOLEAN DEFAULT false,
    geocerca_latitud DECIMAL(11,8),
    geocerca_longitud DECIMAL(11,8),
    geocerca_radio_metros INT DEFAULT 1000,
    
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (turno_id) REFERENCES turnos(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (manager_id) REFERENCES empleados(id)
);
```

---

### 1.3 Asistencia
```sql
CREATE TABLE asistencias (
    id INT PRIMARY KEY AUTO_INCREMENT,
    empleado_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora_entrada TIME,
    hora_salida TIME,
    estatus VARCHAR(50),              -- PUNTUAL, TARDE, FALTA, etc.
    dispositivo VARCHAR(20),          -- 'web', 'mobile', 'biometric'
    ubicacion VARCHAR(500),           -- Texto o coordenadas
    foto LONGBLOB,                    -- Base64 en mobile
    horas_extras INT DEFAULT 0,
    comentarios TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    UNIQUE KEY unique_empleado_fecha (empleado_id, fecha)
);
```

**DTO JSON:**
```json
{
    "id": 123,
    "empleado_id": 5,
    "fecha": "2026-04-07",
    "hora_entrada": "08:00",
    "hora_salida": "18:00",
    "estatus": "PUNTUAL",
    "dispositivo": "mobile",
    "ubicacion": "Calle Principal 123, Montevideo",
    "horas_extras": 0,
    "foto": "iVBORw0KGgoAAAANSUhEUg..."
}
```

---

### 1.4 Período de Nómina
```sql
CREATE TABLE periodos_nomina (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100),              -- "Período 1-15 Abril"
    fecha_inicio DATE,
    fecha_fin DATE,
    numero_periodo INT,               -- 1-24
    anio INT,
    status_cierre VARCHAR(50) DEFAULT 'abierto',  -- 'abierto', 'cerrado'
    fecha_cierre DATETIME,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE KEY unique_periodo (numero_periodo, anio)
);
```

---

### 1.5 Nómina
```sql
CREATE TABLE nominas (
    id INT PRIMARY KEY AUTO_INCREMENT,
    empleado_id INT NOT NULL,
    periodo_id INT NOT NULL,
    
    -- Cálculos
    dias_trabajados INT,
    horas_trabajadas DECIMAL(10,2),
    horas_extras INT,
    
    -- Montos
    salario_base DECIMAL(10,2),
    percepciones DECIMAL(10,2),       -- Ingresos adicionales
    descuentos DECIMAL(10,2),         -- Deducciones
    retenciones DECIMAL(10,2),        -- Impuestos
    neto_a_pagar DECIMAL(10,2),
    
    -- Estado
    status VARCHAR(50),               -- 'pendiente', 'procesada', 'pagada'
    fecha_pago DATETIME,
    banco VARCHAR(100),
    comprobante_url VARCHAR(500),
    
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    FOREIGN KEY (periodo_id) REFERENCES periodos_nomina(id)
);
```

---

### 1.6 Solicitud de Nómina
```sql
CREATE TABLE solicitudes_nomina (
    id INT PRIMARY KEY AUTO_INCREMENT,
    empleado_id INT NOT NULL,
    solicitante_id INT NOT NULL,
    aprobador_id INT,
    
    periodo_id INT,
    concepto_id INT NOT NULL,
    tipo VARCHAR(50),                 -- 'anticipo', 'ajuste', 'liquidacion'
    monto DECIMAL(10,2),
    
    estatus VARCHAR(50) DEFAULT 'PENDIENTE',  -- PENDIENTE, APROBADO, RECHAZADO
    comentarios TEXT,
    evidencia_url VARCHAR(500),
    
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    FOREIGN KEY (solicitante_id) REFERENCES usuarios(id),
    FOREIGN KEY (aprobador_id) REFERENCES usuarios(id),
    FOREIGN KEY (periodo_id) REFERENCES periodos_nomina(id)
);
```

---

### 1.7 Gasto
```sql
CREATE TABLE gastos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    folio VARCHAR(50) UNIQUE,
    
    -- Actores
    solicitante_id INT NOT NULL,
    beneficiario_id INT,
    proveedor_id INT,
    categoria_id INT,
    cliente_id INT,
    
    -- Concepto
    concepto VARCHAR(500),
    
    -- Montos
    subtotal DECIMAL(10,2),
    iva DECIMAL(10,2),                -- 16%
    retenciones DECIMAL(10,2),
    total DECIMAL(10,2),
    
    -- Fechas
    fecha_emision DATE,
    fecha_vencimiento DATE,
    fecha_pago DATE,
    
    -- Estado
    estatus VARCHAR(50),              -- PENDIENTE, APROBADO, PAGADO, RECHAZADO
    
    -- Documentos
    factura_url VARCHAR(500),
    pago_url VARCHAR(500),
    
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (solicitante_id) REFERENCES usuarios(id),
    FOREIGN KEY (beneficiario_id) REFERENCES empleados(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (categoria_id) REFERENCES categorias_gasto(id)
);
```

---

### 1.8 Chat (Mensajes)
```sql
CREATE TABLE conversaciones (
    id INT PRIMARY KEY AUTO_INCREMENT,
    participante1_id INT NOT NULL,
    participante2_id INT NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (participante1_id) REFERENCES usuarios(id),
    FOREIGN KEY (participante2_id) REFERENCES usuarios(id),
    UNIQUE KEY unique_conv (participante1_id, participante2_id)
);

CREATE TABLE mensajes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    conversacion_id INT NOT NULL,
    remitente_id INT NOT NULL,
    contenido TEXT,
    leido BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id),
    FOREIGN KEY (remitente_id) REFERENCES usuarios(id)
);
```

**DTO JSON (Socket.io):**
```json
{
    "conversacionId": 1,
    "remitenteId": 5,
    "remitenteNombre": "Juan Pérez",
    "contenido": "Hola, ¿cómo estás?",
    "timestamp": "2026-04-07T10:30:00Z",
    "leido": true
}
```

---

### 1.9 Empresa/Cliente
```sql
CREATE TABLE clientes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    razon_social VARCHAR(255) UNIQUE,
    nombre_comercial VARCHAR(255),
    rfc VARCHAR(20),
    telefono VARCHAR(20),
    
    -- Dirección
    calle VARCHAR(255),
    numero VARCHAR(20),
    ciudad VARCHAR(100),
    estado VARCHAR(100),
    cp VARCHAR(10),
    
    -- Geofencing
    geocerca_activa BOOLEAN DEFAULT false,
    geocerca_latitud DECIMAL(11,8),
    geocerca_longitud DECIMAL(11,8),
    geocerca_radio_metros INT DEFAULT 1000,
    
    status VARCHAR(50) DEFAULT 'activo',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

### 1.10 Rol y Permisos
```sql
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) UNIQUE,
    descripcion TEXT
);

CREATE TABLE permisos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) UNIQUE,
    descripcion TEXT
);

CREATE TABLE rol_permisos (
    rol_id INT,
    permiso_id INT,
    FOREIGN KEY (rol_id) REFERENCES roles(id),
    FOREIGN KEY (permiso_id) REFERENCES permisos(id),
    PRIMARY KEY (rol_id, permiso_id)
);
```

---

## 2. Flujos de Datos API

### 2.1 Login
```
Request POST /auth/login:
{
    "email": "juan@empresa.com",
    "password": "micontraseña"
}

Response 200 OK:
{
    "success": true,
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
        "id": 1,
        "nombre": "Juan Pérez",
        "email": "juan@empresa.com",
        "rol": "supervisor",
        "empleado_id": 5,
        "permisos": ["ver_asistencia", "aprobar_solicitudes"]
    },
    "expiresIn": 28800
}
```

### 2.2 Guardar Asistencia (Mobile)
```
Request POST /asistencia/registrar:
{
    "empleado_id": 5,
    "fecha": "2026-04-07",
    "hora_entrada": "08:00",
    "dispositivo": "mobile",
    "ubicacion": "-34.9011, -56.1645",
    "foto": "iVBORw0KGgoAAAANS..."
}

Response 200 OK:
{
    "success": true,
    "message": "Asistencia registrada",
    "calculo": {
        "estatus": "PUNTUAL",
        "extras": 0
    }
}
```

### 2.3 Pre-Cálculo Nómina
```
Request POST /nomina/pre-calculo:
{
    "periodo_id": 3
}

Response 200 OK:
{
    "success": true,
    "data": [
        {
            "empleado_id": 5,
            "nombre": "Juan Pérez",
            "dias_trabajados": 15,
            "salario_base": 5000.00,
            "percepciones": 200.00,
            "descuentos": 150.00,
            "retenciones": 750.00,
            "neto": 4300.00
        }
    ]
}
```

### 2.4 Chat - Socket.io
```javascript
// Cliente conecta
socket.on('connect', () => {
    socket.emit('usuario_conectado', { usuarioId: 1 });
});

// Enviar mensaje
socket.emit('nuevo_mensaje', {
    conversacionId: 1,
    contenido: "Hola"
});

// Recibir mensaje
socket.on('mensaje_recibido', (data) => {
    // { id, remitenteId, contenido, timestamp }
});

// Desconectar
socket.on('disconnect', () => {
    // Usuario desconectado
});
```

---

## 3. Estructura Local (Web y Mobile)

### 3.1 LocalStorage (Web/React)
```javascript
// Sesión
localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIs...');
localStorage.setItem('user', JSON.stringify({
    id: 1,
    nombre: "Juan",
    rol: "supervisor"
}));
localStorage.setItem('expiresAt', '1617456000000');

// Cache de datos
localStorage.setItem('periodos', JSON.stringify([...]));
localStorage.setItem('empleados', JSON.stringify([...]));
```

### 3.2 SharedPreferences (Mobile/Kotlin)
```kotlin
// Sesión
prefs.putString("token", "eyJhbGciOiJIUzI1NiIs...")
prefs.putString("user_name", "Juan Pérez")
prefs.putInt("empleado_id", 5)
prefs.putLong("token_expiration", System.currentTimeMillis() + 8*60*60*1000)

// Asistencia
prefs.putString("last_record_type", "Entrada")
prefs.putString("attendance_records_json", "...")

// Geofencing
prefs.putFloat("geocerca_latitud", -34.9011f)
prefs.putFloat("geocerca_longitud", -56.1645f)
prefs.putInt("geocerca_radio_metros", 1000)
```

---

## 4. Relaciones Principales

```
Usuario (1) ──→ (N) Asistencia
Usuario (1) ──→ (N) Solicitud Nómina
Usuario (1) ──→ (N) Conversación

Empleado (1) ──→ (N) Asistencia
Empleado (1) ──→ (N) Nómina
Empleado (1) ──→ (N) Gasto (como solicitante/beneficiario)
Empleado (N) ──→ (1) Empresa

Período (1) ──→ (N) Nómina
Período (1) ──→ (N) Solicitud Nómina

Rol (1) ──→ (N) Usuario
Rol (1) ──→ (N) Permiso (many-to-many)

Conversación (2) ──→ (N) Mensaje
```

---

## 5. Enumeraciones

### Estatus Asistencia
```
PUNTUAL          // Llegó a tiempo
TARDE            // Llegó después de hora
FALTA            // No registró entrada
SALIDA_TEMPRANA  // Salió antes de hora
AUSENCIA_JUST    // Justificado
```

### Estatus Nómina
```
PENDIENTE        // Aún en cálculo
PROCESADA        // Pre-cierre completado
PAGADA           // Pagada
CANCELADA        // Cancelada
```

### Estatus Gasto/Solicitud
```
PENDIENTE        // Esperando aprobación
APROBADO         // Aprobado
RECHAZADO        // Rechazado
PAGADO           // Pagado (gastos)
EN_REVISION      // En revisión
```

### Roles
```
EMPLEADO         // Acceso básico
SUPERVISOR       // Acceso a nómina, asistencias
ADMINISTRADOR    // Acceso total
DIRECTIVO        // Reportes y análisis
RECLUTADOR       // ATS
```

---

## 6. Índices de BD (Optimización)

```sql
CREATE INDEX idx_asistencia_empleado_fecha 
ON asistencias(empleado_id, fecha);

CREATE INDEX idx_nomina_periodo 
ON nominas(periodo_id, status);

CREATE INDEX idx_mensaje_conversacion_fecha 
ON mensajes(conversacion_id, created_at);

CREATE INDEX idx_usuario_email 
ON usuarios(email);

CREATE INDEX idx_empleado_usuario 
ON empleados(usuario_id);
```

---

## 7. Validaciones de Datos

| Campo | Validación | Ejemplo |
|-------|-----------|---------|
| Email | Formato válido, único | usuario@empresa.com |
| Password | Mín 8 caracteres, hash bcrypt | bcrypt hash |
| RFC | Formato válido | ABC123456XYZ |
| Fecha | YYYY-MM-DD | 2026-04-07 |
| Monto | Número positivo, 2 decimales | 1250.50 |
| Hora | HH:mm válido | 08:30 |
| Teléfono | 10 dígitos (MX) | 5551234567 |
| Coordenadas | Lat -90 a 90, Lng -180 a 180 | -34.9011, -56.1645 |

---

**Documento Definitivo v1.0**  
**Abril 2026**
