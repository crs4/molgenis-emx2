<template>
  <div class="container">
    <search-form class="mt-3" v-if="studies.length > 0" layout="inline" />
    <search-form class="mt-3" v-else layout="form" />
    <hr />
    <table v-if="studies.length > 0" class="table table-hover">
      <thead>
        <tr>
          <th v-for="field in fields" :key="field.key">{{ $t(field.label) }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="study in studies"
          :key="study.id"
          class="clickable-row"
          @click="studyDetailsPageHandler(study)">
          <td v-for="field in fields" :key="field.key">{{ study[field.key] }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { useStudiesStore } from '../stores/studies'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from './SearchForm.vue'
const studiesStore = useStudiesStore()

const router = useRouter()

const fields = ref([
  {
    key: 'label',
    label: 'name_label'
  },
  {
    key: 'title',
    label: 'title_label'
  },
  {
    key: 'studyStatus',
    label: 'status_label'
  }
])

// const queryParams = ref(route.query)
const studies = computed(() => studiesStore.studies)
const filters = computed(() => studiesStore.filters)

const runQueryStudies = () => {
  if (filters.value && Object.keys(filters.value).length > 0) {
    const remappingKeys = {
      disease: 'theme',
      therapeuticArea: 'therapeuticArea',
      studyStatus: 'studyStatus',
      collectedParameter: 'collectedParameter'
    }
    const remappedParameters = {}
    for (const key in filters.value) {
      remappedParameters[remappingKeys[key]] = filters.value[key]
    }
    // store.dispatch('queryStudies', remappedParameters)
  }
}

const studyDetailsPageHandler = (study) => {
  // studiesStore.fetchStudies, { studyId: record.identifier, router })
  router.push(`/${study.id}`)
}

watch(filters, runQueryStudies, { immediate: true })
</script>

<style>
.clickable-row {
  cursor: pointer;
}
</style>
