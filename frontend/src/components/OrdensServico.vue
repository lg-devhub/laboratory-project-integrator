<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Plus } from '@lucide/vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import { abrirOrdemServico, listarOrdensServico } from '@/services/ordensServico'
import { ErroInventario } from '@/services/itens'
import { prioridadesOS, statusOS, type OrdemServico, type OrdemServicoDados } from '@/types/ordemServico'
import OrdemServicoEdicao from './OrdemServicoEdicao.vue'
import type { Item } from '@/types/item'

const props = defineProps<{ item: Item; bloqueado?: boolean }>()
const emit = defineEmits<{ acessoPerdido: [erro: ErroInventario]; enviando: [valor: boolean]; alterada: [] }>()
const autenticacao = useAutenticacaoStore()
const ordens = ref<OrdemServico[]>([])
const carregando = ref(true)
const erro = ref('')
const erroFormulario = ref('')
const sucesso = ref('')
const formularioAberto = ref(false)
const editando = ref<number | null>(null)
const enviando = ref(false)
const dados = ref<OrdemServicoDados>({ problema: '', prioridade: 'MEDIA' })
let ativo = true
const contagens = computed(() => ({
  ABERTA: ordens.value.filter(o => o.status === 'ABERTA').length,
  EM_ANDAMENTO: ordens.value.filter(o => o.status === 'EM_ANDAMENTO').length,
  CONCLUIDA: ordens.value.filter(o => o.status === 'CONCLUIDA').length,
}))

function sincronizar(ordem: OrdemServico) {
  const indice = ordens.value.findIndex(o => o.id === ordem.id)
  if (indice >= 0) ordens.value[indice] = ordem
  emit('alterada')
}
function atualizada(ordem: OrdemServico) {
  sincronizar(ordem); editando.value = null
  sucesso.value = ordem.status === 'CONCLUIDA' ? `OS #${ordem.id} concluída.` : `OS #${ordem.id} atualizada.`
}
function envioEdicao(valor: boolean) { enviando.value = valor; emit('enviando', valor) }

function tratarErro(falha: unknown): string {
  if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) emit('acessoPerdido', falha)
  return falha instanceof Error ? falha.message : 'Não foi possível concluir a operação.'
}
function data(valor: string) { return new Date(valor).toLocaleString('pt-BR') }
function prioridade(valor: string) { return prioridadesOS.find(p => p.valor === valor)?.nome || valor }
async function carregar() {
  carregando.value = true; erro.value = ''
  try {
    const resultado = await listarOrdensServico(props.item.ambienteId, props.item.id)
    if (ativo) ordens.value = resultado
  } catch (falha) { if (ativo) erro.value = tratarErro(falha) }
  finally { if (ativo) carregando.value = false }
}
function cancelar() {
  formularioAberto.value = false; erroFormulario.value = ''
  dados.value = { problema: '', prioridade: 'MEDIA' }
}
async function salvar() {
  if (enviando.value || props.bloqueado || props.item.excluidoEm) return
  enviando.value = true; emit('enviando', true); erroFormulario.value = ''; sucesso.value = ''
  try {
    const ordem = await abrirOrdemServico(props.item.ambienteId, props.item.id, dados.value)
    if (!ativo) return
    emit('alterada')
    ordens.value.unshift(ordem); cancelar(); sucesso.value = `OS #${ordem.id} aberta.`
  } catch (falha) { if (ativo) erroFormulario.value = tratarErro(falha) }
  finally { enviando.value = false; emit('enviando', false) }
}
onMounted(carregar)
onBeforeUnmount(() => { ativo = false })
</script>

