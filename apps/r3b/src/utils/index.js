import i18n from "../i18n"

export const getI18nLabel = () => {
  return `label_${i18n.global.locale.value}`
}
