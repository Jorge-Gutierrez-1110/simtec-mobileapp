# 🏗️ SIMTEC - Diseño e Implementación

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Cobertura:** Backend (Express), Frontend (React), Mobile (Kotlin)

---

## 1. Arquitectura General

### 1.1 Arquitectura de Alto Nivel

```
┌─────────────────────────────────────────────────┐
│         PRESENTACIÓN (UI)                        │
│  ┌────────────┬──────────────┬────────────────┐ │
│  │  Web       │   Mobile     │   Admin        │ │
│  │  (React)   │   (Android)  │   (React)      │ │
│  └────────────┴──────────────┴────────────────┘ │
└──────────────────┬──────────────────────────────┘
                   │ HTTP + Socket.io
┌──────────────────▼──────────────────────────────┐
│         CAPA DE API (Backend)                    │
│  ┌────────────────────────────────────────────┐ │
│  │  Express Server                            │ │
│  │  - Routes: auth, empleados, asistencia... │ │
│  │  - Middleware: JWT, permisos, validación  │ │
│  │  - Socket.io: chat en tiempo real         │ │
│  └────────────────────────────────────────────┘ │
└──────────────────┬──────────────────────────────┘
                   │ SQL
┌──────────────────▼──────────────────────────────┐
│         CAPA DE DATOS (Persistencia)             │
│  ┌────────────────────────────────────────────┐ │
│  │  MySQL Database                            │ │
│  │  - 10+ tablas normalizadas                 │ │
│  │  - Índices para performance                │ │
│  └────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

---

## 2. Estructura Backend (Express)

### 2.1 Carpetas y Archivos

```
backend/
├── src/
│   ├── routes/                    # API endpoints
│   │   ├── auth.routes.js         # Login, recuperación
│   │   ├── empleados.routes.js    # CRUD empleados
│   │   ├── asistencia.routes.js   # Registros asistencia
│   │   ├── nomina.routes.js       # Nómina y cálculos
│   │   ├── gastos.routes.js       # Gastos y facturas
│   │   ├── reportes.routes.js     # Reportes Excel/PDF
│   │   ├── chat.routes.js         # Chat Socket.io
│   │   ├── documentos.routes.js   # Documentos legales
│   │   ├── catalogos.routes.js    # Datos dinámicos
│   │   ├── config.routes.js       # Configuración
│   │   ├── ats.routes.js          # Reclutamiento
│   │   └── sistema.routes.js      # Configuración sistema
│   │
│   ├── controllers/               # Lógica de negocio
│   │   ├── authController.js
│   │   ├── empleadosController.js
│   │   ├── asistenciaController.js
│   │   ├── nominaController.js
│   │   ├── gastosController.js
│   │   ├── reportesController.js
│   │   └── ...
│   │
│   ├── middleware/                # Middlewares
│   │   ├── auth.js                # Validación JWT
│   │   ├── permisos.js            # Validación permisos
│   │   ├── validacion.js          # Validación datos
│   │   └── errorHandler.js        # Manejo de errores
│   │
│   ├── models/                    # Modelos/Queries
│   │   ├── Usuario.js
│   │   ├── Empleado.js
│   │   ├── Asistencia.js
│   │   ├── Nomina.js
│   │   ├── Gasto.js
│   │   ├── Chat.js
│   │   └── ...
│   │
│   ├── utils/                     # Funciones auxiliares
│   │   ├── jwt.js                 # Generar/validar JWT
│   │   ├── bcrypt.js              # Hash passwords
│   │   ├── reportGenerator.js     # Generar reportes
│   │   ├── emailService.js        # Envío emails
│   │   ├── uploadHandler.js       # Manejo archivos
│   │   └── dateUtils.js           # Utilidades fecha
│   │
│   ├── socket/                    # Socket.io
│   │   ├── chat.socket.js         # Eventos chat
│   │   ├── notifications.socket.js
│   │   └── handlers.js
│   │
│   ├── database/                  # Conexión BD
│   │   ├── connection.js
│   │   └── migrations.js
│   │
│   └── app.js                     # Express setup
│
├── server.js                      # Entry point
├── package.json
└── .env                           # Variables de entorno
```

---

## 3. Estructura Frontend (React)

### 3.1 Carpetas y Archivos

```
frontend/
├── src/
│   ├── features/                  # Módulos por feature
│   │   ├── auth/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── RecoverPage.jsx
│   │   │   ├── useAuth.js         # Custom hook
│   │   │   └── auth.styles.js
│   │   │
│   │   ├── dashboard/
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── widgets/
│   │   │   └── charts/
│   │   │
│   │   ├── empleados/
│   │   │   ├── DirectoryPage.jsx
│   │   │   ├── ProfilePage.jsx
│   │   │   ├── ExpedientePage.jsx
│   │   │   └── components/
│   │   │
│   │   ├── asistencia/
│   │   │   ├── AsistenciaPage.jsx
│   │   │   ├── HistoryPage.jsx
│   │   │   └── components/
│   │   │
│   │   ├── nomina/
│   │   │   ├── NominaPage.jsx
│   │   │   ├── PreCalculoPage.jsx
│   │   │   ├── CierrePage.jsx
│   │   │   └── components/
│   │   │
│   │   ├── gastos/
│   │   │   ├── GastosPage.jsx
│   │   │   ├── SolicitudPage.jsx
│   │   │   └── components/
│   │   │
│   │   ├── reportes/
│   │   │   ├── ReportesPage.jsx
│   │   │   ├── GeneratorPage.jsx
│   │   │   └── components/
│   │   │
│   │   ├── chat/                  ✅ IMPLEMENTADO
│   │   │   ├── ChatPage.jsx
│   │   │   ├── ConversationList.jsx
│   │   │   ├── ChatBox.jsx
│   │   │   ├── useChat.js         # Socket.io hook
│   │   │   └── chat.styles.js
│   │   │
│   │   ├── reclutamiento/
│   │   │   ├── ATSPage.jsx
│   │   │   ├── CandidatePage.jsx
│   │   │   └── components/
│   │   │
│   │   └── configuracion/
│   │       ├── SettingsPage.jsx
│   │       ├── RolesPage.jsx
│   │       └── CatalogsPage.jsx
│   │
│   ├── shared/                    # Componentes compartidos
│   │   ├── components/
│   │   │   ├── Navbar.jsx
│   │   │   ├── Sidebar.jsx
│   │   │   ├── Button.jsx
│   │   │   ├── Modal.jsx
│   │   │   ├── Table.jsx
│   │   │   ├── Form.jsx
│   │   │   ├── Card.jsx
│   │   │   └── LoadingSpinner.jsx
│   │   │
│   │   ├── context/
│   │   │   ├── AuthContext.jsx    # Gestión de sesión
│   │   │   ├── PermisosContext.jsx
│   │   │   └── NotificationContext.jsx
│   │   │
│   │   ├── hooks/
│   │   │   ├── useAuth.js         # Hook autenticación
│   │   │   ├── useApi.js          # Hook para requests
│   │   │   ├── usePermisos.js     # Hook permisos
│   │   │   ├── useSocket.js       # Hook Socket.io
│   │   │   └── useLocalStorage.js
│   │   │
│   │   ├── services/
│   │   │   ├── api.js             # Configuración fetch
│   │   │   ├── auth.service.js
│   │   │   ├── empleados.service.js
│   │   │   └── ...
│   │   │
│   │   ├── layout/
│   │   │   ├── MainLayout.jsx
│   │   │   └── AuthLayout.jsx
│   │   │
│   │   ├── styles/
│   │   │   ├── theme.js           # Tema centralizado
│   │   │   ├── global.css
│   │   │   └── variables.css
│   │   │
│   │   └── utils/
│   │       ├── formatters.js      # Formateo datos
│   │       ├── validators.js      # Validaciones
│   │       ├── constants.js
│   │       └── helpers.js
│   │
│   ├── App.jsx                    # Root component
│   ├── App.routes.jsx             # Routing config
│   └── index.jsx                  # Entry point
│
├── public/
├── package.json
└── .env
```

---

## 4. Estructura Mobile (Android/Kotlin)

### 4.1 Carpetas y Archivos

```
mobile/
├── app/src/main/java/com/example/simtec_mobileapp/
│   ├── activities/                # Pantallas
│   │   ├── LoginActivity.kt       # Autenticación
│   │   ├── HomeActivity.kt        # Pantalla principal
│   │   ├── CameraActivity.kt      # Detección facial
│   │   ├── MapActivity.kt         # Geofencing
│   │   ├── PerfilActivity.kt      # Perfil empleado
│   │   ├── NominaActivity.kt      # Consulta nómina
│   │   ├── SolicitudesActivity.kt # Solicitudes
│   │   ├── GastosActivity.kt      # Gastos
│   │   ├── ChatActivity.kt        # Chat con supervisores/RH ✅
│   │   └── HistoryActivity.kt     # Historial asistencia
│   │
│   ├── managers/                  # Lógica de negocio
│   │   ├── SessionManager.kt      # Sesión y datos locales
│   │   ├── ApiClient.kt           # Requests HTTP
│   │   ├── ChatManager.kt         # Socket.io chat ✅
│   │   ├── FaceDetectionManager.kt # ML Kit
│   │   ├── LocationManager.kt     # GPS
│   │   └── LoadingHelper.kt       # Diálogos
│   │
│   ├── models/                    # Data classes
│   │   ├── User.kt
│   │   ├── Empleado.kt
│   │   ├── Asistencia.kt
│   │   ├── Nomina.kt
│   │   ├── Mensaje.kt             # Chat messages ✅
│   │   └── ...
│   │
│   ├── adapters/                  # RecyclerView
│   │   ├── HistoryAdapter.kt
│   │   ├── NominaAdapter.kt
│   │   └── ...
│   │
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── res/
│   ├── layout/
│   │   ├── activity_login.xml
│   │   ├── activity_home.xml
│   │   ├── activity_camera.xml
│   │   └── ...
│   │
│   ├── drawable/
│   ├── values/
│   └── menu/
│
└── build.gradle.kts
```

---

## 5. Patrones de Diseño Implementados

### 5.1 Backend: Patrón MVC + Repository

```javascript
// Route (entrada)
app.post('/nomina/pre-calculo', autenticar, (req, res) => {
    nominaController.preCalcular(req, res);
});

