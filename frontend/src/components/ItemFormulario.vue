<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { X } from '@lucide/vue'
import type { Ambiente } from '@/types/ambiente'
import { statusItens, type Item, type ItemDados, type SetorOpcao } from '@/types/item'
import { cadastrarItem, editarItem, listarSetores, ErroInventario } from '@/services/itens'

const props = defineProps<{ ambiente: Ambiente; item: Item | null }>()
const emit = defineEmits<{ fechar: []; salvo: [item: Item]; sincronizado: [item: Item]; acessoPerdido: [erro: ErroInventario] }>()
const dialogo = ref<HTMLDialogElement>()
const inputFoto = ref<HTMLInputElement>()
const baseItem = ref(props.item)
const dados = ref<ItemDados>(campos(props.item))
const setores = ref<SetorOpcao[]>([])
const carregandoSetores = ref(true)
const erroSetores = ref('')
const enviando = ref(false)
const lendoFoto = ref(false)
const foto = ref<{ base64: string; tipo: string }>()
const erroFoto = ref('')
const erro = ref('')
const conflito = ref<Item | null>(null)
const removerFoto = ref(false)
const ajudaFoto = computed(() => foto.value ? 'O arquivo escolhido substituirá a foto ao salvar.'
  : removerFoto.value ? 'A foto atual será removida ao salvar.'
    : baseItem.value?.temFoto ? 'Sem escolher outro arquivo, a foto atual é mantida.' : '')

function campos(item: Item | null): ItemDados {
  return item ? { nome: item.nome, categoria: item.categoria, formaRegistro: item.formaRegistro,
    equipamento: item.equipamento, quantidade: item.quantidade, status: item.status,
    numeroPatrimonio: item.numeroPatrimonio, setorResponsavelId: item.setorResponsavelId,
    descricao: item.descricao, localizacao: item.localizacao }
    : { nome: '', categoria: '', formaRegistro: 'INDIVIDUAL', equipamento: false,
      quantidade: 1, status: 'ATIVO', numeroPatrimonio: null, setorResponsavelId: null, descricao: '', localizacao: '' }
}

function carregarVersaoAtual() {
  if (!conflito.value) return
  baseItem.value = conflito.value
  dados.value = campos(conflito.value)
  emit('sincronizado', conflito.value)
  conflito.value = null
  foto.value = undefined
  removerFoto.value = false
  erro.value = ''
  erroFoto.value = ''
  if (inputFoto.value) inputFoto.value.value = ''
}

watch(removerFoto, remover => {
  if (remover) { foto.value = undefined; erroFoto.value = ''; if (inputFoto.value) inputFoto.value.value = '' }
})

watch(() => dados.value.formaRegistro, forma => {
  if (forma === 'INDIVIDUAL') dados.value.quantidade = 1
  else dados.value.equipamento = false
})
watch(() => dados.value.equipamento, equipamento => {
  if (!equipamento) { dados.value.numeroPatrimonio = null; dados.value.setorResponsavelId = null }
  else if (props.ambiente.tipo === 'SETOR' && setores.value.some(s => s.id === props.ambiente.id)) dados.value.setorResponsavelId = props.ambiente.id
})

async function carregarSetores() {
  carregandoSetores.value = true
  erroSetores.value = ''
  try { setores.value = await listarSetores() }
  catch (falha) {
    if (falha instanceof ErroInventario && falha.status === 401) emit('acessoPerdido', falha)
    else erroSetores.value = falha instanceof Error ? falha.message : 'Não foi possível carregar os setores.'
  } finally { carregandoSetores.value = false }
}

async function selecionarFoto(evento: Event) {
  const input = evento.target as HTMLInputElement
  const arquivo = input.files?.[0]
  foto.value = undefined
  erroFoto.value = ''
  if (!arquivo) return
  removerFoto.value = false
  if (!['image/png', 'image/jpeg'].includes(arquivo.type) || arquivo.size > 2097152) {
    erroFoto.value = 'Selecione uma foto PNG ou JPEG de até 2 MB.'
    input.value = ''
    return
  }
  lendoFoto.value = true
  try {
    const base64 = await new Promise<string>((resolve, reject) => {
      const leitor = new FileReader()
      leitor.onload = () => resolve(String(leitor.result).split(',')[1] || '')
      leitor.onerror = () => reject(new Error('Não foi possível ler a foto.'))
      leitor.readAsDataURL(arquivo)
    })
    foto.value = { base64, tipo: arquivo.type }
  } catch { erroFoto.value = 'Não foi possível ler a foto.' }
  finally { lendoFoto.value = false }
}

