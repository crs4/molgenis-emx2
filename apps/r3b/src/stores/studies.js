import { defineStore } from "pinia";
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settings";
import { ref } from "vue"
import i18n from '../i18n'
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */

export const useStudiesStore = defineStore("studiesStore", () => {
  const settingsStore = useSettingsStore();
  const graphqlEndpoint = settingsStore.graphqlEndpoint;
  const studies = ref([])

  async function fetchStudies(params) {
    const query = new QueryEMX2(graphqlEndpoint)
    .table("Study")
    .select(["id", "title_en", "title_it", "studyStatus.label", "label"])
    // .where("studyStatus.code")
    // .equals("ocre:OCRE001000")

    console.log(params)
    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        query.where(key).equals(value)
      }
    })

    const results = await query.execute();
    const locale = i18n.global.locale.value;
    results.Study = results.Study.map(item => ({
      id: item.id,
      title: locale === 'it' ? item.title_it : item.title_en,
      studyStatus: item.studyStatus.label,
      label: item.label
    }));
    studies.value = results.Study
    return results.Study;
  }

  return {
    studies, fetchStudies
  }

});
