<script setup lang="ts">
import { ref } from 'vue'
import { useHolidayStore } from '../stores/holiday'
import type { DayInfo } from '@holiday/spec'

const store = useHolidayStore()

const dateInput = ref(new Date().toISOString().slice(0, 10))
const regionInput = ref('CN')
const result = ref<DayInfo | null>(null)
const queried = ref(false)

async function query() {
  queried.value = true
  result.value = await store.getDayInfo(dateInput.value, regionInput.value)
}
</script>

<template>
  <div class="playground-view">
    <h1>API Playground</h1>
    <p class="subtitle">Query the Holiday API and inspect results</p>

    <div class="controls">
      <label>
        Date:
        <input v-model="dateInput" type="date" class="input" />
      </label>
      <label>
        Region:
        <input v-model="regionInput" type="text" class="input input-sm" />
      </label>
      <button class="btn-primary" @click="query">Query</button>
    </div>

    <div v-if="store.loading" class="loading">Loading…</div>
    <div v-if="store.error" class="error">{{ store.error }}</div>

    <section v-if="queried && result" class="result-card">
      <h2>Result for {{ result.date }}</h2>
      <table class="result-table">
        <tr>
          <td class="label">Date</td>
          <td>{{ result.date }}</td>
        </tr>
        <tr>
          <td class="label">Holiday</td>
          <td>{{ result.isHoliday ? '✅ Yes' : '❌ No' }}</td>
        </tr>
        <tr>
          <td class="label">Workday</td>
          <td>{{ result.isWorkday ? 'Yes' : 'No' }}</td>
        </tr>
        <tr>
          <td class="label">Statutory</td>
          <td>{{ result.isStatutoryHoliday ? '✅ Yes' : 'No' }}</td>
        </tr>
        <tr>
          <td class="label">Adjusted Workday</td>
          <td>{{ result.isAdjustedWorkday ? '💼 Yes' : 'No' }}</td>
        </tr>
        <tr>
          <td class="label">Weekend</td>
          <td>{{ result.isWeekend ? 'Yes' : 'No' }}</td>
        </tr>
        <tr>
          <td class="label">Names</td>
          <td>{{ result.holidayNames?.['zh-CN']?.join(', ') || '—' }}</td>
        </tr>
        <tr>
          <td class="label">Labels</td>
          <td>{{ result.labels?.join(', ') || '—' }}</td>
        </tr>
      </table>
      <details>
        <summary>Raw JSON</summary>
        <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      </details>
    </section>

    <div v-if="queried && !result && !store.loading" class="muted">
      No result returned for this query.
    </div>
  </div>
</template>

<style scoped>
.playground-view {
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
}

.input-sm {
  width: 80px;
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

.result-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
}

.result-card h2 {
  font-size: 16px;
  margin-bottom: 12px;
}

.result-table {
  width: 100%;
  margin-bottom: 12px;
}

.result-table td {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.result-table .label {
  font-weight: 600;
  color: #666;
  width: 100px;
}

details {
  margin-top: 8px;
}

summary {
  cursor: pointer;
  color: #1976d2;
  font-size: 13px;
}

pre {
  background: #f8f8f8;
  padding: 12px;
  border-radius: 4px;
  font-size: 13px;
  overflow-x: auto;
  margin-top: 8px;
}

.muted {
  color: #999;
}
</style>
