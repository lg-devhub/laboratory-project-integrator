<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  Search,
  Building2,
  Plus,
  User,
  ChevronDown,
  Users,
  KeyRound,
  LogOut,
  Filter,
  Check
} from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import AmbienteCard from '@/components/AmbienteCard.vue'
import AmbienteFormulario from '@/components/AmbienteFormulario.vue'
import AmbienteAcessos from '@/components/AmbienteAcessos.vue'
import AmbienteRetirada from '@/components/AmbienteRetirada.vue'
import AlterarSenha from '@/components/AlterarSenha.vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import { listarAmbientes } from '@/services/ambientes'
import type { Ambiente } from '@/types/ambiente'

import fotoPlanta from '@/assets/images/planta-escritorio.png'

const router = useRouter()
const autenticacao = useAutenticacaoStore()
const ambientes = ref<Ambiente[]>([])
const pesquisa = ref('')
const filtroTipo = ref<'TODOS' | 'SALA' | 'SETOR'>('TODOS')
const menuFiltroAberto = ref(false)

const carregando = ref(true)
const saindo = ref(false)
const erro = ref('')
const erroSaida = ref('')
const administrador = computed(() => autenticacao.usuario?.perfil === 'ADMINISTRADOR')

const menuUsuarioAberto = ref(false)
const formularioAberto = ref(false)
const ambienteEditando = ref<Ambiente | null>(null)
const ambienteAcessos = ref<Ambiente | null>(null)
const sucesso = ref('')
const alterandoSenha = ref(false)
const ambienteRetirando = ref<Ambiente | null>(null)

function alternarMenuUsuario() {
  menuUsuarioAberto.value = !menuUsuarioAberto.value
}

function fecharMenus() {
  menuUsuarioAberto.value = false
  menuFiltroAberto.value = false
}

function ambienteRetirado() {
  ambientes.value = ambientes.value.filter(a => a.id !== ambienteRetirando.value?.id)
  ambienteRetirando.value = null
  sucesso.value = 'Ambiente retirado de uso. Cadastro e atribuições preservados.'
}

async function atualizarRetirada() {
  ambienteRetirando.value = null
  await carregarAmbientes()
}

async function conferirAcessoRetirada() {
  ambienteRetirando.value = null
  await autenticacao.verificarSessao()
  if (!autenticacao.autenticado) await sessaoExpirada()
  else {
    sucesso.value = ''
    await carregarAmbientes()
  }
}

function abrirFormulario(ambiente: Ambiente | null = null) {
  ambienteEditando.value = ambiente
  formularioAberto.value = true
  sucesso.value = ''
}

function ambienteSalvo(ambiente: Ambiente) {
  const indice = ambientes.value.findIndex(a => a.id === ambiente.id)
  if (indice >= 0) ambientes.value[indice] = ambiente
  else ambientes.value.push(ambiente)
  ambientes.value.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
  formularioAberto.value = false
  sucesso.value = indice >= 0 ? 'Ambiente atualizado.' : 'Ambiente criado. Use Acessos para atribuir usuários.'
}

function acessosSalvos() {
  ambienteAcessos.value = null
  sucesso.value = 'Acessos atualizados. As permissões já valem para as próximas consultas.'
}

async function sessaoExpirada() {
  ambienteRetirando.value = null
  alterandoSenha.value = false
  formularioAberto.value = false
  ambienteAcessos.value = null
  autenticacao.usuario = null
  await router.replace({ name: 'login' })
  autenticacao.erroSessao = 'Sua sessão expirou. Entre novamente.'
}

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('pt-BR')
}

const ambientesFiltrados = computed(() => {
  const termo = normalizar(pesquisa.value.trim())
  return ambientes.value.filter(ambiente => {
    // Filtro por tipo (TODOS, SALA ou SETOR)
    if (filtroTipo.value !== 'TODOS' && ambiente.tipo !== filtroTipo.value) {
      return false
    }
    // Filtro textual
    return normalizar(`${ambiente.nome} ${ambiente.tipo} ${ambiente.descricao || ''}`).includes(termo)
  })
})

const labelFiltroTipo = computed(() => {
  if (filtroTipo.value === 'SALA') return 'Salas'
  if (filtroTipo.value === 'SETOR') return 'Setores'
  return 'Todos'
})

