<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Package,
  Layers,
  ClipboardList,
  Mail,
  Lock,
  ArrowRight,
  Eye,
  EyeOff,
  HelpCircle,
  X
} from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'

import slideEquipe from '@/assets/images/slide-equipe.jpg'
import slideGalpao from '@/assets/images/slide-galpao.jpg'
import slideEstoque from '@/assets/images/slide-estoque.jpg'

const router = useRouter()
const autenticacao = useAutenticacaoStore()

const login = ref('')
const senha = ref('')
const senhaVisivel = ref(false)
const enviando = ref(false)
const erroFormulario = ref('')
const mostrarAjudaSenha = ref(false)

const mensagemErro = computed(() => erroFormulario.value || autenticacao.erroSessao)

interface SlideItem {
  id: string
  modulo: string
  icone: any
  categoria: string
  titulo: string
  descricao: string
  imagem: string
  alt: string
}

/* Dados dos slides da coluna esquerda */
const slides: SlideItem[] = [
  {
    id: 'patrimonio',
    modulo: 'PATRIMÔNIO',
    icone: Package,
    categoria: 'PATRIMÔNIO E GESTÃO',
    titulo: 'Cada ativo\nno seu lugar.',
    descricao: 'Organize seus recursos e acompanhe os serviços com mais eficiência.',
    imagem: slideGalpao,
    alt: 'Galpão organizado de estoque e patrimônio'
  },
  {
    id: 'operacao',
    modulo: 'OPERAÇÃO',
    icone: Layers,
    categoria: 'PATRIMÔNIO E OPERAÇÃO',
    titulo: 'Cada ativo\nno seu lugar.',
    descricao: 'Organize seus recursos e acompanhe os serviços com mais eficiência.',
    imagem: slideEquipe,
    alt: 'Equipe de operações e planejamento integrada'
  },
  {
    id: 'controle',
    modulo: 'CONTROLE',
    icone: ClipboardList,
    categoria: 'ESTOQUE E CONTROLE',
    titulo: 'Cada ativo\nno seu lugar.',
    descricao: 'Organize seus recursos e acompanhe os serviços com mais eficiência.',
    imagem: slideEstoque,
    alt: 'Armazém com controle rigoroso de insumos e equipamentos'
  }
]

// Inicia com o slide de Estoque/Armazém ou Operação selecionado
const slideAtual = ref(0)
const slideAtivo = computed<SlideItem>(() => slides[slideAtual.value] ?? (slides[0] as SlideItem))
let intervaloSlide: ReturnType<typeof setInterval> | null = null
const pausado = ref(false)

function iniciarTransicaoAutomatica() {
  pararTransicaoAutomatica()
  intervaloSlide = setInterval(() => {
    if (!pausado.value) {
      slideAtual.value = (slideAtual.value + 1) % slides.length
    }
  }, 6000)
}

function pararTransicaoAutomatica() {
  if (intervaloSlide) {
    clearInterval(intervaloSlide)
    intervaloSlide = null
  }
}

function selecionarSlide(indice: number) {
  slideAtual.value = indice
  iniciarTransicaoAutomatica()
}

onMounted(() => {
  iniciarTransicaoAutomatica()
})

