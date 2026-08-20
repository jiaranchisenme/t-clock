import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import './style.css'

// 注册 Pinia 全局状态、Vue Router，挂载应用
createApp(App).use(createPinia()).use(router).mount('#app')