async function carregarAmbientes() {
  carregando.value = true
  erro.value = ''
  ambientes.value = []
  try {
    const resultado = await listarAmbientes()
    if (resultado === null) {
      await sessaoExpirada()
      return
    }
    ambientes.value = resultado
  } catch (falha) {
    erro.value = falha instanceof Error ? falha.message : 'Não foi possível carregar os ambientes.'
  } finally {
    carregando.value = false
  }
}

async function sair() {
  if (saindo.value) return
  saindo.value = true
  erroSaida.value = ''
  try {
    await autenticacao.sair()
    await router.replace({ name: 'login' })
  } catch (falha) {
    erroSaida.value = falha instanceof Error ? falha.message : 'Não foi possível sair. Tente novamente.'
  } finally {
    saindo.value = false
  }
}

function lidarCliqueGlobal(event: MouseEvent) {
  const alvo = event.target as HTMLElement | null
  if (!alvo) return
  if (!alvo.closest('.menu-usuario-container')) {
    menuUsuarioAberto.value = false
  }
  if (!alvo.closest('.filtro-container')) {
    menuFiltroAberto.value = false
  }
}

onMounted(() => {
  carregarAmbientes()
  window.addEventListener('click', lidarCliqueGlobal)
})

onUnmounted(() => {
  window.removeEventListener('click', lidarCliqueGlobal)
})
</script>

