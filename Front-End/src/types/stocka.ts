export type Perfil = 'ADMINISTRADOR' | 'USUARIO' // == PerfilEnum no DER do ERS

export interface Setor {
  id: number
  nome: string
  codigo: string
  descricao: string
  itens: number
  itensAtencao: number
  // RF14/RN-08: usuário comum só enxerga salas/setores atribuídos a ele.
  // TODO back-end: essa informação depende de uma tabela de atribuição
  // usuário-setor que ainda não existe no DER atual (ver ERS, RF10/RF11).
  atribuidoAoUsuario: boolean
}

export interface SetorFormData {
  nome: string
  codigo: string
  descricao: string
}

// RN-07: status de item só pode assumir um desses três valores
export type StatusItem = 'Ativo' | 'Em Manutenção' | 'Inativo'

export interface Item {
  id: number
  setorId: number
  nome: string
  categoria: string
  quantidade: number
  descricao: string
  status: StatusItem
}

export interface ItemFormData {
  nome: string
  categoria: string
  quantidade: number
  descricao: string
  status: StatusItem
}
