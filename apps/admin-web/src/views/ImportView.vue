<script setup lang="ts">
import { ref } from 'vue'

const fileType = ref('yaml')
const selectedFile = ref<File | null>(null)
const status = ref<string | null>(null)

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] ?? null
}

function handleImport() {
  if (!selectedFile.value) {
    status.value = 'Please select a file first.'
    return
  }
  // Placeholder: would send to API
  status.value = `Importing "${selectedFile.value.name}" as ${fileType.value}…`
}
</script>

<template>
  <div class="import-view">
    <h1>Import Data</h1>
    <p class="subtitle">Upload a holiday data file to import</p>

    <section class="card">
      <h2>Step 1: Select File Type</h2>
      <div class="radio-group">
        <label>
          <input type="radio" v-model="fileType" value="yaml" /> YAML
        </label>
        <label>
          <input type="radio" v-model="fileType" value="json" /> JSON
        </label>
        <label>
          <input type="radio" v-model="fileType" value="csv" /> CSV
        </label>
      </div>
    </section>

    <section class="card">
      <h2>Step 2: Upload File</h2>
      <input type="file" @change="onFileChange" accept=".yaml,.yml,.json,.csv" />
    </section>

    <section class="card">
      <h2>Step 3: Import</h2>
      <button class="btn-primary" @click="handleImport">Import</button>
    </section>

    <div v-if="status" class="status">{{ status }}</div>
  </div>
</template>

<style scoped>
.import-view {
  max-width: 600px;
}

h1 {
  margin-bottom: 4px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.card h2 {
  font-size: 14px;
  margin-bottom: 12px;
  color: #555;
}

.radio-group {
  display: flex;
  gap: 16px;
}

.radio-group label {
  cursor: pointer;
  font-size: 14px;
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

.status {
  margin-top: 12px;
  padding: 10px;
  background: #e8f5e9;
  border-radius: 4px;
  color: #2e7d32;
}
</style>
