create policy "anon leer establecimientos" on public.establecimiento
  for select to anon using (activo);

create policy "anon leer sesion activa" on public.sesion
  for select to anon using (estado = 'activa');

create policy "anon leer solicitudes activas" on public.solicitud
  for select to anon using (estado in ('pendiente', 'reproduciendo'));

create policy "anon leer votos" on public.voto
  for select to anon using (true);

create policy "anon leer canciones" on public.cancion
  for select to anon using (true);
