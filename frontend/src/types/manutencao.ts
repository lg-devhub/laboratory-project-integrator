export interface ManutencaoDados {
  dataRealizacao: string
  descricaoServico: string
  responsavelTecnico: string
  ordemServicoId: number | null
}
export interface Manutencao extends ManutencaoDados {
  id: number
  equipamentoId: number
  usuarioId: number
  usuarioNome: string
  registradoEm: string
}
