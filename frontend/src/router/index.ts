import { createRouter, createWebHistory } from 'vue-router'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import LoginView from '@/views/LoginView.vue'
import AmbientesView from '@/views/AmbientesView.vue'
import InventarioView from '@/views/InventarioView.vue'
import UsuariosView from '@/views/UsuariosView.vue'

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
    {
      path: '/ambientes/:ambienteId',
      name: 'inventario',
      component: InventarioView,
      meta: { exigeAutenticacao: true },
    },
    { path: '/usuarios', name: 'usuarios', component: UsuariosView, meta: { exigeAutenticacao: true, exigeAdministrador: true } },
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
  if (destino.meta.exigeAdministrador && autenticacao.usuario?.perfil !== 'ADMINISTRADOR') return { name: 'ambientes' }
  if (destino.name === 'login' && autenticacao.autenticado) {
    return { name: 'ambientes' }
  }
})

export default router
