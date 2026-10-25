<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Package, Plus, Search } from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import ItemFormulario from '@/components/ItemFormulario.vue'
import ItemDetalhes from '@/components/ItemDetalhes.vue'
import InventarioResumo from '@/components/InventarioResumo.vue'
import { consultarResumoOS } from '@/services/ordensServico'
import type { ResumoOrdensServico } from '@/types/ordemServico'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import { consultarAmbiente, listarItens, ErroInventario } from '@/services/itens'
import { coresAmbiente, type Ambiente } from '@/types/ambiente'
import { statusItens, type Item, type StatusItem } from '@/types/item'

const route = useRoute()
const router = useRouter()
const autenticacao = useAutenticacaoStore()
const ambiente = ref<Ambiente | null>(null)
const itens = ref<Item[]>([])
const carregando = ref(true)
const erro = ref('')
const sucesso = ref('')
const pesquisa = ref('')
const statusFiltro = ref<StatusItem | ''>('')
const formularioAberto = ref(false)
const itemEditando = ref<Item | null>(null)
const itemSelecionado = ref<Item | null>(null)
let consulta = 0
let consultaResumo = 0
const resumoOS = ref<ResumoOrdensServico[]>([])
const carregandoResumoOS = ref(true)
const erroResumoOS = ref('')
const resumoPorItem = computed(() => new Map(resumoOS.value.map(o => [o.itemId, o])))

async function carregarResumoOS() {
  if (!ambiente.value) return
  const id = ambiente.value.id
  const atual = ++consultaResumo
  carregandoResumoOS.value = true; erroResumoOS.value = ''
  try {
    const resultado = await consultarResumoOS(id)
    if (atual === consultaResumo) resumoOS.value = resultado
  } catch (falha) {
    if (atual !== consultaResumo) return
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) await acessoPerdido(falha)
    else erroResumoOS.value = 'Resumo de OS indisponível.'
  } finally { if (atual === consultaResumo) carregandoResumoOS.value = false }
}


const cor = computed(() => coresAmbiente.find(c => c.valor === ambiente.value?.cor)?.hex || '#b64c1d')
function normalizar(texto: string) { return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('pt-BR') }
const itensFiltrados = computed(() => {
  const termo = normalizar(pesquisa.value.trim())
  return itens.value.filter(item => (!statusFiltro.value || item.status === statusFiltro.value)
    && normalizar(`${item.nome} ${item.categoria} ${item.numeroPatrimonio || ''} ${item.localizacao || ''} ${item.id}`).includes(termo))
})

async function acessoPerdido(falha: ErroInventario) {
  consultaResumo++; resumoOS.value = []
  formularioAberto.value = false
  itemEditando.value = null
  itemSelecionado.value = null
  ambiente.value = null
  itens.value = []
  sucesso.value = ''
  if (falha.status === 401) {
    autenticacao.usuario = null
    await router.replace({ name: 'login' })
    autenticacao.erroSessao = falha.message
  } else erro.value = 'Ambiente ou item indisponível. O registro pode não existir ou sua permissão pode ter mudado.'
}

async function carregar() {
  const atual = ++consulta
  consultaResumo++; resumoOS.value = []; carregandoResumoOS.value = true; erroResumoOS.value = ''
  carregando.value = true
  erro.value = ''
  sucesso.value = ''
  ambiente.value = null
  itens.value = []
  formularioAberto.value = false
  itemEditando.value = null
  itemSelecionado.value = null
  pesquisa.value = ''
  statusFiltro.value = ''
  const id = Number(route.params.ambienteId)
  if (!Number.isSafeInteger(id) || id <= 0) { erro.value = 'Ambiente indisponível.'; carregando.value = false; return }
  try {
    const [dados, lista] = await Promise.all([consultarAmbiente(id), listarItens(id)])
    if (atual !== consulta) return
    ambiente.value = dados
    itens.value = lista
    void carregarResumoOS()
  } catch (falha) {
    if (atual !== consulta) return
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) await acessoPerdido(falha)
    else erro.value = falha instanceof Error ? falha.message : 'Não foi possível carregar o inventário.'
  } finally { if (atual === consulta) carregando.value = false }
}

function itemSalvo(item: Item) {
  const indice = itens.value.findIndex(atual => atual.id === item.id)
  if (indice >= 0) itens.value[indice] = item
  else itens.value.push(item)
  itens.value.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
  formularioAberto.value = false
  itemEditando.value = null
  pesquisa.value = ''
  statusFiltro.value = ''
  sucesso.value = indice >= 0 ? 'Item atualizado. Alterações efetivas foram registradas no histórico.' : 'Item cadastrado e histórico registrado.'
}

