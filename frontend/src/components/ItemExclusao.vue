<script setup lang="ts">
import { ref } from 'vue'
import { excluirItem, ErroInventario } from '@/services/itens'
import type { Item } from '@/types/item'

const props = defineProps<{ item: Item }>()
const emit = defineEmits<{ fechar: []; excluido: [item: Item]; atualizado: [item: Item]; enviando: [valor: boolean]; acessoPerdido: [erro: ErroInventario] }>()
const enviando = ref(false)
const erro = ref('')
const atual = ref<Item | null>(null)
async function confirmar() {
  if (enviando.value || atual.value) return
  enviando.value = true; emit('enviando', true); erro.value = ''
  try { emit('excluido', await excluirItem(props.item)) }
  catch (f) {
    if (f instanceof ErroInventario && [401, 404].includes(f.status)) emit('acessoPerdido', f)
    else {
      erro.value = f instanceof Error ? f.message : 'Não foi possível excluir o item.'
      if (f instanceof ErroInventario && f.codigo === 'ITEM_ALTERADO' && f.itemAtual) atual.value = f.itemAtual
    }
  } finally { enviando.value = false; emit('enviando', false) }
}
</script>

<template>
  <section class="confirmacao-exclusao" aria-labelledby="titulo-exclusao" :aria-busy="enviando">
    <h3 id="titulo-exclusao">Excluir este item?</h3>
    <p><strong>{{ item.nome }}</strong> sairá do inventário normal. Seus dados, patrimônio e históricos serão preservados para consulta administrativa.</p>
    <p v-if="item.equipamento">Equipamentos com OS aberta ou em andamento não podem ser excluídos.</p>
    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
    <button v-if="atual" class="botao-secundario" @click="emit('atualizado', atual)">Revisar dados atuais</button>
    <div class="acoes"><button class="botao-secundario" :disabled="enviando" @click="emit('fechar')">Cancelar exclusão</button><button class="botao-principal perigo" :disabled="enviando || !!atual" @click="confirmar">{{ enviando ? 'Excluindo…' : 'Confirmar exclusão' }}</button></div>
  </section>
</template>

<style scoped>
.confirmacao-exclusao { padding: 18px; margin-top: 24px; border: 1px solid #d8ada6; border-radius: 6px; background: #fcf5f3; }
h3 { margin: 0; font-size: 1rem; color: #8f3228; }
p { font-size: .9rem; line-height: 1.5; overflow-wrap: anywhere; }.acoes { display: flex; gap: 12px; flex-wrap: wrap; justify-content: flex-end; margin-top: 18px; }
.perigo { background: #9c352d; color: white; }
@media (max-width: 420px) { .acoes > button { flex: 1; } }
</style>
