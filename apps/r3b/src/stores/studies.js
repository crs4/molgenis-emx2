import { defineStore } from "pinia";
import { QueryEMX2 } from "molgenis-components";
import { useSettingsStore } from "./settings";
import { ref } from "vue"
import i18n from '../i18n'
import { getI18nLabelItem, getOntologyI18nLabelItem } from "../utils";
import { LOCATION_HOSPITAL_IRI, LOCATION_HEALTHCARE_FACILITY_IRI, ORGANIZATIONAL_UNIT } from "./iri";

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

  function getPersonName(person) {
    const prefix = person.honorificPrefix ? person.honorificPrefix : ''
    const suffix = person.honorificSuffix ? `, ${person.honorificSuffix}` : ''
    return `${prefix} ${person.givenName} ${person.familyName}${suffix}`
  }

  async function fetchStudyDetails(studyId) {
    const fields = [
      "id", "label", "title_en", "title_it", "description_it", "description_en", "cohortDimension", "startDate",
      "endDate", "studyStatus.label", "studyStatus.name", "studyStatus.ontologyTermURI",
      "theme.label", "theme.name", "theme.ontologyTermURI", "therapeuticArea.label_it",
      "therapeuticArea.label_en", "therapeuticArea.iri", "principalInvestigator.agent.givenName",
      "principalInvestigator.agent.familyName", "principalInvestigator.agent.honorificPrefix", "dataSteward.agent.givenName",
      "dataSteward.agent.familyName", "dataSteward.agent.honorificPrefix", "ethicCommittee.agent.label",
      "publication.link", "publication.title", "publication.authors", "collectedDataType.name", "collectedDataType.label",
      "collectedDataType.ontologyTermURI", "collectedParameter.name", "collectedParameter.label",
      "collectedParameter.ontologyTermURI", "site.label", "site.type.name", "site.type.label",
      "site.parent.label", "site.parent.type.name", "site.parent.type.label", "site.parent.parent.label",
      "site.parent.parent.type.name", "site.parent.parent.type.label"
    ]
    const query = new QueryEMX2(graphqlEndpoint)
      .table("Study")
      .select(fields)
      .where("id")
      .equals(studyId)

    const response = await query.execute();
    let studyDetails = response.Study[0]

    const locale = i18n.global.locale.value;
    // studyDetails.attribute.forEach(attribute => {
    //   if (attribute.type === THERAPEUTIC_AREA_IRI) {
    //     studyDetails.therapeuticArea = attribute.value
    //   } else if (attribute.type === COLLECTED_SPECIMEN_TYPE_IRI) {
    //     studyDetails.collectedSpecimenType = attribute.value
    //   }
    // })

    const newStudyDetails = {
      id: studyDetails.id,
      label: studyDetails.label,
      title: studyDetails[`title_${locale}`],
      description: studyDetails[`description_${locale}`],
      cohortDimension: studyDetails.cohortDimension || '',
      startDate: studyDetails.startDate,
      endDate: studyDetails.endDate,
      status: studyDetails.studyStatus ? studyDetails.studyStatus[getOntologyI18nLabelItem()] : '',
      disease: studyDetails.theme[getOntologyI18nLabelItem()],
      principalInvestigator: studyDetails.principalInvestigator,
      therapeuticArea: studyDetails.therapeuticArea[getI18nLabelItem()],
      dataSteward: studyDetails.dataSteward?.map(ds => getPersonName(ds.agent)) || [],
      collectedDataType: studyDetails.collectedDataType,
      collectedParameter: studyDetails.collectedParameter,
      collectedSpecimenType: studyDetails.collectedSpecimenType,
      ethicCommittee: studyDetails.ethicCommittee ? studyDetails.ethicCommittee.agent.label : undefined,
      documentsFileURL: studyDetails.documents ? studyDetails.documents.url : undefined,
      // dmpFileURL: studyDetails.dmp ? studyDetails.dmp.url : undefined,
      publication: studyDetails.publication,
      contactPoint: studyDetails.contactPoint,
    }
    let currentLocation = studyDetails.site
    while (currentLocation !== undefined) {
      let foundType = false
      currentLocation.type.forEach(type => {
        if (type.IRI === LOCATION_HOSPITAL_IRI) {
          newStudyDetails.hospital = currentLocation.label
          foundType = true
        } else if (type.IRI === LOCATION_HEALTHCARE_FACILITY_IRI) {
          newStudyDetails.experimentationCenter = currentLocation.label
          foundType = true
        } else if (type.IRI === ORGANIZATIONAL_UNIT) {
          newStudyDetails.department = currentLocation.label
          foundType = true
        }
      })
      if (foundType === false) {
        newStudyDetails.organization = currentLocation.label
      }
      currentLocation = currentLocation.parent
    }
    // organization: studyDetails.location.parent.parent.label,
    // hospital: studyDetails.location.parent.label,
    // experimentationCenter: studyDetails.location.label,
    // state.studyDetails = newStudyDetails
    // if (router != null) {
    //   router.push({ name: 'study', params: { studyId: studyId } })
    // }

    return newStudyDetails;
  }

  return {
    studies, fetchStudies, fetchStudyDetails
  }

});
