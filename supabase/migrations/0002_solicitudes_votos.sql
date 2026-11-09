create table public.solicitud (
  id             uuid primary key default gen_random_uuid(),
  sesion_id      uuid not null references public.sesion(id) on delete cascade,
  cancion_id     uuid not null references public.cancion(id) on delete cascade,
  dispositivo_id uuid not null references public.dispositivo(id) on delete cascade,
  estado         text not null default 'pendiente'
                   check (estado in ('pendiente','reproduciendo','reproducida','rechazada')),
  created_at     timestamptz not null default now(),
  unique (sesion_id, cancion_id)
);

create table public.voto (
  id             uuid primary key default gen_random_uuid(),
  solicitud_id   uuid not null references public.solicitud(id) on delete cascade,
  dispositivo_id uuid not null references public.dispositivo(id) on delete cascade,
  created_at     timestamptz not null default now(),
  unique (solicitud_id, dispositivo_id)
);

alter table public.solicitud enable row level security;
alter table public.voto      enable row level security;

create policy "leer solicitudes activas" on public.solicitud
  for select to authenticated
  using (estado in ('pendiente','reproduciendo'));

create policy "leer votos" on public.voto
  for select to authenticated using (true);

create index solicitud_sesion_estado on public.solicitud (sesion_id, estado);
create index voto_solicitud on public.voto (solicitud_id);