<template>
  <div class="pagina-ambientes">
    <!-- CABEÇALHO SUPERIOR -->
    <header class="cabecalho">
      <div class="cabecalho-conteudo">
        <MarcaStocka />

        <!-- ÁREA DA CONTA DO USUÁRIO COM DROPDOWN E LINKS -->
        <div class="conta-area">
          <!-- Links de abas rápidas em desktop -->
          <nav class="abas-navegacao" aria-label="Navegação da conta">
            <RouterLink
              v-if="administrador"
              :to="{ name: 'usuarios' }"
              class="aba-link"
            >
              <Users :size="16" aria-hidden="true" />
              <span>Gerenciar usuários</span>
            </RouterLink>

            <button
              type="button"
              class="aba-link"
              :disabled="saindo"
              @click="sucesso = ''; alterandoSenha = true"
            >
              <KeyRound :size="16" aria-hidden="true" />
              <span>Alterar senha</span>
            </button>

            <button
              type="button"
              class="aba-link aba-link-sair"
              :disabled="saindo"
              @click="sair"
            >
              <LogOut :size="16" aria-hidden="true" />
              <span>{{ saindo ? 'Saindo…' : 'Sair' }}</span>
            </button>
          </nav>

          <!-- Menu de Perfil / Usuário Dropdown -->
          <div class="menu-usuario-container">
            <button
              type="button"
              class="botao-perfil"
              :aria-expanded="menuUsuarioAberto"
              aria-haspopup="true"
              @click.stop="alternarMenuUsuario"
            >
              <div class="avatar-circulo" aria-hidden="true">
                <User :size="16" :stroke-width="2" />
              </div>
              <span class="nome-usuario">
                {{ autenticacao.usuario?.nome || 'Administrador de teste' }}
              </span>
              <ChevronDown
                :size="14"
                class="chevron-icone"
                :class="{ aberto: menuUsuarioAberto }"
                aria-hidden="true"
              />
            </button>

            <!-- Dropdown Flutuante -->
            <div
              v-if="menuUsuarioAberto"
              class="dropdown-usuario"
              role="menu"
              @click.stop
            >
              <div class="dropdown-cabecalho">
                <span class="dropdown-usuario-nome">
                  {{ autenticacao.usuario?.nome || 'Administrador de teste' }}
                </span>
                <span class="badge-perfil">
                  {{ administrador ? 'Administrador' : 'Usuário' }}
                </span>
              </div>

              <div class="dropdown-divisor" aria-hidden="true"></div>

              <RouterLink
                v-if="administrador"
                :to="{ name: 'usuarios' }"
                class="dropdown-item"
                role="menuitem"
                @click="fecharMenus"
              >
                <Users :size="16" aria-hidden="true" />
                <span>Gerenciar usuários</span>
              </RouterLink>

              <button
                type="button"
                class="dropdown-item"
                role="menuitem"
                @click="fecharMenus(); sucesso = ''; alterandoSenha = true"
              >
                <KeyRound :size="16" aria-hidden="true" />
                <span>Alterar senha</span>
              </button>

              <div class="dropdown-divisor" aria-hidden="true"></div>

              <button
                type="button"
                class="dropdown-item item-sair"
                role="menuitem"
                :disabled="saindo"
                @click="fecharMenus(); sair()"
              >
                <LogOut :size="16" aria-hidden="true" />
                <span>{{ saindo ? 'Saindo…' : 'Sair' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- CONTEÚDO PRINCIPAL (HUB) -->
    <main class="hub">
      <!-- HERO BANNER: PLANTA TROPICAL EM ESCRITÓRIO MINIMALISTA -->
      <section class="banner-hero" aria-label="Apresentação de gestão de ambientes">
        <div class="banner-conteudo">
          <span class="traco-destaque" aria-hidden="true"></span>
          <p class="banner-tag">BEM-VINDO DE VOLTA</p>
          <h1 class="banner-titulo">
            Gerencie seus ambientes,<br />
            <span class="texto-laranja">setores e acessos da equipe.</span>
          </h1>
          <p class="banner-descricao">
            Organize salas, setores e acompanhe os recursos com mais eficiência.
          </p>

          <button
            v-if="administrador"
            class="botao-adicionar"
            :disabled="carregando || !!erro"
            @click="abrirFormulario()"
          >
            <Plus :size="18" :stroke-width="2.2" aria-hidden="true" />
            <span>Adicionar ambiente</span>
          </button>
        </div>

        <!-- Elemento visual fotográfico com corte orgânico -->
        <div class="banner-visual" aria-hidden="true">
          <!-- Curva decorativa em SVG -->
          <svg
            class="arco-decorativo"
            viewBox="0 0 340 300"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M120 0C120 160 220 280 340 280"
              stroke="#edd8c8"
              stroke-width="1.5"
            />
          </svg>

          <div class="banner-imagem-wrapper">
            <img
              :src="fotoPlanta"
              alt="Planta tropical em escritório minimalista"
              class="banner-foto"
            />
          </div>
        </div>
      </section>

      <!-- FEEDBACK DE SUCESSO OU ERRO -->
      <p v-if="sucesso" class="mensagem-sucesso" role="status">{{ sucesso }}</p>
      <p v-if="erroSaida" class="mensagem-erro" role="alert">{{ erroSaida }}</p>

      <!-- BARRA DE PESQUISA E FILTROS -->
      <section class="secao-controles" aria-label="Filtro de ambientes">
        <div class="linha-controles">
          <!-- Campo de Busca -->
          <div class="campo-busca-wrapper">
            <Search :size="18" class="icone-busca" aria-hidden="true" />
            <input
              id="pesquisa-ambientes"
              v-model="pesquisa"
              type="search"
              placeholder="Buscar ambientes..."
              autocomplete="off"
              aria-label="Buscar ambientes por nome, tipo ou descrição"
            />
          </div>

          <!-- Dropdown Seletor de Tipo (Todos / Salas / Setores) -->
          <div class="filtro-container">
            <button
              type="button"
              class="botao-filtro"
              :aria-expanded="menuFiltroAberto"
              @click.stop="menuFiltroAberto = !menuFiltroAberto"
            >
              <Filter :size="16" class="icone-filtro" aria-hidden="true" />
              <span>{{ labelFiltroTipo }}</span>
              <ChevronDown :size="14" class="chevron-filtro" aria-hidden="true" />
            </button>

            <div v-if="menuFiltroAberto" class="dropdown-filtro" role="listbox">
              <button
                type="button"
                class="filtro-opcao"
                :class="{ selecionado: filtroTipo === 'TODOS' }"
                @click="filtroTipo = 'TODOS'; menuFiltroAberto = false"
              >
                <span>Todos</span>
                <Check v-if="filtroTipo === 'TODOS'" :size="14" />
              </button>
              <button
                type="button"
                class="filtro-opcao"
                :class="{ selecionado: filtroTipo === 'SETOR' }"
                @click="filtroTipo = 'SETOR'; menuFiltroAberto = false"
              >
                <span>Setores</span>
                <Check v-if="filtroTipo === 'SETOR'" :size="14" />
              </button>
              <button
                type="button"
                class="filtro-opcao"
                :class="{ selecionado: filtroTipo === 'SALA' }"
                @click="filtroTipo = 'SALA'; menuFiltroAberto = false"
              >
                <span>Salas</span>
                <Check v-if="filtroTipo === 'SALA'" :size="14" />
              </button>
            </div>
          </div>
        </div>

        <!-- Contador discreto de resultados -->
        <p class="contador-ambientes" role="status" aria-live="polite">
          {{ ambientesFiltrados.length }}
          {{ ambientesFiltrados.length === 1 ? 'ambiente encontrado' : 'ambientes encontrados' }}
        </p>
      </section>

      <!-- ESTADOS DE CARREGAMENTO / ERRO / VAZIO / GRADE -->
      <section v-if="carregando" class="estado-feedback" role="status" aria-live="polite">
        <p>Carregando ambientes…</p>
      </section>

      <section v-else-if="erro" class="estado-feedback">
        <p class="mensagem-erro" role="alert">{{ erro }}</p>
        <button class="botao-principal" @click="carregarAmbientes">Tentar novamente</button>
      </section>

      <section v-else-if="ambientes.length === 0" class="estado-feedback">
        <Building2 :size="36" :stroke-width="1.5" class="icone-vazio" aria-hidden="true" />
        <h2>Nenhum ambiente disponível</h2>
        <p>Não há salas ou setores disponíveis para sua conta no momento.</p>
      </section>

      <section v-else aria-label="Lista de ambientes">
        <div v-if="ambientesFiltrados.length" class="grade-ambientes">
          <AmbienteCard
            v-for="ambiente in ambientesFiltrados"
            :key="ambiente.id"
            :ambiente="ambiente"
            :administrador="administrador"
            @editar="abrirFormulario(ambiente)"
            @acessos="sucesso = ''; ambienteAcessos = ambiente"
            @retirar="sucesso = ''; ambienteRetirando = ambiente"
          />
        </div>

        <div v-else class="estado-feedback">
          <h2>Nenhum resultado para sua pesquisa</h2>
          <p>Tente outro termo ou limpe os filtros para visualizar os ambientes cadastrados.</p>
          <button class="botao-secundario" @click="pesquisa = ''; filtroTipo = 'TODOS'">
            Limpar filtros
          </button>
        </div>
      </section>
    </main>

    <!-- DIÁLOGOS E MODAIS ADMINISTRATIVOS -->
    <AmbienteFormulario
      v-if="formularioAberto && administrador"
      :ambiente="ambienteEditando"
      @fechar="formularioAberto = false"
      @salvo="ambienteSalvo"
      @sessao-expirada="sessaoExpirada"
    />

    <AmbienteRetirada
      v-if="ambienteRetirando && administrador"
      :ambiente="ambienteRetirando"
      @fechar="ambienteRetirando = null"
      @retirado="ambienteRetirado"
      @atualizar="atualizarRetirada"
      @acesso-perdido="conferirAcessoRetirada"
    />

    <AmbienteAcessos
      v-if="ambienteAcessos && administrador"
      :ambiente="ambienteAcessos"
      @fechar="ambienteAcessos = null"
      @salvo="acessosSalvos"
      @sessao-expirada="sessaoExpirada"
    />

    <AlterarSenha
      v-if="alterandoSenha"
      @fechar="alterandoSenha = false"
      @salvo="alterandoSenha = false; sucesso = 'Senha alterada. Use a nova senha no próximo login.'"
      @sessao-expirada="sessaoExpirada"
    />
  </div>
</template>

<style scoped>
/* ==========================================================================
   ESTRUTURA GERAL DA PÁGINA
   ========================================================================== */
.pagina-ambientes {
  min-height: 100svh;
  background-color: #f7f5f0;
  color: #1e2630;
}

/* ==========================================================================
   CABEÇALHO E MENU DA CONTA
   ========================================================================== */
.cabecalho {
  background-color: #ffffff;
  border-bottom: 1px solid #e7e4dc;
  position: sticky;
  top: 0;
  z-index: 30;
}

.cabecalho-conteudo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px 32px;
}

.conta-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Abas rápidas de navegação */
.abas-navegacao {
  display: flex;
  align-items: center;
  gap: 8px;
}

.aba-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #556270;
  font-size: 0.88rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.aba-link:hover {
  background-color: #f2efe8;
  color: #1e2630;
  border-color: #e5e1d7;
}

.aba-link-sair:hover {
  background-color: #feeceb;
  color: #b52929;
  border-color: #f7d2d0;
}

/* Botão de Perfil / Dropdown */
.menu-usuario-container {
  position: relative;
}

.botao-perfil {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  background-color: #f8f6f1;
  border: 1px solid #e2ded5;
  border-radius: 20px;
  color: #2b3540;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.botao-perfil:hover {
  background-color: #eeebe3;
  border-color: #d6d0c4;
}

.avatar-circulo {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #e6e2d8;
  color: #4a5460;
}

.nome-usuario {
  max-width: 170px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chevron-icone {
  color: #7b8692;
  transition: transform 0.2s ease;
}

.chevron-icone.aberto {
  transform: rotate(180deg);
}

/* Dropdown Flutuante */
.dropdown-usuario {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 230px;
  background-color: #ffffff;
  border: 1px solid #dfdbd1;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  padding: 6px;
  z-index: 50;
  animation: fadeIn 0.15s ease-out;
}

.dropdown-cabecalho {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dropdown-usuario-nome {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e2630;
}

.badge-perfil {
  align-self: flex-start;
  padding: 2px 8px;
  background-color: #f0ece3;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64707d;
  letter-spacing: 0.04em;
}

.dropdown-divisor {
  height: 1px;
  background-color: #ece8df;
  margin: 4px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #445160;
  font-size: 0.88rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.dropdown-item:hover {
  background-color: #f5f2eb;
  color: #1e2630;
}

.dropdown-item.item-sair {
  color: #c93b3b;
}

.dropdown-item.item-sair:hover {
  background-color: #feefee;
  color: #ab2727;
}

/* ==========================================================================
   HUB PRINCIPAL
   ========================================================================== */
.hub {
  max-width: 1240px;
  margin: 0 auto;
  padding: 36px 32px 72px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* ==========================================================================
   HERO BANNER: PLANTA TROPICAL EM ESCRITÓRIO MINIMALISTA
   ========================================================================== */
.banner-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fbf9f4;
  border: 1px solid #ebdccf;
  border-radius: 16px;
  overflow: hidden;
  min-height: 290px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.banner-conteudo {
  flex: 1;
  padding: clamp(28px, 4.5vw, 48px);
  max-width: 620px;
  z-index: 2;
}

.traco-destaque {
  display: block;
  width: 32px;
  height: 3px;
  background-color: var(--cor-principal, #be4d1e);
  border-radius: 2px;
  margin-bottom: 18px;
}

.banner-tag {
  margin: 0 0 10px;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: #7b8590;
  text-transform: uppercase;
}

.banner-titulo {
  margin: 0;
  font-size: clamp(1.75rem, 2.7vw, 2.35rem);
  font-weight: 750;
  line-height: 1.18;
  letter-spacing: -0.03em;
  color: #1c242c;
}

.texto-laranja {
  color: var(--cor-principal, #be4d1e);
}

.banner-descricao {
  margin: 12px 0 24px;
  font-size: 0.95rem;
  color: #64707d;
  line-height: 1.5;
  max-width: 440px;
}

.botao-adicionar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 20px;
  background-color: var(--cor-principal, #be4d1e);
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(190, 77, 30, 0.22);
  transition: all 0.15s ease;
}

.botao-adicionar:hover:not(:disabled) {
  background-color: var(--cor-principal-hover, #9c3c14);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(190, 77, 30, 0.3);
}

/* Elemento fotográfico no lado direito */
.banner-visual {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 48%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  pointer-events: none;
}

.arco-decorativo {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  height: 100%;
  width: auto;
  opacity: 0.85;
}

.banner-imagem-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  mask-image: radial-gradient(circle at 100% 50%, black 60%, transparent 95%);
  -webkit-mask-image: radial-gradient(circle at 100% 50%, black 60%, transparent 95%);
}

.banner-foto {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 70% 45%;
}

/* ==========================================================================
   FEEDBACKS
   ========================================================================== */
.mensagem-sucesso {
  border-left: 3px solid #3e6d4c;
  padding: 12px 18px;
  background-color: #ebf3ec;
  color: #274f33;
  border-radius: 4px;
  margin: 0;
  font-size: 0.92rem;
}

.mensagem-erro {
  border-left: 3px solid #b52929;
  padding: 12px 18px;
  background-color: #feeceb;
  color: #921b1b;
  border-radius: 4px;
  margin: 0;
  font-size: 0.92rem;
}

/* ==========================================================================
   CONTROLES (PESQUISA, FILTROS E CONTAGEM)
   ========================================================================== */
.secao-controles {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.linha-controles {
  display: flex;
  align-items: center;
  gap: 16px;
}

.campo-busca-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.icone-busca {
  position: absolute;
  left: 14px;
  color: #8c97a4;
  pointer-events: none;
}

.campo-busca-wrapper input {
  width: 100%;
  height: 46px;
  padding: 0 16px 0 42px;
  border: 1px solid #dcd7ce;
  border-radius: 8px;
  background-color: #ffffff;
  color: #1e2630;
  font-size: 0.95rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.campo-busca-wrapper input:focus {
  outline: none;
  border-color: var(--cor-principal, #be4d1e);
  box-shadow: 0 0 0 3px rgba(190, 77, 30, 0.14);
}

.campo-busca-wrapper input::placeholder {
  color: #98a2ad;
}

/* Botão de Filtro por Tipo */
.filtro-container {
  position: relative;
}

.botao-filtro {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 16px;
  background-color: #ffffff;
  border: 1px solid #dcd7ce;
  border-radius: 8px;
  color: #3b4754;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.botao-filtro:hover {
  background-color: #f7f5f0;
  border-color: #cfc9be;
}

.icone-filtro {
  color: #7b8692;
}

.chevron-filtro {
  color: #98a2ad;
}

.dropdown-filtro {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 150px;
  background-color: #ffffff;
  border: 1px solid #dfdbd1;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
  padding: 4px;
  z-index: 20;
}

.filtro-opcao {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #445160;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.filtro-opcao:hover {
  background-color: #f5f2eb;
}

.filtro-opcao.selecionado {
  color: var(--cor-principal, #be4d1e);
  background-color: #faf4ed;
}

.contador-ambientes {
  margin: 0;
  font-size: 0.84rem;
  color: #737f8d;
}

/* ==========================================================================
   GRADE DE AMBIENTES
   ========================================================================== */
.grade-ambientes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

/* Feedback de Estados */
.estado-feedback {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 56px 20px;
  text-align: center;
  background-color: #ffffff;
  border: 1px dashed #ded9cf;
  border-radius: 12px;
}

.icone-vazio {
  color: #98a2ad;
}

.estado-feedback h2 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.estado-feedback p {
  max-width: 440px;
  margin: 0;
  color: #64707d;
  font-size: 0.92rem;
}

/* ==========================================================================
   RESPONSIVIDADE (MOBILE E TABLET)
   ========================================================================== */
@media (max-width: 900px) {
  .abas-navegacao {
    display: none; /* Em telas menores, os links ficam dentro do menu do usuário */
  }

  .banner-hero {
    flex-direction: column;
    align-items: flex-start;
    min-height: auto;
  }

  .banner-conteudo {
    max-width: 100%;
    padding: 32px 24px 24px;
  }

  .banner-visual {
    position: relative;
    width: 100%;
    height: 180px;
  }

  .arco-decorativo {
    display: none;
  }

  .banner-imagem-wrapper {
    mask-image: linear-gradient(to bottom, black 50%, transparent 100%);
    -webkit-mask-image: linear-gradient(to bottom, black 50%, transparent 100%);
  }

  .grade-ambientes {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .cabecalho-conteudo {
    padding: 14px 20px;
  }

  .hub {
    padding: 24px 16px 56px;
    gap: 20px;
  }

  .banner-titulo {
    font-size: 1.7rem;
  }

  .banner-descricao {
    font-size: 0.9rem;
  }

  .linha-controles {
    flex-direction: column;
    align-items: stretch;
  }

  .botao-filtro {
    justify-content: space-between;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