onUnmounted(() => {
  pararTransicaoAutomatica()
})

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
    <!-- SLIDESHOW COM IMAGENS DE BACKGROUND E IDENTIDADE -->
    <section
      class="painel-slides"
      aria-label="Apresentação institucional do STOCKA"
      @mouseenter="pausado = true"
      @mouseleave="pausado = false"
    >
      <!-- Background slides com crossfade suave -->
      <div class="slides-container" aria-hidden="true">
        <div
          v-for="(slide, indice) in slides"
          :key="slide.id"
          class="slide-item"
          :class="{ ativo: indice === slideAtual }"
        >
          <img :src="slide.imagem" :alt="slide.alt" class="slide-imagem" />
        </div>
        <div class="slide-gradiente-sobreposicao"></div>
      </div>

      <!-- Conteúdo sobreposto aos slides -->
      <div class="slides-conteudo">
        <header class="slides-cabecalho">
          <MarcaStocka tema="claro" />
        </header>

        <div class="slides-corpo">
          <!-- Textos dinâmicos do slide (no mobile aparecem antes dos cards) -->
          <div class="slides-textos" role="region" aria-live="polite">
            <p class="slide-subtitulo">
              <span class="linha-traco" aria-hidden="true"></span>
              <span class="slide-subtitulo-texto">{{ slideAtivo.categoria }}</span>
            </p>
            <h2 class="slide-titulo">
              Cada ativo<br />no seu lugar.
            </h2>
            <p class="slide-descricao">
              {{ slideAtivo.descricao }}
            </p>
          </div>

          <!-- Modulos / Abas interativas que controlam os slides -->
          <div class="modulos-grade" role="tablist" aria-label="Módulos em destaque">
            <button
              v-for="(slide, indice) in slides"
              :key="slide.id"
              type="button"
              role="tab"
              class="modulo-card"
              :class="{ ativo: indice === slideAtual }"
              :aria-selected="indice === slideAtual"
              @click="selecionarSlide(indice)"
            >
              <div class="modulo-icone-wrapper">
                <component :is="slide.icone" :size="24" :stroke-width="1.7" />
              </div>
              <span class="modulo-nome">{{ slide.modulo }}</span>
            </button>
          </div>
        </div>

        <footer class="slides-rodape">
          <span class="linha-traco" aria-hidden="true"></span>
          <span>Ativos · Estoque · Ordens de serviço</span>
        </footer>
      </div>
    </section>

    <!-- PAINEL DE LOGIN -->
    <section class="painel-login" aria-labelledby="titulo-login">
      <!-- Decoração de linhas curvas desktop (canto superior direito) -->
      <svg
        class="decoracao-linhas decoracao-linhas-desktop"
        viewBox="0 0 320 260"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path d="M40 0C40 85 105 150 190 150H320" stroke="#ebdccf" stroke-width="1.5" />
        <path d="M110 0C110 110 180 180 290 180H320" stroke="#f0e2d5" stroke-width="1.5" />
        <path d="M180 0C180 135 245 200 320 200" stroke="#f3e8dd" stroke-width="1.5" />
      </svg>

      <!-- Decoração de linhas curvas mobile (canto inferior direito) -->
      <svg
        class="decoracao-linhas decoracao-linhas-mobile"
        viewBox="0 0 240 240"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path d="M240 40C130 40 40 130 40 240" stroke="#ebdccf" stroke-width="1.5" />
        <path d="M240 95C160 95 95 160 95 240" stroke="#f0e2d5" stroke-width="1.5" />
        <path d="M240 150C190 150 150 190 150 240" stroke="#f3e8dd" stroke-width="1.5" />
      </svg>

      <div class="login-conteudo">
        <div class="login-cabecalho">
          <span class="traco-destaque" aria-hidden="true"></span>
          <h1 id="titulo-login">Entrar no STOCKA</h1>
          <p class="login-subtitulo">Use suas credenciais de acesso.</p>
        </div>

        <form class="formulario-login" :aria-busy="enviando" @submit.prevent="entrar">
          <!-- Campo E-mail ou login -->
          <div class="campo-grupo">
            <label for="login">E-mail ou login</label>
            <div class="campo-input-wrapper">
              <span class="campo-icone" aria-hidden="true">
                <Mail :size="18" :stroke-width="1.8" />
              </span>
              <input
                id="login"
                v-model="login"
                name="login"
                type="text"
                autocomplete="username"
                autocapitalize="none"
                :spellcheck="false"
                placeholder="admin@stocka.local"
                :disabled="enviando"
                :aria-describedby="mensagemErro ? 'erro-login' : undefined"
                required
              />
            </div>
          </div>

          <!-- Campo Senha -->
          <div class="campo-grupo">
            <label for="senha">Senha</label>
            <div class="campo-input-wrapper">
              <span class="campo-icone" aria-hidden="true">
                <Lock :size="18" :stroke-width="1.8" />
              </span>
              <input
                id="senha"
                v-model="senha"
                name="senha"
                :type="senhaVisivel ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="Sua senha"
                :disabled="enviando"
                :aria-describedby="mensagemErro ? 'erro-login' : undefined"
                required
              />
              <button
                class="botao-alternar-senha"
                type="button"
                :disabled="enviando"
                :aria-label="senhaVisivel ? 'Ocultar senha' : 'Mostrar senha'"
                :aria-pressed="senhaVisivel"
                aria-controls="senha"
                @click="senhaVisivel = !senhaVisivel"
              >
                <EyeOff v-if="senhaVisivel" :size="18" aria-hidden="true" />
                <Eye v-else :size="18" aria-hidden="true" />
              </button>
            </div>
          </div>

          <!-- Mensagem de erro de login -->
          <p v-if="mensagemErro" id="erro-login" class="mensagem-erro erro-login" role="alert">
            {{ mensagemErro }}
          </p>

          <!-- Botão principal de entrada -->
          <button class="botao-entrar" type="submit" :disabled="enviando">
            <span>{{ enviando ? 'Entrando…' : 'Entrar' }}</span>
            <ArrowRight v-if="!enviando" :size="18" :stroke-width="2" aria-hidden="true" />
          </button>

          <!-- Link esqueceu sua senha -->
          <div class="acoes-auxiliares">
            <button
              type="button"
              class="link-esqueceu-senha"
              @click="mostrarAjudaSenha = true"
            >
              <Lock :size="13" :stroke-width="1.8" aria-hidden="true" />
              <span>Esqueceu sua senha?</span>
            </button>
          </div>
        </form>
      </div>

      <!-- Rodapé com marcação e versão -->
      <footer class="login-rodape">
        <span class="rodape-linha" aria-hidden="true"></span>
        <span class="rodape-versao">STOCKA v1.0</span>
        <span class="rodape-linha rodape-linha-direita" aria-hidden="true"></span>
      </footer>
    </section>

    <!-- Modal informativo para recuperação de senha -->
    <div
      v-if="mostrarAjudaSenha"
      class="modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-ajuda-senha"
      @click.self="mostrarAjudaSenha = false"
    >
      <div class="modal-dialogo">
        <div class="modal-topo">
          <div class="modal-titulo-wrap">
            <HelpCircle :size="20" class="modal-icone-ajuda" />
            <h2 id="titulo-ajuda-senha">Recuperação de Acesso</h2>
          </div>
          <button
            type="button"
            class="modal-fechar"
            aria-label="Fechar"
            @click="mostrarAjudaSenha = false"
          >
            <X :size="18" />
          </button>
        </div>
        <div class="modal-corpo">
          <p>
            Por políticas de segurança interna do <strong>STOCKA</strong>, a redefinição de credenciais
            deve ser efetuada diretamente por um <strong>Administrador do Sistema</strong>.
          </p>
          <p class="modal-dica">
            Solicite ao seu gestor ou administrador de TI para que emita uma nova senha temporária ou
            atualize seus acessos pelo painel administrativo.
          </p>
        </div>
        <div class="modal-rodape">
          <button
            type="button"
            class="botao-entendi"
            @click="mostrarAjudaSenha = false"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ==========================================================================
   LAYOUT GERAL DA PÁGINA
   ========================================================================== */
