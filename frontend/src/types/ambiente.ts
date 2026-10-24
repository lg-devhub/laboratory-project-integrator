export const coresAmbiente = [
  { valor: 'TERRACOTA', nome: 'Terracota', hex: '#b64c1d' },
  { valor: 'AZUL', nome: 'Azul', hex: '#376582' },
  { valor: 'VERDE', nome: 'Verde', hex: '#426a51' },
  { valor: 'VIOLETA', nome: 'Violeta', hex: '#765683' },
  { valor: 'GRAFITE', nome: 'Grafite', hex: '#525c65' },
] as const

export type CorAmbiente = typeof coresAmbiente[number]['valor']

export interface AmbienteDados {
  nome: string
  tipo: 'SALA' | 'SETOR'
  descricao: string | null
  cor: CorAmbiente
}

export interface Ambiente extends AmbienteDados { id: number }

export interface AcessoAmbiente {
  id: number
  nome: string
  login: string
  ativo: boolean
  atribuido: boolean
}
