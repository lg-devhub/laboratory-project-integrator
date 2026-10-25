import { requisicaoApi } from './api'
import type { Ambiente, AmbienteDados, AcessoAmbiente } from '@/types/ambiente'

export class ErroAmbiente extends Error {
  constructor(public status: number, mensagem: string, public codigo?: string) { super(mensagem) }
}

async function conferir(resposta: Response) {
  if (resposta.ok) return
  if (resposta.status === 401) throw new ErroAmbiente(401, 'Sua sessão expirou. Entre novamente.')
  if (resposta.status === 403) throw new ErroAmbiente(403, 'Somente administradores podem realizar esta ação.')
  const dados = await resposta.json().catch(() => null)
  throw new ErroAmbiente(resposta.status, dados?.mensagem || 'Não foi possível concluir a operação. Tente novamente.', dados?.codigo)
}

export async function retirarAmbiente(ambiente: Ambiente): Promise<void> {
  const resposta = await requisicaoApi('/api/ambientes/' + ambiente.id + '?versao=' + ambiente.versao, { method: 'DELETE' })
  await conferir(resposta)
}

// null: sessao invalida; []: conta valida sem ambientes disponiveis.
export async function listarAmbientes(): Promise<Ambiente[] | null> {
  const resposta = await requisicaoApi('/api/ambientes')
  if (resposta.status === 401) return null
  await conferir(resposta)
  return resposta.json()
}

export async function salvarAmbiente(dados: AmbienteDados, id?: number): Promise<Ambiente> {
  const resposta = await requisicaoApi(id === undefined ? '/api/ambientes' : `/api/ambientes/${id}`, {
    method: id === undefined ? 'POST' : 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados),
  })
  await conferir(resposta)
  return resposta.json()
}

export async function listarAcessos(id: number): Promise<AcessoAmbiente[]> {
  const resposta = await requisicaoApi(`/api/ambientes/${id}/acessos`)
  await conferir(resposta)
  return resposta.json()
}

export async function salvarAcessos(id: number, usuarios: number[]): Promise<void> {
  const resposta = await requisicaoApi(`/api/ambientes/${id}/acessos`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ usuarios }),
  })
  await conferir(resposta)
}
