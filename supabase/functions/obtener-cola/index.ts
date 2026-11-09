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

  const { sesion_id } = body as { sesion_id?: string };
  if (!sesion_id || !UUID_RE.test(sesion_id)) {
    return errorResponse("sesion_id debe ser un UUID válido", 400, "parametro_invalido");
  }

  const admin = createAdminClient();

  const { data: solicitudes } = await admin
    .from("solicitud")
    .select("id, estado, created_at, dispositivo_id, cancion_id")
    .eq("sesion_id", sesion_id)
    .neq("estado", "reproducida")
    .order("created_at", { ascending: true });

  if (!solicitudes || solicitudes.length === 0) {
    return jsonResponse({ items: [] });
  }

  const cancionIds = [...new Set(solicitudes.map((s) => s.cancion_id))];
  const solicitudIds = solicitudes.map((s) => s.id);

  const [{ data: canciones }, { data: votos }] = await Promise.all([
    admin.from("cancion").select("id, youtube_id, titulo, artista, duracion_seg, miniatura_url").in("id", cancionIds),
    admin.from("voto").select("solicitud_id, dispositivo_id").in("solicitud_id", solicitudIds),
  ]);

  const cancionMap = new Map((canciones ?? []).map((c) => [c.id, c]));

  const votosPorSolicitud = new Map<string, number>();
  const yaVotadoPorSolicitud = new Set<string>();
  for (const v of votos ?? []) {
    votosPorSolicitud.set(v.solicitud_id, (votosPorSolicitud.get(v.solicitud_id) ?? 0) + 1);
    if (v.dispositivo_id === user.id) {
      yaVotadoPorSolicitud.add(v.solicitud_id);
    }
  }

  const items = solicitudes
    .map((s) => {
      const c = cancionMap.get(s.cancion_id);
      if (!c) return null;
      return {
        solicitud_id: s.id,
        cancion_id: s.cancion_id,
        youtube_id: c.youtube_id,
        titulo: c.titulo,
        artista: c.artista,
        duracion_seg: c.duracion_seg,
        miniatura_url: c.miniatura_url,
        estado: s.estado,
        votos: votosPorSolicitud.get(s.id) ?? 0,
        ya_votado: yaVotadoPorSolicitud.has(s.id),
        es_mia: s.dispositivo_id === user.id,
      };
    })
    .filter(Boolean)
    .sort((a, b) => b!.votos - a!.votos || 0);

  return jsonResponse({ items });
});
