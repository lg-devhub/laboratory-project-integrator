import { createRouter, createWebHistory } from 'vue-router'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import LoginView from '@/views/LoginView.vue'
import AmbientesView from '@/views/AmbientesView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/ambientes',
      name: 'ambientes',
      component: AmbientesView,
      meta: { exigeAutenticacao: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(async (destino) => {
  const autenticacao = useAutenticacaoStore()
  // Nesta base pequena, cada navegacao confere a sessao real com o backend.
  await autenticacao.verificarSessao()

  if (destino.meta.exigeAutenticacao && !autenticacao.autenticado) {
    return { name: 'login' }
  }
  if (destino.name === 'login' && autenticacao.autenticado) {
    return { name: 'ambientes' }
  }
})

export default router
