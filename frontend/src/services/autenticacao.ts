import { requisicaoApi } from './api'
import type { Usuario } from '@/types/usuario'

async function erroDaResposta(resposta: Response): Promise<Error> {
  const dados = await resposta.json().catch(() => null)
  return new Error(dados?.mensagem || 'Não foi possível concluir a solicitação.')
}

export async function entrar(login: string, senha: string): Promise<Usuario> {
  const resposta = await requisicaoApi('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ login, senha }),
  })
  if (!resposta.ok) throw await erroDaResposta(resposta)
  return resposta.json()
}

export async function consultarSessao(): Promise<Usuario | null> {
  const resposta = await requisicaoApi('/api/auth/me')
  if (resposta.status === 401) return null
  if (!resposta.ok) throw await erroDaResposta(resposta)
  return resposta.json()
}

export async function sair(): Promise<void> {
  const resposta = await requisicaoApi('/api/auth/logout', { method: 'POST' })
  if (!resposta.ok) throw await erroDaResposta(resposta)
}

export async function alterarSenha(senhaAtual: string, novaSenha: string, confirmacao: string): Promise<boolean> {
  const resposta = await requisicaoApi('/api/auth/senha', {
    method: 'PUT', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ senhaAtual, novaSenha, confirmacao }),
  })
  if (resposta.status === 401) return false
  if (!resposta.ok) throw await erroDaResposta(resposta)
  return true
}
