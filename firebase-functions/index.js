const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { XMLParser } = require("fast-xml-parser");

initializeApp();

// ID de base de datos Firestore provisionada (cambiar a "(default)" si usas otro proyecto)
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-cristore-7bc79cf9-f11c-4662-977e-16ba6ae3ded4";

const db = getFirestore(DATABASE_ID);
const messaging = getMessaging();

const YOUTUBE_CHANNEL_ID = "UCfg1whOZLgGrtneGLJGCHqQ";
const YOUTUBE_LIVE_URL = `https://www.youtube.com/channel/${YOUTUBE_CHANNEL_ID}/live`;
const YOUTUBE_RSS_URL = `https://www.youtube.com/feeds/videos.xml?channel_id=${YOUTUBE_CHANNEL_ID}`;

/**
 * Envía una notificación push por lotes (máx. 500 tokens por envío)
 * a los dispositivos que tengan habilitado el campo de preferencia indicado.
 */
async function sendPushToSubscribedTokens(preferenceField, title, body, targetTab) {
  const snapshot = await db
    .collection("tokens")
    .where(preferenceField, "==", true)
    .get();

  const tokens = [];
  snapshot.forEach((doc) => {
    const fcmToken = doc.get("fcmToken");
    if (typeof fcmToken === "string" && fcmToken.trim().length > 0) {
      tokens.push(fcmToken.trim());
    }
  });

  if (tokens.length === 0) {
    console.log(`No hay tokens FCM activos para ${preferenceField}.`);
    return;
  }

  for (let i = 0; i < tokens.length; i += 500) {
    const chunk = tokens.slice(i, i + 500);
    const response = await messaging.sendEachForMulticast({
      tokens: chunk,
      notification: { title, body },
      data: { tab: targetTab, title, body },
      android: {
        priority: "high",
        notification: { channelId: "cristo_rey_updates" },
      },
    });
    console.log(
      `Push [${preferenceField}] enviado: ${response.successCount} exitosos, ${response.failureCount} fallidos.`
    );
  }
}

/**
 * 1) Detección automática del Live SIN YouTube Data API v3 (cada 3 minutos).
 */
exports.checkLiveStatus = onSchedule("every 3 minutes", async () => {
  const response = await fetch(YOUTUBE_LIVE_URL, {
    headers: {
      "User-Agent":
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36",
      "Accept-Language": "es-ES,es;q=0.9",
    },
  });

  if (!response.ok) {
    throw new Error(`Error al consultar YouTube Live: HTTP ${response.status}`);
  }

  const html = await response.text();
  const isLiveNow =
    html.includes('"isLive":true') || html.includes('"isLiveNow":true');

  const statusRef = db.collection("app_status").doc("live");
  const prevSnap = await statusRef.get();
  const wasLiveBefore = prevSnap.exists ? Boolean(prevSnap.get("liveActive")) : false;

  await statusRef.set(
    {
      liveActive: isLiveNow,
      title: isLiveNow
        ? "Transmisión en Vivo — Cristo Rey"
        : "Próxima Transmisión — Cristo Rey",
      link: `https://www.youtube.com/@cccristorey/live`,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  // Enviar notificación solo en la transición false -> true para no duplicar
  if (isLiveNow && !wasLiveBefore) {
    await sendPushToSubscribedTokens(
      "liveEnabled",
      "Cristo Rey en Vivo",
      "🔴 ¡Estamos EN VIVO! Únete a la transmisión de Cristo Rey.",
      "live"
    );
  }
});

/**
 * 2) Detección automática de nuevos videos vía Feed RSS público (cada 15 minutos).
 */
exports.checkNewYouTubeVideo = onSchedule("every 15 minutes", async () => {
  const response = await fetch(YOUTUBE_RSS_URL, {
    headers: { "User-Agent": "CristoReyFunctions/1.0" },
  });

  if (!response.ok) {
    throw new Error(`Error al consultar YouTube RSS: HTTP ${response.status}`);
  }

  const xml = await response.text();
  const parser = new XMLParser({ ignoreAttributes: false });
  const parsed = parser.parse(xml);
  const entries = parsed?.feed?.entry;
  const latest = Array.isArray(entries) ? entries[0] : entries;

  if (!latest || !latest["yt:videoId"]) {
    console.log("No se encontraron videos en el feed RSS.");
    return;
  }

  const videoId = String(latest["yt:videoId"]);
  const title = String(latest.title || "Nuevo mensaje de Cristo Rey");
  const link = `https://www.youtube.com/watch?v=${videoId}`;

  const latestRef = db.collection("app_status").doc("latest_video");
  const prevSnap = await latestRef.get();
  const previousVideoId = prevSnap.exists ? prevSnap.get("videoId") : null;

  if (previousVideoId !== videoId) {
    await latestRef.set({
      videoId,
      title,
      link,
      thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      updatedAt: FieldValue.serverTimestamp(),
    });

    // Evitar notificar en la primera inicialización vacía
    if (previousVideoId) {
      await sendPushToSubscribedTokens(
        "videosEnabled",
        "Nuevo video en Cristo Rey",
        `🎥 Nuevo video publicado: ${title}`,
        "videos"
      );
    }
  }
});

/**
 * 3) Webhook para agregador de Instagram (RSS.app / Make / Zapier).
 * Protege este endpoint con un secreto en cabecera x-webhook-secret.
 */
exports.instagramWebhook = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const expectedSecret = process.env.INSTAGRAM_WEBHOOK_SECRET;
  if (expectedSecret && req.get("x-webhook-secret") !== expectedSecret) {
    res.status(401).send("Unauthorized");
    return;
  }

  const { postId, caption, imageUrl, postUrl, publishedAt } = req.body || {};
  if (!postUrl || !imageUrl) {
    res.status(400).send("Faltan campos requeridos: imageUrl, postUrl");
    return;
  }

  const docId = (postId || `ig_${Date.now()}`).replace(/[^a-zA-Z0-9_-]/g, "_");
  const safeCaption = String(caption || "Nueva publicación de @cccristoreyve").slice(0, 2000);

  await db.collection("social_posts").doc(docId).set({
    caption: safeCaption,
    imageUrl: String(imageUrl),
    postUrl: String(postUrl),
    published: true,
    publishedAt: String(publishedAt || new Date().toISOString().slice(0, 10)),
    createdAt: FieldValue.serverTimestamp(),
  });

  await db.collection("app_status").doc("latest_social").set({
    title: safeCaption.slice(0, 120),
    link: String(postUrl),
    thumbnailUrl: String(imageUrl),
    updatedAt: FieldValue.serverTimestamp(),
  });

  await sendPushToSubscribedTokens(
    "socialEnabled",
    "Cristo Rey en Instagram",
    "📢 Nueva publicación en nuestras redes. ¡Échale un vistazo!",
    "social"
  );

  res.status(200).json({ ok: true, docId });
});