.pagina-login {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  min-height: 100svh;
  background-color: #faf8f5;
  color: #1e262f;
  font-family: inherit;
  overflow-x: hidden;
}

/* ==========================================================================
   LADO ESQUERDO: SLIDESHOW E IDENTIDADE VISUAL
   ========================================================================== */
.painel-slides {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 4.5vw, 64px);
  min-height: 100svh;
  overflow: hidden;
  user-select: none;
}

/* Container das imagens com transição de crossfade */
.slides-container {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}

.slide-item {
  position: absolute;
  inset: 0;
  opacity: 0;
  transform: scale(1.05);
  transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1), transform 6s ease-out;
  pointer-events: none;
}

.slide-item.ativo {
  opacity: 1;
  transform: scale(1);
  pointer-events: auto;
}

.slide-imagem {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

/* Camada de sobreposição para contraste refinado e quente */
.slide-gradiente-sobreposicao {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(14, 18, 22, 0.58) 0%,
    rgba(17, 21, 26, 0.45) 40%,
    rgba(12, 15, 18, 0.78) 100%
  );
  backdrop-filter: blur(1px);
}

/* Conteúdo sobreposto nos slides */
.slides-conteudo {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  gap: 36px;
}

.slides-cabecalho {
  display: flex;
  align-items: center;
}

