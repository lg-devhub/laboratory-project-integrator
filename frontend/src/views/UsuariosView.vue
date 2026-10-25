<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ArrowLeft, Plus } from '@lucide/vue'
import MarcaStocka from '@/components/MarcaStocka.vue'
import UsuarioFormulario from '@/components/UsuarioFormulario.vue'
import UsuarioDesativacao from '@/components/UsuarioDesativacao.vue'
import { listarUsuarios, ErroGestaoUsuario } from '@/services/usuarios'
import { useAutenticacaoStore } from '@/stores/autenticacao'
import type { UsuarioGestao } from '@/types/gestaoUsuario'

const router = useRouter(), autenticacao = useAutenticacaoStore()
const contas = ref<UsuarioGestao[]>([])
const pesquisa = ref('')
const carregando = ref(true)
const erro = ref('')
const sucesso = ref('')
const formulario = ref(false)
const editando = ref<UsuarioGestao | null>(null)
const desativando = ref<UsuarioGestao | null>(null)
const administradoresAtivos = computed(() => contas.value.filter(c => c.ativo && c.perfil === 'ADMINISTRADOR').length)
const filtradas = computed(() => {
  const termo = pesquisa.value.trim().toLocaleLowerCase('pt-BR')
  return contas.value.filter(c => (c.nome + ' ' + c.login).toLocaleLowerCase('pt-BR').includes(termo))
})
function sincronizar(conta: UsuarioGestao) {
  const indice = contas.value.findIndex(c => c.id === conta.id)
  if (indice < 0) contas.value.push(conta); else contas.value[indice] = conta
  contas.value.sort((a, b) => Number(b.ativo) - Number(a.ativo) || a.nome.localeCompare(b.nome, 'pt-BR'))
}
async function conferirPermissao() {
  await autenticacao.verificarSessao()
  if (autenticacao.usuario?.perfil !== 'ADMINISTRADOR') {
    contas.value = []; formulario.value = false; desativando.value = null
    await router.replace({ name: autenticacao.autenticado ? 'ambientes' : 'login' })
    return false
  }
  return true
}
async function acessoPerdido() { await conferirPermissao() }
async function carregar() {
  carregando.value = true; erro.value = ''
  try { contas.value = await listarUsuarios() }
  catch (f) {
    if (f instanceof ErroGestaoUsuario && [401, 403].includes(f.status)) await acessoPerdido()
    else erro.value = f instanceof Error ? f.message : 'Não foi possível carregar as contas.'
  } finally { carregando.value = false }
}
async function salvo(conta: UsuarioGestao) {
  sincronizar(conta); formulario.value = false; desativando.value = null
  sucesso.value = conta.ativo ? 'Conta salva. As permissões atualizadas valem nas próximas requisições.' : 'Conta desativada. Identidade e históricos preservados.'
  if (conta.id === autenticacao.usuario?.id) await conferirPermissao()
}
function abrir(conta: UsuarioGestao | null) { editando.value = conta; formulario.value = true; sucesso.value = '' }
onMounted(carregar)
</script>

