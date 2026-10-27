import { assertEquals } from "https://deno.land/std@0.208.0/assert/mod.ts";
import { parsearDuracion, extraerArtista, limpiarTitulo } from "./index.ts";

Deno.test("parsearDuracion - minutos y segundos", () => {
  assertEquals(parsearDuracion("PT4M12S"), 252);
});
Deno.test("parsearDuracion - solo segundos", () => {
  assertEquals(parsearDuracion("PT45S"), 45);
});
Deno.test("parsearDuracion - horas minutos segundos", () => {
  assertEquals(parsearDuracion("PT1H2M3S"), 3723);
});
Deno.test("parsearDuracion - formato inválido", () => {
  assertEquals(parsearDuracion(""), 0);
});
Deno.test("extraerArtista - separador guion", () => {
  assertEquals(extraerArtista("Marc Anthony - Vivir Mi Vida", "Marc Anthony VEVO"), "Marc Anthony");
});
Deno.test("extraerArtista - canal VEVO", () => {
  assertEquals(extraerArtista("Vivir Mi Vida Official Video", "MarcAnthonyVEVO"), "MarcAnthony");
});
Deno.test("extraerArtista - canal Topic", () => {
  assertEquals(extraerArtista("Cumbia Mix 2024", "Grupo Niche - Topic"), "Grupo Niche");
});
Deno.test("limpiarTitulo - quita Video Oficial", () => {
  assertEquals(limpiarTitulo("Vivir Mi Vida (Video Oficial)"), "Vivir Mi Vida");
});
Deno.test("limpiarTitulo - quita Official Video", () => {
  assertEquals(limpiarTitulo("Shape of You [Official Video]"), "Shape of You");
});
