<template>
  <header class="header">
    <div class="logo-wrap">
      <button class="logo-button" @click="voltarParaHub" title="Voltar ao hub">
        <img src="/STOCKA_Logo2.png" alt="STOCKA_LOGO">
      </button>
    </div>

    <div class="user-area">
      <!-- SIMULAÇÃO DE PERFIL — só até o login devolver o perfil de
           verdade (ver TODO em state/sessao.ts). Some daqui quando isso
           existir de verdade no back-end. -->
      <label class="perfil-switch" title="Simular perfil de acesso">
        <select :value="sessao.perfil" @change="onPerfilChange">
          <option value="ADMINISTRADOR">Administrador</option>
          <option value="USUARIO">Usuário</option>
        </select>
      </label>

      <button
        class="icon-button"
        title="Configurações"
        @click="abrirConfiguracoes"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
      </button>

      <div class="divider"></div>

      <div class="profile">
        <div class="avatar">
          <img
            v-if="usuario.foto"
            :src="usuario.foto"
            alt="Foto do usuário"
          />
          <span v-else>{{ inicial }}</span>
        </div>

        <div class="user-info">
          <span class="user-name">{{ usuario.nome }}</span>
          <span v-if="usuario.email" class="user-email">
            {{ usuario.email }}
          </span>
          <span class="user-perfil" :class="{ 'user-perfil--admin': isAdmin }">
            {{ isAdmin ? 'Administrador' : 'Usuário' }}
          </span>
        </div>
      </div>

      <button class="logout-button" @click="sair" title="Sair">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
      </button>
    </div>
  </header>

  <!-- Modal de configurações -->
  <Teleport to="body">
    <div v-if="mostrarConfiguracoes" class="config-overlay" @click.self="fecharConfiguracoes">
      <div class="config-modal">
        <div class="config-modal-header">
          <h2>Configurações</h2>
          <button class="config-close" @click="fecharConfiguracoes" title="Fechar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="config-modal-body">
          <p>Em breve.</p>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sessao, definirPerfil } from '../state/sessao'
import type { Perfil } from '../types/stocka'

const router = useRouter()

const voltarParaHub = () => {
  router.push('/hub')
}

const usuario = reactive({
  nome: 'Usuário não identificado',
  email: 'teste@gmail.com',
  foto: 'https://i.pinimg.com/736x/f6/8c/3c/f68c3c3bbe53d0d93dcc031676d24383.jpg'
})

const dadosSalvos = sessionStorage.getItem('stocka_usuario')

if (dadosSalvos) {
  try {
    const dados = JSON.parse(dadosSalvos)

    usuario.nome = dados.nome || 'Usuário não identificado'
    usuario.email = dados.email || ''
    usuario.foto = dados.foto || ''
  } catch {
    sessionStorage.removeItem('stocka_usuario')
  }
}

const inicial = computed(() => {
  return usuario.nome.charAt(0).toUpperCase()
})

const isAdmin = computed(() => sessao.perfil === 'ADMINISTRADOR')

function onPerfilChange(event: Event) {
  const valor = (event.target as HTMLSelectElement).value as Perfil
  definirPerfil(valor)
}

const sair = () => {
  sessionStorage.removeItem('stocka_usuario')
  router.push('/login')
}

// ADICIONADO: controle do modal de configurações (só abre/fecha, sem lógica interna ainda)
const mostrarConfiguracoes = ref(false)

const abrirConfiguracoes = () => {
  mostrarConfiguracoes.value = true
}

const fecharConfiguracoes = () => {
  mostrarConfiguracoes.value = false
}
// FIM ADICIONADO
</script>

<style scoped>
*{
  font-family: 'Manrope', system-ui, sans-serif;
}

.header {
  --bg: #2b2d42;
  --bg-alt: #8d99ae;
  --accent: #edf2f4;
  --text: #edf2f4;
  --text-muted: #edf2f4;
  --line: #ef233c;

  height: 84px;
  padding: 0 32px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  background-color: var(--bg);

  box-sizing: border-box;
}

