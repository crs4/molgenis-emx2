import { createI18n } from 'vue-i18n'
import en from './config/locales/en.json'
import it from './config/locales/it.json'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('user-locale') || 'it',
  fallbackLocale: 'en',
  globalInjection: true,
  messages: { it, en }
})

export default i18n
