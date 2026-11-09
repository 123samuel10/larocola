import { handleCors } from "../_shared/cors.ts";
import { createAdminClient } from "../_shared/admin-client.ts";
import { errorResponse, jsonResponse } from "../_shared/errors.ts";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

Deno.serve(async (req) => {
  const corsResponse = handleCors(req);
  if (corsResponse) return corsResponse;
  if (req.method !== "POST") return errorResponse("Método no permitido", 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return errorResponse("Sin autorización", 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

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

  const { sesion_id, cancion_id } = body as { sesion_id?: string; cancion_id?: string };
  if (!sesion_id || !UUID_RE.test(sesion_id) || !cancion_id || !UUID_RE.test(cancion_id)) {
    return errorResponse("sesion_id y cancion_id deben ser UUIDs válidos", 400, "parametros_invalidos");
  }

  const admin = createAdminClient();

  const { data: sesion } = await admin
    .from("sesion")
    .select("id")
    .eq("id", sesion_id)
    .eq("estado", "activa")
    .maybeSingle();

  if (!sesion) return errorResponse("Sesión no encontrada o inactiva", 404, "sesion_no_encontrada");

  const { data: cancion } = await admin
    .from("cancion")
    .select("id, reproducible")
    .eq("id", cancion_id)
    .maybeSingle();

  if (!cancion) return errorResponse("Canción no encontrada", 404, "cancion_no_encontrada");
  if (!cancion.reproducible) return errorResponse("La canción no es reproducible", 403, "no_reproducible");

  await admin
    .from("dispositivo")
    .upsert({ id: user.id, ultimo_uso: new Date().toISOString() }, { onConflict: "id" });

  const { data: existente } = await admin
    .from("solicitud")
    .select("id")
    .eq("sesion_id", sesion_id)
    .eq("cancion_id", cancion_id)
    .maybeSingle();

  if (existente) {
    return jsonResponse({ solicitud_id: existente.id, ya_existia: true });
  }

  const { data: nueva, error: insertError } = await admin
    .from("solicitud")
    .insert({ sesion_id, cancion_id, dispositivo_id: user.id })
    .select("id")
    .single();

  if (insertError || !nueva) {
    return errorResponse("Error al crear la solicitud", 500);
  }

  EdgeRuntime.waitUntil(
    admin.from("evento").insert({
      tipo: "solicitud_registrada",
      sesion_id,
      cancion_id,
      datos: { cancion_id },
    })
  );

  return jsonResponse({ solicitud_id: nueva.id, ya_existia: false });
});
