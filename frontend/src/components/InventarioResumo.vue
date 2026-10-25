<script setup lang="ts">
import { computed } from 'vue'
import type { Item, StatusItem } from '@/types/item'
import type { ResumoOrdensServico } from '@/types/ordemServico'

const props = defineProps<{ itens: Item[]; ordens: ResumoOrdensServico[]; filtro: StatusItem | ''; carregandoOrdens: boolean; erroOrdens: string }>()
const emit = defineEmits<{ filtrar: [status: StatusItem | '']; recarregarOrdens: [] }>()
const equipamentos = computed(() => props.itens.filter(i => i.equipamento).length)
const porQuantidade = computed(() => props.itens.filter(i => i.formaRegistro === 'QUANTIDADE').length)
const numeros = computed(() => [
  { status: '' as const, nome: 'Todos os registros', valor: props.itens.length, classe: 'todos' },
  { status: 'ATIVO' as const, nome: 'Ativos', valor: props.itens.filter(i => i.status === 'ATIVO').length, classe: 'ativos' },
  { status: 'EM_MANUTENCAO' as const, nome: 'Em manutenção', valor: props.itens.filter(i => i.status === 'EM_MANUTENCAO').length, classe: 'manutencao' },
  { status: 'INATIVO' as const, nome: 'Inativos', valor: props.itens.filter(i => i.status === 'INATIVO').length, classe: 'inativos' },
])
const totalOS = computed(() => props.ordens.reduce((t, o) => ({
  abertas: t.abertas + o.abertas, emAndamento: t.emAndamento + o.emAndamento, concluidas: t.concluidas + o.concluidas,
}), { abertas: 0, emAndamento: 0, concluidas: 0 }))
</script>

<template>
  <section class="resumo-inventario" aria-labelledby="titulo-resumo">
    <div class="cabecalho-resumo"><h2 id="titulo-resumo">Visão do ambiente</h2><p>{{ equipamentos }} {{ equipamentos === 1 ? 'equipamento' : 'equipamentos' }} · {{ porQuantidade }} {{ porQuantidade === 1 ? 'registro' : 'registros' }} por quantidade</p></div>
    <div class="indicadores"><button v-for="n in numeros" :key="n.classe" class="indicador" :class="[n.classe, { selecionado: filtro === n.status }]" :aria-pressed="filtro === n.status" @click="emit('filtrar', n.status)"><span class="rotulo"><span class="ponto" aria-hidden="true"></span>{{ n.nome }}</span><strong>{{ n.valor.toLocaleString('pt-BR') }}</strong><span class="unidade">{{ n.valor === 1 ? 'registro' : 'registros' }}</span></button></div>
    <div class="resumo-os" aria-label="Ordens de serviço do ambiente"><strong>Ordens de serviço</strong>
      <span v-if="carregandoOrdens" role="status">Consultando OS…</span>
      <template v-else-if="erroOrdens"><span class="mensagem-erro" role="alert">{{ erroOrdens }}</span><button class="recarregar" @click="emit('recarregarOrdens')">Tentar novamente</button></template>
      <template v-else><span class="os-aberta">{{ totalOS.abertas }} {{ totalOS.abertas === 1 ? 'aberta' : 'abertas' }}</span><span class="os-andamento">{{ totalOS.emAndamento }} em andamento</span><span class="os-concluida">{{ totalOS.concluidas }} {{ totalOS.concluidas === 1 ? 'concluída' : 'concluídas' }}</span></template>
    </div>
    <p class="nota">Totais de todo o ambiente. Clique em um status para filtrar a lista. Quantidades de materiais diferentes não são somadas.</p>
  </section>
</template>

<style scoped>
.resumo-inventario { margin-bottom: 28px; }.cabecalho-resumo { display: flex; justify-content: space-between; align-items: baseline; gap: 10px 20px; flex-wrap: wrap; margin-bottom: 14px; }
h2 { margin: 0; font-size: 1rem; font-weight: 600; }.cabecalho-resumo p, .nota { font-size: .8rem; color: var(--cor-texto-secundario); margin: 0; }
.indicadores { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.indicador { --tom: #525c65; text-align: left; display: grid; gap: 8px; padding: 18px; border: 1px solid var(--cor-borda); border-radius: 8px; background: var(--cor-superficie); color: var(--cor-texto); cursor: pointer; }
.indicador:hover { border-color: var(--tom); }.indicador.selecionado { outline: 2px solid var(--tom); outline-offset: -2px; }
.ativos { --tom: #426a51; }.manutencao { --tom: #94631f; }.todos { --tom: var(--cor-ambiente, #b64c1d); }
.rotulo { display: flex; align-items: center; gap: 8px; font-size: .8rem; }.ponto { width: 7px; height: 7px; border-radius: 50%; background: var(--tom); flex: 0 0 auto; }
.indicador strong { font-size: 1.9rem; line-height: 1.1; font-weight: 600; font-variant-numeric: tabular-nums; }.unidade { color: var(--cor-texto-secundario); font-size: .75rem; }
.resumo-os { display: flex; gap: 10px 14px; align-items: center; flex-wrap: wrap; margin: 18px 0 12px; font-size: .8rem; }.resumo-os strong { font-weight: 600; margin-right: 4px; }
.resumo-os > span { padding: 4px 8px; border-radius: 4px; }.os-aberta { background: #fbf1dd; color: #795320; }.os-andamento { background: #eaf0f7; color: #376582; }.os-concluida { background: #eef3ed; color: #30523b; }
.nota { line-height: 1.5; }.recarregar { border: 0; background: transparent; color: var(--cor-principal); min-height: 40px; text-decoration: underline; padding: 4px; }
@media (max-width: 600px) { .indicadores { grid-template-columns: repeat(2, 1fr); }.indicador { padding: 14px; }.rotulo { font-size: .75rem; } }
</style>
