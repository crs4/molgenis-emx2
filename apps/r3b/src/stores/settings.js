import { defineStore } from "pinia";
import i18n from '../i18n'
import { ref } from 'vue'

export const useSettingsStore = defineStore("settingsStore",  () => {
  const locale = ref(i18n.global.locale.value)
  const graphqlEndpoint = "/graphql"

  function setLocale(newLocale) {
    locale.value = newLocale
    i18n.global.locale.value = newLocale
    localStorage.setItem('user-locale', newLocale)
    document.querySelector('html').setAttribute('lang', newLocale)
  }

  return { locale, graphqlEndpoint, setLocale }
});
