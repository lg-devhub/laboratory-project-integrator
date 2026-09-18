import { reactive } from 'vue'
import type { Item } from '../types/stocka'

// Itens fictícios, alguns por setor (setorId bate com o id em setoresMock.ts).
// Servem só pra popular a tela nova de itens (RF17/RF18) — quando o
// back-end tiver as rotas, isso é substituído pelo retorno de
// listarItens() em services/itens.ts.
export const itensMock = reactive<Item[]>([
  // Setor 1 — Laboratório de Informática
  { id: 1, setorId: 1, nome: 'Computador Dell OptiPlex', categoria: 'Informática', quantidade: 12, descricao: 'Estações de trabalho dos alunos.', status: 'Ativo' },
  { id: 2, setorId: 1, nome: 'Monitor LG 21"', categoria: 'Informática', quantidade: 12, descricao: 'Monitores das estações.', status: 'Ativo' },
  { id: 3, setorId: 1, nome: 'Projetor Epson', categoria: 'Audiovisual', quantidade: 1, descricao: 'Uso didático, sala principal.', status: 'Em Manutenção' },
  { id: 4, setorId: 1, nome: 'Roteador TP-Link', categoria: 'Rede', quantidade: 2, descricao: 'Distribuição de internet do laboratório.', status: 'Ativo' },

  // Setor 2 — Sala 101
  { id: 5, setorId: 2, nome: 'Carteira escolar', categoria: 'Mobiliário', quantidade: 40, descricao: 'Carteiras dos alunos.', status: 'Ativo' },
  { id: 6, setorId: 2, nome: 'Quadro branco', categoria: 'Mobiliário', quantidade: 1, descricao: '', status: 'Ativo' },

  // Setor 3 — Almoxarifado
  { id: 7, setorId: 3, nome: 'Caixa de parafusos M4', categoria: 'Material de manutenção', quantidade: 300, descricao: '', status: 'Ativo' },
  { id: 8, setorId: 3, nome: 'Furadeira Bosch', categoria: 'Ferramenta', quantidade: 3, descricao: '', status: 'Em Manutenção' },
  { id: 9, setorId: 3, nome: 'Extintor de incêndio', categoria: 'Segurança', quantidade: 4, descricao: 'Validade a verificar.', status: 'Inativo' },

  // Setor 4 — Cantina
  { id: 10, setorId: 4, nome: 'Geladeira industrial', categoria: 'Eletrodoméstico', quantidade: 1, descricao: '', status: 'Ativo' },
  { id: 11, setorId: 4, nome: 'Fogão industrial', categoria: 'Eletrodoméstico', quantidade: 1, descricao: '', status: 'Ativo' }
])
