<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { X } from '@lucide/vue'
import { salvarUsuario, ErroGestaoUsuario } from '@/services/usuarios'
import type { UsuarioDados, UsuarioGestao } from '@/types/gestaoUsuario'

const props = defineProps<{ conta: UsuarioGestao | null; propria: boolean }>()
const emit = defineEmits<{ fechar: []; salvo: [conta: UsuarioGestao]; sincronizada: [conta: UsuarioGestao]; acessoPerdido: [erro: ErroGestaoUsuario] }>()
const dialogo = ref<HTMLDialogElement>()
const base = ref(props.conta)
function campos(conta: UsuarioGestao | null): UsuarioDados {
  return { nome: conta?.nome || '', login: conta?.login || '', perfil: conta?.perfil || 'USUARIO', senha: '', versao: conta?.versao }
}
const dados = ref(campos(props.conta))
const confirmacao = ref('')
const enviando = ref(false)
const erro = ref('')
const conflito = ref<UsuarioGestao | null>(null)
function carregarAtual() {
  if (!conflito.value) return
  base.value = conflito.value; dados.value = campos(conflito.value); confirmacao.value = ''
  emit('sincronizada', conflito.value); conflito.value = null; erro.value = ''
}
async function salvar() {
  if (enviando.value || conflito.value) return
  erro.value = ''
  if (dados.value.senha && dados.value.senha !== confirmacao.value) { erro.value = 'A confirmação da senha não confere.'; return }
  enviando.value = true
  try { emit('salvo', await salvarUsuario(base.value?.id ?? null, dados.value)); dados.value.senha = ''; confirmacao.value = '' }
  catch (f) {
    if (f instanceof ErroGestaoUsuario && [401, 403].includes(f.status)) emit('acessoPerdido', f)
    else { erro.value = f instanceof Error ? f.message : 'Não foi possível salvar.'; if (f instanceof ErroGestaoUsuario && f.usuarioAtual) conflito.value = f.usuarioAtual }
  } finally { enviando.value = false }
}
onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => { dados.value.senha = ''; confirmacao.value = ''; dialogo.value?.close() })
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka" aria-labelledby="titulo-usuario" @cancel.prevent="!enviando && emit('fechar')">
    <form @submit.prevent="salvar" :aria-busy="enviando">
      <header class="dialogo-cabecalho"><h2 id="titulo-usuario">{{ base ? 'Editar usuário' : 'Cadastrar usuário' }}</h2><button type="button" class="botao-icone" aria-label="Fechar formulário de usuário" :disabled="enviando" @click="emit('fechar')"><X :size="20" /></button></header>
      <p v-if="propria" class="ajuda">Você está editando sua própria conta. Alterar o perfil pode retirar seu acesso administrativo.</p>
      <p v-if="base && !base.ativo" class="ajuda">Conta desativada. Editar seus dados não reativa o acesso.</p>
      <fieldset class="dialogo-campos" :disabled="enviando">
        <div class="campo"><label for="usuario-nome">Nome</label><input id="usuario-nome" v-model="dados.nome" required maxlength="120" autofocus autocomplete="off"></div>
        <div class="campo"><label for="usuario-login">E-mail/login</label><input id="usuario-login" v-model="dados.login" required maxlength="254" autocomplete="off"></div>
        <div class="campo"><label for="usuario-perfil">Perfil</label><select id="usuario-perfil" v-model="dados.perfil"><option value="USUARIO">Usuário</option><option value="ADMINISTRADOR">Administrador</option></select></div>
        <div class="campo"><label for="usuario-senha">{{ base ? 'Nova senha (opcional)' : 'Senha inicial' }}</label><input id="usuario-senha" v-model="dados.senha" type="password" :required="!base" minlength="8" autocomplete="new-password" aria-describedby="regra-senha-usuario"><p id="regra-senha-usuario" class="ajuda">Pelo menos 8 caracteres, com letra e número; até 72 bytes em UTF-8. {{ base ? 'Deixe vazio para manter a senha atual.' : '' }}</p></div>
        <div v-if="!base || dados.senha" class="campo"><label for="usuario-confirmacao">Confirmar senha</label><input id="usuario-confirmacao" v-model="confirmacao" type="password" required autocomplete="new-password"></div>
      </fieldset>
      <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
      <div v-if="conflito"><p class="ajuda">Carregar a versão atual substituirá os campos e limpará a senha digitada.</p><button type="button" class="botao-secundario" @click="carregarAtual">Carregar versão atual</button></div>
      <footer class="dialogo-acoes"><button type="button" class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar</button><button class="botao-principal" :disabled="enviando || !!conflito">{{ enviando ? 'Salvando…' : 'Salvar usuário' }}</button></footer>
    </form>
  </dialog>
</template>

<style scoped>
.ajuda { color: var(--cor-texto-secundario); font-size: .85rem; line-height: 1.5; margin: 10px 0; }
</style>