.slides-corpo {
  margin-block: auto;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* ==========================================================================
   CARDS DE MÓDULOS / TABS DO SLIDESHOW
   ========================================================================== */
.modulos-grade {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  order: 1; /* Padrão Desktop: cards acima dos textos */
}

.modulo-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  text-align: center;
  transition: transform 0.25s ease, opacity 0.25s ease;
  color: #ffffff;
}

.modulo-icone-wrapper {
  display: grid;
  place-items: center;
  width: 74px;
  height: 74px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  color: #2b333a;
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.modulo-card:hover .modulo-icone-wrapper {
  background: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.24);
}

/* Estado ativo do card do slide: borda superior terracota como no mockup */
.modulo-card.ativo .modulo-icone-wrapper {
  background: #ffffff;
  transform: translateY(-6px);
  border-top: 4px solid var(--cor-principal, #b64c1d);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.28);
}

.modulo-nome {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.85);
  transition: color 0.2s ease;
}

.modulo-card.ativo .modulo-nome {
  color: #ffffff;
  font-weight: 800;
}

/* ==========================================================================
   TEXTOS DO SLIDE (TÍTULO, SUBTÍTULO E DESCRIÇÃO)
   ========================================================================== */
.slides-textos {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 480px;
  order: 2; /* Padrão Desktop: textos abaixo dos cards */
}

.slide-subtitulo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #ffffff;
}

.linha-traco {
  display: inline-block;
  width: 28px;
  height: 2px;
  background-color: var(--cor-principal, #b64c1d);
  border-radius: 2px;
}

.slide-titulo {
  margin: 0;
  font-size: clamp(2.4rem, 4.2vw, 3.8rem);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: #ffffff;
  text-shadow: 0 3px 12px rgba(0, 0, 0, 0.4);
}

.slide-descricao {
  margin: 6px 0 0;
  font-size: clamp(0.95rem, 1.3vw, 1.1rem);
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.88);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
  max-width: 420px;
}

.slides-rodape {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.04em;
}

/* ==========================================================================
   LADO DIREITO: PAINEL DE LOGIN
   ========================================================================== */
.painel-login {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: clamp(32px, 5vw, 64px);
  background-color: #fbf9f5;
  min-height: 100svh;
}

/* Decorações de linhas */
.decoracao-linhas {
  position: absolute;
  pointer-events: none;
  opacity: 0.85;
}

.decoracao-linhas-desktop {
  top: 0;
  right: 0;
  width: 260px;
  height: auto;
  display: block;
}

.decoracao-linhas-mobile {
  display: none;
}

.login-conteudo {
  width: 100%;
  max-width: 390px;
  margin-block: auto;
  padding: 24px 0;
  position: relative;
  z-index: 2;
}

.login-cabecalho {
  margin-bottom: 32px;
}

