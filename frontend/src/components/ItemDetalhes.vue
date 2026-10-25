<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { X, Pencil } from '@lucide/vue'
import { statusItens, type Item, type ItemHistorico } from '@/types/item'
import { consultarHistorico, consultarFoto, ErroInventario } from '@/services/itens'
import OrdensServico from '@/components/OrdensServico.vue'
import Manutencoes from '@/components/Manutencoes.vue'
import ItemExclusao from '@/components/ItemExclusao.vue'

const props = defineProps<{ item: Item; ambienteNome: string }>()
const emit = defineEmits<{ fechar: []; editar: [item: Item]; acessoPerdido: [erro: ErroInventario]; osAlterada: []; excluido: [item: Item]; atualizado: [item: Item] }>()
const dialogo = ref<HTMLDialogElement>()
const historico = ref<ItemHistorico[]>([])
const carregando = ref(true)
const erro = ref('')
const fotoUrl = ref('')
const erroFoto = ref('')
const enviandoOS = ref(false)
const enviandoManutencao = ref(false)
const confirmandoExclusao = ref(false)
const enviandoExclusao = ref(false)
const ocupado = computed(() => enviandoOS.value || enviandoManutencao.value || enviandoExclusao.value)
let ativo = true

function data(valor: string) { return new Date(valor).toLocaleString('pt-BR') }
function acao(valor: string) {
  return ({ CADASTRO: 'Cadastro do item', EDICAO: 'Edição do item', ALTERACAO_STATUS: 'Alteração de status', EXCLUSAO: 'Exclusão do item' } as Record<string, string>)[valor] || valor
}
function status(valor: string) { return statusItens.find(s => s.valor === valor)?.nome || valor }

async function carregarHistorico() {
  carregando.value = true
  erro.value = ''
  try { historico.value = await consultarHistorico(props.item.ambienteId, props.item.id) }
  catch (falha) {
    if (!ativo) return
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) emit('acessoPerdido', falha)
    else erro.value = falha instanceof Error ? falha.message : 'Não foi possível consultar o histórico.'
  } finally { carregando.value = false }
}

async function carregarFoto() {
  if (!props.item.temFoto) return
  try {
    const foto = await consultarFoto(props.item.ambienteId, props.item.id)
    if (ativo) fotoUrl.value = URL.createObjectURL(foto)
  } catch (falha) {
    if (!ativo) return
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) emit('acessoPerdido', falha)
    else erroFoto.value = 'Não foi possível carregar a foto.'
  }
}

