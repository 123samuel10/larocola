create extension if not exists pgcrypto;

-- Establecimiento
create table public.establecimiento (
  id           uuid primary key default gen_random_uuid(),
  nombre       text not null,
  slug         text not null unique,
  zona_horaria text not null default 'America/Bogota',
  activo       boolean not null default true,
  created_at   timestamptz not null default now()
);

-- Horarios: dia_semana 0=domingo ... 6=sábado.
-- Si hora_fin <= hora_inicio, el horario cruza la medianoche.
create table public.horario (
  id                 uuid primary key default gen_random_uuid(),
  establecimiento_id uuid not null references public.establecimiento(id) on delete cascade,
  dia_semana         smallint not null check (dia_semana between 0 and 6),
  hora_inicio        time not null,
  hora_fin           time not null,
  unique (establecimiento_id, dia_semana)
);

-- Sesión: jornada de operación. Solo una activa por establecimiento.
create table public.sesion (
  id                 uuid primary key default gen_random_uuid(),
  establecimiento_id uuid not null references public.establecimiento(id) on delete cascade,
  estado             text not null default 'activa' check (estado in ('activa','cerrada')),
  abierta_en         timestamptz not null default now(),
  cerrada_en         timestamptz
);
create unique index sesion_una_activa
  on public.sesion (establecimiento_id) where estado = 'activa';

-- Dispositivo: id = auth.uid() del usuario anónimo.
create table public.dispositivo (
  id              uuid primary key references auth.users(id) on delete cascade,
  id_anonimizado  text not null unique,
  created_at      timestamptz not null default now(),
  ultimo_uso      timestamptz not null default now()
);

create table public.sesion_dispositivo (
  sesion_id      uuid not null references public.sesion(id) on delete cascade,
  dispositivo_id uuid not null references public.dispositivo(id) on delete cascade,
  ingreso_en     timestamptz not null default now(),
  primary key (sesion_id, dispositivo_id)
);

-- Canción: metadatos del video de YouTube (no se almacena contenido).
create table public.cancion (
  id             uuid primary key default gen_random_uuid(),
  youtube_id     text not null unique,
  titulo         text not null,
  artista        text not null,
  canal          text,
  duracion_seg   integer not null check (duracion_seg >= 0),
  miniatura_url  text not null,
  reproducible   boolean not null default true,
  actualizado_en timestamptz not null default now()
);

-- Caché de búsquedas para ahorrar cuota de YouTube (TTL 24 h, lo aplica la función).
create table public.cache_busqueda (
  consulta_normalizada text primary key,
  resultado            jsonb not null,
  creado_en            timestamptz not null default now()
);

-- Eventos analíticos. Solo guarda el hash; sin relación con dispositivo.
create table public.evento (
  id                     bigint generated always as identity primary key,
  tipo                   text not null,
  sesion_id              uuid,
  dispositivo_anonimizado text,
  cancion_id             uuid,
  solicitud_id           uuid,
  datos                  jsonb not null default '{}'::jsonb,
  created_at             timestamptz not null default now()
);
create index evento_tipo_fecha on public.evento (tipo, created_at);
create index evento_sesion on public.evento (sesion_id);

-- Row Level Security
alter table public.establecimiento      enable row level security;
alter table public.horario              enable row level security;
alter table public.sesion               enable row level security;
alter table public.dispositivo          enable row level security;
alter table public.sesion_dispositivo   enable row level security;
alter table public.cancion              enable row level security;
alter table public.cache_busqueda       enable row level security;
alter table public.evento               enable row level security;

-- Lectura pública (usuarios autenticados incluidos anónimos)
create policy "leer establecimientos activos" on public.establecimiento
  for select to authenticated using (activo);
create policy "leer horarios" on public.horario
  for select to authenticated using (true);
create policy "leer sesion activa" on public.sesion
  for select to authenticated using (estado = 'activa');
create policy "leer canciones" on public.cancion
  for select to authenticated using (true);
