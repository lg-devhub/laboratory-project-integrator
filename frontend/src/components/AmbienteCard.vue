<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Building2, DoorOpen, Pencil, Users, Trash2, Package } from '@lucide/vue'
import { coresAmbiente, type Ambiente } from '@/types/ambiente'

import fotoGalpao from '@/assets/images/slide-galpao.jpg'
import fotoEstoque from '@/assets/images/slide-estoque.jpg'
import fotoEquipe from '@/assets/images/slide-equipe.jpg'

const props = defineProps<{ ambiente: Ambiente; administrador: boolean }>()
defineEmits<{ editar: []; acessos: []; retirar: [] }>()

const corHex = computed(() => coresAmbiente.find(c => c.valor === props.ambiente.cor)?.hex || '#b64c1d')

// Imagem temática de capa conforme o tipo e cor do ambiente
const imagemCapa = computed(() => {
  if (props.ambiente.tipo === 'SETOR') {
    return fotoGalpao
  }
  if (props.ambiente.cor === 'AZUL') {
    return fotoEstoque
  }
  return fotoEquipe
})
</script>

<template>
  <article class="ambiente-card">
    <!-- Área principal inteiramente clicável para abrir o ambiente -->
    <RouterLink
      :to="{ name: 'inventario', params: { ambienteId: ambiente.id } }"
      class="card-area-clicavel"
      :aria-label="`Abrir inventário de ${ambiente.nome}`"
    >
      <!-- Imagem de Capa com Badges -->
      <div class="card-capa">
        <img :src="imagemCapa" :alt="ambiente.nome" class="capa-imagem" />
        <div class="capa-gradiente" aria-hidden="true"></div>

        <!-- Badge de Ícone e Cor Oficial no Canto Superior Esquerdo -->
        <div class="icone-badge" :style="{ backgroundColor: corHex }">
          <Building2 v-if="ambiente.tipo === 'SETOR'" :size="20" :stroke-width="1.8" />
          <DoorOpen v-else :size="20" :stroke-width="1.8" />
        </div>

        <!-- Badge de Tipo no Canto Superior Direito -->
        <span class="tipo-badge">
          {{ ambiente.tipo === 'SETOR' ? 'Setor' : 'Sala' }}
        </span>
      </div>

      <!-- Conteúdo do Card -->
      <div class="card-corpo">
        <h2 class="card-titulo">
          {{ ambiente.nome }}
        </h2>

        <p class="card-descricao">
          {{ ambiente.descricao || 'Sem descrição cadastrada.' }}
        </p>

        <!-- Linha de Metadados: Acessos e Ativos -->
        <div class="card-meta">
          <span class="meta-item">
            <Users :size="15" class="meta-icone" aria-hidden="true" />
            <span>{{ ambiente.tipo === 'SETOR' ? '5 acessos' : '3 acessos' }}</span>
          </span>
          <span class="meta-divisor" aria-hidden="true">|</span>
          <span class="meta-item">
            <Package :size="15" class="meta-icone" aria-hidden="true" />
            <span>{{ ambiente.tipo === 'SETOR' ? '2 ativos' : '4 ativos' }}</span>
          </span>
        </div>
      </div>
    </RouterLink>

    <!-- Rodapé com Ações Administrativas -->
    <footer v-if="administrador" class="card-acoes">
      <button
        type="button"
        class="botao-acao"
        :aria-label="`Editar ${ambiente.nome}`"
        @click.stop="$emit('editar')"
      >
        <Pencil :size="15" aria-hidden="true" />
        <span>Editar</span>
      </button>

      <button
        type="button"
        class="botao-acao"
        :aria-label="`Gerenciar acessos de ${ambiente.nome}`"
        @click.stop="$emit('acessos')"
      >
        <Users :size="15" aria-hidden="true" />
        <span>Acessos</span>
      </button>

      <button
        type="button"
        class="botao-acao acao-retirar"
        :aria-label="`Retirar ${ambiente.nome} de uso`"
        @click.stop="$emit('retirar')"
      >
        <Trash2 :size="15" aria-hidden="true" />
        <span>Retirar</span>
      </button>
    </footer>

    <footer v-else class="card-acoes">
      <RouterLink
        class="botao-acao botao-abrir"
        :to="{ name: 'inventario', params: { ambienteId: ambiente.id } }"
      >
        Abrir inventário →
      </RouterLink>
    </footer>
  </article>
</template>

<style scoped>
.ambiente-card {
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  border: 1px solid #e7e5de;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  cursor: pointer;
}

.ambiente-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.09);
  border-color: #d6d1c4;
}

/* Link que engloba toda a área superior do card */
.card-area-clicavel {
  display: flex;
  flex-direction: column;
  flex: 1;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

/* Capa fotográfica com badges */
.card-capa {
  position: relative;
  width: 100%;
  height: 140px;
  overflow: hidden;
  background-color: #2b333a;
}

.capa-imagem {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.4s ease;
}

.ambiente-card:hover .capa-imagem {
  transform: scale(1.04);
}

.capa-gradiente {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(14, 18, 22, 0.45) 0%,
    rgba(14, 18, 22, 0.15) 50%,
    rgba(14, 18, 22, 0.55) 100%
  );
}

/* Badge de ícone e cor */
.icone-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  z-index: 2;
}

/* Badge de Tipo (Setor ou Sala) */
.tipo-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 4px 12px;
  background: rgba(22, 26, 30, 0.65);
  backdrop-filter: blur(4px);
  color: #ffffff;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 6px;
  letter-spacing: 0.04em;
  z-index: 2;
}

/* Corpo do Card */
.card-corpo {
  flex: 1;
  padding: 20px 22px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-titulo {
  margin: 0;
  font-size: 1.22rem;
  font-weight: 700;
  line-height: 1.25;
  color: #1e2630;
  transition: color 0.15s ease;
}

.ambiente-card:hover .card-titulo {
  color: var(--cor-principal, #be4d1e);
}

.card-descricao {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.5;
  color: #636f7d;
  min-height: 2.6em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Metadados */
.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  font-size: 0.84rem;
  color: #64707d;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta-icone {
  color: #8994a0;
}

.meta-divisor {
  color: #dcd7cf;
  font-weight: 300;
}

/* Ações no Rodapé */
.card-acoes {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  border-top: 1px solid #f0eee8;
  background-color: #faf9f6;
  gap: 8px;
}

.botao-acao {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #556270;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
  text-decoration: none;
}

.botao-acao:hover {
  background-color: #eeebe3;
  color: #1f2730;
}

.botao-acao.acao-retirar {
  color: #c93b3b;
}

.botao-acao.acao-retirar:hover {
  background-color: #ffebe9;
  color: #b32525;
}

.botao-abrir {
  color: var(--cor-principal, #be4d1e);
  margin-left: auto;
}

.botao-abrir:hover {
  color: var(--cor-principal-hover, #9c3c14);
}
</style>
