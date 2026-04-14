<script setup lang="ts">
import { ref } from 'vue'

interface ValidationResult {
  level: 'error' | 'warning' | 'info'
  message: string
  path?: string
}

const results = ref<ValidationResult[]>([])
const validated = ref(false)

function runValidation() {
  // Placeholder: would call API validation endpoint
  results.value = [
    { level: 'info', message: 'Validation complete. No issues found.' },
  ]
  validated.value = true
}

function levelClass(level: string) {
  return `level-${level}`
}
</script>

<template>
  <div class="validate-view">
    <h1>Validate Data</h1>
    <p class="subtitle">Run validation checks on current holiday data</p>

    <button class="btn-primary" @click="runValidation">Run Validation</button>

    <div v-if="validated" class="results">
      <div
        v-for="(result, i) in results"
        :key="i"
        class="result-item"
        :class="levelClass(result.level)"
      >
        <span class="badge">{{ result.level.toUpperCase() }}</span>
        <span>{{ result.message }}</span>
        <code v-if="result.path">{{ result.path }}</code>
      </div>
      <p v-if="!results.length" class="muted">No results</p>
    </div>
  </div>
</template>

<style scoped>
.validate-view {
  max-width: 700px;
}

h1 {
  margin-bottom: 4px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.btn-primary {
  padding: 8px 20px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary:hover {
  background: #1565c0;
}

.results {
  margin-top: 20px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 4px;
  margin-bottom: 6px;
  font-size: 14px;
}

.badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 3px;
}

.level-error {
  background: #ffeaea;
}

.level-error .badge {
  background: #d32f2f;
  color: #fff;
}

.level-warning {
  background: #fff8e1;
}

.level-warning .badge {
  background: #f9a825;
  color: #fff;
}

.level-info {
  background: #e3f2fd;
}

.level-info .badge {
  background: #1976d2;
  color: #fff;
}

.muted {
  color: #999;
}

code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}
</style>
