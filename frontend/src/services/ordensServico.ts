import { requisicaoApi } from './api'
import { conferir, ErroInventario } from './itens'
import type { OrdemServico, OrdemServicoDados, OrdemServicoEdicao } from '@/types/ordemServico'

export class ConflitoOrdemServico extends ErroInventario {
  constructor(mensagem: string, public ordemAtual: OrdemServico) { super(409, mensagem) }
}

function caminho(ambienteId: number, itemId: number) {
  return `/api/ambientes/${ambienteId}/itens/${itemId}/ordens-servico`
}
export async function listarOrdensServico(ambienteId: number, itemId: number): Promise<OrdemServico[]> {
  const resposta = await requisicaoApi(caminho(ambienteId, itemId))
  await conferir(resposta)
  return resposta.json()
}
export async function abrirOrdemServico(ambienteId: number, itemId: number, dados: OrdemServicoDados): Promise<OrdemServico> {
  const resposta = await requisicaoApi(caminho(ambienteId, itemId), {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
  })
  await conferir(resposta)
  return resposta.json()
}

export async function atualizarOrdemServico(ambienteId: number, itemId: number, id: number, dados: OrdemServicoEdicao): Promise<OrdemServico> {
  const resposta = await requisicaoApi(`${caminho(ambienteId, itemId)}/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
  })
  if (resposta.status === 409) {
    const falha = await resposta.json()
    if (falha.ordemAtual) throw new ConflitoOrdemServico(falha.mensagem, falha.ordemAtual)
    throw new ErroInventario(409, falha.mensagem || 'A OS foi alterada. Atualize a consulta.')
  }
  await conferir(resposta)
  return resposta.json()
}
