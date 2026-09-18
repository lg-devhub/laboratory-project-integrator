import type { Setor, SetorFormData } from '../types/stocka'

const BASE_URL = 'http://localhost:8080'

// Funções de requisição para salas/setores — prontas para quando o
// back-end tiver essas rotas (hoje só /login existe, ver
// Controller_Login.java). Nada aqui é chamado automaticamente ainda:
// o HubView continua lendo de data/setoresMock.ts. Quando o back
// estiver pronto, trocar a leitura do mock por uma chamada a
// listarSetores() (dentro de um onMounted, por exemplo) resolve isso.
//
// Ajuste os nomes dos campos no corpo das requisições conforme o
// Model_Sector/DAO_Sector reais forem definidos no back-end.

export async function listarSetores(): Promise<Setor[]> {
  const response = await fetch(`${BASE_URL}/setores`)

  if (!response.ok) {
    throw new Error('Não foi possível carregar as salas e setores.')
  }

  return response.json()
}

export async function criarSetor(dados: SetorFormData): Promise<Setor> {
  const response = await fetch(`${BASE_URL}/setores`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  })

  if (!response.ok) {
    throw new Error('Não foi possível criar a sala/setor.')
  }

  return response.json()
}

export async function editarSetor(id: number, dados: SetorFormData): Promise<Setor> {
  const response = await fetch(`${BASE_URL}/setores/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados)
  })

  if (!response.ok) {
    throw new Error('Não foi possível salvar as alterações da sala/setor.')
  }

  return response.json()
}

export async function excluirSetor(id: number): Promise<void> {
  const response = await fetch(`${BASE_URL}/setores/${id}`, {
    method: 'DELETE'
  })

  if (!response.ok) {
    throw new Error('Não foi possível excluir a sala/setor.')
  }
}
