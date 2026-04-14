<script setup lang="ts">
import { ref } from 'vue'
import { HolidayCalendar } from '@holiday/vue'
import type { DayInfo } from '@holiday/spec'
import DayDetail from '../components/DayDetail.vue'

const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const region = ref('CN')
const selectedDay = ref<DayInfo | null>(null)

function onDayClick(day: DayInfo) {
  selectedDay.value = day
}

function prevMonth() {
  if (month.value === 1) {
    month.value = 12
    year.value--
  } else {
    month.value--
  }
}

function nextMonth() {
  if (month.value === 12) {
    month.value = 1
    year.value++
  } else {
    month.value++
  }
}
</script>

<template>
  <div class="calendar-view">
    <h1>Calendar Editor</h1>

    <div class="controls">
      <label>
        Region:
        <input v-model="region" type="text" class="input" />
      </label>
      <div class="month-nav">
        <button class="btn" @click="prevMonth">← Prev</button>
        <span class="month-label">{{ year }}-{{ String(month).padStart(2, '0') }}</span>
        <button class="btn" @click="nextMonth">Next →</button>
      </div>
    </div>

    <div class="calendar-layout">
      <div class="calendar-panel">
        <HolidayCalendar
          :year="year"
          :month="month"
          :region-code="region"
          @day-click="onDayClick"
        />
      </div>
      <div class="detail-panel">
        <DayDetail :day="selectedDay" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar-view {
  max-width: 1000px;
}

h1 {
  margin-bottom: 16px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 16px;
}

.input {
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  width: 80px;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.month-label {
  font-weight: 600;
  font-size: 16px;
  min-width: 90px;
  text-align: center;
}

.btn {
  padding: 6px 14px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn:hover {
  background: #f0f0f0;
}

.calendar-layout {
  display: flex;
  gap: 24px;
}

.calendar-panel {
  flex: 1;
}

.detail-panel {
  width: 280px;
  flex-shrink: 0;
}
</style>
