import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import i18n from './i18n'
import router from './router'

import 'bootstrap/dist/css/bootstrap.min.css'
import './assets/css/mg-r3b-4.css'
import 'bootstrap'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(i18n)
app.use(router)
app.mount('#app')
