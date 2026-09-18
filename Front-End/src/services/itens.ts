import type { Item, ItemFormData } from '../types/stocka'

const BASE_URL = 'http://localhost:8080'

// Mesmo espírito de services/setores.ts: prontas para o dia em que o
// back-end tiver as rotas de item (RF17-RF21), não chamadas
// automaticamente ainda. SetorItemsView.vue usa data/itensMock.ts por
// enquanto.

export async function listarItens(setorId: number): Promise<Item[]> {
  const response = await fetch(`${BASE_URL}/setores/${setorId}/itens`)

  if (!response.ok) {
    throw new Error('Não foi possível carregar os itens dessa sala.')
  }

  return response.json()
}

export async function criarItem(setorId: number, dados: ItemFormData): Promise<Item> {
  const response = await fetch(`${BASE_URL}/setores/${setorId}/itens`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  })

  if (!response.ok) {
    throw new Error('Não foi possível cadastrar o item.')
  }

  return response.json()
}

export async function editarItem(setorId: number, itemId: number, dados: ItemFormData): Promise<Item> {
  const response = await fetch(`${BASE_URL}/setores/${setorId}/itens/${itemId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  })

  if (!response.ok) {
    throw new Error('Não foi possível salvar as alterações do item.')
  }

  return response.json()
}

export async function excluirItem(setorId: number, itemId: number): Promise<void> {
  const response = await fetch(`${BASE_URL}/setores/${setorId}/itens/${itemId}`, {
    method: 'DELETE'
  })

  if (!response.ok) {
    throw new Error('Não foi possível excluir o item.')
  }
}
