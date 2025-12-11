import { QueryEMX2 } from "molgenis-components";
import { defineStore } from "pinia";
import { useSettingsStore } from "./settings";
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */

export const useFiltersStore = defineStore("filtersStore", () => {
  const settingsStore = useSettingsStore();
  const graphqlEndpoint = settingsStore.graphqlEndpoint;

  async function getStudyStatusOptions() {
    const query = new QueryEMX2(graphqlEndpoint)
    .table("StudyStatus")
    .select(["name", "label", "code", "ontologyTermURI"]);

    const options = await query.execute()
    return options.StudyStatus;
  }

  async function getTherapeuticAreaOptions() {
    const query = new QueryEMX2(graphqlEndpoint)
    .table("TherapeuticArea")
    .select(["id", "label_en", "label_it", "iri"]);

    const options = await query.execute()
    return options.TherapeuticArea;

  }

  return {getStudyStatusOptions, getTherapeuticAreaOptions}

  // state: () => ({
  //   statusOptions: [[
  //     {
  //       "name": "Active",
  //       "label": "Attivo",
  //       "code": "ocre:OCRE001000",
  //       "codesystem": "OCRE"
  //     },
  //     {
  //       "name": "Withdrawn",
  //       "label": "Cancellato",
  //       "code": "ocre:OCRE400006",
  //       "codesystem": "OCRE"
  //     },
  //     {
  //       "name": "Completed",
  //       "label": "Completato",
  //       "code": "ocre:OCRE400053",
  //       "codesystem": "OCRE"
  //     },
  //     {
  //       "name": "Suspended",
  //       "label": "Sospeso",
  //       "code": "ocre:OCRE824000",
  //       "codesystem": "OCRE"
  //     },
  //     {
  //       "name": "Terminated",
  //       "label": "Terminato",
  //       "code": "ocre:OCRE908000",
  //       "codesystem": "OCRE"
  //     }
  //   ]],
  //   diseaseOptions: [],
  //   therapeuticAreaOptions: [
  //     {
  //       "id": "NCIT_C25178",
  //       "label_en": "Health",
  //       "label_it": "Salute",
  //       "iri": "http://purl.obolibrary.org/obo/NCIT_C25178"
  //     },
  //     {
  //       "id": "MESH_D008495",
  //       "label_en": "Medical Oncology",
  //       "label_it": "Oncologia Medica",
  //       "iri": "http://id.nlm.nih.gov/mesh/D008495"
  //     },
  //     {
  //       "id": "MESH_D009462",
  //       "label_en": "Neurology",
  //       "label_it": "Neurologia",
  //       "iri": "http://id.nlm.nih.gov/mesh/D009462"
  //     },
  //     {
  //       "id": "MESH_D010810",
  //       "label_en": "Physical and Rehabilitation Medicine",
  //       "label_it": "Medicina fisica e riabilitativa",
  //       "iri": "http://id.nlm.nih.gov/mesh/D010810"
  //     },
  //     {
  //       "id": "MESH_D013514",
  //       "label_en": "Surgical Procedures, Operative",
  //       "label_it": "Procedure chirurgiche, operative",
  //       "iri": "http://id.nlm.nih.gov/mesh/D013514"
  //     },
  //     {
  //       "id": "MESH_D017081",
  //       "label_en": "Cholecystectomy, Laparoscopic",
  //       "label_it": "Colecistectomia laparoscopica",
  //       "iri": "http://id.nlm.nih.gov/mesh/D017081"
  //     }
  //   ],
  //   collectedParameterOptions: []
  // }),
  // actions: {

  // }
});
