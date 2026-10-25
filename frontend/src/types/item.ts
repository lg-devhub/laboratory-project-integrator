export type FormaRegistro = 'INDIVIDUAL' | 'QUANTIDADE'
export type StatusItem = 'ATIVO' | 'EM_MANUTENCAO' | 'INATIVO'

export const statusItens: { valor: StatusItem; nome: string }[] = [
  { valor: 'ATIVO', nome: 'Ativo' },
  { valor: 'EM_MANUTENCAO', nome: 'Em manutenção' },
  { valor: 'INATIVO', nome: 'Inativo' },
]

export interface ItemDados {
  nome: string
  categoria: string
  formaRegistro: FormaRegistro
  equipamento: boolean
  quantidade: number
  status: StatusItem
  numeroPatrimonio: string | null
  setorResponsavelId: number | null
  descricao: string | null
  localizacao: string | null
}

export interface Item extends ItemDados {
  id: number
  ambienteId: number
  setorResponsavelNome: string | null
  temFoto: boolean
  criadoEm: string
  versao: number
  excluidoEm: string | null
}

export interface ItemHistorico {
  id: number
  acao: string
  usuarioId: number
  usuarioNome: string
  dataHora: string
  statusAnterior: StatusItem | null
  statusNovo: StatusItem | null
}

export interface ItemEdicao {
  nome: string
  categoria: string
  quantidade: number
  status: StatusItem
  descricao: string | null
  localizacao: string | null
  versao: number
  fotoAcao: 'MANTER' | 'SUBSTITUIR' | 'REMOVER'
  fotoBase64: string | null
  fotoTipo: string | null
}

export interface SetorOpcao { id: number; nome: string }
