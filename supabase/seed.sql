insert into public.establecimiento (nombre, slug)
values ('La Esquina Bar', 'la-esquina');

-- Jueves a sábado (0=domingo).
insert into public.horario (establecimiento_id, dia_semana, hora_inicio, hora_fin)
select id, d.dia, d.ini::time, d.fin::time
from public.establecimiento e,
     (values (4,'19:00','01:00'), (5,'20:00','02:00'), (6,'20:00','03:00')) as d(dia, ini, fin)
where e.slug = 'la-esquina';

-- Sesión activa para desarrollo y demo.
insert into public.sesion (establecimiento_id)
select id from public.establecimiento where slug = 'la-esquina';
