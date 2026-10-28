<script setup lang="ts">
import { ScanLine } from '@lucide/vue'
</script>

<template>
  <div class="marca-stocka" aria-label="STOCKA">
    <span class="marca-simbolo" aria-hidden="true"><ScanLine :size="24" :stroke-width="1.8" /></span>
    <span>STOCKA</span>
  </div>
</template>

<style scoped>
.marca-stocka {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--cor-texto);
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.marca-simbolo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: var(--raio-campo);
  background: var(--cor-principal);
  color: var(--cor-sobre-principal);
}
</style>
