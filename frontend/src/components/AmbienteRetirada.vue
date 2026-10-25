<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import type { Ambiente } from '@/types/ambiente'
import { retirarAmbiente, ErroAmbiente } from '@/services/ambientes'

const props = defineProps<{ ambiente: Ambiente }>()
const emit = defineEmits<{ fechar: []; retirado: []; atualizar: []; acessoPerdido: [] }>()
const dialogo = ref<HTMLDialogElement>()
const enviando = ref(false)
const erro = ref('')
const atualizar = ref(false)
async function confirmar() {
  if (enviando.value || atualizar.value) return
  enviando.value = true; erro.value = ''
  try { await retirarAmbiente(props.ambiente); emit('retirado') }
  catch (f) {
    if (f instanceof ErroAmbiente && [401, 403].includes(f.status)) emit('acessoPerdido')
    else {
      erro.value = f instanceof Error ? f.message : 'Não foi possível retirar o ambiente.'
      atualizar.value = f instanceof ErroAmbiente && (f.codigo === 'AMBIENTE_ALTERADO' || f.status === 404)
    }
  } finally { enviando.value = false }
}
onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka" aria-labelledby="titulo-retirada" @cancel.prevent="!enviando && emit('fechar')">
    <h2 id="titulo-retirada">Retirar ambiente de uso?</h2>
    <p><strong>{{ ambiente.nome }}</strong> será desativado e deixará de aparecer no hub. O cadastro e as atribuições serão preservados.</p>
    <p>A operação será bloqueada se houver itens localizados neste ambiente ou equipamentos sob sua responsabilidade, inclusive itens excluídos logicamente.</p>
    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
    <button v-if="atualizar" class="botao-secundario" @click="emit('atualizar')">Atualizar ambientes</button>
    <footer class="dialogo-acoes">
      <button class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar retirada</button>
      <button class="botao-principal perigo" :disabled="enviando || atualizar" @click="confirmar">{{ enviando ? 'Retirando…' : 'Confirmar retirada' }}</button>
    </footer>
  </dialog>
</template>

<style scoped>
h2 { font-size: 1.3rem; }p { font-size: .9rem; line-height: 1.6; overflow-wrap: anywhere; }.perigo { background: #9c352d; }
</style>
