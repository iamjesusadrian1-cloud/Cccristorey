# Cristo Rey — Aplicación Android (Kotlin + Jetpack Compose + Firebase)

Aplicación móvil oficial para **Cristo Rey** diseñada con una estética espiritual, moderna y acogedora (paleta azul profundo, dorado y blanco), tipografía clara para todas las edades (**Playfair Display** + **Plus Jakarta Sans**) e integración directa con YouTube e Instagram sin necesidad de API Keys de YouTube Data API v3 ni tokens de Meta en su primera versión.

---

## 1. Identificadores y Canales Configurados

Los datos oficiales están centralizados en `app/src/main/java/com/example/data/CristoReyConfig.kt`:

- **Canal de YouTube**: `@cccristorey` (`Channel ID`: `UCfg1whOZLgGrtneGLJGCHqQ`)
- **Feed RSS de YouTube**: `https://www.youtube.com/feeds/videos.xml?channel_id=UCfg1whOZLgGrtneGLJGCHqQ`
- **Detección Live (sin API Key)**: `https://www.youtube.com/channel/UCfg1whOZLgGrtneGLJGCHqQ/live`
- **Reproductor iFrame Live**: `https://www.youtube.com/embed/live_stream?channel=UCfg1whOZLgGrtneGLJGCHqQ`
- **Enlace directo Live**: `https://www.youtube.com/@cccristorey/live`
- **Instagram**: `@cccristoreyve` (`https://www.instagram.com/cccristoreyve`)

---

## 2. Estructura de la Aplicación

La navegación inferior adaptativa (con `NavigationRail` automático en tablets/pantallas anchas) incluye 5 pestañas:

1. **Inicio**: Banner de bienvenida con el logotipo oficial de Cristo Rey, botón principal **"Ver en Vivo"**, tarjeta de **Último Video** y **Publicación Destacada**.
2. **En Vivo**: Insignia visual 🔴 **EN VIVO** / ⚪ **Próximamente**, reproductor embebido iFrame oficial, verificación instantánea y botón secundario **"Ver en YouTube"**.
3. **Videos**: Lista de los últimos ~15 videos parseados del Feed RSS XML público de YouTube, buscador por título y reproductor embebido iFrame al tocar cualquier mensaje.
4. **Redes**: Feed de `@cccristoreyve` compatible con agregadores gratuitos (RSS.app, EmbedSocial, SnapWidget) + visor embebido oficial de Instagram y enlace directo.
5. **Info**: Horarios de cultos, dirección, contacto, **Ajustes de Notificaciones** (Live / Videos / Redes sincronizados en Firestore `tokens/{uid}`), gestión de cuenta y diálogo **"Acerca de Cristo Rey"**.

---

## 3. Configuración de Firebase y FCM

