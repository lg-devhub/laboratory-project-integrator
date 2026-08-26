<script setup lang="ts">
import { ref } from 'vue'

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)

async function handleLogin() {
  errorMessage.value = ''
  const normalizedEmail = email.value.trim()

  if (!normalizedEmail || !password.value) {
    errorMessage.value = 'Preencha o e-mail e a senha.'
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(normalizedEmail)) {
    errorMessage.value = 'Digite um e-mail válido.'
    return
  }

  isLoading.value = true

  try {
    const response = await fetch('http://localhost:8080/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify({
        email: normalizedEmail,
        password: password.value
      })
    })

    let data: { message?: string; token?: string } = {}

    try {
      data = await response.json()
    } catch {}

    if (response.status === 401 || response.status === 403) {
      errorMessage.value = data.message || 'E-mail ou senha inválidos.'
      return
    }

    if (!response.ok) {
      errorMessage.value = data.message || 'Ocorreu um erro no servidor.'
      return
    }

    console.log('Login realizado:', data)

    if (data.token) {
      localStorage.setItem('token', data.token)
    }
  } catch (error) {
    console.error('Erro no login:', error)
    errorMessage.value = 'Não foi possível conectar ao servidor.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="ambient ambient-one"></div>
    <div class="ambient ambient-two"></div>
    <div class="grid-overlay"></div>

    <section class="login-shell">
      <aside class="brand-panel">
        <div class="brand">
          <div class="brand-mark">
            <span></span><span></span><span></span>
          </div>
          <span class="brand-name">STOCKA</span>
        </div>

        <div class="brand-copy">
          <span class="eyebrow">INVENTORY MANAGEMENT</span>
          <h2>Controle seu estoque.<br><strong>Tenha clareza.</strong></h2>
          <p>Uma plataforma centralizada para acompanhar produtos, ativos e movimentações com mais agilidade.</p>
        </div>

        <div class="feature-list">
          <div class="feature">
            <span class="feature-icon">✓</span>
            <div>
              <strong>Gestão centralizada</strong>
              <span>Tenha tudo organizado em um só lugar.</span>
            </div>
          </div>

          <div class="feature">
            <span class="feature-icon">↗</span>
            <div>
              <strong>Visão operacional</strong>
              <span>Acompanhe suas movimentações em tempo real.</span>
            </div>
          </div>

          <div class="feature">
            <span class="feature-icon">⌁</span>
            <div>
              <strong>Acesso seguro</strong>
              <span>Seus dados protegidos durante toda a sessão.</span>
            </div>
          </div>
        </div>

        <div class="panel-footer">
          <span class="status-dot"></span>
          Sistema operacional
        </div>
      </aside>

      <div class="login-side">
        <div class="mobile-brand">
          <div class="brand-mark">
            <span></span><span></span><span></span>
          </div>
          <span class="brand-name">STOCKA</span>
        </div>

        <div class="login-card">
          <div class="login-header">
            <span class="eyebrow">ÁREA RESTRITA</span>
            <h1>Bem-vindo de volta.</h1>
            <p>Entre com suas credenciais para acessar a plataforma.</p>
          </div>

          <form @submit.prevent="handleLogin" :aria-busy="isLoading" novalidate>
            <div class="input-group">
              <label for="email">E-mail</label>
              <div class="input-wrapper">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 5h16c1.1 0 2 .9 2 2v10c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V7c0-1.1.9-2 2-2Z"/>
                  <path d="m3 7 9 6 9-6"/>
                </svg>
                <input id="email" v-model="email" type="email" name="email" placeholder="seu@email.com" autocomplete="email" inputmode="email" :disabled="isLoading" required />
              </div>
            </div>

            <div class="input-group">
              <div class="label-row">
                <label for="password">Senha</label>
                <a href="#">Esqueceu a senha?</a>
              </div>

              <div class="input-wrapper">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <rect x="4" y="10" width="16" height="11" rx="2"/>
                  <path d="M8 10V7a4 4 0 0 1 8 0v3"/>
                </svg>
                <input id="password" v-model="password" type="password" name="password" placeholder="Digite sua senha" autocomplete="current-password" :disabled="isLoading" required />
              </div>
            </div>

            <p v-if="errorMessage" class="error-message" role="alert">
              <span>!</span>
              {{ errorMessage }}
            </p>

            <button type="submit" :disabled="isLoading">
              <span v-if="!isLoading">Entrar na plataforma</span>
              <span v-else class="loading">
                <span class="spinner"></span>
                Autenticando...
              </span>
              <svg v-if="!isLoading" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 12h14"/>
                <path d="m13 6 6 6-6 6"/>
              </svg>
            </button>
          </form>

          <div class="security">
            <div class="security-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/>
                <path d="m9 12 2 2 4-4"/>
              </svg>
            </div>
            <div>
              <strong>Conexão protegida</strong>
              <span>Seus dados são tratados com segurança.</span>
            </div>
          </div>
        </div>

        <footer>
          <span>© 2026 STOCKA</span>
          <span class="footer-separator"></span>
          <span>Gestão inteligente de inventário</span>
        </footer>
      </div>
    </section>
  </main>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@500;600;700;800&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  min-height: 100%;
}

