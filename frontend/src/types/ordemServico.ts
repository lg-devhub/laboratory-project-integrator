export const prioridadesOS = [
  { valor: 'BAIXA', nome: 'Baixa' },
  { valor: 'MEDIA', nome: 'Média' },
  { valor: 'ALTA', nome: 'Alta' },
] as const

export interface OrdemServicoDados { problema: string; prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' }
export type StatusOS = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA'
export const statusOS: Record<StatusOS, string> = { ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída' }
export interface OrdemServicoEdicao extends OrdemServicoDados {
  status: StatusOS
  descricaoAtendimento: string | null
  versao: number
}
export interface OrdemServico extends OrdemServicoDados {
  id: number
  equipamentoId: number
  solicitanteId: number
  solicitanteNome: string
  status: StatusOS
  abertaEm: string
  descricaoAtendimento: string | null
  fechadaEm: string | null
  versao: number
}