async function salvar() {
  if (enviando.value || lendoFoto.value || erroFoto.value || conflito.value) return
  enviando.value = true
  erro.value = ''
  try {
    if (baseItem.value) {
      const { nome, categoria, quantidade, status, descricao, localizacao } = dados.value
      emit('salvo', await editarItem(props.ambiente.id, baseItem.value.id, {
        nome, categoria, quantidade, status, descricao, localizacao, versao: baseItem.value.versao,
        fotoAcao: foto.value ? 'SUBSTITUIR' : removerFoto.value ? 'REMOVER' : 'MANTER',
        fotoBase64: foto.value?.base64 ?? null, fotoTipo: foto.value?.tipo ?? null,
      }))
    } else emit('salvo', await cadastrarItem(props.ambiente.id, dados.value, foto.value))
  }
  catch (falha) {
    if (falha instanceof ErroInventario && [401, 404].includes(falha.status)) emit('acessoPerdido', falha)
    else {
      erro.value = falha instanceof Error ? falha.message : 'Não foi possível salvar o item.'
      if (falha instanceof ErroInventario && falha.codigo === 'ITEM_ALTERADO' && falha.itemAtual) {
        conflito.value = falha.itemAtual
        emit('sincronizado', falha.itemAtual)
      }
    }
  } finally { enviando.value = false }
}

