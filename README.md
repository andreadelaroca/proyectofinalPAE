# BunBun Hub — Sistema de Gestión Comercial e Inventario

Sistema de gestión integral desarrollado en **Java 21**, **JavaFX** y **Hibernate ORM** sobre **PostgreSQL** (`ni.edu.uam.proyectofinalpae`), diseñado para optimizar la operación del emprendimiento **BunBun Stitches**.

El sistema centraliza la administración de clientes, colaboradores, catálogo de productos, pedidos con geolocalización y ventas directas con facturación y reportes en PDF, automatizando el control de inventario y facilitando el análisis de demanda y rentabilidad del negocio.

---

## Contexto y Objetivos del Proyecto

Desarrollado como proyecto académico en la **Universidad Americana (UAM)** para el emprendimiento **BunBun Stitches**, **BunBun Hub** resuelve la gestión manual de órdenes e ingresos mediante los siguientes objetivos:

* **Gestión Centralizada de Pedidos:** Registrar, consultar y actualizar el estado de los pedidos en tiempo real, mejorando la coordinación entre colaboradores y clientes.
* **Control de Inventario y Ventas:** Automatizar el descuento y reposición de existencias, registrando ingresos, impuestos y facturación de forma precisa.
* **Análisis de Demanda y Reportes:** Conservar el historial detallado de pedidos y ventas para generar informes financieros y detectar patrones de demanda que respalden decisiones sobre precios y promociones.

---

## Características Principales

* **Autenticación Segura y Verificación por Correo (SMTP):**
  * Almacenamiento seguro de contraseñas mediante **BCrypt** (factor de costo 12).
  * Registro de cuentas con código numérico de verificación de 6 dígitos enviado por correo electrónico (validez de 15 minutos) y opción de reenvío.
  * Control de acceso por rol: redirige automáticamente a los **Colaboradores** al panel administrativo (`main-admin.fxml`) y a los **Clientes** a su vista principal (`cliente_home.fxml`).
* **Gestión de Clientes y Geolocalización (OpenStreetMap):**
  * Registro, edición y búsqueda en tiempo real de clientes.
  * Selección y visualización interactiva de direcciones mediante coordenadas (`latitud` y `longitud`) integrando **OpenStreetMap** y **Leaflet.js** dentro de un `WebView` de JavaFX.
* **Catálogo e Inventario de Productos:**
  * Administración de productos y marcas con generación automática de códigos correlativos (`BUN-001`, `BUN-002`, etc.).
  * Selector de color hexadecimal con vista previa visual en tabla, gestión de stock y ruta de imagen.
  * Control de concurrencia en inventario mediante bloqueo pesimista en base de datos (`LockMode.PESSIMISTIC_WRITE`) para evitar sobreventa.
* **Gestión de Pedidos:**
  * Creación de pedidos con búsqueda rápida por código `BUN-`, verificación de disponibilidad en tiempo real y descuento automático de existencias.
  * Seguimiento por estados (`PENDIENTE`, `EN_PROCESO`, `LISTO`, `ENTREGADO`, `CANCELADO`), filtro para ocultar pedidos finalizados y devolución automática de stock al cancelar.
* **Ventas Directas y Facturación PDF:**
  * Registro de ventas a clientes registrados o *Consumidor final* con métodos de pago (`EFECTIVO`, `TARJETA`, `TRANSFERENCIA`), cálculo automático de subtotal, **IVA (15%)** y total, y numeración correlativa de factura (`F-000001`).
  * Conversión de pedidos a ventas sin duplicar el descuento de inventario y anulación de ventas con restitución de stock.
* **Reportes Dinámicos con JasperReports:**
  * Exportación a PDF de **facturas individuales de venta** y **reportes de pedidos por rango de fechas**, compilados programáticamente en tiempo de ejecución sin depender de plantillas `.jrxml` externas.
* **Interfaz Fluida y Asíncrona:**
  * Ejecución de consultas y transacciones en segundo plano (`Task` / hilos daemon) con paleta visual personalizada (`global.css` y `login.css`).

---

## Stack Tecnológico

| Categoría | Tecnología / Librería | Versión |
| :--- | :--- | :--- |
| **Lenguaje y Plataforma** | Java (JPMS / `module-info.java`) | 21 |
| **Gestor de Construcción** | Apache Maven (`javafx-maven-plugin`) | 3.x / 0.0.8 |
| **Interfaz Gráfica** | JavaFX (`controls`, `fxml`, `web`) | 21.0.6 / 21.0.11 |
| **ORM y Persistencia** | Hibernate ORM Core + Jakarta Persistence API | 6.6.59.Final / 3.1.0 |
| **Base de Datos** | PostgreSQL JDBC Driver | 42.7.13 |
| **Reportes PDF** | JasperReports (`core`, `pdf`, `jdt`, `fonts`) | 7.0.8 |
| **Seguridad** | `at.favre.lib:bcrypt` | 0.10.2 |
| **Correo Electrónico** | Eclipse Angus / Jakarta Mail | 2.0.3 |
| **Mapas** | OpenStreetMap + Leaflet.js | 1.9.4 |
| **Utilidades y Pruebas** | Project Lombok, Google Gson, JUnit Jupiter | 1.18.46 / 2.10.1 / 5.12.1 |

---

## Arquitectura del Proyecto

El proyecto sigue una arquitectura en capas modularizada bajo `ni.edu.uam.proyectofinalpae`:

