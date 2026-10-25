import { requisicaoApi } from './api'
import type { Ambiente } from '@/types/ambiente'
import type { Item, ItemDados, ItemEdicao, ItemHistorico, SetorOpcao } from '@/types/item'

export class ErroInventario extends Error {
  constructor(public status: number, mensagem: string, public codigo?: string, public itemAtual?: Item) { super(mensagem) }
}

export async function conferir(resposta: Response) {
  if (resposta.ok) return
  if (resposta.status === 401) throw new ErroInventario(401, 'Sua sessão expirou. Entre novamente.')
  if (resposta.status === 404) throw new ErroInventario(404, 'Registro indisponível ou sem permissão de acesso.')
  const dados = await resposta.json().catch(() => null)
  throw new ErroInventario(resposta.status, dados?.mensagem || 'Não foi possível concluir a operação. Tente novamente.', dados?.codigo, dados?.itemAtual)
}

export async function consultarAmbiente(id: number): Promise<Ambiente> {
  const resposta = await requisicaoApi(`/api/ambientes/${id}`)
  await conferir(resposta)
  return resposta.json()
}

export async function listarItens(id: number, excluidos = false): Promise<Item[]> {
  const resposta = await requisicaoApi('/api/ambientes/' + id + '/itens?excluidos=' + excluidos)
  await conferir(resposta)
  return resposta.json()
}

export async function listarSetores(): Promise<SetorOpcao[]> {
  const resposta = await requisicaoApi('/api/itens/setores-responsaveis')
  await conferir(resposta)
  return resposta.json()
}

export async function cadastrarItem(id: number, dados: ItemDados, foto?: { base64: string; tipo: string }): Promise<Item> {
  const resposta = await requisicaoApi(`/api/ambientes/${id}/itens`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...dados, fotoBase64: foto?.base64 ?? null, fotoTipo: foto?.tipo ?? null }),
  })
  await conferir(resposta)
  return resposta.json()
}

export async function consultarHistorico(ambienteId: number, itemId: number): Promise<ItemHistorico[]> {
  const resposta = await requisicaoApi(`/api/ambientes/${ambienteId}/itens/${itemId}/historico`)
  await conferir(resposta)
  return resposta.json()
}

export async function editarItem(ambienteId: number, itemId: number, dados: ItemEdicao): Promise<Item> {
  const resposta = await requisicaoApi(`/api/ambientes/${ambienteId}/itens/${itemId}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
  })
  await conferir(resposta)
  return resposta.json()
}

export async function consultarFoto(ambienteId: number, itemId: number): Promise<Blob> {
  const resposta = await requisicaoApi(`/api/ambientes/${ambienteId}/itens/${itemId}/foto`)
  await conferir(resposta)
  return resposta.blob()
}

export async function excluirItem(item: Item): Promise<Item> {
  const resposta = await requisicaoApi('/api/ambientes/' + item.ambienteId + '/itens/' + item.id + '?versao=' + item.versao, { method: 'DELETE' })
  await conferir(resposta)
  return resposta.json()
}
