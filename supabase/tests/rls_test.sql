-- Ejecutar con un cliente que tenga rol authenticated (usuario anónimo real)
-- Debe funcionar:
select id, nombre, slug from public.establecimiento where activo = true;
select id, dia_semana, hora_inicio, hora_fin from public.horario;
select id, estado from public.sesion where estado = 'activa';
select id, youtube_id, titulo from public.cancion limit 5;

-- Debe fallar (RLS bloquea, retorna 0 filas o error):
select * from public.dispositivo;
select * from public.sesion_dispositivo;
select * from public.cache_busqueda;
select * from public.evento;

-- Intentar insertar en dispositivo debe fallar:
-- insert into public.dispositivo (id, id_anonimizado) values (gen_random_uuid(), 'test');
