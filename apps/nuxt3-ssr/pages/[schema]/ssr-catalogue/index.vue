<script setup lang="ts">
import type { IFilter } from "~/interfaces/types";

//add redirect middleware for cohortOnly to skip this page
definePageMeta({
  middleware: [
    function (to, from) {
      const cohortOnly =
        to.query["cohort-only"] === "true" ||
        useRuntimeConfig().public.cohortOnly;
      if (cohortOnly) {
        return navigateTo(`/${to.params.schema}/ssr-catalogue/all`, {
          replace: true,
        });
      }
    },
  ],
});

useHead({ title: "Health Data and Samples Catalogue" });

const route = useRoute();
const config = useRuntimeConfig();

let filters: IFilter[] = reactive([
  {
    title: "Search in catalogues",
    columnType: "_SEARCH",
    search: "",
    initialCollapsed: false,
  },
]);

let search = computed(() => {
  // @ts-ignore
  return filters.find((f) => f.columnType === "_SEARCH").search;
});

const query = computed(() => {
  return `
  query Catalogues($filter:CataloguesFilter){
    Catalogues(filter:$filter) {
      network {
        id
        name
        acronym
        description
        logo {
          id
          size
          extension
          url
        }
      }
      type {name}
    }
    Catalogues_agg (filter:$filter){
      count
    }
  }
  `;
});

const filter = computed(() => buildQueryFilter(filters, search.value));

let graphqlURL = computed(() => `/${route.params.schema}/graphql`);
const { data, pending, error, refresh } = await useFetch(graphqlURL.value, {
  key: "catalogues",
  method: "POST",
  body: {
    query,
    variables: { filter },
  },
});
let activeName = ref("compact");

const thematicCatalogues = computed(() => {
  let result = data?.value?.data?.Catalogues
    ? data.value?.data?.Catalogues?.filter((c) => c.type?.name === "theme")
    : [];
  result.sort((a, b) => a.network.id.localeCompare(b.network.id));
  return result;
});
const projectCatalogues = computed(() => {
  let result = data?.value?.data?.Catalogues
    ? data.value?.data?.Catalogues?.filter((c) => c.type?.name === "project")
    : [];
  result.sort((a, b) => a.network.id.localeCompare(b.network.id));
  return result;
});
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      title="European Health Research Data and Sample Catalogue"
      description="A collaborative effort to integrate the catalogues of diverse EU research projects and networks to accelerate reuse and improve citizens health."
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        >
          <div class="flex flex-col items-center max-w-sm lg:mt-5">
            <NuxtLink :to="`/${route.params.schema}/ssr-catalogue/all`">
              <Button label="Search all" />
            </NuxtLink>
            <p
              class="mt-1 mb-0 text-center lg:mt-10 text-body-lg"
              v-if="
                thematicCatalogues.length > 0 || projectCatalogues.length > 0
              "
            >
              or select a specific catalogue below:
            </p>
          </div>
        </div>
      </template>
    </PageHeader>
    <ContentBlockCatalogues
      v-if="thematicCatalogues.length > 0"
      title="Thematic catalogues"
      description="Catalogues focused on a particular theme, developed by a collaboration of projects, networks and/or organisations:"
      :catalogues="thematicCatalogues"
    />
    <ContentBlockCatalogues
      v-if="projectCatalogues.length > 0"
      title="Project catalogues"
      description="Catalogues maintained by individual research projects or consortia:"
      :catalogues="projectCatalogues"
    />
  </LayoutsLandingPage>
</template>
