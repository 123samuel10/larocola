const SLUG_RE = /^[a-z0-9-]{2,40}$/;

export function isValidSlug(slug: unknown): slug is string {
  return typeof slug === "string" && SLUG_RE.test(slug);
}

export function isValidQuery(q: unknown): q is string {
  if (typeof q !== "string") return false;
  const trimmed = q.trim();
  return trimmed.length >= 2 && trimmed.length <= 100;
}
