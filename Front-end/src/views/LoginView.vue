<script setup lang="ts">
import { ref } from 'vue'

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)

async function handleLogin() {
  errorMessage.value = ''

  if (!email.value || !password.value) {
    errorMessage.value = 'Preencha o e-mail e a senha.'
    return
  }

  isLoading.value = true

  try {
    const response = await fetch('http://localhost:8080/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: email.value,
        password: password.value,
      }),
    })

    if (!response.ok) {
      throw new Error('E-mail ou senha inválidos.')
    }

    const data = await response.json()

    console.log('Login realizado:', data)

    // Futuramente:
    // redirecionar para o Dashboard
  } catch (error) {
    console.error(error)
    errorMessage.value = 'Não foi possível realizar o login.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">

      <div class="brand">
        <div class="brand-icon">
          <span></span>
          <span></span>
          <span></span>
        </div>

        <h1>STOCKA</h1>
      </div>

      <div class="login-header">
        <h2>Bem-vindo de volta</h2>
        <p>Entre para acessar o controle de inventário.</p>
      </div>

      <form @submit.prevent="handleLogin">

        <div class="input-group">
          <label for="email">E-mail</label>

          <input
            id="email"
            v-model="email"
            type="email"
            placeholder="seu@email.com"
            autocomplete="email"
          />
        </div>

        <div class="input-group">
          <label for="password">Senha</label>

          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="Digite sua senha"
            autocomplete="current-password"
          />
        </div>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <button
          type="submit"
          :disabled="isLoading"
        >
          {{ isLoading ? 'Entrando...' : 'Entrar' }}
        </button>

      </form>

      <footer>
        <span>Controle de inventário</span>
        <span class="separator">•</span>
        <span>Gestão organizada</span>
      </footer>

    </section>
  </main>
</template>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background:
    #f6f7f9;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 44px;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e7e9ee;
  border-radius: 18px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 42px;
}

.brand h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: 2px;
  color: #202633;
}

.login-header {
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 8px;
  font-size: 25px;
  font-weight: 700;
  color: #202633;
}

.login-header p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: #737b8c;
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

.input-group label {
  font-size: 13px;
  font-weight: 600;
  color: #3d4554;
}

.input-group input {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  box-sizing: border-box;
  border: 1px solid #dfe2e8;
  border-radius: 9px;
  outline: none;
  background: #ffffff;
  color: #202633;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-group input::placeholder {
  color: #a5abb7;
}

.input-group input:focus {
  border-color: #4361ee;
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
}

button {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border: none;
  border-radius: 9px;
  background: #4361ee;
  color: #ffffff;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

button:hover:not(:disabled) {
  background: #3451d1;
}

button:active:not(:disabled) {
  transform: translateY(1px);
}

button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error-message {
  margin: -4px 0 -4px;
  font-size: 13px;
  color: #d64545;
}

footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 7px;
  margin-top: 30px;
  font-size: 11px;
  color: #a0a6b1;
}

.separator {
  color: #c9cdd4;
}

@media (max-width: 480px) {
  .login-page {
    padding: 16px;
  }

  .login-card {
    padding: 32px 24px;
  }
}
</style>