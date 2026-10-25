<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Plus } from '@lucide/vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import { listarManutencoes, registrarManutencao } from '@/services/manutencoes'
import { listarOrdensServico } from '@/services/ordensServico'
import { ErroInventario } from '@/services/itens'
import { statusOS, type OrdemServico } from '@/types/ordemServico'
import type { Item } from '@/types/item'
import type { Manutencao, ManutencaoDados } from '@/types/manutencao'

const props = defineProps<{ item: Item; bloqueado: boolean }>()
const emit = defineEmits<{ enviando: [valor: boolean]; acessoPerdido: [erro: ErroInventario] }>()
const autenticacao = useAutenticacaoStore()
const registros = ref<Manutencao[]>([])
const ordens = ref<OrdemServico[]>([])
const carregando = ref(true)
const formulario = ref(false)
const carregandoOS = ref(false)
const erroOS = ref('')
const erro = ref('')
const erroFormulario = ref('')
const sucesso = ref('')
const enviando = ref(false)
function campos(): ManutencaoDados {
  return { dataRealizacao: '', descricaoServico: '', responsavelTecnico: '', ordemServicoId: null }
}
const dados = ref(campos())
let ativo = true
function falha(f: unknown) {
  if (f instanceof ErroInventario && [401, 404].includes(f.status)) emit('acessoPerdido', f)
  return f instanceof Error ? f.message : 'Não foi possível concluir a operação.'
}
function data(valor: string) { const [ano, mes, dia] = valor.split('-'); return dia + '/' + mes + '/' + ano }
async function carregar() {
  carregando.value = true; erro.value = ''
  try { const lista = await listarManutencoes(props.item.ambienteId, props.item.id); if (ativo) registros.value = lista }
  catch (f) { if (ativo) erro.value = falha(f) }
  finally { if (ativo) carregando.value = false }
}
async function carregarOS() {
  carregandoOS.value = true; erroOS.value = ''
  try { const lista = await listarOrdensServico(props.item.ambienteId, props.item.id); if (ativo) ordens.value = lista }
  catch (f) { if (ativo) erroOS.value = falha(f) }
  finally { if (ativo) carregandoOS.value = false }
}
function cancelar() { formulario.value = false; dados.value = campos(); erroFormulario.value = '' }
function abrir() { formulario.value = true; sucesso.value = ''; void carregarOS() }
async function salvar() {
  if (enviando.value || props.bloqueado || props.item.excluidoEm) return
  enviando.value = true; emit('enviando', true); erroFormulario.value = ''; sucesso.value = ''
  try {
    const registro = await registrarManutencao(props.item.ambienteId, props.item.id, dados.value)
    if (!ativo) return
    registros.value.push(registro)
    registros.value.sort((a, b) => b.dataRealizacao.localeCompare(a.dataRealizacao) || b.id - a.id)
    cancelar(); sucesso.value = 'Manutenção registrada no histórico.'
  } catch (f) { if (ativo) erroFormulario.value = falha(f) }
  finally { enviando.value = false; emit('enviando', false) }
}
onMounted(carregar)
onBeforeUnmount(() => { ativo = false })
</script>

