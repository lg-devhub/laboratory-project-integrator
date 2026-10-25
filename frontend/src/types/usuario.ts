// Somente os campos publicos retornados pela API; nao inclui senha nem hash.
export interface Usuario {
  id: number
  nome: string
  login: string
  perfil: 'ADMINISTRADOR' | 'USUARIO'
}
