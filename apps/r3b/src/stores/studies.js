import { defineStore } from "pinia";
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */

export const useStudiesStore = defineStore("studiesStore", {
  state: () => ({
    studies: [],
    filters: []
  }),
});