<template>
  <section class="manutencoes" aria-labelledby="titulo-manutencoes">
    <div class="titulo"><h3 id="titulo-manutencoes">Histórico de manutenção</h3><button v-if="!formulario && !item.excluidoEm" class="botao-secundario" :disabled="carregando || !!erro || bloqueado" @click="abrir"><Plus :size="16" aria-hidden="true" /> Registrar manutenção</button></div>
    <p v-if="sucesso" class="sucesso" role="status">{{ sucesso }}</p>
    <p v-if="carregando" role="status">Carregando manutenções…</p>
    <div v-else-if="erro"><p class="mensagem-erro" role="alert">{{ erro }}</p><button class="botao-secundario" @click="carregar">Tentar novamente</button></div>
    <template v-else>
      <form v-if="formulario" class="formulario-manutencao" @submit.prevent="salvar" :aria-busy="enviando">
        <p class="ajuda">Informe o serviço realizado e o técnico responsável. O lançamento será registrado por {{ autenticacao.usuario?.nome }}.</p>
        <fieldset class="dialogo-campos" :disabled="enviando || bloqueado">
          <div class="campo"><label for="manutencao-data">Data de realização</label><input id="manutencao-data" v-model="dados.dataRealizacao" type="date" required></div>
          <div class="campo"><label for="manutencao-descricao">Descrição do serviço</label><textarea id="manutencao-descricao" v-model="dados.descricaoServico" rows="4" required maxlength="2000"></textarea></div>
          <div class="campo"><label for="manutencao-tecnico">Responsável técnico</label><input id="manutencao-tecnico" v-model="dados.responsavelTecnico" required maxlength="120"></div>
          <div class="campo"><label for="manutencao-os">Ordem de serviço (opcional)</label><select id="manutencao-os" v-model="dados.ordemServicoId" :disabled="carregandoOS || !!erroOS"><option :value="null">Sem vínculo com OS</option><option v-for="o in ordens" :key="o.id" :value="o.id">OS #{{ o.id }} · {{ statusOS[o.status] }}</option></select><p class="ajuda">Apenas OS deste equipamento. O registro não altera a fase da OS nem o status do equipamento.</p></div>
        </fieldset>
        <p v-if="carregandoOS" class="ajuda" role="status">Consultando OS para vínculo opcional…</p>
        <div v-if="erroOS"><p class="mensagem-erro" role="alert">{{ erroOS }} Você pode registrar sem vínculo.</p><button type="button" class="botao-secundario" :disabled="enviando || bloqueado" @click="carregarOS">Tentar carregar OS</button></div>
        <p v-if="erroFormulario" class="mensagem-erro" role="alert">{{ erroFormulario }}</p>
        <footer><button type="button" class="botao-secundario" :disabled="enviando" @click="cancelar">Cancelar manutenção</button><button class="botao-principal" :disabled="enviando || bloqueado">{{ enviando ? 'Registrando…' : 'Salvar manutenção' }}</button></footer>
      </form>
      <p v-if="!registros.length" class="ajuda">Nenhuma manutenção registrada para este equipamento.</p>
      <ol v-else class="registros-manutencao">
        <li v-for="m in registros" :key="m.id">
          <div class="data-servico"><time :datetime="m.dataRealizacao">{{ data(m.dataRealizacao) }}</time><span>{{ m.ordemServicoId ? 'OS #' + m.ordemServicoId : 'Sem vínculo com OS' }}</span></div>
          <p class="servico">{{ m.descricaoServico }}</p>
          <dl><div><dt>Responsável técnico</dt><dd>{{ m.responsavelTecnico }}</dd></div><div><dt>Registrado por</dt><dd>{{ m.usuarioNome }}<span class="ajuda">{{ new Date(m.registradoEm).toLocaleString('pt-BR') }}</span></dd></div></dl>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped>
.manutencoes { border-top: 1px solid var(--cor-borda); padding-top: 22px; margin-top: 28px; }
.titulo { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
h3 { margin: 0; font-size: 1rem; font-weight: 600; }
.titulo button { gap: 6px; }
.ajuda { color: var(--cor-texto-secundario); font-size: .85rem; line-height: 1.5; }
.formulario-manutencao { border: 1px solid var(--cor-borda); border-radius: 6px; padding: 16px; margin-bottom: 20px; }
.formulario-manutencao > .ajuda { margin-top: 0; }
footer { display: flex; justify-content: flex-end; gap: 12px; flex-wrap: wrap; margin-top: 20px; }
.registros-manutencao { list-style: none; padding: 0; }
.registros-manutencao li { padding: 18px 0 18px 16px; border-left: 3px solid var(--cor-principal); border-bottom: 1px solid var(--cor-borda); margin-top: 12px; }
.data-servico { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; font-size: .85rem; }
.data-servico time { font-weight: 600; }.data-servico span { color: var(--cor-texto-secundario); }
.servico { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.5; font-size: .9rem; }
dl { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; font-size: .85rem; margin-bottom: 0; }
dt { color: var(--cor-texto-secundario); margin-bottom: 4px; }dd { margin: 0; overflow-wrap: anywhere; }dd span { display: block; }
.sucesso { color: #30523b; background: #eef3ed; border-left: 3px solid #426a51; padding: 10px; }
@media (max-width: 420px) { dl { grid-template-columns: 1fr; } footer > button { flex: 1; } }
</style>
