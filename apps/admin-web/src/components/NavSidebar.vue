<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/', label: '📊 Dashboard' },
  { path: '/calendar', label: '📅 Calendar' },
  { path: '/import', label: '📥 Import' },
  { path: '/validate', label: '✅ Validate' },
  { path: '/diff', label: '🔀 Diff' },
  { path: '/publish', label: '🚀 Publish' },
  { path: '/playground', label: '🧪 Playground' },
]
</script>

<template>
  <nav class="nav-sidebar">
    <div class="nav-brand">
      <h2>🏖️ Holiday Admin</h2>
    </div>
    <ul class="nav-list">
      <li v-for="item in navItems" :key="item.path">
        <RouterLink
          :to="item.path"
          class="nav-link"
          :class="{ active: route.path === item.path }"
        >
          {{ item.label }}
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.nav-sidebar {
  width: 220px;
  background: #1a1a2e;
  color: #eee;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.nav-brand {
  padding: 20px 16px;
  border-bottom: 1px solid #2a2a4a;
}

.nav-brand h2 {
  font-size: 16px;
  font-weight: 600;
}

.nav-list {
  list-style: none;
  padding: 8px 0;
}

.nav-link {
  display: block;
  padding: 10px 16px;
  color: #bbb;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
  background: #2a2a4a;
  color: #fff;
}

.nav-link.active {
  background: #3a3a6a;
  color: #fff;
  font-weight: 600;
}
</style>
