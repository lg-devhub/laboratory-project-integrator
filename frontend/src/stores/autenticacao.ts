import { defineStore } from 'pinia'
import * as autenticacaoApi from '@/services/autenticacao'
import type { Usuario } from '@/types/usuario'

export const useAutenticacaoStore = defineStore('autenticacao', {
  state: () => ({
    usuario: null as Usuario | null,
    verificandoSessao: false,
    erroSessao: '',
  }),

  getters: {
    autenticado: (state) => state.usuario !== null,
  },

  actions: {
    async verificarSessao() {
      this.verificandoSessao = true
      this.erroSessao = ''
      try {
        this.usuario = await autenticacaoApi.consultarSessao()
      } catch (erro) {
        this.usuario = null
        this.erroSessao = erro instanceof Error ? erro.message : 'Não foi possível verificar a sessão.'
      } finally {
        this.verificandoSessao = false
      }
    },

    async entrar(login: string, senha: string) {
      this.erroSessao = ''
      this.usuario = null
      this.usuario = await autenticacaoApi.entrar(login, senha)
      this.erroSessao = ''
    },

    async sair() {
      await autenticacaoApi.sair()
      this.usuario = null
      this.erroSessao = ''
    },
  },
})