onMounted(() => { dialogo.value?.showModal(); if (!baseItem.value) void carregarSetores(); else carregandoSetores.value = false })
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka formulario-item" aria-labelledby="titulo-item" @cancel.prevent="!enviando && !lendoFoto && emit('fechar')">
    <form @submit.prevent="salvar" :aria-busy="enviando || lendoFoto">
      <header class="dialogo-cabecalho">
        <div><p class="dialogo-categoria">{{ ambiente.nome }}</p><h2 id="titulo-item">{{ baseItem ? 'Editar item' : 'Cadastrar item' }}</h2></div>
        <button type="button" class="botao-icone" aria-label="Fechar formulário de item" :disabled="enviando || lendoFoto" @click="emit('fechar')"><X :size="20" /></button>
      </header>
      <fieldset class="dialogo-campos" :disabled="enviando || lendoFoto">
        <div class="campo"><label for="item-nome">Nome *</label><input id="item-nome" v-model="dados.nome" required maxlength="120" autofocus></div>
        <div class="dupla">
          <div class="campo"><label for="item-categoria">Categoria *</label><input id="item-categoria" v-model="dados.categoria" required maxlength="120" placeholder="Ex.: Ferramentas"></div>
          <div class="campo"><label for="item-forma">Forma de registro</label><select id="item-forma" v-model="dados.formaRegistro" :disabled="!!baseItem"><option value="INDIVIDUAL">Individual</option><option value="QUANTIDADE">Por quantidade</option></select></div>
        </div>
        <label v-if="dados.formaRegistro === 'INDIVIDUAL'" class="opcao-equipamento"><input v-model="dados.equipamento" type="checkbox" :disabled="!!baseItem"> É um equipamento com controle patrimonial</label>
        <p v-else class="ajuda">O status se aplica ao conjunto. OS e manutenções por unidade são exclusivas de equipamentos individuais.</p>
        <div class="dupla">
          <div class="campo"><label for="item-quantidade">Quantidade *</label><input id="item-quantidade" v-model.number="dados.quantidade" type="number" min="1" max="2147483647" step="1" required :readonly="dados.formaRegistro === 'INDIVIDUAL'"></div>
          <div class="campo"><label for="item-status">Status</label><select id="item-status" v-model="dados.status"><option v-for="status in statusItens" :key="status.valor" :value="status.valor">{{ status.nome }}</option></select></div>
        </div>
        <div v-if="baseItem" class="identificacao-fixa">
          <p v-if="baseItem.equipamento"><strong>Patrimônio:</strong> {{ baseItem.numeroPatrimonio }}<br><strong>Setor responsável:</strong> {{ baseItem.setorResponsavelNome }}</p>
          <p class="ajuda">A identificação e a forma de registro são preservadas nesta edição. Mudanças de ambiente e responsabilidade serão feitas pela movimentação administrativa.</p>
        </div>
        <template v-if="dados.equipamento && !baseItem">
          <div class="campo"><label for="item-patrimonio">Número de patrimônio *</label><input id="item-patrimonio" v-model="dados.numeroPatrimonio" required maxlength="120"></div>
          <div class="campo"><label for="item-responsavel">Setor responsável *</label>
            <select id="item-responsavel" v-model="dados.setorResponsavelId" required :disabled="carregandoSetores || !!erroSetores"><option :value="null" disabled>{{ carregandoSetores ? 'Carregando setores…' : 'Selecione um setor' }}</option><option v-for="setor in setores" :key="setor.id" :value="setor.id">{{ setor.nome }}</option></select>
            <p v-if="erroSetores" class="mensagem-erro" role="alert">{{ erroSetores }}</p>
            <button v-if="erroSetores" type="button" class="botao-secundario" @click="carregarSetores">Tentar carregar setores</button>
            <p v-else-if="!carregandoSetores && !setores.length" class="ajuda">É necessário que um administrador cadastre um setor antes de registrar equipamentos.</p>
          </div>
        </template>
        <div class="campo"><label for="item-localizacao">Detalhamento da localização <span>(opcional)</span></label><input id="item-localizacao" v-model="dados.localizacao" maxlength="300" placeholder="Ex.: Armário 2, prateleira B"></div>
        <div class="campo"><label for="item-descricao">Descrição <span>(opcional)</span></label><textarea id="item-descricao" v-model="dados.descricao" maxlength="2000" rows="3" /></div>
        <div class="campo"><label for="item-foto">{{ baseItem?.temFoto ? 'Substituir foto' : 'Foto' }} <span>(opcional)</span></label><input id="item-foto" ref="inputFoto" type="file" accept="image/png,image/jpeg" @change="selecionarFoto"><p class="ajuda">PNG ou JPEG, até 2 MB e 10 megapixels. {{ ajudaFoto }}</p></div>
        <label v-if="baseItem?.temFoto" class="opcao-equipamento"><input v-model="removerFoto" type="checkbox"> Remover a foto atual</label>
      </fieldset>
      <p v-if="erroFoto" class="mensagem-erro" role="alert">{{ erroFoto }}</p>
      <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
      <div v-if="conflito" class="aviso-conflito">
        <p>Seu preenchimento foi mantido e não foi gravado. Carregar a versão atual substituirá os campos deste formulário pelos dados mais recentes.</p>
        <button type="button" class="botao-secundario" @click="carregarVersaoAtual">Carregar versão atual</button>
      </div>
      <footer class="dialogo-acoes">
        <button type="button" class="botao-secundario" :disabled="enviando || lendoFoto" @click="emit('fechar')">Cancelar</button>
        <button class="botao-principal" :disabled="enviando || lendoFoto || !!erroFoto || !!conflito || (!baseItem && dados.equipamento && (carregandoSetores || !!erroSetores || !setores.length))">{{ enviando ? 'Salvando…' : lendoFoto ? 'Lendo foto…' : baseItem ? 'Salvar alterações' : 'Cadastrar item' }}</button>
      </footer>
    </form>
  </dialog>
</template>

<style scoped>
.formulario-item { width: min(660px, calc(100% - 32px)); }
.dupla { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.campo { min-width: 0; }
.campo span, .ajuda { font-weight: 400; font-size: .8rem; color: var(--cor-texto-secundario); }
.ajuda { margin: 6px 0 0; }
.opcao-equipamento { display: flex; align-items: start; gap: 10px; font-size: .9rem; }
.opcao-equipamento input { flex-shrink: 0; width: 18px; height: 18px; accent-color: var(--cor-principal); }
input[type='file'] { font-size: .85rem; }
input[readonly] { background: var(--cor-fundo); }
.identificacao-fixa { font-size: .85rem; overflow-wrap: anywhere; }
.identificacao-fixa p { margin: 0 0 8px; }
.aviso-conflito { font-size: .85rem; color: var(--cor-texto-secundario); }
@media (max-width: 520px) { .dupla { grid-template-columns: 1fr; } }
</style>
