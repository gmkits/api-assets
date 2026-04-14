<script setup lang="ts">
import type { DayInfo } from '@holiday/spec'

defineProps<{
  day: DayInfo | null
}>()
</script>

<template>
  <div class="day-detail" v-if="day">
    <h3>{{ day.date }}</h3>
    <table class="detail-table">
      <tr>
        <td class="label">Name</td>
        <td>{{ day.name }}</td>
      </tr>
      <tr>
        <td class="label">Kind</td>
        <td>{{ day.kind }}</td>
      </tr>
      <tr>
        <td class="label">Is Off</td>
        <td>{{ day.isOff ? 'Yes ✅' : 'No' }}</td>
      </tr>
    </table>
  </div>
  <div class="day-detail empty" v-else>
    <p>Select a day to view details</p>
  </div>
</template>

<style scoped>
.day-detail {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
}

.day-detail.empty {
  color: #999;
}

.detail-table {
  width: 100%;
  margin-top: 8px;
}

.detail-table td {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-table .label {
  font-weight: 600;
  color: #666;
  width: 100px;
}
</style>