<template>
  <section class="ordens-servico" aria-labelledby="titulo-ordens">
    <div class="titulo-ordens"><h3 id="titulo-ordens">Ordens de serviço</h3><button v-if="!formularioAberto && !item.excluidoEm" class="botao-secundario" :disabled="carregando || !!erro || editando !== null || enviando || bloqueado" @click="formularioAberto = true; sucesso = ''"><Plus :size="16" aria-hidden="true" /> Abrir OS</button></div>
    <p v-if="sucesso" class="sucesso-os" role="status">{{ sucesso }}</p>
    <div v-if="!carregando && !erro" class="contadores-os" aria-label="Quantidade de OS por status"><span v-for="(nome, chave) in statusOS" :key="chave" class="status-os" :class="'os-' + chave.toLowerCase()">{{ contagens[chave] }} · {{ nome }}</span></div>
    <p v-if="carregando" role="status">Carregando ordens de serviço…</p>
    <div v-else-if="erro"><p class="mensagem-erro" role="alert">{{ erro }}</p><button class="botao-secundario" @click="carregar">Tentar novamente</button></div>
    <template v-else>
      <form v-if="formularioAberto" class="formulario-os" @submit.prevent="salvar" :aria-busy="enviando">
        <p class="ajuda">Solicitante: <strong>{{ autenticacao.usuario?.nome }}</strong>. A OS será aberta com status Aberta e a data do registro.</p>
        <fieldset class="dialogo-campos" :disabled="enviando || bloqueado">
          <div class="campo"><label for="os-problema">Descrição do problema</label><textarea id="os-problema" v-model="dados.problema" rows="4" maxlength="2000" required></textarea></div>
          <div class="campo"><label for="os-prioridade">Prioridade</label><select id="os-prioridade" v-model="dados.prioridade"><option v-for="p in prioridadesOS" :key="p.valor" :value="p.valor">{{ p.nome }}</option></select></div>
        </fieldset>
        <p v-if="erroFormulario" class="mensagem-erro" role="alert">{{ erroFormulario }}</p>
        <div class="acoes-os"><button type="button" class="botao-secundario" :disabled="enviando || bloqueado" @click="cancelar">Cancelar OS</button><button class="botao-principal" :disabled="enviando || bloqueado">{{ enviando ? 'Abrindo…' : 'Confirmar abertura' }}</button></div>
      </form>
      <p v-if="!ordens.length" class="ajuda">Nenhuma ordem de serviço para este equipamento.</p>
      <details v-for="ordem in ordens" :key="ordem.id" class="ordem">
        <summary><strong>OS #{{ ordem.id }}</strong><span class="status-os" :class="`os-${ordem.status.toLowerCase()}`">{{ statusOS[ordem.status] }}</span><span class="prioridade-os" :class="'prioridade-' + ordem.prioridade.toLowerCase()">Prioridade {{ prioridade(ordem.prioridade).toLowerCase() }}</span></summary>
        <dl><div><dt>Solicitante</dt><dd>{{ ordem.solicitanteNome }}</dd></div><div><dt>Abertura</dt><dd><time :datetime="ordem.abertaEm">{{ data(ordem.abertaEm) }}</time></dd></div><div class="problema"><dt>Problema relatado</dt><dd>{{ ordem.problema }}</dd></div></dl>
        <dl v-if="ordem.status === 'CONCLUIDA'"><div><dt>Fechamento</dt><dd><time v-if="ordem.fechadaEm" :datetime="ordem.fechadaEm">{{ data(ordem.fechadaEm) }}</time></dd></div><div class="problema"><dt>Atendimento realizado</dt><dd>{{ ordem.descricaoAtendimento }}</dd></div></dl>
        <OrdemServicoEdicao v-if="editando === ordem.id" :item="item" :ordem="ordem" :bloqueado="bloqueado" @fechar="editando = null" @salva="atualizada" @sincronizada="sincronizar" @enviando="envioEdicao" @acesso-perdido="emit('acessoPerdido', $event)" />
        <button v-else-if="ordem.status !== 'CONCLUIDA' && !item.excluidoEm" class="botao-secundario atualizar-os" :disabled="editando !== null || formularioAberto || enviando || bloqueado" @click="editando = ordem.id; sucesso = ''">Atualizar OS</button>
        <p v-else class="ajuda">{{ item.excluidoEm ? 'Equipamento excluído. OS disponível somente para consulta.' : 'OS concluída, disponível somente para consulta.' }}</p>
      </details>
    </template>
  </section>
</template>

<style scoped>
.ordens-servico { border-top: 1px solid var(--cor-borda); margin-top: 28px; padding-top: 22px; }
.titulo-ordens { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
h3 { margin: 0; font-size: 1rem; font-weight: 600; }
.titulo-ordens button { gap: 6px; }
.ajuda { font-size: .85rem; color: var(--cor-texto-secundario); line-height: 1.5; }
.formulario-os { padding: 16px; border: 1px solid var(--cor-borda); border-radius: 6px; margin-bottom: 20px; }
.formulario-os > .ajuda { margin-top: 0; }
.acoes-os { display: flex; justify-content: flex-end; gap: 12px; flex-wrap: wrap; margin-top: 20px; }
.ordem { border: 1px solid var(--cor-borda); border-radius: 6px; padding: 14px; margin-top: 12px; }
summary { cursor: pointer; font-size: .85rem; overflow-wrap: anywhere; }
summary > span { display: inline-block; margin: 4px 0 4px 10px; }
.status-os { background: #fbf1dd; color: #795320; padding: 3px 8px; border-radius: 4px; }
.contadores-os { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.prioridade-os { padding: 3px 8px; border-radius: 4px; background: var(--cor-painel); }
.prioridade-alta { color: #943c26; background: #f9eae5; }
.prioridade-media { color: #795320; background: #fbf1dd; }
.os-em_andamento { background: #eaf0f7; color: #376582; }
.os-concluida { background: #eef3ed; color: #30523b; }
.atualizar-os { margin-top: 20px; }
dl { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 20px 0 0; font-size: .85rem; }
dt { color: var(--cor-texto-secundario); margin-bottom: 4px; }
dd { margin: 0; overflow-wrap: anywhere; white-space: pre-wrap; }
.problema { grid-column: 1 / -1; }
.sucesso-os { color: #30523b; border-left: 3px solid #426a51; padding: 10px; background: #eef3ed; }
@media (max-width: 420px) { dl { grid-template-columns: 1fr; } .acoes-os > button { flex: 1; } }
</style>