.traco-destaque {
  display: block;
  width: 36px;
  height: 3px;
  background-color: var(--cor-principal, #b64c1d);
  border-radius: 2px;
  margin-bottom: 22px;
}

.login-cabecalho h1 {
  margin: 0;
  font-size: clamp(1.8rem, 2.5vw, 2.25rem);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.025em;
  color: #1f2730;
}

.login-subtitulo {
  margin: 10px 0 0;
  font-size: 0.95rem;
  color: #64707d;
}

/* Formulário de Login */
.formulario-login {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.campo-grupo {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.campo-grupo label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #2a343f;
}

.campo-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.campo-icone {
  position: absolute;
  left: 14px;
  display: grid;
  place-items: center;
  color: #8c97a4;
  pointer-events: none;
}

.campo-input-wrapper input {
  width: 100%;
  height: 48px;
  padding: 0 16px 0 44px;
  border: 1px solid #dcdad3;
  border-radius: 6px;
  background-color: #f7f6f2;
  color: #1f2730;
  font-size: 0.95rem;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.campo-input-wrapper input:focus {
  outline: none;
  border-color: var(--cor-principal, #b64c1d);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(182, 76, 29, 0.14);
}

.campo-input-wrapper input::placeholder {
  color: #9ea7b1;
  opacity: 1;
}

/* Input de senha com botão de visualização */
.campo-input-wrapper:has(.botao-alternar-senha) input {
  padding-right: 46px;
}

.botao-alternar-senha {
  position: absolute;
  right: 4px;
  top: 4px;
  bottom: 4px;
  display: grid;
  place-items: center;
  width: 40px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: #8c97a4;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.botao-alternar-senha:hover {
  color: #2a343f;
  background-color: rgba(0, 0, 0, 0.04);
}

/* Erro */
.erro-login {
  margin: 0;
  padding: 12px 14px;
  border-radius: 4px;
  border-left: 3px solid var(--cor-erro, #a22d27);
  background: var(--cor-fundo-erro, #fff0eb);
  font-size: 0.88rem;
  line-height: 1.4;
}

/* Botão de Entrar */
.botao-entrar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 6px;
  background-color: var(--cor-principal, #be4d1e);
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
  box-shadow: 0 4px 14px rgba(190, 77, 30, 0.2);
}

.botao-entrar:hover:not(:disabled) {
  background-color: var(--cor-principal-hover, #9c3c14);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(190, 77, 30, 0.3);
}

.botao-entrar:active:not(:disabled) {
  transform: translateY(0);
}

.botao-entrar:disabled {
  opacity: 0.75;
  cursor: wait;
}

/* Link de Esqueceu a Senha */
.acoes-auxiliares {
  display: flex;
  justify-content: center;
  margin-top: 4px;
}

.link-esqueceu-senha {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  padding: 6px 8px;
  color: #7d8793;
  font-size: 0.82rem;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.15s ease;
}

.link-esqueceu-senha:hover {
  color: #333d48;
  text-decoration: underline;
}

/* Rodapé do painel de login */
.login-rodape {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 390px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
}

.rodape-linha {
  display: block;
  flex: 1;
  max-width: 60px;
  height: 1.5px;
  background-color: #dfdbd2;
}

.rodape-linha-direita {
  display: none;
}

.rodape-versao {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #9aa3ad;
}

/* ==========================================================================
   MODAL INFORMATIVO: ESQUECEU SUA SENHA
   ========================================================================== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  background-color: rgba(16, 21, 26, 0.6);
  backdrop-filter: blur(4px);
  padding: 20px;
  animation: fadeIn 0.2s ease-out;
}

.modal-dialogo {
  width: min(460px, 100%);
  background-color: #ffffff;
  border: 1px solid #d5d5ce;
  border-radius: 8px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  animation: modalEntrada 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #e8e6df;
  background-color: #faf8f5;
}

.modal-titulo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-icone-ajuda {
  color: var(--cor-principal, #b64c1d);
}

.modal-topo h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2730;
}

.modal-fechar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #717d8a;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.modal-fechar:hover {
  background-color: #eeebe3;
  color: #1f2730;
}

.modal-corpo {
  padding: 22px;
  font-size: 0.92rem;
  line-height: 1.6;
  color: #3b4652;
}

.modal-corpo p {
  margin: 0 0 12px;
}

.modal-dica {
  margin-bottom: 0 !important;
  padding: 12px 14px;
  background-color: #f7f5ef;
  border-radius: 6px;
  border-left: 3px solid var(--cor-principal, #b64c1d);
  font-size: 0.88rem;
  color: #55606b;
}

.modal-rodape {
  display: flex;
  justify-content: flex-end;
  padding: 14px 22px;
  border-top: 1px solid #e8e6df;
  background-color: #faf8f5;
}

.botao-entendi {
  min-height: 38px;
  padding: 8px 20px;
  background-color: var(--cor-principal, #b64c1d);
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.botao-entendi:hover {
  background-color: var(--cor-principal-hover, #9c3c14);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalEntrada {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* ==========================================================================
   VERSÃO MOBILE / RESPONSIVA
   Fiel ao design de 'Tela de Login STOCKA em Armazém.png'
   ========================================================================== */
@media (max-width: 860px) {
  .pagina-login {
    grid-template-columns: 1fr;
  }

  /* Painel superior com a foto e os slides */
  .painel-slides {
    min-height: 460px;
    padding: 28px 20px 24px;
    justify-content: flex-start;
  }

  .slides-cabecalho {
    margin-bottom: 32px;
  }

  .slides-corpo {
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  /* No mobile, os textos vêm ANTES dos cards */
  .slides-textos {
    order: 1;
    max-width: 100%;
    gap: 8px;
  }

  .slide-subtitulo {
    margin-bottom: 4px;
  }

  /* No mobile, apenas o traço laranja é exibido sobre o título */
  .slide-subtitulo-texto {
    display: none;
  }

  .linha-traco {
    width: 32px;
    height: 3px;
  }

  .slide-titulo {
    font-size: clamp(2rem, 7.5vw, 2.5rem);
    line-height: 1.12;
  }

  .slide-descricao {
    font-size: 0.95rem;
    line-height: 1.45;
    max-width: 330px;
    margin-top: 4px;
  }

  /* No mobile, os cards ficam alinhados na base do hero */
  .modulos-grade {
    order: 2;
    margin-top: 8px;
    display: flex;
    justify-content: flex-start;
    gap: 16px;
  }

  .modulo-card {
    gap: 8px;
  }

  .modulo-icone-wrapper {
    width: 66px;
    height: 66px;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.22);
  }

  .modulo-card.ativo .modulo-icone-wrapper {
    transform: translateY(-2px);
    border-top: 4px solid var(--cor-principal, #b64c1d);
  }

  .modulo-nome {
    font-size: 0.7rem;
    letter-spacing: 0.1em;
    color: #ffffff;
  }

  .slides-rodape {
    display: none;
  }

  /* Painel de Login inferior */
  .painel-login {
    min-height: auto;
    padding: 36px 20px 28px;
    justify-content: flex-start;
    overflow: hidden;
  }

  .decoracao-linhas-desktop {
    display: none;
  }

  .decoracao-linhas-mobile {
    display: block;
    position: absolute;
    bottom: -15px;
    right: -15px;
    width: 210px;
    height: 210px;
    pointer-events: none;
    z-index: 1;
    opacity: 0.8;
  }

  .login-conteudo {
    padding: 0;
    max-width: 100%;
  }

  .login-cabecalho {
    margin-bottom: 24px;
  }

  .traco-destaque {
    width: 36px;
    height: 3px;
    margin-bottom: 16px;
  }

  .login-cabecalho h1 {
    font-size: 1.95rem;
  }

  .login-subtitulo {
    margin-top: 6px;
    font-size: 0.95rem;
  }

  .formulario-login {
    gap: 18px;
  }

  .campo-input-wrapper input {
    height: 50px;
    background-color: #f6f5f0;
    border: 1px solid #d9d6ce;
  }

  .botao-entrar {
    height: 50px;
    border-radius: 8px;
    font-size: 1.02rem;
  }

  /* Rodapé centralizado no mobile com traços laterais */
  .login-rodape {
    justify-content: center;
    gap: 18px;
    padding-top: 36px;
    max-width: 100%;
  }

  .rodape-linha {
    flex: 0 0 36px;
    max-width: 36px;
    height: 1.5px;
    background-color: #ddd9ce;
  }

  .rodape-linha-direita {
    display: block;
    flex: 0 0 36px;
    max-width: 36px;
    height: 1.5px;
    background-color: #ddd9ce;
  }

  .rodape-versao {
    font-size: 0.72rem;
    color: #929ba5;
  }
}

@media (max-width: 380px) {
  .modulos-grade {
    gap: 10px;
  }

  .modulo-icone-wrapper {
    width: 58px;
    height: 58px;
  }

  .modulo-nome {
    font-size: 0.65rem;
  }

  .slide-titulo {
    font-size: 1.85rem;
  }
}
</style>
