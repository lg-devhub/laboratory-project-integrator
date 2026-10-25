<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Building2, DoorOpen, Pencil, Users } from '@lucide/vue'
import { coresAmbiente, type Ambiente } from '@/types/ambiente'

const props = defineProps<{ ambiente: Ambiente; administrador: boolean }>()
defineEmits<{ editar: []; acessos: [] }>()
const cor = computed(() => coresAmbiente.find(c => c.valor === props.ambiente.cor)?.hex || '#b64c1d')
</script>

<template>
  <article class="ambiente-card" :style="{ '--cor-ambiente': cor }">
    <div class="cartao-cabecalho">
      <span class="ambiente-icone" aria-hidden="true">
        <DoorOpen v-if="ambiente.tipo === 'SALA'" :size="26" :stroke-width="1.5" />
        <Building2 v-else :size="26" :stroke-width="1.5" />
      </span>
      <span class="ambiente-tipo">{{ ambiente.tipo === 'SALA' ? 'Sala' : 'Setor' }}</span>
    </div>
    <div class="cartao-conteudo"><h2><RouterLink :to="{ name: 'inventario', params: { ambienteId: ambiente.id } }">{{ ambiente.nome }}</RouterLink></h2><p>{{ ambiente.descricao || 'Sem descrição cadastrada.' }}</p>
      <RouterLink class="abrir-inventario" :to="{ name: 'inventario', params: { ambienteId: ambiente.id } }" :aria-label="`Abrir inventário de ${ambiente.nome}`">Abrir inventário →</RouterLink>
    </div>
    <footer v-if="administrador" class="cartao-acoes">
      <button class="acao-cartao" :aria-label="`Editar ${ambiente.nome}`" @click="$emit('editar')"><Pencil :size="16" aria-hidden="true" /> Editar</button>
      <button class="acao-cartao" :aria-label="`Gerenciar acessos de ${ambiente.nome}`" @click="$emit('acessos')"><Users :size="17" aria-hidden="true" /> Acessos</button>
    </footer>
  </article>
</template>

<style scoped>
.ambiente-card { display: flex; flex-direction: column; overflow: hidden; min-width: 0; border: 1px solid var(--cor-borda); border-radius: 8px; background: var(--cor-superficie); }
.cartao-cabecalho { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 22px; background: color-mix(in srgb, var(--cor-ambiente) 7%, var(--cor-fundo)); border-top: 4px solid var(--cor-ambiente); }
.ambiente-icone { display: grid; place-items: center; width: 44px; height: 44px; border: 1px solid color-mix(in srgb, var(--cor-ambiente) 30%, var(--cor-borda)); border-radius: 6px; background: var(--cor-superficie); color: var(--cor-ambiente); }
.ambiente-tipo { font-size: .8rem; font-weight: 600; color: var(--cor-texto-secundario); }
.cartao-conteudo { flex: 1; padding: 24px 22px; overflow-wrap: anywhere; }
h2 { margin: 0 0 12px; font-size: 1.15rem; font-weight: 600; line-height: 1.3; }
h2 a { color: inherit; text-decoration: none; }
h2 a:hover { text-decoration: underline; }
.abrir-inventario { display: inline-flex; align-items: center; min-height: 44px; margin-top: 12px; color: var(--cor-ambiente); text-decoration: none; font-size: .85rem; font-weight: 600; }
.abrir-inventario:hover { text-decoration: underline; }
p { margin: 0; color: var(--cor-texto-secundario); font-size: .9rem; line-height: 1.6; white-space: pre-line; }
.cartao-acoes { display: flex; justify-content: space-between; flex-wrap: wrap; padding: 8px 14px; border-top: 1px solid var(--cor-borda); gap: 6px; }
.acao-cartao { display: inline-flex; align-items: center; gap: 8px; min-height: 42px; padding: 8px; border: 0; border-radius: 4px; background: transparent; color: var(--cor-texto-secundario); font: inherit; font-size: .85rem; }
.acao-cartao:hover { background: var(--cor-fundo); color: var(--cor-texto); }
</style>
