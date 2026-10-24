<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Building2, Plus } from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import AmbienteCard from '@/components/AmbienteCard.vue'
import AmbienteFormulario from '@/components/AmbienteFormulario.vue'
import AmbienteAcessos from '@/components/AmbienteAcessos.vue'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import { listarAmbientes } from '@/services/ambientes'
import type { Ambiente } from '@/types/ambiente'

const router = useRouter()
const autenticacao = useAutenticacaoStore()
const ambientes = ref<Ambiente[]>([])
const pesquisa = ref('')
const carregando = ref(true)
const saindo = ref(false)
const erro = ref('')
const erroSaida = ref('')
const administrador = computed(() => autenticacao.usuario?.perfil === 'ADMINISTRADOR')
const formularioAberto = ref(false)
const ambienteEditando = ref<Ambiente | null>(null)
const ambienteAcessos = ref<Ambiente | null>(null)
const sucesso = ref('')

function abrirFormulario(ambiente: Ambiente | null = null) {
  ambienteEditando.value = ambiente
  formularioAberto.value = true
  sucesso.value = ''
}

function ambienteSalvo(ambiente: Ambiente) {
  const indice = ambientes.value.findIndex(a => a.id === ambiente.id)
  if (indice >= 0) ambientes.value[indice] = ambiente
  else ambientes.value.push(ambiente)
  ambientes.value.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
  formularioAberto.value = false
  sucesso.value = indice >= 0 ? 'Ambiente atualizado.' : 'Ambiente criado. Use Acessos para atribuir usuários.'
}

function acessosSalvos() {
  ambienteAcessos.value = null
  sucesso.value = 'Acessos atualizados. As permissões já valem para as próximas consultas.'
}

async function sessaoExpirada() {
  formularioAberto.value = false
  ambienteAcessos.value = null
  autenticacao.usuario = null
  await router.replace({ name: 'login' })
  autenticacao.erroSessao = 'Sua sessão expirou. Entre novamente.'
}

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('pt-BR')
}

const ambientesFiltrados = computed(() => {
  const termo = normalizar(pesquisa.value.trim())
  return ambientes.value.filter(ambiente => normalizar(
    `${ambiente.nome} ${ambiente.tipo} ${ambiente.descricao || ''}`,
  ).includes(termo))
})

async function carregarAmbientes() {
  carregando.value = true
  erro.value = ''
  ambientes.value = []
  try {
    const resultado = await listarAmbientes()
    if (resultado === null) {
      await sessaoExpirada()
      return
    }
    ambientes.value = resultado
  } catch (falha) {
    erro.value = falha instanceof Error ? falha.message : 'Não foi possível carregar os ambientes.'
  } finally {
    carregando.value = false
  }
}

async function sair() {
  if (saindo.value) return
  saindo.value = true
  erroSaida.value = ''
  try {
    await autenticacao.sair()
    await router.replace({ name: 'login' })
  } catch (falha) {
    erroSaida.value = falha instanceof Error ? falha.message : 'Não foi possível sair. Tente novamente.'
  } finally {
    saindo.value = false
  }
}

onMounted(carregarAmbientes)
</script>

