<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { X, Users } from '@lucide/vue'
import type { Ambiente, AcessoAmbiente } from '@/types/ambiente'
import { listarAcessos, salvarAcessos, ErroAmbiente } from '@/services/ambientes'

const props = defineProps<{ ambiente: Ambiente }>()
const emit = defineEmits<{ fechar: []; salvo: []; sessaoExpirada: [] }>()
const dialogo = ref<HTMLDialogElement>()
const usuarios = ref<AcessoAmbiente[]>([])
const selecionados = ref<number[]>([])
const carregando = ref(true)
const enviando = ref(false)
const erro = ref('')
const erroCarga = ref(false)

function informarFalha(falha: unknown) {
  if (falha instanceof ErroAmbiente && falha.status === 401) emit('sessaoExpirada')
  else erro.value = falha instanceof Error ? falha.message : 'Não foi possível atualizar os acessos.'
}

async function carregar() {
  carregando.value = true
  erro.value = ''
  erroCarga.value = false
  try {
    usuarios.value = await listarAcessos(props.ambiente.id)
    selecionados.value = usuarios.value.filter(u => u.atribuido).map(u => u.id)
  } catch (falha) { erroCarga.value = true; informarFalha(falha) }
  finally { carregando.value = false }
}

async function salvar() {
  if (enviando.value || carregando.value || erroCarga.value) return
  enviando.value = true
  erro.value = ''
  try { await salvarAcessos(props.ambiente.id, selecionados.value); emit('salvo') }
  catch (falha) { informarFalha(falha) }
  finally { enviando.value = false }
}

onMounted(() => { dialogo.value?.showModal(); void carregar() })
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog ref="dialogo" class="dialogo-stocka" aria-labelledby="titulo-acessos" aria-describedby="explicacao-acessos"
    @cancel.prevent="!enviando && emit('fechar')">
    <form @submit.prevent="salvar" :aria-busy="carregando || enviando">
      <header class="dialogo-cabecalho">
        <div><p class="dialogo-categoria">{{ ambiente.nome }}</p><h2 id="titulo-acessos">Gerenciar acessos</h2></div>
        <button type="button" class="botao-icone" aria-label="Fechar acessos" :disabled="enviando" @click="emit('fechar')"><X :size="20" /></button>
      </header>
      <p id="explicacao-acessos" class="explicacao">Marque quem pode acessar este ambiente. Administradores já têm acesso a todos os ambientes.</p>
      <p v-if="carregando" role="status">Carregando usuários…</p>
      <div v-else-if="erroCarga"><p class="mensagem-erro" role="alert">{{ erro }}</p>
        <button type="button" class="botao-secundario" @click="carregar">Tentar novamente</button></div>
      <fieldset v-else class="lista-usuarios" :disabled="enviando">
        <legend class="visualmente-oculto">Usuários com acesso</legend>
        <p v-if="!usuarios.length" class="explicacao"><Users :size="20" aria-hidden="true" /> Nenhum usuário disponível para atribuição.</p>
        <label v-for="usuario in usuarios" :key="usuario.id" class="usuario-opcao">
          <input v-model="selecionados" type="checkbox" :value="usuario.id">
          <span><strong>{{ usuario.nome }}</strong><span class="usuario-login">{{ usuario.login }}</span>
            <span v-if="!usuario.ativo" class="aviso-inativo">Conta inativa: esta atribuição não libera acesso.</span></span>
        </label>
      </fieldset>
      <p v-if="!carregando && !erroCarga" class="resumo" role="status">{{ selecionados.length }} selecionado(s). Desmarcar revoga o acesso; salvar sem seleção revoga todos os acessos de usuários a este ambiente.</p>
      <p v-if="erro && !erroCarga" class="mensagem-erro" role="alert">{{ erro }}</p>
      <footer class="dialogo-acoes">
        <button type="button" class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar</button>
        <button class="botao-principal" :disabled="carregando || enviando || erroCarga">{{ enviando ? 'Salvando…' : 'Salvar acessos' }}</button>
      </footer>
    </form>
  </dialog>
</template>

<style scoped>
.explicacao, .resumo { color: var(--cor-texto-secundario); font-size: .9rem; }
.lista-usuarios { padding: 0; margin: 24px 0 16px; border: 1px solid var(--cor-borda); border-radius: 6px; max-height: 290px; overflow-y: auto; }
.usuario-opcao { display: flex; align-items: start; gap: 14px; padding: 16px; cursor: pointer; overflow-wrap: anywhere; }
.usuario-opcao + .usuario-opcao { border-top: 1px solid var(--cor-borda); }
.usuario-opcao input { width: 18px; height: 18px; flex-shrink: 0; margin-top: 3px; accent-color: var(--cor-principal); }
.usuario-opcao strong { font-size: .9rem; font-weight: 600; }
.usuario-login, .aviso-inativo { display: block; color: var(--cor-texto-secundario); font-size: .8rem; }
.aviso-inativo { margin-top: 6px; }
.resumo { font-size: .8rem; }
</style>
