import { requisicaoApi } from './api'
import type { UsuarioDados, UsuarioGestao } from '@/types/gestaoUsuario'

export class ErroGestaoUsuario extends Error {
  constructor(public status: number, mensagem: string, public usuarioAtual?: UsuarioGestao) { super(mensagem) }
}
async function respostaConta(resposta: Response): Promise<UsuarioGestao> {
  const dados = await resposta.json().catch(() => null)
  if (!resposta.ok) throw new ErroGestaoUsuario(resposta.status, dados?.mensagem || 'Não foi possível concluir a operação.', dados?.usuarioAtual)
  return dados
}
export async function listarUsuarios(): Promise<UsuarioGestao[]> {
  const resposta = await requisicaoApi('/api/usuarios')
  if (!resposta.ok) { await respostaConta(resposta); throw new Error('Consulta indisponível.') }
  return resposta.json()
}
export async function salvarUsuario(id: number | null, dados: UsuarioDados): Promise<UsuarioGestao> {
  return respostaConta(await requisicaoApi('/api/usuarios' + (id === null ? '' : '/' + id), {
    method: id === null ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
  }))
}
export async function desativarUsuario(conta: UsuarioGestao): Promise<UsuarioGestao> {
  return respostaConta(await requisicaoApi('/api/usuarios/' + conta.id + '/desativacao', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ versao: conta.versao }),
  }))
}