<template>
  <div><header><MarcaStocka /><span>{{ autenticacao.usuario?.nome }}</span></header>
    <main class="usuarios"><RouterLink :to="{ name: 'ambientes' }" class="voltar"><ArrowLeft :size="18" /> Todos os ambientes</RouterLink>
      <div class="apresentacao"><div><p class="categoria">Administração</p><h1>Usuários</h1><p>Gerencie contas e perfis. As atribuições aos ambientes ficam na opção Acessos do hub.</p></div><button class="botao-principal" @click="abrir(null)"><Plus :size="18" aria-hidden="true" /> Cadastrar usuário</button></div>
      <p v-if="sucesso" class="sucesso" role="status">{{ sucesso }}</p>
      <p v-if="carregando" role="status">Carregando usuários…</p>
      <div v-else-if="erro"><p class="mensagem-erro" role="alert">{{ erro }}</p><button class="botao-secundario" @click="carregar">Tentar novamente</button></div>
      <template v-else>
        <div class="campo busca"><label for="pesquisa-usuarios">Buscar usuários</label><input id="pesquisa-usuarios" v-model="pesquisa" type="search" placeholder="Nome ou e-mail/login"></div>
        <p class="ajuda">{{ filtradas.length }} {{ filtradas.length === 1 ? 'conta encontrada' : 'contas encontradas' }} · {{ administradoresAtivos }} {{ administradoresAtivos === 1 ? 'administrador ativo' : 'administradores ativos' }}</p>
        <div v-if="filtradas.length" class="tabela" role="region" aria-label="Usuários; em telas pequenas, role horizontalmente" tabindex="0"><table><caption class="visualmente-oculto">Contas cadastradas no STOCKA</caption><thead><tr><th>Nome e login</th><th>Perfil</th><th>Situação</th><th>Ações</th></tr></thead><tbody><tr v-for="c in filtradas" :key="c.id"><td><strong>{{ c.nome }}</strong><span class="voce" v-if="c.id === autenticacao.usuario?.id">Você</span><span class="login">{{ c.login }}</span></td><td>{{ c.perfil === 'ADMINISTRADOR' ? 'Administrador' : 'Usuário' }}</td><td><span class="situacao" :class="{ inativa: !c.ativo }">{{ c.ativo ? 'Ativo' : 'Desativado' }}</span></td><td><div class="acoes"><button class="botao-secundario" :aria-label="'Editar ' + c.nome" @click="abrir(c)">Editar</button><button v-if="c.ativo" class="botao-secundario desativar" :aria-label="'Desativar ' + c.nome" @click="desativando = c; sucesso = ''">Desativar</button></div></td></tr></tbody></table></div>
        <p v-else>Nenhum usuário encontrado para essa pesquisa.</p>
      </template>
    </main>
    <UsuarioFormulario v-if="formulario" :conta="editando" :propria="editando?.id === autenticacao.usuario?.id" @fechar="formulario = false" @salvo="salvo" @sincronizada="sincronizar" @acesso-perdido="acessoPerdido" />
    <UsuarioDesativacao v-if="desativando" :conta="desativando" :propria="desativando.id === autenticacao.usuario?.id" @fechar="desativando = null" @salvo="salvo" @atualizar="sincronizar($event); desativando = null" @acesso-perdido="acessoPerdido" />
  </div>
</template>

<style scoped>
header { display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 22px max(20px, calc((100vw - 1176px) / 2)); border-bottom: 1px solid var(--cor-borda); background: var(--cor-superficie); flex-wrap: wrap; }
header > span { color: var(--cor-texto-secundario); font-size: .85rem; overflow-wrap: anywhere; }
.usuarios { max-width: 1240px; margin: auto; padding: 28px 32px 60px; }.voltar { display: inline-flex; align-items: center; gap: 8px; min-height: 44px; text-decoration: none; color: var(--cor-texto-secundario); font-size: .85rem; margin-bottom: 24px; }
.apresentacao { display: flex; justify-content: space-between; gap: 20px; flex-wrap: wrap; align-items: flex-start; margin-bottom: 28px; }.apresentacao > div { flex: 1; min-width: 200px; }.apresentacao p { color: var(--cor-texto-secundario); font-size: .9rem; line-height: 1.5; }.categoria { text-transform: uppercase; letter-spacing: .1em; color: var(--cor-principal) !important; font-size: .75rem !important; margin: 0 0 8px; }
h1 { margin: 0; font-size: 2rem; font-weight: 600; }.apresentacao button { gap: 8px; }.busca { max-width: 520px; }.ajuda { color: var(--cor-texto-secundario); font-size: .85rem; margin: 20px 0 14px; }
.tabela { overflow-x: auto; border: 1px solid var(--cor-borda); border-radius: 8px; background: var(--cor-superficie); }table { width: 100%; border-collapse: collapse; font-size: .9rem; text-align: left; }th { font-size: .75rem; color: var(--cor-texto-secundario); background: var(--cor-painel); padding: 14px 18px; }td { padding: 18px; border-top: 1px solid var(--cor-borda); }td:first-child { min-width: 200px; overflow-wrap: anywhere; max-width: 350px; }td strong { font-weight: 600; }.login { display: block; font-size: .8rem; color: var(--cor-texto-secundario); margin-top: 6px; }.voce { font-size: .7rem; margin-left: 8px; color: var(--cor-principal); }.acoes { display: flex; gap: 8px; }.acoes button { font-size: .8rem; }.situacao { font-size: .75rem; padding: 4px 8px; border-radius: 4px; color: #31573e; background: #edf3ed; }.situacao.inativa { color: #525c65; background: #eef0f2; }.desativar { color: #9c352d; }.sucesso { padding: 12px; background: #eef3ed; color: #30523b; border-left: 3px solid #426a51; font-size: .9rem; }
@media (max-width: 600px) { .usuarios { padding: 20px; } }
</style>
