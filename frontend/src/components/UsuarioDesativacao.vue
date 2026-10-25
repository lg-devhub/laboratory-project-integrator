<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { desativarUsuario, ErroGestaoUsuario } from '@/services/usuarios'
import type { UsuarioGestao } from '@/types/gestaoUsuario'
const props = defineProps<{ conta: UsuarioGestao; propria: boolean }>()
const emit = defineEmits<{ fechar: []; salvo: [conta: UsuarioGestao]; atualizar: [conta: UsuarioGestao]; acessoPerdido: [erro: ErroGestaoUsuario] }>()
const dialogo = ref<HTMLDialogElement>()
const enviando = ref(false)
const erro = ref('')
const atual = ref<UsuarioGestao | null>(null)
async function confirmar() {
  if (enviando.value || atual.value) return
  enviando.value = true; erro.value = ''
  try { emit('salvo', await desativarUsuario(props.conta)) }
  catch (f) {
    if (f instanceof ErroGestaoUsuario && [401, 403].includes(f.status)) emit('acessoPerdido', f)
    else { erro.value = f instanceof Error ? f.message : 'Não foi possível desativar.'; if (f instanceof ErroGestaoUsuario && f.usuarioAtual) atual.value = f.usuarioAtual }
  } finally { enviando.value = false }
}
onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka" aria-labelledby="titulo-desativacao" @cancel.prevent="!enviando && emit('fechar')">
    <h2 id="titulo-desativacao">Desativar usuário?</h2>
    <p><strong>{{ conta.nome }}</strong> ({{ conta.login }}) perderá o acesso. Os registros e a autoria anteriores serão preservados.</p>
    <p v-if="propria">Você está desativando sua própria conta e sua sessão será encerrada.</p>
    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
    <button v-if="atual" class="botao-secundario" @click="emit('atualizar', atual)">Revisar dados atuais</button>
    <footer class="dialogo-acoes"><button class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar desativação</button><button class="botao-principal perigo" :disabled="enviando || !!atual" @click="confirmar">{{ enviando ? 'Desativando…' : 'Confirmar desativação' }}</button></footer>
  </dialog>
</template>

<style scoped>
h2 { font-size: 1.3rem; }p { line-height: 1.5; font-size: .9rem; overflow-wrap: anywhere; }.perigo { background: #9c352d; }
</style>