// Controller (orquestación)
class NominaController {
    async preCalcular(req, res) {
        const { periodo_id } = req.body;
        const empleados = await nominaRepository.getEmpleados(periodo_id);
        const calculos = empleados.map(e => this.calcularNomina(e));
        return res.json({ success: true, data: calculos });
    }
}

// Repository (acceso datos)
class NominaRepository {
    async getEmpleados(periodoId) {
        const query = `
            SELECT e.* FROM empleados e
            WHERE e.cliente_id = ? AND e.status = 'activo'
        `;
        return db.query(query, [periodoId]);
    }
}
```

---

### 5.2 Frontend: Patrón Hooks + Context

```jsx
// AuthContext proporciona autenticación global
const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));
    
    const login = async (email, password) => {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        const data = await response.json();
        setToken(data.token);
        setUser(data.user);
        localStorage.setItem('token', data.token);
    };
    
    return (
        <AuthContext.Provider value={{ user, token, login }}>
            {children}
        </AuthContext.Provider>
    );
}

// Custom hook para usar autenticación
export function useAuth() {
    return useContext(AuthContext);
}

// Uso en componente
function LoginPage() {
    const { login } = useAuth();
    
    const handleSubmit = async (email, password) => {
        await login(email, password);
    };
}
```

---

### 5.3 Mobile: Patrón Manager + Coroutines

```kotlin
// SessionManager centraliza persistencia
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("simtec", Context.MODE_PRIVATE)
    
    fun saveLoginWithToken(token: String, user: User) {
        prefs.edit().apply {
            putString("token", token)
            putString("user_name", user.nombre)
            putInt("empleado_id", user.empleado_id)
            putLong("token_expiration", System.currentTimeMillis() + 8*60*60*1000)
            apply()
        }
    }
    
    fun getToken(): String? = prefs.getString("token", null)
    fun isLogged(): Boolean = getToken() != null
}