<template>
  <div class="pagina-ambientes">
    <header class="cabecalho">
      <div class="cabecalho-conteudo">
        <MarcaStocka />
        <div class="conta">
          <span v-if="autenticacao.usuario" class="nome-conta">{{ autenticacao.usuario.nome }}</span>
          <button class="botao-secundario" :disabled="saindo" @click="sair">{{ saindo ? 'Saindo…' : 'Sair' }}</button>
        </div>
      </div>
    </header>

    <main class="hub">
      <div class="hub-apresentacao">
        <p class="hub-categoria">Patrimônio e operação</p>
        <div class="titulo-linha"><h1>Ambientes</h1>
          <button v-if="administrador" class="botao-principal" :disabled="carregando || !!erro" @click="abrirFormulario()"><Plus :size="19" aria-hidden="true" /> Adicionar ambiente</button>
        </div>
        <p>{{ administrador ? 'Organize salas, setores e os acessos da equipe.' : 'Salas e setores disponíveis para sua conta.' }}</p>
        <span class="perfil-conta">{{ administrador ? 'Administrador' : 'Usuário' }}</span>
      </div>
      <p v-if="sucesso" class="mensagem-sucesso" role="status">{{ sucesso }}</p>
      <p v-if="erroSaida" class="mensagem-erro" role="alert">{{ erroSaida }}</p>

      <section v-if="carregando" class="estado" role="status" aria-live="polite">
        <p>Carregando ambientes…</p>
      </section>
      <section v-else-if="erro" class="estado">
        <p class="mensagem-erro" role="alert">{{ erro }}</p>
        <button class="botao-principal" @click="carregarAmbientes">Tentar novamente</button>
      </section>
      <section v-else-if="ambientes.length === 0" class="estado">
        <Building2 :size="32" :stroke-width="1.5" aria-hidden="true" />
        <h2>Nenhum ambiente disponível</h2>
        <p>Não há salas ou setores disponíveis para sua conta no momento.</p>
      </section>
      <section v-else aria-label="Ambientes disponíveis">
        <div class="pesquisa">
          <label for="pesquisa-ambientes">Buscar ambientes</label>
          <div class="campo-pesquisa">
            <Search :size="20" aria-hidden="true" />
            <input id="pesquisa-ambientes" v-model="pesquisa" type="search" placeholder="Nome, tipo ou descrição" autocomplete="off">
          </div>
        </div>
        <p class="total-resultados" role="status" aria-live="polite">
          {{ ambientesFiltrados.length }} {{ ambientesFiltrados.length === 1 ? 'ambiente encontrado' : 'ambientes encontrados' }}
        </p>
        <div v-if="ambientesFiltrados.length" class="grade-ambientes">
          <AmbienteCard v-for="ambiente in ambientesFiltrados" :key="ambiente.id" :ambiente="ambiente" :administrador="administrador"
            @editar="abrirFormulario(ambiente)" @acessos="sucesso = ''; ambienteAcessos = ambiente" />
        </div>
        <div v-else class="estado">
          <h2>Nenhum resultado para sua pesquisa</h2>
          <p>Tente outro nome, tipo ou descrição.</p>
          <button class="botao-secundario" @click="pesquisa = ''">Limpar pesquisa</button>
        </div>
      </section>
    </main>
    <AmbienteFormulario v-if="formularioAberto && administrador" :ambiente="ambienteEditando" @fechar="formularioAberto = false" @salvo="ambienteSalvo" @sessao-expirada="sessaoExpirada" />
    <AmbienteAcessos v-if="ambienteAcessos && administrador" :ambiente="ambienteAcessos" @fechar="ambienteAcessos = null" @salvo="acessosSalvos" @sessao-expirada="sessaoExpirada" />
  </div>
</template>

<style scoped>
.cabecalho { border-bottom: 1px solid var(--cor-borda); background: var(--cor-superficie); }
.cabecalho-conteudo { display: flex; align-items: center; justify-content: space-between; gap: 24px; max-width: 1240px; margin: auto; padding: 22px 32px; }
.conta { display: flex; align-items: center; gap: 24px; min-width: 0; }
.nome-conta { font-size: 0.9rem; overflow-wrap: anywhere; color: var(--cor-texto-secundario); }
.hub { max-width: 1240px; margin: auto; padding: 48px 32px 72px; }
.hub-apresentacao { margin-bottom: 32px; }
.titulo-linha { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; }
.titulo-linha button { gap: 8px; }
.perfil-conta { display: inline-block; margin-top: 14px; padding: 3px 10px; background: var(--cor-painel); border-radius: 4px; color: var(--cor-texto-secundario); font-size: .75rem; }
.mensagem-sucesso { border-left: 3px solid #426a51; padding: 12px 16px; margin-bottom: 24px; background: #eef3ed; color: #30523b; }
.hub-apresentacao > p { color: var(--cor-texto-secundario); }
.hub-categoria { margin: 0 0 10px; color: var(--cor-texto-secundario); font-size: 0.72rem; letter-spacing: 0.14em; text-transform: uppercase; }
h1 { margin: 0; font-size: clamp(1.8rem, 3vw, 2.3rem); font-weight: 600; letter-spacing: -0.03em; }
.hub-apresentacao > p:last-child { margin: 10px 0 0; color: var(--cor-texto-secundario); }
.pesquisa { max-width: 560px; margin: 0 auto 30px; }
.pesquisa label { display: block; margin-bottom: 8px; font-size: 0.85rem; font-weight: 600; }
.campo-pesquisa { display: flex; align-items: center; gap: 10px; padding: 0 14px; border: 1px solid var(--cor-borda); border-radius: var(--raio-campo); background: var(--cor-superficie); color: var(--cor-texto-secundario); }
.campo-pesquisa input { width: 100%; min-width: 0; height: 48px; border: 0; background: transparent; color: var(--cor-texto); }
.total-resultados { margin: 0 0 16px; color: var(--cor-texto-secundario); font-size: 0.85rem; }
.grade-ambientes { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 24px; }
.estado { display: grid; justify-items: center; gap: 16px; padding: 64px 20px; text-align: center; }
.estado h2 { margin: 0; font-size: 1.2rem; font-weight: 600; }
.estado p { max-width: 440px; margin: 0; color: var(--cor-texto-secundario); }
.estado .mensagem-erro { color: var(--cor-erro); }
@media (max-width: 950px) { .grade-ambientes { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) {
  .cabecalho-conteudo { flex-wrap: wrap; padding: 20px; }
  .conta { width: 100%; justify-content: space-between; gap: 12px; }
  .hub { padding: 32px 20px 48px; }
  .grade-ambientes { grid-template-columns: 1fr; gap: 20px; }
  .estado { padding: 40px 0; }
}
</style>