### Paso 1: Configurar el proyecto en Firebase Console
1. Abre [Firebase Console](https://console.firebase.google.com/) y selecciona el proyecto vinculado (`app/google-services.json`).
2. Verifica que estén activos:
   - **Authentication** -> Proveedor **Google** habilitado.
   - **Cloud Firestore** -> Reglas de seguridad desplegadas desde `/firestore.rules`.
   - **Cloud Messaging (FCM)** -> Habilitado por defecto en el proyecto para enviar y recibir notificaciones push.

### Paso 2: Colecciones en Firestore
- `tokens/{userId}`: Almacena `userId`, `fcmToken`, `platform`, `liveEnabled`, `videosEnabled`, `socialEnabled`, `createdAt`, `updatedAt`.
- `app_status/{statusId}`: Documentos administrados por Cloud Functions (`live`, `latest_video`, `latest_social`).
- `social_posts/{postId}`: Publicaciones sincronizadas desde el webhook de Instagram (`published: true`).

Para desplegar las reglas manualmente desde la raíz del proyecto:
```bash
firebase deploy --only firestore:rules
```

---

## 4. Despliegue de Cloud Functions y Cron Jobs (Cloud Scheduler)

El código de backend listo para desplegar se encuentra en la carpeta `/firebase-functions/`:

- `checkLiveStatus`: Se ejecuta cada **3 minutos** mediante Cloud Scheduler, consulta `https://www.youtube.com/channel/UCfg1whOZLgGrtneGLJGCHqQ/live`, busca `"isLive":true` y envía push FCM únicamente cuando el canal pasa de offline a vivo (evitando duplicados con `app_status/live`).
- `checkNewYouTubeVideo`: Se ejecuta cada **15 minutos**, lee el Feed RSS XML de YouTube y envía push FCM cuando aparece un `videoId` nuevo.
- `instagramWebhook`: Endpoint HTTPS (`POST`) para recibir webhooks de RSS.app, Zapier o Make cuando hay nueva publicación en `@cccristoreyve`.

### Pasos para desplegar:
1. Asegúrate de tener el plan **Blaze (Pago por uso)** activo en Firebase (necesario para Cloud Scheduler y llamadas salientes desde Cloud Functions; incluye cuota gratuita mensual generosa).
2. Instala Firebase CLI e inicia sesión:
   ```bash
   npm install -g firebase-tools
   firebase login
   ```
3. Instala las dependencias de las funciones:
   ```bash
   cd firebase-functions
   npm install
   ```
4. Configura el secreto del webhook de Instagram (opcional pero recomendado):
   ```bash
   firebase functions:secrets:set INSTAGRAM_WEBHOOK_SECRET
   ```
5. Despliega las funciones:
   ```bash
   firebase deploy --only functions
   ```
6. Cloud Scheduler creará automáticamente los trabajos programados `checkLiveStatus` (cada 3 min) y `checkNewYouTubeVideo` (cada 15 min).

---

## 5. Configuración de Redes Sociales y Migración Futura a Instagram Graph API

### Opción A (Actual sin tokens de Meta):
- **RSS.app**: Crea un feed RSS gratuito para `https://www.instagram.com/cccristoreyve`.
  - Pega la URL XML en `CristoReyConfig.SOCIAL_AGGREGATOR_RSS_URL` para mostrar tarjetas nativas automáticamente.
  - Opcionalmente configura el webhook de RSS.app apuntando a la URL de tu función `instagramWebhook` para disparar notificaciones push automáticas.
- **SnapWidget / EmbedSocial**: Pega la URL del widget en `CristoReyConfig.SOCIAL_WIDGET_EMBED_URL`.

### Opción B (Migración futura a Instagram Graph API):
En `app/src/main/java/com/example/data/social/SocialFeedRepository.kt` ya existe la interfaz `SocialFeedDataSource` y la clase preparada `InstagramGraphApiDataSource`:
1. Obtén un **Instagram User ID** y un **Access Token** de larga duración desde Meta for Developers.
2. Agrega las variables en `.env` (o en una Cloud Function proxy para mayor seguridad).
3. Implementa la llamada `GET https://graph.facebook.com/v20.0/{ig-user-id}/media?fields=id,caption,media_url,permalink,timestamp` en `InstagramGraphApiDataSource` e inyéctala en `CristoReyViewModel`.

---

## 6. Íconos y Logo Oficial

La imagen oficial proporcionada para **Cristo Rey** ya fue procesada e instalada en todas las densidades Android:
- **Ícono adaptativo**: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml` (foreground `ic_launcher_foreground.xml` con `ic_cristo_rey_mark.png` sobre fondo `#FFFFFF`).
- **Densidades raster**: `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi` (`ic_launcher.png` e `ic_launcher_round.png`).
- **Splash Screen e Inicio**: `app/src/main/res/drawable/cristo_rey_logo.png`.

Si deseas regenerar los íconos desde una nueva imagen fuente usando ImageMagick:
```bash
convert nueva_imagen.png -resize 512x512 PNG32:app/src/main/res/drawable/cristo_rey_logo.png
```

---

## 7. Compilación de APK / AAB y Publicación en Google Play Store

1. **Generar APK de depuración / pruebas**:
   ```bash
   gradle :app:assembleDebug
   ```
2. **Generar App Bundle firmado (.aab) para Google Play Store**:
   - Configura las variables de entorno `KEYSTORE_PATH`, `STORE_PASSWORD` y `KEY_PASSWORD` para tu llave de firma `upload` (sin subir la llave al repositorio).
   - Ejecuta:
     ```bash
     gradle :app:bundleRelease
     ```
   - El archivo final se generará en `app/build/outputs/bundle/release/app-release.aab`.
3. **Subida a Google Play Console**:
   - Sube `app-release.aab` en *Producción* o *Prueba interna*.
   - Agrega el certificado **SHA-1 / SHA-256 de firma de Google Play App Signing** en **Firebase Console -> Configuración del proyecto -> Tus apps Android** para que Google Sign-In funcione en la versión publicada en Play Store.
