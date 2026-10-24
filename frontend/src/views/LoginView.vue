<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Package, Layers, ClipboardList, Eye, EyeOff } from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'

const router = useRouter()
const autenticacao = useAutenticacaoStore()
const login = ref('')
const senha = ref('')
const senhaVisivel = ref(false)
const enviando = ref(false)
const erroFormulario = ref('')
const mensagemErro = computed(() => erroFormulario.value || autenticacao.erroSessao)

async function entrar() {
  if (enviando.value) return
  erroFormulario.value = ''
  if (!login.value.trim() || !senha.value) {
    erroFormulario.value = 'Informe seu login e sua senha.'
    return
  }

  enviando.value = true
  try {
    await autenticacao.entrar(login.value.trim(), senha.value)
    await router.replace({ name: 'ambientes' })
    if (!autenticacao.autenticado && !autenticacao.erroSessao) {
      erroFormulario.value = 'Não foi possível manter sua sessão. Tente entrar novamente.'
    }
  } catch (erro) {
    erroFormulario.value = erro instanceof Error ? erro.message : 'Não foi possível entrar. Tente novamente.'
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <main class="pagina-login">
    <section class="painel-identidade" aria-label="STOCKA — patrimônio e operação">
      <MarcaStocka />

      <div class="identidade-conteudo">
        <div class="modulos" aria-hidden="true">
          <span><Package :size="28" :stroke-width="1.5" /></span>
          <span><Layers :size="28" :stroke-width="1.5" /></span>
          <span><ClipboardList :size="28" :stroke-width="1.5" /></span>
        </div>
        <p class="identidade-categoria">Patrimônio e operação</p>
        <h2>Cada ativo<br> no seu lugar.</h2>
        <p class="identidade-descricao">Organize seus recursos e acompanhe os serviços.</p>
      </div>

      <p class="identidade-rodape">Ativos · Estoque · Ordens de serviço</p>
    </section>

    <section class="painel-acesso" aria-labelledby="titulo-login">
      <div class="acesso-conteudo">
        <span class="acesso-detalhe" aria-hidden="true"></span>
        <h1 id="titulo-login">Entrar no STOCKA</h1>
        <p class="acesso-descricao">Use suas credenciais de acesso.</p>

        <form class="formulario-login" :aria-busy="enviando" @submit.prevent="entrar">
          <div class="campo">
            <label for="login">E-mail ou login</label>
            <input id="login" v-model="login" name="login" type="text" autocomplete="username"
              autocapitalize="none" :spellcheck="false" placeholder="Seu identificador de acesso"
              :disabled="enviando" :aria-describedby="mensagemErro ? 'erro-login' : undefined" required>
          </div>

          <div class="campo">
            <label for="senha">Senha</label>
            <div class="campo-senha">
              <input id="senha" v-model="senha" name="senha" :type="senhaVisivel ? 'text' : 'password'"
                autocomplete="current-password" placeholder="Sua senha" :disabled="enviando"
                :aria-describedby="mensagemErro ? 'erro-login' : undefined" required>
              <button class="alternar-senha" type="button" :disabled="enviando"
                :aria-label="senhaVisivel ? 'Ocultar senha' : 'Mostrar senha'"
                :aria-pressed="senhaVisivel" aria-controls="senha" @click="senhaVisivel = !senhaVisivel">
                <EyeOff v-if="senhaVisivel" :size="20" aria-hidden="true" />
                <Eye v-else :size="20" aria-hidden="true" />
              </button>
            </div>
          </div>

          <p v-if="mensagemErro" id="erro-login" class="mensagem-erro erro-login" role="alert">{{ mensagemErro }}</p>
          <button class="botao-principal" type="submit" :disabled="enviando">{{ enviando ? 'Entrando…' : 'Entrar' }}</button>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.pagina-login {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  min-height: 100svh;
}

.painel-identidade {
  display: flex;
  flex-direction: column;
  gap: 48px;
  padding: clamp(32px, 5.5vw, 80px);
  background: var(--cor-painel);
  border-right: 1px solid var(--cor-borda);
}

.identidade-conteudo { margin-block: auto; padding-block: 20px; }
.modulos { display: flex; align-items: center; gap: 12px; margin-bottom: 36px; min-height: 120px; }
.modulos span {
  display: grid;
  place-items: center;
  width: 76px;
  height: 92px;
  border: 1px solid var(--cor-borda);
  border-radius: var(--raio-campo);
  background: var(--cor-superficie);
}
.modulos span:nth-child(2) { transform: translateY(-18px); border-top: 3px solid var(--cor-principal); }
.identidade-categoria { margin: 0 0 16px; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.16em; }
.identidade-conteudo h2 { margin: 0; font-size: clamp(2.15rem, 3.5vw, 3.5rem); font-weight: 650; line-height: 1.12; letter-spacing: -0.04em; }
.identidade-descricao { max-width: 290px; margin: 22px 0 0; color: var(--cor-texto-secundario); }
.identidade-rodape { margin: 0; font-size: 0.8rem; color: var(--cor-texto-secundario); }

.painel-acesso { display: flex; align-items: center; justify-content: center; padding: 56px 32px; }
.acesso-conteudo { width: 100%; max-width: 360px; }
.acesso-detalhe { display: block; width: 36px; height: 3px; margin-bottom: 24px; background: var(--cor-principal); }
.acesso-conteudo h1 { margin: 0; font-size: clamp(1.65rem, 2.5vw, 2rem); line-height: 1.2; letter-spacing: -0.025em; font-weight: 600; }
.acesso-descricao { margin: 12px 0 32px; color: var(--cor-texto-secundario); }
.formulario-login { display: grid; gap: 22px; }
.campo label { display: block; margin-bottom: 8px; font-size: 0.9rem; font-weight: 600; }
.campo input { width: 100%; height: 48px; padding: 0 12px; border: 1px solid var(--cor-borda); border-radius: var(--raio-campo); background: var(--cor-superficie); color: var(--cor-texto); }
.campo input::placeholder { color: var(--cor-texto-secundario); opacity: 1; }
.campo-senha { position: relative; }
.campo-senha input { padding-right: 50px; }
.alternar-senha { position: absolute; right: 2px; top: 2px; display: grid; place-items: center; width: 44px; height: 44px; padding: 0; border: 0; border-radius: var(--raio-campo); color: var(--cor-texto-secundario); background: transparent; }
.erro-login { margin: 0; padding: 12px; border-left: 3px solid var(--cor-erro); background: var(--cor-fundo-erro); font-size: 0.9rem; }

@media (max-width: 700px) {
  .pagina-login { grid-template-columns: 1fr; }
  .painel-identidade { gap: 28px; padding: 28px; border-right: 0; border-bottom: 1px solid var(--cor-borda); }
  .identidade-conteudo { padding: 0; }
  .identidade-conteudo h2 { font-size: 2rem; }
  .identidade-conteudo h2 br { display: none; }
  .modulos, .identidade-rodape { display: none; }
  .identidade-descricao { margin-top: 14px; }
  .painel-acesso { padding: 36px 28px 48px; }
}
</style>
