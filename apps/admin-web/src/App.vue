<script setup lang="ts">
import NavSidebar from './components/NavSidebar.vue'
</script>

<template>
  <div class="app-layout">
    <NavSidebar />
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style>
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  color: #1a1a1a;
  background: #f5f5f5;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