/* LOGO */

.logo-wrap {
  display: flex;
  align-items: center;
}

.logo-button {
  display: flex;
  align-items: center;

  padding: 6px 8px;
  border: none;
  border-radius: 10px;
  background: transparent;

  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.15s ease;
}

.logo-button img {
  height: 64px;
  width: auto;
  display: block;
}

.logo-button:hover {
  transform: scale(1.03);
}

.logo-button:active {
  transform: scale(0.98);
}

/* ÁREA DO USUÁRIO */

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* SIMULAÇÃO DE PERFIL */

.perfil-switch select {
  height: 34px;
  padding: 0 10px;

  border: 1px dashed rgba(237, 242, 244, 0.4);
  border-radius: 8px;
  background: transparent;
  color: var(--accent);

  font-family: inherit;
  font-size: 12.5px;

  cursor: pointer;
}

.perfil-switch select option {
  color: #111827;
}

/* BOTÕES DE ÍCONE (configurações / sair) */

.icon-button,
.logout-button {
  width: 38px;
  height: 38px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted);

  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.icon-button svg,
.logout-button svg {
  width: 18px;
  height: 18px;
}

.icon-button svg {
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.icon-button:hover {
  background-color: var(--bg-alt);
  color: var(--accent);
  border-color: #ef233c;
}

.icon-button:hover svg {
  transform: rotate(90deg);
}

.logout-button:hover {
  background-color: var(--bg-alt);
  color: var(--accent);
  border-color: #ef233c;
}

.icon-button:focus-visible,
.logout-button:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* DIVISOR */

.divider {
  width: 1px;
  height: 28px;
  background: linear-gradient(to bottom, transparent, var(--line) 30%, var(--line) 70%, transparent);
}

/* PERFIL */

.profile {
  display: flex;
  align-items: center;
  gap: 11px;

  padding: 4px 6px;
  border-radius: 12px;

  cursor: default;
  transition: background-color 0.2s ease;
}

.profile:hover {
  background-color: var(--bg-alt);
}

.avatar {
  width: 42px;
  height: 42px;
  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  overflow: hidden;
  border-radius: 50%;

  background-color: #ef233c;
  color: var(--accent);

  font-size: 14px;
  font-weight: 700;

  box-shadow: 0 0 0 2px var(--bg), 0 0 0 3.5px #ef233c;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  letter-spacing: -0.01em;
}

.user-email {
  font-size: 12px;
  color: var(--text-muted);
}

.user-perfil {
  margin-top: 2px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--bg-alt);
  width: fit-content;
}

.user-perfil--admin {
  color: #ef233c;
}

/* RESPONSIVIDADE */

@media (max-width: 700px) {
  .perfil-switch {
    display: none;
  }
}

@media (max-width: 600px) {
  .header {
    padding: 0 16px;
  }

  .user-area {
    gap: 8px;
  }

  .divider {
    display: none;
  }

  .user-email {
    display: none;
  }
}

@media (max-width: 430px) {
  .user-info {
    display: none;
  }

  .profile {
    padding: 0;
  }

  .avatar {
    width: 36px;
    height: 36px;
  }
}

/* modal de configurações */
.config-overlay {
  position: fixed;
  inset: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  background-color: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(2px);

  z-index: 1000;
}

.config-modal {
  width: 100%;
  max-width: 380px;
  margin: 16px;

  background-color: #ffffff;
  border-radius: 14px;

  overflow: hidden;
}

.config-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 18px 20px;
  background-color: #2b2d42;
}

.config-modal-header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #f4f2ea;
}

.config-close {
  width: 28px;
  height: 28px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-radius: 8px;
  background: transparent;
  color: #93a89f;

  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.config-close svg {
  width: 15px;
  height: 15px;
}

.config-close:hover {
  background-color: rgba(255, 255, 255, 0.08);
  color: #e3a63d;
}

.config-modal-body {
  padding: 20px;
}

.config-modal-body p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.5;
  color: #4b5a54;
}
</style>
