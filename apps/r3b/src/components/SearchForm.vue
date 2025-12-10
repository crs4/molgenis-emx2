<template>
  <div v-if="layout === 'form'" class="container">
    <form @submit.prevent="onSubmit" @reset.prevent="onReset">
      <div class="mb-3">
        <!-- <label for="disease" class="form-label">{{ $t('disease_label') }}</label> -->
        <!-- <vue-typeahead-bootstrap
          id="disease"
          v-model="disease"
          :data="diseaseOptions"
          :serializer="(item) => `${item['label-it']} - ${item.id}`"
          :placeholder="$t('select_disease_message')"
          min-matching-results="1"
          @hit="selectedDisease = $event"
          @input="diseaseSearch"/> -->
      </div>
      <div class="mb-3">
        <label for="therapeutic-area" class="form-label">{{ $t('therapeutic_area_label') }}</label>
        <select
          id="therapeutic-area"
          v-model="therapeuticArea"
          class="form-select">
          <option :value="null">-- {{ $t('select_therapeutic_area_message') }} --</option>
          <option v-for="item in therapeuticAreaOptions" :key="item.iri" :value="item.iri">
            {{ item[getI18nLabel()] }}
          </option>
        </select>
      </div>
      <div class="mb-3">
        <label for="study-status" class="form-label">{{ $t('study_status_label') }}</label>
        <select
          id="study-status"
          v-model="studyStatus"
          class="form-select">
          <option :value="null">-- {{ $t('select_study_status_message') }} --</option>
          <option v-for="item in statusOptions" :key="item.id" :value="item.id">
            {{ item[getI18nLabel()] }}
          </option>
        </select>
      </div>
      <!-- <div class="mb-3"> -->
        <!-- <label for="collected-parameters" class="form-label">{{ $t('collected_parameters_label') }}</label> -->
        <!-- <vue-typeahead-bootstrap
          id="collected-parameters"
          v-model="collectedParameter"
          :data="collectedParameterOptions"
          :serializer="(item) => `${item[getI18nLabel]} - ${item.id}`"
          :placeholder="$t('select_collected_parameters_message')"
          min-matching-results="1"
          @hit="selectedCollectedParameter = $event"/> -->
      <!-- </div> -->
      <button
        type="submit"
        class="btn btn-primary me-2 mt-2"
        :disabled="isSubmitButtonDisabled">
        {{ $t("search_button") }}
      </button>
      <button
        type="reset"
        class="btn btn-info me-2 mt-2"
        :disabled="isResetButtonDisabled">
        {{ $t("reset_button") }}
      </button>
      <button
        :id="getAllButtonId"
        type="submit"
        class="btn btn-secondary mt-2">
        {{ $t("search_all_button") }}
      </button>
    </form>
  </div>
  <div v-else class="container">
    <form @submit.prevent="onSubmit" @reset.prevent="onReset" class="row g-3 align-items-end">
      <div class="col-auto">
        <!-- <label for="disease-inline" class="form-label">{{ $t('disease_label') }}</label> -->
        <!-- <vue-typeahead-bootstrap
          id="disease-inline"
          v-model="disease"
          :data="diseaseOptions"
          :serializer="(item) => `${item[getI18nLabel]} - ${item.id}`"
          :placeholder="$t('select_disease_message')"
          min-matching-results="1"
          @hit="selectedDisease = $event"
          @input="diseaseSearch"/> -->
      </div>
      <div class="col-auto">
        <label for="therapeutic-area-inline" class="form-label">{{ $t('therapeutic_area_label') }}</label>
        <select
          id="therapeutic-area-inline"
          v-model="therapeuticArea"
          class="form-select">
          <option :value="null">-- {{ $t('select_therapeutic_area_message') }} --</option>
          <option v-for="item in therapeuticAreaOptions" :key="item.prefixIRI" :value="item.prefixIRI">
            {{ item[getI18nLabel()] }}
          </option>
        </select>
      </div>
      <div class="col-auto">
        <label for="study-status-inline" class="form-label">{{ $t('study_status_label') }}</label>
        <select
          id="study-status-inline"
          v-model="studyStatus"
          class="form-select">
          <option :value="null">-- {{ $t('select_study_status_message') }} --</option>
          <option v-for="item in statusOptions" :key="item.id" :value="item.id">
            {{ item[getI18nLabel()] }}
          </option>
        </select>
      </div>
      <div class="col-auto">
        <!-- <label for="collected-parameters-inline" class="form-label">{{ $t('collected_parameters_label') }}</label>
        <vue-typeahead-bootstrap
          id="collected-parameters-inline"
          v-model="collectedParameter"
          :data="collectedParameterOptions"
          :serializer="(item) => `${item[getI18nLabel]} ${item.id}`"
          :placeholder="$t('select_collected_parameters_message')"
          min-matching-results="1"
          @hit="selectedCollectedParameter = $event"/> -->
      </div>
      <div class="col-auto">
        <button
          id="search-button"
          type="submit"
          class="btn btn-primary"
          :disabled="isSubmitButtonDisabled">
          {{ $t('search_button') }}
        </button>
        <button
          type="reset"
          class="btn btn-info ms-2"
          :disabled="isResetButtonDisabled">
          {{ $t('reset_button') }}
        </button>
        <button
          :id="getAllButtonId"
          type="submit"
          class="btn btn-secondary ms-2">
          {{ $t('search_all_button') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import _ from 'underscore'
import { useFiltersStore } from '../stores/filters'
import { computed, ref, onMounted } from 'vue'
import { getI18nLabel } from '../utils/index'
// import VueTypeaheadBootstrap from 'vue-typeahead-bootstrap'

defineProps({
  layout: {
    type: String,
    default: 'form'
  }
})
const filtersStore = useFiltersStore()

// const router = useRouter()
// const route = useRoute()
// const store = useStore()

const therapeuticArea = ref(null)
const studyStatus = ref(null)
const selectedCollectedParameter = ref({})
const selectedDisease = ref({})
const collectedParameter = ref('')
const disease = ref('')
const getAllButtonId = ref('get-all-button')

const statusOptions = computed(() => filtersStore.statusOptions)
const diseaseOptions = computed(() => filtersStore.diseaseOptions)
const therapeuticAreaOptions = computed(() => filtersStore.therapeuticAreaOptions)
const collectedParameterOptions = computed(() => filtersStore.collectedParameterOptions)

const isSubmitButtonDisabled = computed(() => {
  return (
    !studyStatus.value &&
    !disease.value &&
    !therapeuticArea.value &&
    !collectedParameter.value
  )
})

const isResetButtonDisabled = computed(() => isSubmitButtonDisabled.value)

const diseaseSearch = _.debounce(function (query) {
  if (query === '') {
    selectedDisease.value = {}
  } else {
    store.dispatch('getDiseaseOptions', query)
  }
}, 500)

const collectedParameterSearch = _.debounce(function (query) {
  if (query === '') {
    selectedCollectedParameter.value = {}
  } else {
    store.dispatch('getCollectedParameterOptions', query)
  }
}, 500)

const onSubmit = function (event) {
  if (event.submitter.id === getAllButtonId.value) {
    studyStatus.value = null
    disease.value = null
    therapeuticArea.value = null
    collectedParameter.value = null
  }
  const params = {
    studyStatus: studyStatus.value,
    disease: selectedDisease.value ? selectedDisease.value.id : null,
    therapeuticArea: therapeuticArea.value,
    collectedParameter: selectedCollectedParameter.value ? selectedCollectedParameter.value.id : null
  }

  // store.commit('updateFilters', { filters: params, router })
}

const onReset = function () {
  studyStatus.value = null
  disease.value = null
  therapeuticArea.value = null
  collectedParameter.value = null

  if (props.layout === 'inline') {
    const params = {
      studyStatus: null,
      disease: null,
      therapeuticArea: null,
      collectedParameter: null
    }
    // store.commit('updateFilters', { filters: params, router })
  }
}

const getFiltersFromUrl = function () {
  const params = {}
  for (const param in route.query) {
    if (['studyStatus', 'therapeuticArea'].includes(param)) {
      eval(`${param}.value = params[param] = route.query[param]`)
    } else if (param === 'disease') {
      // store.dispatch('getDiseaseData', {
      //   diseaseCode: route.query[param],
      //   callback: (diseaseData) => {
      //     selectedDisease.value = diseaseData
      //     disease.value =
      //       selectedDisease.value[getI18nLabel()] + ' ' + selectedDisease.value.code
      //   }
      // })
    } else if (param === 'collectedParameter') {
      // store.dispatch('getCollectedParameterData', {
      //   collectedParameterCode: route.query[param],
      //   callback: (collectedParameterData) => {
      //     selectedCollectedParameter.value = collectedParameterData
      //     collectedParameter.value =
      //       selectedCollectedParameter.value[getI18nLabel()]
      //   }
      // })
    }
  }
}

onMounted(() => {
  // store.dispatch('getDiseaseOptions')
  // store.dispatch('getStudyStatusOptions')
  // store.dispatch('getTherapeuticAreaOptions')
  // store.dispatch('getCollectedParameterOptions')
  // getFiltersFromUrl()
})
</script>

<style>
.search-param {
  max-width: 230px;
}
</style>