```text
proyectofinalPAE/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── ni/edu/uam/proyectofinalpae/
    │       ├── application/   # MainApplication (JavaFX) y BootstrapColaborador (carga inicial)
    │       ├── config/        # HibernateUtil (SessionFactory y variables de entorno de BD)
    │       ├── controller/    # Controladores FXML (Login, Admin, Clientes, Colaboradores, Productos, Pedidos, Ventas)
    │       ├── map/           # MapaDireccionDialog (WebView con Leaflet y OpenStreetMap)
    │       ├── model/         # Entidades JPA (herencia JOINED en Usuario) y enums de negocio
    │       ├── report/        # ReporteService (diseño y exportación PDF con JasperReports)
    │       ├── repository/    # GenericRepository<T, ID> (CRUD genérico sobre Hibernate)
    │       ├── service/       # Lógica de negocio, reglas de stock, correo SMTP y autenticación
    │       └── util/          # SceneManager, Tx (transacciones), Async, PasswordUtil, NegocioException
    └── resources/
        ├── hibernate.properties
        └── ni/edu/uam/proyectofinalpae/
            ├── fxml/          # Vistas y modales FXML
            └── styles/        # Hojas de estilo CSS (global.css, login.css)
```

---

## Requisitos Previos

* **JDK 21** instalado y configurado en `JAVA_HOME`.
* **Apache Maven 3.8+** instalado.
* **PostgreSQL** en ejecución (por defecto espera una base de datos llamada `proyectofinalpae`).
* Credenciales de un servidor **SMTP** (por ejemplo, Gmail con contraseña de aplicación, Mailtrap o servidor institucional) para el envío de códigos de verificación.

---

## Configuración

### 1. Base de Datos (`hibernate.properties` o Variables de Entorno)

Por defecto, el archivo `src/main/resources/hibernate.properties` está configurado para conectarse a PostgreSQL local con generación/actualización automática de tablas (`hibernate.hbm2ddl.auto=update`):

```properties
hibernate.connection.driver_class=org.postgresql.Driver
hibernate.connection.url=jdbc:postgresql://localhost:5432/proyectofinalpae
hibernate.connection.username=postgres
hibernate.connection.password=bd1234
hibernate.hbm2ddl.auto=update
```

Si deseas sobrescribir estos valores sin modificar el archivo, puedes definir las siguientes variables de entorno:

| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/proyectofinalpae` |
| `DB_USER` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `bd1234` |

### 2. Servidor de Correo SMTP (Obligatorio para Registro)

El servicio `CorreoService` lee las siguientes variables de entorno o propiedades del sistema para enviar los códigos de verificación:

| Variable | Obligatoria | Descripción | Valor por defecto |
| :--- | :--- | :--- | :--- |
| `SMTP_HOST` | Sí | Servidor SMTP (ej. `smtp.gmail.com`) | — |
| `SMTP_USER` | Sí | Usuario / cuenta de correo autenticada | — |
| `SMTP_PASSWORD` | Sí | Contraseña o *App Password* del correo | — |
| `SMTP_FROM` | No | Dirección del remitente | Valor de `SMTP_USER` |
| `SMTP_PORT` | No | Puerto del servidor SMTP | `587` |
| `SMTP_SSL` | No | Habilitar SSL directo (`true` / `false`, usa STARTTLS si es `false`) | `false` |

### 3. Mapas OpenStreetMap (Opcional)

| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `OSM_CONTACT` | Identificador de contacto enviado en el `User-Agent` hacia OpenStreetMap | `proyecto-academico` |
| `OSM_TILE_URL` | Plantilla URL del servidor de teselas (*tiles*) | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` |

---

## Instalación y Ejecución

1. **Clonar el repositorio y crear la base de datos:**
   ```bash
   git clone https://github.com/andreadelaroca/proyectofinalPAE.git
   cd proyectofinalPAE
   ```
   Crea la base de datos en PostgreSQL:
   ```sql
   CREATE DATABASE proyectofinalpae;
   ```

2. **Compilar el proyecto con Maven:**
   ```bash
   mvn clean compile
   ```

3. **Crear el primer Colaborador / Administrador (Bootstrap):**
   Dado que el registro público en la interfaz crea cuentas de tipo `Cliente`, utiliza la clase `BootstrapColaborador` para registrar el primer usuario administrativo. Define estas variables de entorno antes de ejecutarla:
   * `BOOTSTRAP_USER`
   * `BOOTSTRAP_NOMBRES`
   * `BOOTSTRAP_APELLIDOS`
   * `BOOTSTRAP_EMAIL`
   * `BOOTSTRAP_TELEFONO`
   * `BOOTSTRAP_DIRECCION`
   * `BOOTSTRAP_PASSWORD`

   Ejecuta `ni.edu.uam.proyectofinalpae.application.BootstrapColaborador` desde tu IDE o mediante Maven, y revisa el correo configurado para obtener el código de confirmación SMTP.

4. **Ejecutar la aplicación principal:**
   ```bash
   mvn javafx:run
   ```
   En la pantalla de inicio de sesión (`BunBun Hub - Ingreso`), ingresa tu usuario y el código recibido por correo para activar la cuenta y acceder al sistema.

---

## Equipo de Trabajo y Créditos

Proyecto desarrollado en la **Universidad Americana (UAM)** — Managua, Nicaragua, para el emprendimiento **BunBun Stitches**.

| Integrante | Rol en el Proyecto |
| :--- | :--- |
| **Sara Alejandra Zambrana Taylor** | Jefe de Proyecto |
| **Andrea Sofía de la Roca Delgado** | Diseñadora de Interfaz |
| **Denis Gabriel Molina Arauz** | Analista de Sistemas |
| **Arelys Brigitte Obando Castillo** | Desarrolladora |
| **Kenneth Saul Blandon Larios** | Desarrollador |