onMounted(() => { dialogo.value?.showModal(); void carregarHistorico(); void carregarFoto() })
onBeforeUnmount(() => { ativo = false; dialogo.value?.close(); if (fotoUrl.value) URL.revokeObjectURL(fotoUrl.value) })
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka detalhes-item" aria-labelledby="titulo-detalhes" @cancel.prevent="!ocupado && emit('fechar')">
    <header class="dialogo-cabecalho">
      <div><p class="dialogo-categoria">{{ ambienteNome }} · Registro #{{ item.id }}</p><h2 id="titulo-detalhes">{{ item.nome }}</h2></div>
      <button type="button" class="botao-icone" aria-label="Fechar detalhes" autofocus :disabled="ocupado" @click="emit('fechar')"><X :size="20" /></button>
    </header>
    <img v-if="fotoUrl" :src="fotoUrl" :alt="`Foto de ${item.nome}`" class="foto-item">
    <p v-if="erroFoto" class="mensagem-erro" role="alert">{{ erroFoto }}</p>
    <p v-if="item.excluidoEm" class="aviso-excluido" role="status">Excluído em {{ data(item.excluidoEm) }}. Dados preservados para consulta; o status abaixo é o registrado antes da exclusão.</p>
    <dl class="informacoes">
      <div><dt>Categoria</dt><dd>{{ item.categoria }}</dd></div>
      <div><dt>Status</dt><dd>{{ statusItens.find(s => s.valor === item.status)?.nome }}</dd></div>
      <div><dt>Forma de registro</dt><dd>{{ item.equipamento ? 'Equipamento individual' : item.formaRegistro === 'INDIVIDUAL' ? 'Item individual' : 'Por quantidade' }}</dd></div>
      <div><dt>Quantidade</dt><dd>{{ item.quantidade }}</dd></div>
      <template v-if="item.equipamento">
        <div><dt>Patrimônio</dt><dd>{{ item.numeroPatrimonio }}</dd></div>
        <div><dt>Setor responsável</dt><dd>{{ item.setorResponsavelNome }}</dd></div>
      </template>
      <div><dt>Ambiente atual</dt><dd>{{ ambienteNome }}</dd></div>
      <div><dt>Detalhamento da localização</dt><dd>{{ item.localizacao || 'Não informado' }}</dd></div>
      <div class="descricao"><dt>Descrição</dt><dd>{{ item.descricao || 'Não informada' }}</dd></div>
    </dl>
    <OrdensServico v-if="item.equipamento" :item="item" :bloqueado="enviandoManutencao || confirmandoExclusao" @enviando="enviandoOS = $event" @alterada="emit('osAlterada')" @acesso-perdido="emit('acessoPerdido', $event)" />
    <Manutencoes v-if="item.equipamento" :item="item" :bloqueado="enviandoOS || confirmandoExclusao" @enviando="enviandoManutencao = $event" @acesso-perdido="emit('acessoPerdido', $event)" />
    <section class="historico" aria-labelledby="titulo-historico">
      <h3 id="titulo-historico">Histórico de alterações</h3>
      <p class="ajuda">Auditoria automática das alterações no cadastro do item, com autoria e data.</p>
      <p v-if="carregando" role="status">Carregando histórico…</p>
      <template v-else-if="erro"><p class="mensagem-erro" role="alert">{{ erro }}</p><button class="botao-secundario" @click="carregarHistorico">Tentar novamente</button></template>
      <ol v-else class="registros">
        <li v-for="registro in historico" :key="registro.id"><strong>{{ acao(registro.acao) }}</strong><span v-if="registro.statusAnterior && registro.statusNovo">{{ status(registro.statusAnterior) }} → {{ status(registro.statusNovo) }}</span><span>{{ registro.usuarioNome }}</span><time :datetime="registro.dataHora">{{ data(registro.dataHora) }}</time></li>
      </ol>
    </section>
    <ItemExclusao v-if="confirmandoExclusao && !item.excluidoEm" :item="item" @fechar="confirmandoExclusao = false" @enviando="enviandoExclusao = $event" @excluido="emit('excluido', $event)" @atualizado="emit('atualizado', $event)" @acesso-perdido="emit('acessoPerdido', $event)" />
    <footer class="dialogo-acoes"><button class="botao-secundario" :disabled="ocupado" @click="emit('fechar')">Fechar</button><template v-if="!item.excluidoEm"><button class="botao-secundario excluir" :disabled="ocupado || confirmandoExclusao" @click="confirmandoExclusao = true">Excluir item</button><button class="botao-principal" :disabled="ocupado || confirmandoExclusao" @click="emit('editar', item)"><Pencil :size="16" aria-hidden="true" /> Editar item</button></template></footer>
  </dialog>
</template>

<style scoped>
.aviso-excluido { font-size: .85rem; padding: 12px; background: var(--cor-painel); border-left: 3px solid var(--cor-texto-secundario); line-height: 1.5; }
.excluir { color: #9c352d; border-color: #d8ada6; }
.detalhes-item { width: min(680px, calc(100% - 32px)); }
.dialogo-cabecalho h2 { overflow-wrap: anywhere; }
.foto-item { display: block; max-width: 100%; max-height: 250px; object-fit: contain; margin: 0 auto 24px; }
.informacoes { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 0; }
.informacoes > div { min-width: 0; overflow-wrap: anywhere; }
dt { color: var(--cor-texto-secundario); font-size: .8rem; margin-bottom: 4px; }
dd { margin: 0; white-space: pre-line; font-size: .95rem; }
.descricao { grid-column: 1 / -1; }
.historico { border-top: 1px solid var(--cor-borda); margin-top: 28px; padding-top: 22px; }
h3 { margin: 0 0 8px; font-size: 1rem; font-weight: 600; }
.ajuda { margin: 0 0 20px; color: var(--cor-texto-secundario); font-size: .8rem; }
.registros { list-style: none; margin: 0; padding: 0; }
.registros li { display: grid; gap: 3px; padding-left: 14px; border-left: 3px solid var(--cor-principal); margin-bottom: 16px; font-size: .85rem; }
.registros time { color: var(--cor-texto-secundario); font-size: .8rem; }
.botao-principal { gap: 8px; }
@media (max-width: 420px) { .informacoes { grid-template-columns: 1fr; } }
</style>