function editar(item: Item) {
  itemSelecionado.value = null
  itemEditando.value = item
  formularioAberto.value = true
  sucesso.value = ''
}

function sincronizarItem(item: Item) {
  const indice = itens.value.findIndex(atual => atual.id === item.id)
  if (indice >= 0) itens.value[indice] = item
}

watch(() => route.params.ambienteId, carregar, { immediate: true })
onBeforeUnmount(() => { consulta++; consultaResumo++ })
</script>

<template>
  <div class="pagina-inventario" :style="{ '--cor-ambiente': cor }">
    <header class="cabecalho-inventario"><div class="cabecalho-conteudo"><MarcaStocka /><span class="nome-conta">{{ autenticacao.usuario?.nome }}</span></div></header>
    <main class="inventario">
      <RouterLink :to="{ name: 'ambientes' }" class="voltar"><ArrowLeft :size="18" aria-hidden="true" /> Todos os ambientes</RouterLink>
      <section v-if="carregando" class="estado" role="status">Carregando inventário…</section>
      <section v-else-if="erro" class="estado"><h1>Inventário indisponível</h1><p class="mensagem-erro" role="alert">{{ erro }}</p><button class="botao-secundario" @click="carregar">Tentar novamente</button></section>
      <template v-else-if="ambiente">
        <section class="apresentacao" aria-labelledby="nome-ambiente">
          <div class="titulo-inventario"><div><p class="categoria-ambiente">{{ ambiente.tipo === 'SALA' ? 'Sala' : 'Setor' }} · Inventário</p><h1 id="nome-ambiente">{{ ambiente.nome }}</h1><p class="descricao-ambiente">{{ ambiente.descricao || 'Itens localizados neste ambiente.' }}</p></div>
            <button class="botao-principal" @click="sucesso = ''; itemEditando = null; formularioAberto = true"><Plus :size="19" aria-hidden="true" /> Cadastrar item</button>
          </div>
          <p class="explicacao">Este inventário mostra a localização atual dos itens. O setor responsável de cada equipamento aparece nos detalhes.</p>
        </section>
        <InventarioResumo :itens="itens" :ordens="resumoOS" :filtro="statusFiltro" :carregando-ordens="carregandoResumoOS" :erro-ordens="erroResumoOS" @filtrar="statusFiltro = $event" @recarregar-ordens="carregarResumoOS" />
        <p v-if="sucesso" class="mensagem-sucesso" role="status">{{ sucesso }}</p>
        <section v-if="!itens.length" class="estado"><Package :size="36" :stroke-width="1.5" aria-hidden="true" /><h2>Este ambiente ainda não tem itens</h2><p>Use Cadastrar item para adicionar o primeiro registro ao inventário.</p></section>
        <section v-else aria-label="Itens do ambiente">
          <div class="filtros-inventario">
            <div class="busca"><label for="pesquisa-itens">Buscar no inventário</label><div class="campo-busca"><Search :size="19" aria-hidden="true" /><input id="pesquisa-itens" v-model="pesquisa" type="search" autocomplete="off" placeholder="Nome, categoria, patrimônio ou identificação"></div></div>
            <div class="filtro-status"><label for="filtro-status">Status</label><select id="filtro-status" v-model="statusFiltro"><option value="">Todos os status</option><option v-for="status in statusItens" :key="status.valor" :value="status.valor">{{ status.nome }}</option></select></div>
          </div>
          <p class="contagem" role="status">{{ itensFiltrados.length }} {{ itensFiltrados.length === 1 ? 'registro encontrado' : 'registros encontrados' }}</p>
          <p v-if="itensFiltrados.length" class="dica-tabela">Deslize a tabela para os lados para ver quantidade, status e OS.</p>
          <div v-if="itensFiltrados.length" class="tabela-container" role="region" aria-label="Inventário; em telas estreitas, role horizontalmente" tabindex="0">
            <table class="tabela-itens"><caption class="visualmente-oculto">Itens localizados em {{ ambiente.nome }}</caption>
              <thead><tr><th scope="col">Item</th><th scope="col">Identificação</th><th scope="col">Quantidade</th><th scope="col">Status do item</th><th scope="col">Ordens de serviço</th><th scope="col"><span class="visualmente-oculto">Ações</span></th></tr></thead>
              <tbody><tr v-for="item in itensFiltrados" :key="item.id">
                <td><strong class="item-nome">{{ item.nome }}</strong><span class="item-categoria">{{ item.categoria }}</span></td>
                <td><span class="patrimonio">{{ item.numeroPatrimonio || `Registro #${item.id}` }}</span><span class="item-categoria">{{ item.equipamento ? 'Equipamento individual' : item.formaRegistro === 'INDIVIDUAL' ? 'Item individual' : 'Por quantidade' }}</span></td>
                <td class="quantidade"><strong>{{ item.quantidade.toLocaleString('pt-BR') }}</strong><span class="item-categoria">{{ item.formaRegistro === 'QUANTIDADE' ? 'unidades neste registro' : 'unidade individual' }}</span></td>
                <td><span class="status-item" :class="`status-${item.status.toLowerCase()}`">{{ statusItens.find(s => s.valor === item.status)?.nome }}</span></td>
                <td class="coluna-os"><span v-if="!item.equipamento" class="item-categoria">Não se aplica</span><span v-else-if="carregandoResumoOS" class="item-categoria">Consultando…</span><span v-else-if="erroResumoOS" class="item-categoria">Indisponível</span><div v-else-if="resumoPorItem.has(item.id)" class="etiquetas-os"><span v-if="resumoPorItem.get(item.id)?.abertas" class="etiqueta-os aberta">{{ resumoPorItem.get(item.id)?.abertas }} {{ resumoPorItem.get(item.id)?.abertas === 1 ? 'aberta' : 'abertas' }}</span><span v-if="resumoPorItem.get(item.id)?.emAndamento" class="etiqueta-os andamento">{{ resumoPorItem.get(item.id)?.emAndamento }} em andamento</span><span v-if="resumoPorItem.get(item.id)?.concluidas" class="etiqueta-os concluida">{{ resumoPorItem.get(item.id)?.concluidas }} {{ resumoPorItem.get(item.id)?.concluidas === 1 ? 'concluída' : 'concluídas' }}</span></div><span v-else class="item-categoria">Sem OS</span></td>
                <td><button class="botao-detalhes" :aria-label="`Ver detalhes de ${item.nome}`" @click="itemSelecionado = item">Ver detalhes</button></td>
              </tr></tbody>
            </table>
          </div>
          <div v-else class="estado"><h2>Nenhum item encontrado</h2><p>Tente outro termo ou status.</p><button class="botao-secundario" @click="pesquisa = ''; statusFiltro = ''">Limpar filtros</button></div>
        </section>
      </template>
    </main>
    <ItemFormulario v-if="formularioAberto && ambiente" :ambiente="ambiente" :item="itemEditando" @fechar="formularioAberto = false; itemEditando = null" @salvo="itemSalvo" @sincronizado="sincronizarItem" @acesso-perdido="acessoPerdido" />
    <ItemDetalhes v-if="itemSelecionado && ambiente" :item="itemSelecionado" :ambiente-nome="ambiente.nome" @os-alterada="carregarResumoOS" @fechar="itemSelecionado = null" @editar="editar" @acesso-perdido="acessoPerdido" />
  </div>
