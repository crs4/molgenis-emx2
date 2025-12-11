import i18n from "../i18n"

export const getOntologyI18nLabelItem = () => {
  return i18n.global.locale.value == "en" ? "name" : "label"
}


export const getI18nLabelItem = () => {
  return `label_${i18n.global.locale.value}`
}
