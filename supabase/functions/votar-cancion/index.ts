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

  const { solicitud_id } = body as { solicitud_id?: string };
  if (!solicitud_id || !UUID_RE.test(solicitud_id)) {
    return errorResponse("solicitud_id debe ser un UUID válido", 400, "parametro_invalido");
  }

  const admin = createAdminClient();

  const { data: solicitud } = await admin
    .from("solicitud")
    .select("id, sesion_id")
    .eq("id", solicitud_id)
    .in("estado", ["pendiente", "reproduciendo"])
    .maybeSingle();

  if (!solicitud) return errorResponse("Solicitud no encontrada o no votable", 404, "solicitud_no_encontrada");

  await admin
    .from("dispositivo")
    .upsert({ id: user.id, ultimo_uso: new Date().toISOString() }, { onConflict: "id" });

  const { data: votoExistente } = await admin
    .from("voto")
    .select("id")
    .eq("solicitud_id", solicitud_id)
    .eq("dispositivo_id", user.id)
    .maybeSingle();

  let yaVotado: boolean;
  if (votoExistente) {
    await admin.from("voto").delete().eq("id", votoExistente.id);
    yaVotado = false;
  } else {
    await admin.from("voto").insert({ solicitud_id, dispositivo_id: user.id });
    yaVotado = true;
    EdgeRuntime.waitUntil(
      admin.from("evento").insert({
        tipo: "voto_registrado",
        sesion_id: solicitud.sesion_id,
        datos: { solicitud_id },
      })
    );
  }

  const { count } = await admin
    .from("voto")
    .select("*", { count: "exact", head: true })
    .eq("solicitud_id", solicitud_id);

  return jsonResponse({ total_votos: count ?? 0, ya_votado: yaVotado });
});
