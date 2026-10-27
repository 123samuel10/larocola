import { handleCors, corsHeaders } from "../_shared/cors.ts";
import { createAdminClient } from "../_shared/admin-client.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";
import { isValidSlug } from "../_shared/validation.ts";

// Calcula los próximos N horarios desde ahora en la zona horaria dada.
export function calcularProximosHorarios(
  horarios: Array<{ dia_semana: number; hora_inicio: string; hora_fin: string }>,
  zonaTZ: string,
  cantidad = 2
): Array<{ etiqueta: string; inicio: string; fin: string }> {
  const ahora = new Date(
    new Date().toLocaleString("en-US", { timeZone: zonaTZ })
  );
  const diaActual = ahora.getDay(); // 0=domingo
  const horaActual = ahora.getHours() * 60 + ahora.getMinutes();

  const DIAS_ES = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];

  const proximos: Array<{ etiqueta: string; inicio: string; fin: string }> = [];

  for (let offset = 0; offset <= 13 && proximos.length < cantidad; offset++) {
    const diaBuscado = (diaActual + offset) % 7;
    const horario = horarios.find((h) => h.dia_semana === diaBuscado);
    if (!horario) continue;

    const [hIni, mIni] = horario.hora_inicio.split(":").map(Number);
    const minIni = hIni * 60 + mIni;

    // Si es hoy, verificar que el horario aún no haya pasado
    if (offset === 0 && horaActual >= minIni) continue;

    let etiqueta: string;
    if (offset === 0) {
      const nombreDia = DIAS_ES[diaBuscado];
      etiqueta = `Hoy, ${nombreDia.toLowerCase()}`;
    } else if (offset === 1) {
      etiqueta = "Mañana";
    } else {
      etiqueta = DIAS_ES[diaBuscado];
    }

    proximos.push({
      etiqueta,
      inicio: horario.hora_inicio.slice(0, 5),
      fin: horario.hora_fin.slice(0, 5),
    });
  }

  return proximos;
}

// HMAC-SHA256 en hex usando WebCrypto
async function hmacSha256Hex(secret: string, message: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    enc.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, enc.encode(message));
  return Array.from(new Uint8Array(sig))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

Deno.serve(async (req) => {
  const corsResponse = handleCors(req);
  if (corsResponse) return corsResponse;

  if (req.method !== "POST") {
    return errorResponse("Método no permitido", 405);
  }

  // Verificar JWT y obtener usuario
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return errorResponse("Sin autorización", 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const hashSecret = Deno.env.get("HASH_SECRET");
  if (!hashSecret) return errorResponse("Configuración incompleta", 500);

  // Usar el cliente anon con el JWT del usuario para verificarlo
  const { createClient } = await import("https://esm.sh/@supabase/supabase-js@2");
  const userClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: { user }, error: authError } = await userClient.auth.getUser();
  if (authError || !user) return errorResponse("Token inválido", 401);

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return errorResponse("Body inválido", 400);
  }

  const { slug } = body;
  if (!isValidSlug(slug)) {
    return errorResponse("slug inválido", 400, "slug_invalido");
  }

  const admin = createAdminClient();

  // Anonimizar dispositivo
  const idAnonimizado = await hmacSha256Hex(hashSecret, user.id);

  // Upsert dispositivo
  const { data: dispositivoExistente } = await admin
    .from("dispositivo")
    .select("id")
    .eq("id", user.id)
    .maybeSingle();

  await admin.from("dispositivo").upsert(
    { id: user.id, id_anonimizado: idAnonimizado, ultimo_uso: new Date().toISOString() },
    { onConflict: "id" }
  );

  const esNuevo = !dispositivoExistente;
  if (esNuevo) {
    EdgeRuntime.waitUntil(
      admin.from("evento").insert({
        tipo: "dispositivo_registrado",
        dispositivo_anonimizado: idAnonimizado,
        datos: {},
      })
    );
  }

  // Buscar establecimiento
  const { data: establecimiento, error: estError } = await admin
    .from("establecimiento")
    .select("id, nombre, slug, zona_horaria")
    .eq("slug", slug)
    .eq("activo", true)
    .maybeSingle();

  if (estError || !establecimiento) {
    return errorResponse("Establecimiento no encontrado", 404, "establecimiento_no_encontrado");
  }

  // Buscar sesión activa
  const { data: sesion } = await admin
    .from("sesion")
    .select("id")
    .eq("establecimiento_id", establecimiento.id)
    .eq("estado", "activa")
    .maybeSingle();

  // Buscar horarios para calcular próximos
  const { data: horarios } = await admin
    .from("horario")
    .select("dia_semana, hora_inicio, hora_fin")
    .eq("establecimiento_id", establecimiento.id);

  const proximosHorarios = calcularProximosHorarios(
    horarios ?? [],
    establecimiento.zona_horaria
  );

  if (sesion) {
    const { error: sdError } = await admin
      .from("sesion_dispositivo")
      .insert({ sesion_id: sesion.id, dispositivo_id: user.id })
      .select();

    // Si no hubo error de conflicto (ingreso nuevo), registrar evento
    if (!sdError) {
      EdgeRuntime.waitUntil(
        admin.from("evento").insert({
          tipo: "ingreso_sesion",
          sesion_id: sesion.id,
          dispositivo_anonimizado: idAnonimizado,
          datos: {},
        })
      );
    }
  }

  return jsonResponse({
    estado: sesion ? "activa" : "cerrada",
    sesion_id: sesion?.id ?? null,
    establecimiento: {
      id: establecimiento.id,
      nombre: establecimiento.nombre,
      slug: establecimiento.slug,
    },
    proximos_horarios: proximosHorarios,
  });
});
