import { reactive } from 'vue'
import type { Setor } from '../types/stocka'

// Mesmos setores que já estavam no HubView original — só movi pra cá pra
// que a tela de itens (SetorItemsView) também consiga localizar o setor
// pelo id da rota, e pra que criar/editar/excluir sala num lugar reflita
// em qualquer outro componente que leia essa lista.
//
// itens/itensAtencao aqui viram só um valor inicial: o HubView recalcula
// os dois números de verdade a partir de itensMock.ts, pra nunca ficar
// inconsistente com os itens que aparecem na tela de cada sala.
export const setoresMock = reactive<Setor[]>([
  {
    id: 1,
    nome: 'Laboratório de Informática',
    codigo: 'LAB-01',
    descricao: 'Laboratório principal',
    itens: 0,
    itensAtencao: 0,
    atribuidoAoUsuario: true
  },
  {
    id: 2,
    nome: 'Sala 101',
    codigo: 'SAL-101',
    descricao: 'Sala de aula',
    itens: 0,
    itensAtencao: 0,
    atribuidoAoUsuario: false
  },
  {
    id: 3,
    nome: 'Almoxarifado',
    codigo: 'ALM-01',
    descricao: 'Armazenamento de materiais',
    itens: 0,
    itensAtencao: 0,
    atribuidoAoUsuario: true
  },
  {
    id: 4,
    nome: 'Cantina',
    codigo: 'Cantina',
    descricao: 'Armazenamento de alimentos',
    itens: 0,
    itensAtencao: 0,
    atribuidoAoUsuario: false
  }
])
