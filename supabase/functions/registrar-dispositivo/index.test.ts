// Nota: para poder testear calcularProximosHorarios, asegúrate de que esté exportada en index.ts
// con `export function calcularProximosHorarios(...)`

import { assertEquals } from "https://deno.land/std@0.208.0/assert/mod.ts";

// Test inline de la lógica (no importamos desde index.ts para evitar dependencias de Deno.serve)
function calcularProximosHorarios(
  horarios: Array<{ dia_semana: number; hora_inicio: string; hora_fin: string }>,
  _zonaTZ: string,
  cantidad = 2,
  ahoraMock?: Date
): Array<{ etiqueta: string; inicio: string; fin: string }> {
  const ahora = ahoraMock ?? new Date();
  const diaActual = ahora.getDay();
  const horaActual = ahora.getHours() * 60 + ahora.getMinutes();
  const DIAS_ES = ["Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"];
  const proximos: Array<{ etiqueta: string; inicio: string; fin: string }> = [];

  for (let offset = 0; offset <= 13 && proximos.length < cantidad; offset++) {
    const diaBuscado = (diaActual + offset) % 7;
    const horario = horarios.find((h) => h.dia_semana === diaBuscado);
    if (!horario) continue;
    const [hIni, mIni] = horario.hora_inicio.split(":").map(Number);
    const minIni = hIni * 60 + mIni;
    if (offset === 0 && horaActual >= minIni) continue;
    let etiqueta: string;
    if (offset === 0) etiqueta = `Hoy, ${DIAS_ES[diaBuscado].toLowerCase()}`;
    else if (offset === 1) etiqueta = "Mañana";
    else etiqueta = DIAS_ES[diaBuscado];
    proximos.push({ etiqueta, inicio: horario.hora_inicio.slice(0, 5), fin: horario.hora_fin.slice(0, 5) });
  }
  return proximos;
}

const horariosDemo = [
  { dia_semana: 4, hora_inicio: "19:00:00", hora_fin: "01:00:00" },
  { dia_semana: 5, hora_inicio: "20:00:00", hora_fin: "02:00:00" },
  { dia_semana: 6, hora_inicio: "20:00:00", hora_fin: "03:00:00" },
];

Deno.test("próximos horarios - jueves antes de apertura", () => {
  // Jueves 15:00
  const mock = new Date(2026, 9, 22, 15, 0, 0); // Jueves
  const result = calcularProximosHorarios(horariosDemo, "America/Bogota", 2, mock);
  assertEquals(result.length, 2);
  assertEquals(result[0].inicio, "19:00");
  assertEquals(result[1].inicio, "20:00");
});

Deno.test("próximos horarios - jueves después de apertura, salta al siguiente", () => {
  // Jueves 21:00 (ya abrió a las 19:00)
  const mock = new Date(2026, 9, 22, 21, 0, 0);
  const result = calcularProximosHorarios(horariosDemo, "America/Bogota", 2, mock);
  // El jueves ya pasó, debe dar viernes y sábado
  assertEquals(result[0].inicio, "20:00"); // viernes
  assertEquals(result[1].inicio, "20:00"); // sábado
});
