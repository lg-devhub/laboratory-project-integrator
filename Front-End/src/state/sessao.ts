import { reactive } from 'vue'
import type { Perfil } from '../types/stocka'

// Estado mínimo de sessão, compartilhado entre Header.vue, HubView.vue e
// SetorItemsView.vue — eles não têm relação pai/filho direta, então um
// pouco de estado compartilhado aqui evita ter que passar o perfil por
// props em vários níveis.
//
// TODO back-end: quando o login devolver o perfil de verdade (o
// Model_User hoje só tem id/name/email/password — falta o PerfilEnum do
// DER), substitua a simulação abaixo por esse valor, guardado junto do
// resto do usuário no sessionStorage.
export const sessao = reactive({
  perfil: 'ADMINISTRADOR' as Perfil
})

export function definirPerfil(perfil: Perfil) {
  sessao.perfil = perfil
}
