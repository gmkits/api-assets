<script setup lang="ts">
import { onMounted } from 'vue'
import { useHolidayStore } from '../stores/holiday'

const store = useHolidayStore()

onMounted(() => {
  store.fetchRegions()
  store.fetchManifest()
})
</script>

<template>
  <div class="dashboard">
    <h1>Dashboard</h1>
    <p class="subtitle">Holiday Data Platform overview</p>

    <div v-if="store.loading" class="loading">Loading…</div>
    <div v-if="store.error" class="error">{{ store.error }}</div>

    <section class="card">
      <h2>Regions</h2>
      <ul v-if="store.regions.length">
        <li v-for="region in store.regions" :key="region">{{ region }}</li>
      </ul>
      <p v-else class="muted">No regions loaded</p>
    </section>

    <section class="card">
      <h2>Manifest</h2>
      <pre v-if="store.manifest">{{ JSON.stringify(store.manifest, null, 2) }}</pre>
      <p v-else class="muted">No manifest loaded</p>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 800px;
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
  font-size: 16px;
  margin-bottom: 12px;
}

.muted {
  color: #999;
}

.loading {
  color: #1976d2;
  margin-bottom: 12px;
}

.error {
  color: #d32f2f;
  background: #ffeaea;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 12px;
}

pre {
  background: #f8f8f8;
  padding: 12px;
  border-radius: 4px;
  font-size: 13px;
  overflow-x: auto;
}

ul {
  list-style: none;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

li {
  background: #e3f2fd;
  color: #1565c0;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
}
</style>
