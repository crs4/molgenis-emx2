import { defineStore } from "pinia";
import i18n from '../i18n'

export const useSettingsStore = defineStore("settingsStore", {
  state: () => ({
    locale: i18n.global.locale.value
  }),
  actions: {
    setLocale(locale) {
      console.log(locale)
      console.log(this.locale)
      this.locale = locale
      console.log(this.locale)
      i18n.global.locale.value = locale
      localStorage.setItem('user-locale', locale)
      document.querySelector('html').setAttribute('lang', locale)
    }
  }
});
