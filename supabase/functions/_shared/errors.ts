import { corsHeaders } from "./cors.ts";

export function errorResponse(
  message: string,
  status: number,
  code?: string
): Response {
  return new Response(
    JSON.stringify({ error: code ?? message }),
    { status, headers: { ...corsHeaders, "Content-Type": "application/json" } }
  );
}

export function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
