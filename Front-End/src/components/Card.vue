<template>
  <article
    class="card"
    tabindex="0"
    role="button"
    :aria-label="`Abrir ${setor.nome}`"
    @click="$emit('abrir', setor)"
    @keydown.enter="$emit('abrir', setor)"
  >
    <!-- bolinha de status no canto -->
    <span
      class="status-dot"
      :class="setor.itensAtencao > 0 ? 'status-dot--atencao' : 'status-dot--ok'"
    ></span>

    <div class="card-header">
      <div>
        <h2><span class="title-badge">{{ setor.nome }}</span></h2>
        <span class="codigo">{{ setor.codigo }}</span>
      </div>
    </div>

    <p v-if="setor.descricao" class="descricao">
      {{ setor.descricao }}
    </p>

    <div class="card-info">
      <div class="items">
        <strong>{{ setor.itens }}</strong>
        <span>itens</span>
      </div>

      <div v-if="setor.itensAtencao > 0" class="atencao">
        {{ setor.itensAtencao }} itens exigem atenção
      </div>
    </div>

    <!-- RF12/RF13: só o administrador edita/exclui a sala/setor -->
    <div v-if="isAdmin" class="admin-actions">
      <button
        type="button"
        class="admin-action"
        title="Editar sala"
        @click.stop="$emit('editar', setor)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M12 20h9"></path>
          <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"></path>
        </svg>
      </button>
      <button
        type="button"
        class="admin-action admin-action--danger"
        title="Excluir sala"
        @click.stop="$emit('excluir', setor)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M3 6h18"></path>
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path>
        </svg>
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { Setor } from '../types/stocka'

defineProps<{
  setor: Setor
  isAdmin: boolean
}>()

defineEmits<{
  (e: 'abrir', setor: Setor): void
  (e: 'editar', setor: Setor): void
  (e: 'excluir', setor: Setor): void
}>()
</script>

<style scoped>
.card {
  --bg: #2b2d42;
  --bg-alt: #8d99ae;
  --accent: #ef233c;
  --text: #111827;
  --text-muted: #6b7280;

  position: relative;

  padding: 24px;
  background-color: #ffffff;
  border: 1px solid #e2e4ea;
  border-radius: 10px;

  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover,
.card:focus-visible {
  border-color: var(--bg);
  transform: translateY(-2px);
  box-shadow: 0 10px 24px -14px rgba(43, 45, 66, 0.35);
}

.card:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 3px;
}

/* bolinha de status */
.status-dot {
  position: absolute;
  top: 16px;
  right: 16px;

  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.status-dot--atencao {
  background-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(239, 35, 60, 0.15);
}

.status-dot--ok {
  background-color: #2d9d6f;
  box-shadow: 0 0 0 3px rgba(45, 157, 111, 0.15);
}

.card-header {
  margin: -24px -24px 0;
  padding: 20px 24px 16px;

  background-color: #2b2d42;
  border-radius: 10px 10px 0 0;
}

.card-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #edf2f4;
}

.codigo {
  font-size: 13px;
  color: var(--text-muted);
}

.descricao {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-muted);
}

.card-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eef0f3;
}

.items {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.items strong {
  font-size: 24px;
  color: var(--text);
}

.items span {
  font-size: 13px;
  color: var(--text-muted);
}

.atencao,
.normal {
  font-size: 13px;
  font-weight: 500;
}

.atencao {
  color: var(--accent);
}

.normal {
  color: #2d9d6f;
}

/* ações de administrador — reveladas no hover/foco do card, mesmo
   padrão de comportamento usado no Header (ícone só ganha destaque na
   interação) */
.admin-actions {
  position: absolute;
  top: 14px;
  left: 14px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.card:hover .admin-actions,
.card:focus-within .admin-actions {
  opacity: 1;
}

.admin-action {
  width: 28px;
  height: 28px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 1px solid #e2e4ea;
  border-radius: 8px;
  background-color: #ffffff;
  color: var(--bg);

  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.admin-action svg {
  width: 14px;
  height: 14px;
}

.admin-action:hover {
  border-color: var(--bg);
}

.admin-action--danger:hover {
  border-color: var(--accent);
  color: var(--accent);
}
</style>
