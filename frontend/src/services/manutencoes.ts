import { requisicaoApi } from './api'
import { conferir } from './itens'
import type { Manutencao, ManutencaoDados } from '@/types/manutencao'

function caminho(ambiente: number, item: number) { return '/api/ambientes/' + ambiente + '/itens/' + item + '/manutencoes' }
export async function listarManutencoes(ambiente: number, item: number): Promise<Manutencao[]> {
  const resposta = await requisicaoApi(caminho(ambiente, item))
  await conferir(resposta)
  return resposta.json()
}
export async function registrarManutencao(ambiente: number, item: number, dados: ManutencaoDados): Promise<Manutencao> {
  const resposta = await requisicaoApi(caminho(ambiente, item), {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(dados),
  })
  await conferir(resposta)
  return resposta.json()
}
