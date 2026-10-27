import { handleCors, corsHeaders } from "../_shared/cors.ts";
import { createAdminClient } from "../_shared/admin-client.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { isValidQuery } from "../_shared/validation.ts";

// Límite simple en memoria: máx 20 búsquedas por minuto por usuario
const rateLimitMap = new Map<string, { count: number; resetAt: number }>();

function checkRateLimit(userId: string): boolean {
  const now = Date.now();
  const entry = rateLimitMap.get(userId);
  if (!entry || now > entry.resetAt) {
    rateLimitMap.set(userId, { count: 1, resetAt: now + 60_000 });
    return true;
  }
  if (entry.count >= 20) return false;
  entry.count++;
  return true;
}

// Normalizar consulta: minúsculas, sin tildes, espacios colapsados
function normalizar(q: string): string {
  return q
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

// Parsear duración ISO 8601 a segundos
export function parsearDuracion(iso: string): number {
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
  if (!match) return 0;
  const h = parseInt(match[1] ?? "0", 10);
  const m = parseInt(match[2] ?? "0", 10);
  const s = parseInt(match[3] ?? "0", 10);
  return h * 3600 + m * 60 + s;
}

// Heurística de artista
export function extraerArtista(titulo: string, canal: string): string {
  const separatorIdx = titulo.indexOf(" - ");
  if (separatorIdx > 0 && separatorIdx <= 40) {
    return titulo.substring(0, separatorIdx).trim();
  }
  return canal
    .replace(/ - Topic$/i, "")
    .replace(/VEVO$/i, "")
    .replace(/\s*Official\s*$/i, "")
    .replace(/\s*Music\s*$/i, "")
    .trim();
}

// Limpiar título para display
export function limpiarTitulo(titulo: string): string {
  return titulo
    .replace(/\s*[\[(](?:video oficial|official video|official audio|audio oficial|hd|4k|lyric video|letra|lyrics)[\])]?\s*/gi, "")
    .trim();
}

// Verificar región bloqueada
function esReproducible(
  embeddable: boolean,
  privacyStatus: string,
  regionRestriction?: { blocked?: string[]; allowed?: string[] }
): boolean {
  if (!embeddable || privacyStatus !== "public") return false;
  if (regionRestriction?.blocked?.includes("CO")) return false;
  if (regionRestriction?.allowed && !regionRestriction.allowed.includes("CO")) return false;
  return true;
}

Deno.serve(async (req) => {
  const corsResponse = handleCors(req);
  if (corsResponse) return corsResponse;

  if (req.method !== "POST") return errorResponse("Método no permitido", 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return errorResponse("Sin autorización", 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const youtubeKey = Deno.env.get("YOUTUBE_API_KEY");
  if (!youtubeKey) return errorResponse("Configuración incompleta", 500);

  const { createClient } = await import("https://esm.sh/@supabase/supabase-js@2");
  const userClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return errorResponse("Token inválido", 401);

  if (!checkRateLimit(user.id)) {
    return errorResponse("Demasiadas solicitudes", 429, "rate_limit");
  }

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return errorResponse("Body inválido", 400);
  }

  const { q, sesion_id } = body;
  if (!isValidQuery(q)) {
    return errorResponse("Consulta inválida: entre 2 y 100 caracteres", 400, "query_invalida");
  }

  const consulta = (q as string).trim();
  const consultaNormalizada = normalizar(consulta);
  const admin = createAdminClient();
  const inicio = Date.now();

  // Verificar caché
  const { data: cached } = await admin
    .from("cache_busqueda")
    .select("resultado, creado_en")
    .eq("consulta_normalizada", consultaNormalizada)
    .maybeSingle();

  if (cached) {
    const age = Date.now() - new Date(cached.creado_en).getTime();
    if (age < 24 * 60 * 60 * 1000) {
      return jsonResponse({ ...cached.resultado, cache: true });
    }
  }

  // search.list — 100 unidades
  const searchUrl = new URL("https://www.googleapis.com/youtube/v3/search");
  searchUrl.searchParams.set("part", "snippet");
  searchUrl.searchParams.set("type", "video");
  searchUrl.searchParams.set("maxResults", "10");
  searchUrl.searchParams.set("videoEmbeddable", "true");
  searchUrl.searchParams.set("videoCategoryId", "10");
  searchUrl.searchParams.set("regionCode", "CO");
  searchUrl.searchParams.set("relevanceLanguage", "es");
  searchUrl.searchParams.set("safeSearch", "moderate");
  searchUrl.searchParams.set("q", consulta);
  searchUrl.searchParams.set("key", youtubeKey);

  let searchData: Record<string, unknown>;
  try {
    const searchRes = await fetch(searchUrl.toString());
    if (!searchRes.ok) {
      const errBody = await searchRes.json().catch(() => ({}));
      if ((errBody as Record<string, unknown>)?.error?.errors?.[0]?.reason === "quotaExceeded") {
        return errorResponse("Cuota de YouTube agotada", 429, "cuota_agotada");
      }
      return errorResponse("Error de YouTube", 502, "youtube_error");
    }
    searchData = await searchRes.json();
  } catch {
    return errorResponse("No se pudo conectar a YouTube", 502, "youtube_error");
  }

  const items = (searchData.items as unknown[]) ?? [];
  if (items.length === 0) {
    const resultado = { cache: false, total: 0, resultados: [] };
    EdgeRuntime.waitUntil(
      Promise.all([
        admin.from("cache_busqueda").upsert({
          consulta_normalizada: consultaNormalizada,
          resultado,
          creado_en: new Date().toISOString(),
        }),
        admin.from("evento").insert({
          tipo: "error_contenido",
          sesion_id: sesion_id ?? null,
          datos: { tipo: "sin_resultados", texto: consulta },
        }),
      ])
    );
    return jsonResponse(resultado);
  }

  const ids = items
    .map((i: unknown) => (i as Record<string, unknown>)?.id?.videoId)
    .filter(Boolean)
    .join(",");

  // videos.list — 1 unidad
  const videosUrl = new URL("https://www.googleapis.com/youtube/v3/videos");
  videosUrl.searchParams.set("part", "snippet,contentDetails,status");
  videosUrl.searchParams.set("id", ids);
  videosUrl.searchParams.set("key", youtubeKey);

  let videosData: Record<string, unknown>;
  try {
    const videosRes = await fetch(videosUrl.toString());
    videosData = await videosRes.json();
  } catch {
    return errorResponse("No se pudo obtener detalles de YouTube", 502, "youtube_error");
  }

  const videos = (videosData.items as unknown[]) ?? [];
  const ms = Date.now() - inicio;

  const resultados = [];
  const upsertCanciones = [];
  const eventosErrores = [];

  for (const v of videos) {
    const video = v as Record<string, unknown>;
    const snippet = video.snippet as Record<string, unknown>;
    const contentDetails = video.contentDetails as Record<string, unknown>;
    const status = video.status as Record<string, unknown>;

    const youtubeId = video.id as string;
    const tituloOriginal = snippet.title as string;
    const canal = snippet.channelTitle as string ?? "";
    const artista = extraerArtista(tituloOriginal, canal);
    const titulo = limpiarTitulo(tituloOriginal);
    const duracionSeg = parsearDuracion(contentDetails.duration as string ?? "PT0S");
    const reproducible = esReproducible(
      status.embeddable as boolean,
      status.privacyStatus as string,
      contentDetails.regionRestriction as { blocked?: string[]; allowed?: string[] } | undefined
    );

    const thumbnails = snippet.thumbnails as Record<string, { url: string }>;
    const miniaturaUrl =
      thumbnails?.medium?.url ?? thumbnails?.high?.url ?? thumbnails?.default?.url ?? "";

    upsertCanciones.push({
      youtube_id: youtubeId,
      titulo,
      artista,
      canal,
      duracion_seg: duracionSeg,
      miniatura_url: miniaturaUrl,
      reproducible,
      actualizado_en: new Date().toISOString(),
    });

    if (!reproducible) {
      eventosErrores.push(
        admin.from("evento").insert({
          tipo: "error_contenido",
          sesion_id: sesion_id ?? null,
          datos: { tipo: "no_reproducible", youtube_id: youtubeId },
        })
      );
    }

    resultados.push({
      cancion_id: null, // se actualiza después del upsert si se necesita
      youtube_id: youtubeId,
      titulo,
      artista,
      duracion_seg: duracionSeg,
      miniatura_url: miniaturaUrl,
      reproducible,
      motivo_no_reproducible: reproducible ? null : "no_embeddable",
    });
  }

  // Upsert canciones y obtener IDs
  const { data: cancionesUpsert } = await admin
    .from("cancion")
    .upsert(upsertCanciones, { onConflict: "youtube_id" })
    .select("id, youtube_id");

  // Actualizar cancion_id en resultados
  if (cancionesUpsert) {
    const idMap = new Map(cancionesUpsert.map((c) => [c.youtube_id, c.id]));
    for (const r of resultados) {
      r.cancion_id = idMap.get(r.youtube_id) ?? null;
    }
  }

  const respuesta = { cache: false, total: resultados.length, resultados };

  EdgeRuntime.waitUntil(
    Promise.all([
      admin.from("cache_busqueda").upsert({
        consulta_normalizada: consultaNormalizada,
        resultado: respuesta,
        creado_en: new Date().toISOString(),
      }),
      admin.from("evento").insert({
        tipo: "busqueda_realizada",
        sesion_id: sesion_id ?? null,
        datos: { texto: consulta, resultados: resultados.length, ms },
      }),
      ...eventosErrores,
    ])
  );

  return jsonResponse(respuesta);
});
