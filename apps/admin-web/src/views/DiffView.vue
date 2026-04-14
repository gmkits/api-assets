<script setup lang="ts">
import { ref } from 'vue'

const versionA = ref('')
const versionB = ref('')
const diffOutput = ref<string | null>(null)

function computeDiff() {
  if (!versionA.value || !versionB.value) {
    diffOutput.value = 'Please enter both version identifiers.'
    return
  }
  // Placeholder: would call API diff endpoint
  diffOutput.value = `Diff between "${versionA.value}" and "${versionB.value}":\n\nNo changes detected (placeholder).`
}
</script>

<template>
  <div class="diff-view">
    <h1>Version Diff</h1>
    <p class="subtitle">Compare two versions of holiday data</p>

    <div class="controls">
      <label>
        Version A:
        <input v-model="versionA" type="text" class="input" placeholder="e.g. v2024.1" />
      </label>
      <span class="arrow">→</span>
      <label>
        Version B:
        <input v-model="versionB" type="text" class="input" placeholder="e.g. v2024.2" />
      </label>
      <button class="btn-primary" @click="computeDiff">Compare</button>
    </div>

    <pre v-if="diffOutput" class="diff-output">{{ diffOutput }}</pre>
  </div>
</template>

<style scoped>
.diff-view {
  max-width: 700px;
}

h1 {
  margin-bottom: 4px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.controls {
  display: flex;
  align-items: end;
  gap: 12px;
  margin-bottom: 16px;
}

.controls label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #555;
}

.input {
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  width: 150px;
}

.arrow {
  font-size: 20px;
  color: #999;
  padding-bottom: 4px;
}

.btn-primary {
  padding: 8px 20px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  align-self: end;
}

.btn-primary:hover {
  background: #1565c0;
}

.diff-output {
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 16px;
  font-size: 13px;
  white-space: pre-wrap;
}
</style>