</template>

<style scoped>
.cabecalho-inventario { background: var(--cor-superficie); border-bottom: 1px solid var(--cor-borda); }
.cabecalho-conteudo { max-width: 1240px; padding: 22px 32px; margin: auto; display: flex; justify-content: space-between; align-items: center; gap: 24px; flex-wrap: wrap; }
.nome-conta { color: var(--cor-texto-secundario); font-size: .9rem; overflow-wrap: anywhere; }
.inventario { max-width: 1240px; margin: auto; padding: 28px 32px 64px; }
.voltar { display: inline-flex; align-items: center; gap: 8px; min-height: 44px; color: var(--cor-texto-secundario); font-size: .85rem; text-decoration: none; margin-bottom: 26px; }
.voltar:hover { color: var(--cor-principal); text-decoration: underline; }
.apresentacao { padding: 0 0 26px 20px; border-left: 4px solid var(--cor-ambiente); margin-bottom: 32px; }
.titulo-inventario { display: flex; justify-content: space-between; align-items: start; gap: 24px; flex-wrap: wrap; }
.titulo-inventario > div { flex: 1; min-width: 200px; }
.titulo-inventario button { gap: 8px; }
.categoria-ambiente { margin: 0 0 8px; color: var(--cor-ambiente); font-size: .75rem; text-transform: uppercase; letter-spacing: .1em; }
h1 { margin: 0; font-size: clamp(1.8rem, 3vw, 2.3rem); letter-spacing: -.03em; font-weight: 600; overflow-wrap: anywhere; }
.descricao-ambiente { margin: 10px 0 0; color: var(--cor-texto-secundario); overflow-wrap: anywhere; }
.explicacao { margin: 22px 0 0; color: var(--cor-texto-secundario); font-size: .8rem; }
.filtros-inventario { display: flex; gap: 20px; align-items: end; margin-bottom: 20px; flex-wrap: wrap; }
.busca { flex: 1; min-width: 220px; }
.filtros-inventario label { display: block; font-size: .85rem; margin-bottom: 8px; font-weight: 600; }
.campo-busca { display: flex; align-items: center; gap: 10px; padding: 0 14px; background: var(--cor-superficie); border: 1px solid var(--cor-borda); border-radius: var(--raio-campo); color: var(--cor-texto-secundario); }
.campo-busca input { width: 100%; min-width: 0; height: 46px; border: 0; color: var(--cor-texto); background: transparent; }
.filtro-status select { height: 48px; padding: 8px 12px; border: 1px solid var(--cor-borda); border-radius: var(--raio-campo); background: var(--cor-superficie); color: var(--cor-texto); }
.dica-tabela { display: none; font-size: .8rem; color: var(--cor-texto-secundario); }
@media (max-width: 600px) { .dica-tabela { display: block; } }
.contagem { font-size: .85rem; color: var(--cor-texto-secundario); }
.tabela-container { position: relative; overflow-x: auto; border: 1px solid var(--cor-borda); border-radius: 8px; background: var(--cor-superficie); }
.tabela-itens { width: 100%; border-collapse: collapse; text-align: left; font-size: .9rem; }
th { padding: 14px 20px; background: var(--cor-painel); font-size: .75rem; font-weight: 600; color: var(--cor-texto-secundario); white-space: nowrap; }
td { padding: 20px; border-top: 1px solid var(--cor-borda); }
td:first-child { min-width: 190px; max-width: 300px; overflow-wrap: anywhere; }
.item-nome { font-weight: 600; }
.item-categoria { display: block; font-size: .8rem; margin-top: 5px; color: var(--cor-texto-secundario); }
.patrimonio { display: block; max-width: 220px; overflow-wrap: anywhere; min-width: 130px; }
.quantidade { font-variant-numeric: tabular-nums; }.quantidade strong { font-size: 1.05rem; font-weight: 600; }
.coluna-os { min-width: 160px; }.etiquetas-os { display: flex; flex-wrap: wrap; gap: 5px; }
.etiqueta-os { padding: 4px 7px; font-size: .72rem; border-radius: 4px; white-space: nowrap; }
.aberta { color: #795320; background: #fbf1dd; }.andamento { color: #376582; background: #eaf0f7; }.concluida { color: #30523b; background: #eef3ed; }
.status-item { display: inline-block; border: 1px solid currentColor; border-radius: 4px; padding: 4px 8px; font-size: .75rem; white-space: nowrap; }
.status-ativo { color: #31573e; background: #edf3ed; }
.status-em_manutencao { color: #795320; background: #fbf1dd; }
.status-inativo { color: #525c65; background: #eef0f2; }
.botao-detalhes { min-height: 42px; border: 0; padding: 8px; color: var(--cor-principal); background: transparent; white-space: nowrap; font-size: .85rem; font-weight: 600; }
.botao-detalhes:hover { text-decoration: underline; }
.estado { display: grid; justify-items: center; text-align: center; gap: 16px; padding: 54px 20px; }
.estado h1, .estado h2 { font-size: 1.3rem; margin: 0; font-weight: 600; }
.estado p { margin: 0; max-width: 520px; }
.mensagem-sucesso { border-left: 3px solid #426a51; padding: 12px 16px; margin-bottom: 24px; background: #eef3ed; color: #30523b; }
@media (max-width: 600px) { .cabecalho-conteudo { padding: 20px; } .inventario { padding: 20px 20px 48px; } .filtro-status { width: 100%; } .filtro-status select { width: 100%; } .busca { min-width: 0; width: 100%; flex-basis: 100%; } .apresentacao { padding-left: 14px; } }
</style>
