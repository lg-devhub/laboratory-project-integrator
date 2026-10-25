<script setup lang="ts">
import { computed, ref } from 'vue'
import { atualizarOrdemServico, ConflitoOrdemServico } from '@/services/ordensServico'
import { ErroInventario } from '@/services/itens'
import { prioridadesOS, statusOS, type OrdemServico, type OrdemServicoEdicao } from '@/types/ordemServico'
import type { Item } from '@/types/item'

const props = defineProps<{ item: Item; ordem: OrdemServico }>()
const emit = defineEmits<{ fechar: []; salva: [ordem: OrdemServico]; sincronizada: [ordem: OrdemServico]; enviando: [valor: boolean]; acessoPerdido: [erro: ErroInventario] }>()
const base = ref(props.ordem)
function campos(ordem: OrdemServico): OrdemServicoEdicao {
  return { problema: ordem.problema, prioridade: ordem.prioridade, status: ordem.status,
    descricaoAtendimento: ordem.descricaoAtendimento, versao: ordem.versao }
}
const dados = ref(campos(props.ordem))
const enviando = ref(false)
const erro = ref('')
const conflito = ref<OrdemServico | null>(null)
const proximosStatus = computed(() => base.value.status === 'ABERTA'
  ? ['ABERTA', 'EM_ANDAMENTO'] as const : ['EM_ANDAMENTO', 'CONCLUIDA'] as const)
function carregarAtual() {
  if (!conflito.value) return
  const atual = conflito.value
  emit('sincronizada', atual)
  if (atual.status === 'CONCLUIDA') { emit('fechar'); return }
  base.value = atual; dados.value = campos(atual); conflito.value = null; erro.value = ''
}
async function salvar() {
  if (enviando.value || conflito.value) return
  enviando.value = true; emit('enviando', true); erro.value = ''
  try {
    const ordem = await atualizarOrdemServico(props.item.ambienteId, props.item.id, base.value.id, {
      ...dados.value, descricaoAtendimento: dados.value.status === 'CONCLUIDA' ? dados.value.descricaoAtendimento : null,
    })
    emit('salva', ordem)
  } catch (falha) {
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) emit('acessoPerdido', falha)
    else {
      erro.value = falha instanceof Error ? falha.message : 'Não foi possível atualizar a OS.'
      if (falha instanceof ConflitoOrdemServico) { conflito.value = falha.ordemAtual; emit('sincronizada', falha.ordemAtual) }
    }
  } finally { enviando.value = false; emit('enviando', false) }
}
</script>

<template>
  <form class="edicao-os" @submit.prevent="salvar" :aria-busy="enviando">
    <h4>Atualizar OS #{{ base.id }}</h4>
    <fieldset class="dialogo-campos" :disabled="enviando">
      <div class="campo"><label :for="`os-problema-${base.id}`">Problema relatado</label><textarea :id="`os-problema-${base.id}`" v-model="dados.problema" rows="3" required maxlength="2000"></textarea></div>
      <div class="campo"><label :for="`os-prioridade-${base.id}`">Prioridade da OS</label><select :id="`os-prioridade-${base.id}`" v-model="dados.prioridade"><option v-for="p in prioridadesOS" :key="p.valor" :value="p.valor">{{ p.nome }}</option></select></div>
      <div class="campo"><label :for="`os-status-${base.id}`">Status da OS</label><select :id="`os-status-${base.id}`" v-model="dados.status"><option v-for="s in proximosStatus" :key="s" :value="s">{{ statusOS[s] }}</option></select></div>
      <div v-if="dados.status === 'CONCLUIDA'" class="campo"><label :for="`os-atendimento-${base.id}`">Descrição do atendimento</label><textarea :id="`os-atendimento-${base.id}`" v-model="dados.descricaoAtendimento" rows="4" maxlength="2000" required></textarea><p class="ajuda">Ao concluir, a data de fechamento será registrada e esta OS ficará disponível somente para consulta.</p></div>
    </fieldset>
    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
    <div v-if="conflito" class="conflito-os"><p>Seu rascunho foi mantido. Carregar a versão atual substituirá os campos deste formulário.</p><button type="button" class="botao-secundario" @click="carregarAtual">{{ conflito.status === 'CONCLUIDA' ? 'Consultar OS concluída' : 'Carregar versão atual' }}</button></div>
    <footer><button type="button" class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar atualização</button><button class="botao-principal" :disabled="enviando || !!conflito">{{ enviando ? 'Salvando…' : dados.status === 'CONCLUIDA' ? 'Concluir OS' : 'Salvar OS' }}</button></footer>
  </form>
</template>

<style scoped>
.edicao-os { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--cor-borda); }
h4 { margin: 0 0 16px; font-weight: 600; }
.ajuda, .conflito-os { color: var(--cor-texto-secundario); font-size: .85rem; line-height: 1.5; }
.conflito-os { padding: 12px; background: var(--cor-painel); }
footer { display: flex; justify-content: flex-end; gap: 12px; flex-wrap: wrap; margin-top: 20px; }
@media (max-width: 420px) { footer > button { flex: 1; } }
</style>