body {
  font-family: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #080d18;
  color: #172033;
}

button,
input {
  font: inherit;
}

button,
a {
  -webkit-tap-highlight-color: transparent;
}

.login-page {
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  overflow: hidden;
  padding: 32px;
  background:
    radial-gradient(circle at 8% 15%, rgba(78, 116, 255, .18), transparent 28%),
    radial-gradient(circle at 88% 90%, rgba(35, 211, 175, .10), transparent 30%),
    linear-gradient(135deg, #080d18 0%, #0d1423 52%, #0a101c 100%);
}

.ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
  pointer-events: none;
}

.ambient-one {
  width: 480px;
  height: 480px;
  top: -310px;
  right: -160px;
  border: 1px solid rgba(119, 147, 255, .18);
  box-shadow: 0 0 100px rgba(67, 97, 238, .08);
}

.ambient-two {
  width: 620px;
  height: 620px;
  left: -450px;
  bottom: -410px;
  border: 1px solid rgba(255, 255, 255, .05);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  opacity: .25;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, .025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .025) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(to bottom, transparent, black 30%, black 70%, transparent);
}

.login-shell {
  position: relative;
  z-index: 2;
  width: min(1060px, 100%);
  min-height: 650px;
  display: grid;
  grid-template-columns: 1.03fr .97fr;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, .10);
  border-radius: 28px;
  background: rgba(15, 23, 39, .78);
  box-shadow:
    0 40px 100px rgba(0, 0, 0, .38),
    0 0 0 1px rgba(255, 255, 255, .025) inset;
  backdrop-filter: blur(24px);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 52px;
  overflow: hidden;
  background:
    radial-gradient(circle at 75% 18%, rgba(76, 111, 255, .16), transparent 35%),
    linear-gradient(145deg, rgba(28, 41, 70, .96), rgba(13, 20, 35, .96));
  border-right: 1px solid rgba(255, 255, 255, .08);
}

.brand-panel::after {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  right: -130px;
  bottom: -140px;
  border: 1px solid rgba(255, 255, 255, .06);
  border-radius: 50%;
  box-shadow:
    0 0 0 40px rgba(255, 255, 255, .015),
    0 0 0 80px rgba(255, 255, 255, .01);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: 1px solid rgba(255, 255, 255, .13);
  border-radius: 12px;
  background: linear-gradient(145deg, #4f6fff, #304bc3);
  box-shadow: 0 10px 25px rgba(61, 91, 224, .28);
}

.brand-mark span {
  width: 4px;
  border-radius: 4px;
  background: #fff;
}

.brand-mark span:nth-child(1) {
  height: 12px;
}

.brand-mark span:nth-child(2) {
  height: 21px;
}

.brand-mark span:nth-child(3) {
  height: 16px;
}

.brand-name {
  color: #fff;
  font-family: 'Manrope', sans-serif;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 3px;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin-top: auto;
  margin-bottom: 54px;
}

.eyebrow {
  display: block;
  margin-bottom: 14px;
  color: #718dff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

.brand-copy h2 {
  max-width: 430px;
  color: #fff;
  font-family: 'Manrope', sans-serif;
  font-size: clamp(32px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: -1.8px;
}

.brand-copy h2 strong {
  color: #829aff;
}

.brand-copy p {
  max-width: 400px;
  margin-top: 20px;
  color: #94a2ba;
  font-size: 14px;
  line-height: 1.75;
}

.feature-list {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 18px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 13px;
}

.feature-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid rgba(130, 154, 255, .20);
  border-radius: 9px;
  background: rgba(104, 130, 255, .08);
  color: #8ba1ff;
  font-size: 13px;
}

.feature div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.feature strong {
  color: #e8edfa;
  font-size: 12px;
  font-weight: 600;
}

.feature div span {
  color: #7e8da7;
  font-size: 10px;
}

.panel-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 40px;
  color: #64738c;
  font-size: 10px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3ed3ad;
  box-shadow: 0 0 10px rgba(62, 211, 173, .65);
}

.login-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 64px;
  background: rgba(250, 252, 255, .98);
}

