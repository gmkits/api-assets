<script setup lang="ts">
import { ref } from 'vue'

type Action = 'compile' | 'publish' | 'rollback'

const lastAction = ref<string | null>(null)
const actionStatus = ref<string | null>(null)

function perform(action: Action) {
  lastAction.value = action
  // Placeholder: would call respective API endpoint
  actionStatus.value = `Action "${action}" triggered successfully. (placeholder)`
}
</script>

<template>
  <div class="publish-view">
    <h1>Publish</h1>
    <p class="subtitle">Compile, publish, or rollback holiday data</p>

    <div class="actions">
      <div class="action-card">
        <h2>🔨 Compile</h2>
        <p>Compile canonical YAML into binary bundles.</p>
        <button class="btn" @click="perform('compile')">Compile</button>
      </div>

      <div class="action-card">
        <h2>🚀 Publish</h2>
        <p>Push compiled bundles to the production CDN.</p>
        <button class="btn btn-green" @click="perform('publish')">Publish</button>
      </div>

      <div class="action-card">
        <h2>⏪ Rollback</h2>
        <p>Revert to the previous published version.</p>
        <button class="btn btn-red" @click="perform('rollback')">Rollback</button>
      </div>
    </div>

    <div v-if="actionStatus" class="status">
      <strong>{{ lastAction }}:</strong> {{ actionStatus }}
    </div>
  </div>
</template>

<style scoped>
.publish-view {
  max-width: 800px;
}

h1 {
  margin-bottom: 4px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.action-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
}

.action-card h2 {
  font-size: 16px;
  margin-bottom: 8px;
}

.action-card p {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.btn {
  padding: 6px 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn:hover {
  background: #f0f0f0;
}

.btn-green {
  background: #4caf50;
  color: #fff;
  border-color: #43a047;
}

.btn-green:hover {
  background: #43a047;
}

.btn-red {
  background: #e53935;
  color: #fff;
  border-color: #d32f2f;
}

.btn-red:hover {
  background: #d32f2f;
}

.status {
  padding: 12px;
  background: #e8f5e9;
  border-radius: 4px;
  color: #2e7d32;
}
</style>
