<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { X } from '@lucide/vue'
import { alterarSenha } from '@/services/autenticacao'

const emit = defineEmits<{ fechar: []; salvo: []; sessaoExpirada: [] }>()
const dialogo = ref<HTMLDialogElement>()
const senhaAtual = ref('')
const novaSenha = ref('')
const confirmacao = ref('')
const enviando = ref(false)
const erro = ref('')

async function salvar() {
  if (enviando.value) return
  erro.value = ''
  if (novaSenha.value !== confirmacao.value) { erro.value = 'A confirmação da nova senha não confere.'; return }
  enviando.value = true
  try {
    const sucesso = await alterarSenha(senhaAtual.value, novaSenha.value, confirmacao.value)
    senhaAtual.value = ''; novaSenha.value = ''; confirmacao.value = ''
    if (sucesso) emit('salvo')
    else emit('sessaoExpirada')
  } catch (falha) {
    erro.value = falha instanceof Error ? falha.message : 'Não foi possível alterar a senha.'
  } finally { enviando.value = false }
}

onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => { senhaAtual.value = ''; novaSenha.value = ''; confirmacao.value = ''; dialogo.value?.close() })
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka alterar-senha" aria-labelledby="titulo-senha" @cancel.prevent="!enviando && emit('fechar')">
    <form @submit.prevent="salvar" :aria-busy="enviando">
      <header class="dialogo-cabecalho">
        <h2 id="titulo-senha">Alterar minha senha</h2>
        <button type="button" class="botao-icone" aria-label="Fechar alteração de senha" :disabled="enviando" @click="emit('fechar')"><X :size="20" /></button>
      </header>
      <fieldset class="dialogo-campos" :disabled="enviando">
        <div class="campo"><label for="senha-atual">Senha atual</label><input id="senha-atual" v-model="senhaAtual" type="password" autocomplete="current-password" required autofocus></div>
        <div class="campo"><label for="nova-senha">Nova senha</label><input id="nova-senha" v-model="novaSenha" type="password" autocomplete="new-password" minlength="8" required aria-describedby="regra-senha"><p id="regra-senha" class="ajuda">Pelo menos 8 caracteres, com uma letra e um número. Limite técnico: 72 bytes em UTF-8; caracteres acentuados podem ocupar mais de um byte.</p></div>
        <div class="campo"><label for="confirmacao-senha">Confirmar nova senha</label><input id="confirmacao-senha" v-model="confirmacao" type="password" autocomplete="new-password" required></div>
      </fieldset>
      <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
      <footer class="dialogo-acoes"><button type="button" class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar</button><button class="botao-principal" :disabled="enviando">{{ enviando ? 'Salvando…' : 'Salvar senha' }}</button></footer>
    </form>
  </dialog>
</template>

<style scoped>
.alterar-senha { width: min(480px, calc(100% - 32px)); }
h2 { margin: 0; font-size: 1.3rem; }
.ajuda { font-size: .8rem; color: var(--cor-texto-secundario); margin: 8px 0 0; }
</style>