.mobile-brand {
  display: none;
}

.login-card {
  width: 100%;
  max-width: 390px;
  margin: auto;
}

.login-header {
  margin-bottom: 34px;
}

.login-header .eyebrow {
  color: #526ee8;
  margin-bottom: 11px;
}

.login-header h1 {
  color: #101827;
  font-family: 'Manrope', sans-serif;
  font-size: 31px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -1.2px;
}

.login-header p {
  margin-top: 10px;
  color: #7d8798;
  font-size: 13px;
  line-height: 1.6;
}

form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-group label {
  color: #303949;
  font-size: 12px;
  font-weight: 600;
}

.label-row a {
  color: #6171a5;
  font-size: 11px;
  font-weight: 600;
  text-decoration: none;
  transition: color .2s;
}

.label-row a:hover {
  color: #4361ee;
}

.input-wrapper {
  position: relative;
}

.input-wrapper svg {
  position: absolute;
  left: 15px;
  top: 50%;
  width: 17px;
  height: 17px;
  transform: translateY(-50%);
  fill: none;
  stroke: #9aa5b6;
  stroke-width: 1.7;
  pointer-events: none;
  transition: stroke .2s;
}

.input-wrapper:focus-within svg {
  stroke: #526ee8;
}

.input-wrapper input {
  width: 100%;
  height: 52px;
  padding: 0 15px 0 44px;
  border: 1px solid #dfe4eb;
  border-radius: 12px;
  outline: none;
  background: #f7f9fc;
  color: #172033;
  font-size: 13px;
  transition: .2s;
}

.input-wrapper input::placeholder {
  color: #aab2bf;
}

.input-wrapper input:hover:not(:disabled) {
  border-color: #cbd3df;
  background: #fff;
}

.input-wrapper input:focus {
  border-color: #657ff0;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(82, 110, 232, .09);
}

.input-wrapper input:disabled {
  opacity: .7;
  cursor: not-allowed;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 12px;
  border: 1px solid #f0d0d0;
  border-radius: 10px;
  background: #fff7f7;
  color: #bd4545;
  font-size: 11px;
  line-height: 1.4;
}

.error-message span {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 50%;
  background: #d95252;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

button {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 3px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #536ff0, #3d58ce);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 25px rgba(62, 85, 204, .20);
  transition: transform .2s, box-shadow .2s, filter .2s;
}

button svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  transition: transform .2s;
}

button:hover:not(:disabled) {
  filter: brightness(1.04);
  transform: translateY(-1px);
  box-shadow: 0 15px 30px rgba(62, 85, 204, .25);
}

button:hover:not(:disabled) svg {
  transform: translateX(3px);
}

button:active:not(:disabled) {
  transform: translateY(0);
}

button:disabled {
  opacity: .65;
  cursor: not-allowed;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, .3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin .7s linear infinite;
}

.security {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: 25px;
  padding: 13px 14px;
  border: 1px solid #e8edf3;
  border-radius: 12px;
  background: #f8fafc;
}

.security-icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 9px;
  background: #eef2ff;
}

.security svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: #536ee1;
  stroke-width: 1.6;
}

.security div:last-child {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.security strong {
  color: #4b5669;
  font-size: 10px;
  font-weight: 700;
}

.security div:last-child span {
  color: #929cab;
  font-size: 9px;
}

footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-top: 28px;
  color: #a0a8b6;
  font-size: 9px;
}

.footer-separator {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #c4cad3;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .login-page {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    max-width: 520px;
    min-height: auto;
  }

  .brand-panel {
    display: none;
  }

  .login-side {
    min-height: 680px;
    padding: 42px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 11px;
    margin-bottom: 42px;
  }

  .mobile-brand .brand-name {
    color: #101827;
  }

  .login-card {
    margin: 0 auto;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 12px;
  }

  .login-shell {
    border-radius: 21px;
  }

  .login-side {
    min-height: 650px;
    padding: 32px 22px;
  }

  .mobile-brand {
    margin-bottom: 34px;
  }

  .login-header h1 {
    font-size: 27px;
  }

  .login-header {
    margin-bottom: 28px;
  }

  .ambient,
  .grid-overlay {
    display: none;
  }
}
</style>