<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { X, Check } from '@lucide/vue'
import { coresAmbiente, type Ambiente, type CorAmbiente } from '@/types/ambiente'
import { salvarAmbiente, ErroAmbiente } from '@/services/ambientes'

const props = defineProps<{ ambiente: Ambiente | null }>()
const emit = defineEmits<{ fechar: []; salvo: [ambiente: Ambiente]; sessaoExpirada: [] }>()
const dialogo = ref<HTMLDialogElement>()
const nome = ref(props.ambiente?.nome || '')
const tipo = ref<'SALA' | 'SETOR'>(props.ambiente?.tipo || 'SALA')
const descricao = ref(props.ambiente?.descricao || '')
const cor = ref<CorAmbiente>(props.ambiente?.cor || 'TERRACOTA')
const enviando = ref(false)
const erro = ref('')

async function salvar() {
  if (enviando.value) return
  enviando.value = true
  erro.value = ''
  try {
    const ambiente = await salvarAmbiente({ nome: nome.value, tipo: tipo.value,
      descricao: descricao.value, cor: cor.value }, props.ambiente?.id)
    emit('salvo', ambiente)
  } catch (falha) {
    if (falha instanceof ErroAmbiente && falha.status === 401) emit('sessaoExpirada')
    else erro.value = falha instanceof Error ? falha.message : 'Não foi possível salvar o ambiente.'
  } finally { enviando.value = false }
}

onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka" aria-labelledby="titulo-ambiente"
    @cancel.prevent="!enviando && emit('fechar')">
    <form @submit.prevent="salvar" :aria-busy="enviando">
      <header class="dialogo-cabecalho">
        <div><p class="dialogo-categoria">Organização dos ambientes</p>
          <h2 id="titulo-ambiente">{{ ambiente ? 'Editar ambiente' : 'Novo ambiente' }}</h2></div>
        <button type="button" class="botao-icone" aria-label="Fechar formulário" :disabled="enviando" @click="emit('fechar')"><X :size="20" /></button>
      </header>
      <fieldset class="dialogo-campos" :disabled="enviando">
        <div class="campo"><label for="ambiente-nome">Nome <span aria-hidden="true">*</span></label>
          <input id="ambiente-nome" v-model="nome" required maxlength="120" autofocus placeholder="Ex.: Sala de manutenção"></div>
        <div class="campo"><label for="ambiente-tipo">Tipo</label>
          <select id="ambiente-tipo" v-model="tipo"><option value="SALA">Sala</option><option value="SETOR">Setor</option></select></div>
        <div class="campo"><label for="ambiente-descricao">Descrição <span class="opcional">(opcional)</span></label>
          <textarea id="ambiente-descricao" v-model="descricao" maxlength="2000" rows="3" placeholder="Uma breve descrição para identificar este ambiente." /></div>
        <fieldset class="seletor-cores"><legend>Cor de identificação</legend>
          <div class="cores">
            <label v-for="opcao in coresAmbiente" :key="opcao.valor" class="opcao-cor" :style="{ '--cor-amostra': opcao.hex }">
              <input v-model="cor" type="radio" name="cor-ambiente" :value="opcao.valor">
              <span class="amostra"><Check v-if="cor === opcao.valor" :size="18" aria-hidden="true" /></span>
              <span>{{ opcao.nome }}</span>
            </label>
          </div>
        </fieldset>
      </fieldset>
      <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
      <footer class="dialogo-acoes">
        <button type="button" class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar</button>
        <button class="botao-principal" :disabled="enviando">{{ enviando ? 'Salvando…' : 'Salvar ambiente' }}</button>
      </footer>
    </form>
  </dialog>
</template>

<style scoped>
.opcional { font-weight: 400; color: var(--cor-texto-secundario); }
.seletor-cores { border: 0; padding: 0; margin: 0; }
.seletor-cores legend { margin-bottom: 12px; font-size: .9rem; font-weight: 600; }
.cores { display: flex; flex-wrap: wrap; gap: 14px; }
.opcao-cor { position: relative; display: grid; justify-items: center; gap: 6px; cursor: pointer; font-size: .75rem; }
.opcao-cor input { position: absolute; width: 36px; height: 36px; margin: 0; opacity: 0; cursor: pointer; }
.amostra { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 50%; background: var(--cor-amostra); color: white; }
.opcao-cor input:focus-visible + .amostra { outline: 3px solid var(--cor-texto); outline-offset: 3px; }
.opcao-cor input:checked + .amostra { box-shadow: 0 0 0 3px var(--cor-superficie), 0 0 0 4px var(--cor-amostra); }
</style>
