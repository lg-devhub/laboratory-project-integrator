<template>
  <div class="hub">
    <AppHeader />

    <main class="content">
      <div class="page-head">
        <div>
          <h1>Salas e Setores</h1>
          <p v-if="isAdmin">Selecione uma sala ou setor para acessar o inventário.</p>
          <p v-else>Salas e setores para os quais você tem acesso liberado.</p>
        </div>

        <!-- RF08: só o administrador cria salas/setores -->
        <button v-if="isAdmin" type="button" class="btn-nova-sala" @click="abrirModalNovaSala">
          + Nova sala / setor
        </button>
      </div>

      <div class="search-field">
        <input
          v-model="pesquisa"
          type="text"
          placeholder="Pesquisar por nome da sala ou setor..."
          aria-label="Pesquisar sala ou setor"
        />
      </div>

      <section v-if="setoresVisiveis.length" class="cards">
        <Card
          v-for="setor in setoresVisiveis"
          :key="setor.id"
          :setor="setor"
          :is-admin="isAdmin"
          @abrir="abrirSala"
          @editar="abrirModalEdicao"
          @excluir="pedirConfirmacaoExclusao"
        />
      </section>

      <p v-else class="vazio">
        {{
          pesquisa
            ? 'Nenhuma sala ou setor encontrado para essa pesquisa.'
            : 'Nenhuma sala ou setor disponível no momento.'
        }}
      </p>
    </main>

    <!-- RF08/RF09/RF12: modal de criação/edição de sala -->
    <div v-if="modalAberto" class="modal-overlay" @click.self="fecharModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ setorEmEdicao ? 'Editar sala / setor' : 'Nova sala / setor' }}</h2>
        </div>

        <form class="modal-body" @submit.prevent="salvarSala">
          <div class="campo">
            <label for="nome">Nome</label>
            <input id="nome" v-model.trim="form.nome" type="text" required maxlength="80" />
          </div>

          <div class="campo">
            <label for="codigo">Código</label>
            <input id="codigo" v-model.trim="form.codigo" type="text" placeholder="Ex.: LAB-02" maxlength="20" />
          </div>

          <div class="campo">
            <label for="descricao">Descrição</label>
            <textarea id="descricao" v-model.trim="form.descricao" rows="3" maxlength="200"></textarea>
          </div>

          <p v-if="erroFormulario" class="erro">{{ erroFormulario }}</p>

          <div class="modal-actions">
            <button type="button" class="btn-secundario" @click="fecharModal">Cancelar</button>
            <button type="submit" class="btn-primario">
              {{ setorEmEdicao ? 'Salvar alterações' : 'Criar sala' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- RF13/RN-05: confirmação de exclusão -->
    <div v-if="setorParaExcluir" class="modal-overlay" @click.self="setorParaExcluir = null">
      <div class="modal modal--small">
        <div class="modal-header">
          <h2>Excluir "{{ setorParaExcluir.nome }}"?</h2>
        </div>

        <div class="modal-body">
          <p v-if="setorParaExcluir.itens > 0" class="erro">
            Essa sala possui {{ setorParaExcluir.itens }} item(ns) vinculado(s). Pela RN-05, os itens
            precisam ser realocados ou removidos antes da exclusão do setor.
          </p>
          <p v-else>Essa ação não pode ser desfeita.</p>

          <div class="modal-actions">
            <button type="button" class="btn-secundario" @click="setorParaExcluir = null">
              Cancelar
            </button>
            <button
              type="button"
              class="btn-perigo"
              :disabled="setorParaExcluir.itens > 0"
              @click="confirmarExclusao"
            >
              Excluir
            </button>
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
import Card from '../components/Card.vue'
import { sessao } from '../state/sessao'
import { setoresMock } from '../data/setoresMock'
import { itensMock } from '../data/itensMock'
import type { Setor, SetorFormData } from '../types/stocka'

const router = useRouter()

const isAdmin = computed(() => sessao.perfil === 'ADMINISTRADOR')

// Os números de itens/itens em atenção não vêm mais fixos no mock: são
// recalculados a partir de itensMock, pra nunca contradizer o que
// aparece dentro de cada sala (SetorItemsView).
const setoresComContagem = computed<Setor[]>(() =>
  setoresMock.map((setor) => ({
    ...setor,
    itens: itensMock.filter((item) => item.setorId === setor.id).length,
    itensAtencao: itensMock.filter(
      (item) => item.setorId === setor.id && item.status === 'Em Manutenção'
    ).length
  }))
)

const pesquisa = ref('')

// RF14/RF15/RF16/RF27: administrador vê tudo; usuário comum só vê o que
// lhe foi atribuído. A pesquisa filtra por nome em cima disso.
const setoresVisiveis = computed(() => {
  const base = isAdmin.value
    ? setoresComContagem.value
    : setoresComContagem.value.filter((s) => s.atribuidoAoUsuario)

  const termo = pesquisa.value.trim().toLowerCase()
  if (!termo) return base

  return base.filter((s) => s.nome.toLowerCase().includes(termo))
})

function abrirSala(setor: Setor) {
  router.push(`/setores/${setor.id}`)
}

// ============================================================
// MODAL DE CRIAÇÃO / EDIÇÃO (RF08, RF09, RF12)
// ============================================================
const modalAberto = ref(false)
const setorEmEdicao = ref<Setor | null>(null)
const erroFormulario = ref('')

const form = reactive<SetorFormData>({
  nome: '',
  codigo: '',
  descricao: ''
})

function abrirModalNovaSala() {
  setorEmEdicao.value = null
  form.nome = ''
  form.codigo = ''
  form.descricao = ''
  erroFormulario.value = ''
  modalAberto.value = true
}

function abrirModalEdicao(setor: Setor) {
  setorEmEdicao.value = setor
  form.nome = setor.nome
  form.codigo = setor.codigo
  form.descricao = setor.descricao
  erroFormulario.value = ''
  modalAberto.value = true
}

function fecharModal() {
  modalAberto.value = false
}

function salvarSala() {
  // RN-02: nome de sala/setor deve ser único (checagem só no front por
  // enquanto — a que vale de verdade precisa vir do back-end).
  const nomeDuplicado = setoresMock.some(
    (s) => s.nome.toLowerCase() === form.nome.toLowerCase() && s.id !== setorEmEdicao.value?.id
  )

  if (nomeDuplicado) {
    erroFormulario.value = 'Já existe uma sala ou setor com esse nome (RN-02).'
    return
  }

  if (setorEmEdicao.value) {
    // TODO back-end: PUT /setores/{id} (ver services/setores.ts → editarSetor)
    const alvo = setoresMock.find((s) => s.id === setorEmEdicao.value?.id)
    if (alvo) {
      alvo.nome = form.nome
      alvo.codigo = form.codigo
      alvo.descricao = form.descricao
    }
  } else {
    // TODO back-end: POST /setores (ver services/setores.ts → criarSetor)
    setoresMock.push({
      id: Math.max(0, ...setoresMock.map((s) => s.id)) + 1,
      nome: form.nome,
      codigo: form.codigo,
      descricao: form.descricao,
      itens: 0,
      itensAtencao: 0,
      atribuidoAoUsuario: true
    })
  }

  fecharModal()
}

// ============================================================
// EXCLUSÃO (RF13, RN-05)
// ============================================================
const setorParaExcluir = ref<Setor | null>(null)

function pedirConfirmacaoExclusao(setor: Setor) {
  // Usa a contagem real (setoresComContagem), não o valor cru do mock
  const setorAtual = setoresComContagem.value.find((s) => s.id === setor.id)
  setorParaExcluir.value = setorAtual ?? setor
}

function confirmarExclusao() {
  if (!setorParaExcluir.value || setorParaExcluir.value.itens > 0) return

  // TODO back-end: DELETE /setores/{id} (ver services/setores.ts → excluirSetor)
  const index = setoresMock.findIndex((s) => s.id === setorParaExcluir.value?.id)
  if (index !== -1) setoresMock.splice(index, 1)

  setorParaExcluir.value = null
}
</script>

<style scoped>
.hub {
  min-height: 100vh;
  background-color: #dadfe4;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 32px;
  font-family: 'Manrope', system-ui, sans-serif;
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
  font-size: 28px;
  color: #111827;
}

.content p {
  margin: 0;
  color: #6b7280;
}

.btn-nova-sala {
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

.btn-nova-sala:hover {
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
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-field input:focus {
  border-color: #2b2d42;
  box-shadow: 0 0 0 3px rgba(43, 45, 66, 0.1);
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 24px;
}

.vazio {
  margin-top: 40px;
  text-align: center;
}

@media (max-width: 900px) {
  .cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .content {
    padding: 28px 16px;
  }

  .cards {
    grid-template-columns: 1fr;
  }
}

/* ---------------------------------------------------------- */
/* Modais (mesmo padrão visual do modal de Configurações no Header) */
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
  max-width: 420px;

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

.campo label {
  font-size: 13px;
  font-weight: 600;
  color: #3d4554;
}

.campo input,
.campo textarea {
  padding: 10px 12px;
  border: 1px solid #dfe2e8;
  border-radius: 8px;
  font-family: inherit;
  font-size: 14px;
  color: #202633;
  resize: vertical;
}

.campo input:focus,
.campo textarea:focus {
  outline: none;
  border-color: #2b2d42;
}

.erro {
  margin: 0 0 14px;
  font-size: 13px;
  color: #ef233c;
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

.btn-perigo:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
