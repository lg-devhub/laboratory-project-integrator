<template>
  <div class="itens-view">
    <AppHeader />

    <main class="content">
      <button type="button" class="voltar" @click="router.push('/hub')">
        ← Salas e setores
      </button>

      <template v-if="!setor">
        <p class="vazio">Sala ou setor não encontrado.</p>
      </template>

      <!-- RF14/RN-08: usuário comum só acessa salas atribuídas a ele -->
      <template v-else-if="!isAdmin && !setor.atribuidoAoUsuario">
        <div class="acesso-negado">
          <h1>Acesso não autorizado</h1>
          <p>Você não tem acesso a "{{ setor.nome }}". Fale com um administrador se isso não fizer sentido.</p>
        </div>
      </template>

      <template v-else>
        <div class="page-head">
          <div>
            <h1>{{ setor.nome }} <span class="codigo">{{ setor.codigo }}</span></h1>
            <p v-if="setor.descricao">{{ setor.descricao }}</p>
          </div>

          <!-- RF17: qualquer usuário com acesso à sala cadastra item, não só o admin -->
          <button v-if="podeGerenciar" type="button" class="btn-novo-item" @click="abrirModalNovoItem">
            + Novo item
          </button>
        </div>

        <div class="search-field">
          <input
            v-model="pesquisa"
            type="text"
            placeholder="Pesquisar item por nome ou categoria..."
            aria-label="Pesquisar item"
          />
        </div>

        <section v-if="itensVisiveis.length" class="tabela">
          <div class="tabela-cabecalho">
            <span>Item</span>
            <span>Categoria</span>
            <span>Qtd.</span>
            <span>Status</span>
            <span v-if="podeGerenciar"></span>
          </div>

          <div v-for="item in itensVisiveis" :key="item.id" class="linha">
            <div class="linha-nome">
              <strong>{{ item.nome }}</strong>
              <span v-if="item.descricao" class="linha-descricao">{{ item.descricao }}</span>
            </div>
            <span class="linha-categoria">{{ item.categoria || '—' }}</span>
            <span class="linha-quantidade">{{ item.quantidade }}</span>
            <span class="status-pill" :class="classeStatus(item.status)">{{ item.status }}</span>

            <div v-if="podeGerenciar" class="linha-acoes">
              <button type="button" class="acao" title="Editar item" @click="abrirModalEdicaoItem(item)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M12 20h9"></path>
                  <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"></path>
                </svg>
              </button>
              <button type="button" class="acao acao--danger" title="Excluir item" @click="itemParaExcluir = item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M3 6h18"></path>
                  <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path>
                </svg>
              </button>
            </div>
          </div>
        </section>

        <p v-else class="vazio">
          {{
            pesquisa
              ? 'Nenhum item encontrado para essa pesquisa.'
              : 'Nenhum item cadastrado nessa sala ainda.'
          }}
        </p>
      </template>
    </main>

    <!-- RF17/RF18/RF19/RF21: modal de criação/edição de item -->
    <div v-if="modalAberto" class="modal-overlay" @click.self="fecharModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ itemEmEdicao ? 'Editar item' : 'Novo item' }}</h2>
        </div>

        <form class="modal-body" @submit.prevent="salvarItem">
          <div class="campo">
            <label for="item-nome">Nome</label>
            <input id="item-nome" v-model.trim="form.nome" type="text" required maxlength="80" />
          </div>

          <div class="campo-linha">
            <div class="campo">
              <label for="item-categoria">Categoria</label>
              <input id="item-categoria" v-model.trim="form.categoria" type="text" maxlength="40" />
            </div>

            <div class="campo campo-qtd">
              <label for="item-quantidade">Quantidade</label>
              <input id="item-quantidade" v-model.number="form.quantidade" type="number" min="0" required />
            </div>
          </div>

          <div class="campo">
            <label for="item-status">Status</label>
            <select id="item-status" v-model="form.status">
              <option value="Ativo">Ativo</option>
              <option value="Em Manutenção">Em Manutenção</option>
              <option value="Inativo">Inativo</option>
            </select>
          </div>

          <div class="campo">
            <label for="item-descricao">Descrição</label>
            <textarea id="item-descricao" v-model.trim="form.descricao" rows="3" maxlength="200"></textarea>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-secundario" @click="fecharModal">Cancelar</button>
            <button type="submit" class="btn-primario">
              {{ itemEmEdicao ? 'Salvar alterações' : 'Cadastrar item' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- RF20: confirmação de exclusão de item -->
    <div v-if="itemParaExcluir" class="modal-overlay" @click.self="itemParaExcluir = null">
      <div class="modal modal--small">
        <div class="modal-header">
          <h2>Excluir "{{ itemParaExcluir.nome }}"?</h2>
        </div>

        <div class="modal-body">
          <p>Essa ação não pode ser desfeita.</p>

          <div class="modal-actions">
            <button type="button" class="btn-secundario" @click="itemParaExcluir = null">Cancelar</button>
            <button type="button" class="btn-perigo" @click="confirmarExclusaoItem">Excluir</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '../components/Header.vue'
import { sessao } from '../state/sessao'
import { setoresMock } from '../data/setoresMock'
import { itensMock } from '../data/itensMock'
import type { Item, ItemFormData, StatusItem } from '../types/stocka'

// props: true no router entrega o :id da rota como prop "id" (string)
const props = defineProps<{ id: string }>()

const router = useRouter()

const isAdmin = computed(() => sessao.perfil === 'ADMINISTRADOR')

const setorId = computed(() => Number(props.id))
const setor = computed(() => setoresMock.find((s) => s.id === setorId.value) ?? null)

// RF17: administrador sempre pode gerenciar itens; usuário comum só se
// a sala estiver atribuída a ele (mesma regra de acesso do RF14).
const podeGerenciar = computed(() => isAdmin.value || Boolean(setor.value?.atribuidoAoUsuario))

const pesquisa = ref('')

// RF26: pesquisa de itens dentro da sala
const itensVisiveis = computed(() => {
  const doSetor = itensMock.filter((item) => item.setorId === setorId.value)

  const termo = pesquisa.value.trim().toLowerCase()
  if (!termo) return doSetor

  return doSetor.filter(
    (item) =>
      item.nome.toLowerCase().includes(termo) || item.categoria.toLowerCase().includes(termo)
  )
})

function classeStatus(status: StatusItem) {
  if (status === 'Ativo') return 'status-pill--ativo'
  if (status === 'Em Manutenção') return 'status-pill--manutencao'
  return 'status-pill--inativo'
}

// ============================================================
// MODAL DE CRIAÇÃO / EDIÇÃO (RF17, RF18, RF19, RF21)
// ============================================================
const modalAberto = ref(false)
const itemEmEdicao = ref<Item | null>(null)

const form = reactive<ItemFormData>({
  nome: '',
  categoria: '',
  quantidade: 1,
  descricao: '',
  status: 'Ativo'
})

function abrirModalNovoItem() {
  itemEmEdicao.value = null
  form.nome = ''
  form.categoria = ''
  form.quantidade = 1
  form.descricao = ''
  form.status = 'Ativo'
  modalAberto.value = true
}

function abrirModalEdicaoItem(item: Item) {
  itemEmEdicao.value = item
  form.nome = item.nome
  form.categoria = item.categoria
  form.quantidade = item.quantidade
  form.descricao = item.descricao
  form.status = item.status
  modalAberto.value = true
}

function fecharModal() {
  modalAberto.value = false
}

function salvarItem() {
  if (itemEmEdicao.value) {
    // TODO back-end: PUT /setores/{id}/itens/{itemId} (ver services/itens.ts → editarItem)
    // Isso também cobre o RF21 (mudança de status), já que o status é
    // só mais um campo do mesmo formulário.
    const alvo = itensMock.find((i) => i.id === itemEmEdicao.value?.id)
    if (alvo) {
      alvo.nome = form.nome
      alvo.categoria = form.categoria
      alvo.quantidade = form.quantidade
      alvo.descricao = form.descricao
      alvo.status = form.status
    }
  } else {
    // TODO back-end: POST /setores/{id}/itens (ver services/itens.ts → criarItem)
    itensMock.push({
      id: Math.max(0, ...itensMock.map((i) => i.id)) + 1,
      setorId: setorId.value,
      nome: form.nome,
      categoria: form.categoria,
      quantidade: form.quantidade,
      descricao: form.descricao,
      status: form.status
    })
  }

  fecharModal()
}

// ============================================================
// EXCLUSÃO (RF20)
// ============================================================
const itemParaExcluir = ref<Item | null>(null)

function confirmarExclusaoItem() {
  if (!itemParaExcluir.value) return

  // TODO back-end: DELETE /setores/{id}/itens/{itemId} (ver services/itens.ts → excluirItem)
  const index = itensMock.findIndex((i) => i.id === itemParaExcluir.value?.id)
  if (index !== -1) itensMock.splice(index, 1)

  itemParaExcluir.value = null
}
</script>

<style scoped>
.itens-view {
  min-height: 100vh;
  background-color: #dadfe4;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 32px 60px;
  font-family: 'Manrope', system-ui, sans-serif;
}

.voltar {
  border: none;
  background: none;
  padding: 0;
  margin-bottom: 20px;
  color: #2b2d42;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.voltar:hover {
  color: #ef233c;
}

.acesso-negado {
  max-width: 480px;
  margin: 60px auto 0;
  text-align: center;
}

.acesso-negado h1 {
  margin: 0 0 10px;
  font-size: 22px;
  color: #2b2d42;
}

.acesso-negado p {
  margin: 0;
  color: #6b7280;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.content h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #111827;
}

.codigo {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.content p {
  margin: 0;
  color: #6b7280;
}

.btn-novo-item {
  padding: 11px 18px;
  border: none;
  border-radius: 9px;
  background-color: #2b2d42;
  color: #edf2f4;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s ease;
}

.btn-novo-item:hover {
  background-color: #ef233c;
}

.search-field {
  margin-top: 24px;
  max-width: 420px;
}

.search-field input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  box-sizing: border-box;
  border: 1px solid #dfe2e8;
  border-radius: 9px;
  outline: none;
  background: #ffffff;
  color: #202633;
  font-size: 14px;
  font-family: inherit;
}

.search-field input:focus {
  border-color: #2b2d42;
  box-shadow: 0 0 0 3px rgba(43, 45, 66, 0.1);
}

/* ---------------------------------------------------------- */
/* Tabela de itens                                              */
/* ---------------------------------------------------------- */

.tabela {
  margin-top: 24px;
  background: #ffffff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e4ea;
}

.tabela-cabecalho,
.linha {
  display: grid;
  grid-template-columns: 2fr 1fr 0.6fr 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
}

.tabela-cabecalho {
  background: #f4f5f8;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #6b7280;
}

.linha {
  border-top: 1px solid #eef0f3;
  font-size: 14px;
  color: #111827;
}

.linha-nome {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.linha-descricao {
  font-size: 12.5px;
  color: #6b7280;
}

.linha-categoria,
.linha-quantidade {
  color: #4b5563;
}

.status-pill {
  display: inline-block;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-pill--ativo {
  background: rgba(45, 157, 111, 0.12);
  color: #2d9d6f;
}

.status-pill--manutencao {
  background: rgba(227, 166, 61, 0.16);
  color: #a5701a;
}

.status-pill--inativo {
  background: rgba(141, 153, 174, 0.2);
  color: #5b6674;
}

.linha-acoes {
  display: flex;
  gap: 6px;
  justify-self: end;
}

.acao {
  width: 28px;
  height: 28px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 1px solid #e2e4ea;
  border-radius: 8px;
  background-color: #ffffff;
  color: #2b2d42;

  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.acao svg {
  width: 14px;
  height: 14px;
}

.acao:hover {
  border-color: #2b2d42;
}

.acao--danger:hover {
  border-color: #ef233c;
  color: #ef233c;
}

.vazio {
  margin-top: 40px;
  text-align: center;
}

@media (max-width: 720px) {
  .tabela-cabecalho {
    display: none;
  }

  .linha {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .linha-acoes {
    justify-self: start;
    margin-top: 6px;
  }
}

/* ---------------------------------------------------------- */
/* Modais (mesmo padrão visual usado no HubView)                */
/* ---------------------------------------------------------- */

.modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(2px);
  padding: 16px;
  z-index: 1000;
}

.modal {
  width: 100%;
  max-width: 440px;
  background-color: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  font-family: 'Manrope', system-ui, sans-serif;
}

.modal--small {
  max-width: 400px;
}

.modal-header {
  padding: 18px 22px;
  background-color: #2b2d42;
}

.modal-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #edf2f4;
}

.modal-body {
  padding: 20px 22px 22px;
}

.campo {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.campo-linha {
  display: flex;
  gap: 12px;
}

.campo-linha .campo {
  flex: 1;
}

.campo-qtd {
  max-width: 120px;
}

.campo label {
  font-size: 13px;
  font-weight: 600;
  color: #3d4554;
}

.campo input,
.campo select,
.campo textarea {
  padding: 10px 12px;
  border: 1px solid #dfe2e8;
  border-radius: 8px;
  font-family: inherit;
  font-size: 14px;
  color: #202633;
  resize: vertical;
  background: #ffffff;
}

.campo input:focus,
.campo select:focus,
.campo textarea:focus {
  outline: none;
  border-color: #2b2d42;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.btn-secundario {
  padding: 10px 16px;
  border: 1px solid #dfe2e8;
  border-radius: 8px;
  background: transparent;
  color: #202633;
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
}

.btn-primario {
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: #2b2d42;
  color: #edf2f4;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-primario:hover {
  background: #1d1e2e;
}

.btn-perigo {
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: #ef233c;
  color: #ffffff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
</style>