// Activity usa SessionManager + Coroutines
class LoginActivity : AppCompatActivity() {
    private val sessionManager by lazy { SessionManager(this) }
    private val apiClient = ApiClient()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    fun performLogin(email: String, password: String) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiClient.login(email, password)
                }
                
                if (response.success) {
                    sessionManager.saveLoginWithToken(response.token, response.user)
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

---

## 6. Flujo de Autenticación

```
┌─────────────────────────────────────────────────────┐
│  Web (React) / Mobile (Android)                     │
│                                                     │
│  User ingresa email/password                        │
│  └─→ onClick: handleLogin()                         │
└──────────────────┬──────────────────────────────────┘
                   │ fetch/HTTP POST
┌──────────────────▼──────────────────────────────────┐
│  Backend Express                                    │
│                                                     │
│  POST /auth/login                                   │
│  ├─ Valida email existe                            │
│  ├─ Compara password con bcrypt                    │
│  ├─ Genera JWT: sign({ id, rol, permisos })       │
│  └─ Retorna token + user data                      │
└──────────────────┬──────────────────────────────────┘
                   │ JSON response
┌──────────────────▼──────────────────────────────────┐
│  Web/Mobile                                         │
│                                                     │
│  Recibe token                                       │
│  ├─ localStorage.setItem('token', token) [Web]     │
│  ├─ SharedPreferences.putString('token', token) [M]│
│  ├─ Actualiza AuthContext/SessionManager           │
│  └─ Navega a HomeActivity/DashboardPage            │
└──────────────────────────────────────────────────────┘

En requests subsecuentes:
Request → Authorization: Bearer ${token} →  Backend valida JWT
```

---

## 7. Flujo de Chat (Socket.io)

```
Web (React)               Backend (Express)         Mobile (Future)
    │                           │                         │
    │ socket.connect()          │                         │
    ├──────────────────────────→│                         │
    │                      socket.on('connect')           │
    │                           │                         │
    │ socket.emit('usuario_conectado')                    │
    ├──────────────────────────→│                         │
    │                      Guarda usuario online          │
    │                           │                         │
    │                                                     │
    │ socket.emit('nuevo_mensaje', {msg})                │
    ├──────────────────────────→│                         │
    │                      Guarda en BD MySQL             │
    │                           │                         │
    │                      socket.broadcast()             │
    │←──────────────────────────┤                         │
    │ socket.on('mensaje_recibido')                       │
    │                                                     │
    │                                    [Future: connect]│
    │                           │←─────────────────────────┤
    │                      Envía mensajes a mobile        │
    │                                                     │
    │ socket.disconnect()       │                         │
    ├──────────────────────────→│                         │
    │                      Marca usuario offline          │
```

---

## 8. Seguridad

### 8.1 Autenticación (Backend)

```javascript
// Middleware JWT
function verificarToken(req, res, next) {
    const token = req.headers.authorization?.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: 'Token requerido' });
    }
    
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;  // { id, rol, permisos }
        next();
    } catch (error) {
        return res.status(401).json({ error: 'Token inválido o expirado' });
    }
}

// Middleware de permisos
function requierePermiso(permiso) {
    return (req, res, next) => {
        if (!req.user.permisos.includes(permiso)) {
            return res.status(403).json({ error: 'Permiso denegado' });
        }
        next();
    };
}

// Uso en route
app.post('/nomina/cerrar', 
    verificarToken, 
    requierePermiso('cerrar_nomina'),
    nominaController.cerrar
);
```

### 8.2 Hashing de Passwords (Backend)

```javascript
const bcrypt = require('bcrypt');

// Al registrar usuario
async function registrarUsuario(email, password) {
    const hashedPassword = await bcrypt.hash(password, 10);
    await db.query(
        'INSERT INTO usuarios (email, password_hash) VALUES (?, ?)',
        [email, hashedPassword]
    );
}

// Al hacer login
async function login(email, password) {
    const user = await db.query('SELECT * FROM usuarios WHERE email = ?', [email]);
    
    const passwordValido = await bcrypt.compare(password, user.password_hash);
    if (!passwordValido) {
        throw new Error('Contraseña incorrecta');
    }
    
    return generarToken(user);
}
```

### 8.3 Validaciones (Frontend y Backend)

```javascript
// Backend: validar entrada
const { email, password } = req.body;

if (!email || !email.includes('@')) {
    return res.status(400).json({ error: 'Email inválido' });
}

if (!password || password.length < 8) {
    return res.status(400).json({ error: 'Password mínimo 8 caracteres' });
}

// Frontend: validar antes de enviar
function validarFormulario(email, password) {
    if (!email.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/)) {
        setError('Email inválido');
        return false;
    }
    
    if (password.length < 8) {
        setError('Contraseña muy corta');
        return false;
    }
    
    return true;
}
```

---

## 9. Decisiones de Arquitectura Justificadas

### ¿Por qué Express en lugar de Nest.js?

| Aspecto | Express | Nest.js |
|--------|---------|---------|
| Curva aprendizaje | Baja | Alta |
| Boilerplate | Mínimo | Mucho |
| Flexibilidad | Alta | Media |
| Para equipo pequeño | ✓ | ✗ |
| **Decisión** | ✅ Elegido | |

**Justificación:** Con solo 2 web developers, Express es más rápido de desarrollar.

---

### ¿Por qué Socket.io en lugar de WebSockets puros?

| Aspecto | Socket.io | WebSockets |
|--------|-----------|-----------|
| Fallback (IE, proxies) | Automático | Manual |
| Reconexión | Automática | Manual |
| Eventos nombrados | Sí | No (binary) |
| Cross-platform | React + móvil | Complejo |
| **Decisión** | ✅ Elegido | |

**Justificación:** Socket.io es más robusto para web + mobile futuro.

---

### ¿Por qué React sin TypeScript?

| Aspecto | Vanilla JS | TypeScript |
|--------|-----------|-----------|
| Setup | Rápido | Lento |
| Curva aprendizaje | Baja | Alta |
| Type safety | No | Sí |
| Para MVP | ✓ | ✗ |
| **Decisión** | ✅ Ahora | ⏳ Futuro |

**Justificación:** Para MVP es más rápido. **RECOMENDACIÓN:** Migrar a TypeScript después con DTOs formales.

---

### ¿Por qué Kotlin en Mobile?

| Aspecto | Java | Kotlin |
|--------|------|--------|
| Sintaxis | Verbosa | Concisa |
| Nullability | Propensa a crashes | Type-safe |
| Coroutines | Callbacks (RxJava) | Nativas |
| Modernidad | Deprecada | Estándar oficial |
| **Decisión** | | ✅ Kotlin |

**Justificación:** Kotlin es estándar oficial de Android, más seguro y moderno.

---

## 10. Problemas Identificados y Soluciones

### ❌ Problema 1: Sin TypeScript en Frontend

**Impacto:** 
- No hay type-checking en tiempo de compilación
- Difícil colaboración entre web y mobile (DTOs inconsistentes)
- Errores se descubren en runtime

**Solución (Recomendada):**
```
1. Instalar TypeScript: npm install typescript
2. Crear interfaz para cada DTO:
   interface LoginResponse {
       success: boolean;
       token: string;
       user: User;
   }
3. Generar tipos automáticamente desde OpenAPI/Swagger
4. Usar types en todos los services y components
```

---

### ❌ Problema 2: Sin Tests

**Impacto:**
- Cambios rompen funcionalidad sin notarse
- Refactoring arriesgado
- Deuda técnica

**Solución (Recomendada):**
```javascript
// Backend: Jest + Supertest
test('POST /auth/login con credenciales válidas', async () => {
    const res = await request(app)
        .post('/auth/login')
        .send({ email: 'test@test.com', password: 'test1234' });
    
    expect(res.status).toBe(200);
    expect(res.body.token).toBeDefined();
});

// Frontend: React Testing Library
test('LoginPage submit envia credenciales', () => {
    const { getByRole } = render(<LoginPage />);
    fireEvent.change(getByRole('textbox', { name: /email/i }), {
        target: { value: 'test@test.com' }
    });
    fireEvent.click(getByRole('button', { name: /login/i }));
    // Assertions
});

// Mobile: Kotest + Mockito
@Test
fun loginSuccess() {
    val apiClient = mock(ApiClient::class.java)
    val response = LoginResponse(true, "token123", user)
    whenever(apiClient.login(email, password)).thenReturn(response)
    
    val result = apiClient.login(email, password)
    
    assertTrue(result.success)
    assertEquals("token123", result.token)
}
```

---

### ✅ Chat Implementado en Mobile

**Situación:** Socket.io en backend está implementado y mobile ya lo consume en v4.0

**Implementación:**
```kotlin
// Mobile: Socket.io-client para comunicación real-time
dependencies {
    implementation("io.socket:socket.io-client:4.5.4")
}

// Manager para Socket.io
class ChatManager(private val token: String) {
    private val socket = IO.socket("https://api.simtec.com", options)
    
    fun conectar() {
        socket.on(Socket.EVENT_CONNECT) {
            socket.emit("usuario_conectado", JSONObject().apply {
                put("usuarioId", sessionManager.getUserId())
            })
        }
    }
    
    fun enviarMensaje(conversacionId: Int, contenido: String) {
        socket.emit("nuevo_mensaje", JSONObject().apply {
            put("conversacionId", conversacionId)
            put("contenido", contenido)
        })
    }
    
    fun escucharMensajes(callback: (mensaje: Mensaje) -> Unit) {
        socket.on("mensaje_recibido") { args ->
            val mensaje = Gson().fromJson(args[0].toString(), Mensaje::class.java)
            callback(mensaje)
        }
    }
}

// Activity para chat con supervisores/RH
class ChatActivity : AppCompatActivity() {
    private val chatManager by lazy { ChatManager(sessionManager.getToken()!!) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatManager.conectar()
        chatManager.escucharMensajes { mensaje ->
            actualizarUI(mensaje)
        }
    }
}
```

---

## 11. Mejoras Futuras Recomendadas

| Mejora | Prioridad | Esfuerzo | Beneficio |
|--------|-----------|----------|-----------|
| TypeScript en Frontend | Alta | Medio | Previene bugs 🐛 |
| Tests (Jest, Testing Lib) | Alta | Medio | Confianza en cambios ✓ |
| Clean Architecture | Media | Alto | Mantenibilidad 🏗️ |
| GraphQL | Baja | Alto | Eficiencia queries |
| Micro-servicios | Baja | Muy Alto | Escalabilidad 📈 |
| CI/CD Pipeline | Media | Bajo | Despliegues automáticos 🚀 |

---

## 12. Checklist de Implementación

- [ ] Backend: JWT validando en cada endpoint
- [ ] Backend: Chat implementado con Socket.io
- [ ] Frontend: AuthContext configurado
- [ ] Frontend: Chat visual funcionando
- [ ] Mobile: SessionManager persistiendo datos
- [ ] Mobile: Autenticación funcionando
- [ ] Mobile: Detección facial implementada
- [ ] Mobile: Geofencing validando
- [ ] Todos: Errores manejados y logueados
- [ ] Todos: Variables de entorno en .env
- [ ] Todos: Documentación actualizada

---

**Documento Definitivo v1.0**  
**Abril 2026**  
**Equipo SIMTEC (4 personas)**
