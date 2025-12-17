<template>
  <div class="container">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <a :href="backHref">{{ $t('back_label') }}</a>
        </li>
      </ol>
    </nav>
    <h2>{{ getAttributeOrEmpty('title').toUpperCase() }}</h2>
    <hr />
    <study-attributes-group
      attribute-alignment="vertical"
      :attributes="headerAttributes"/>
    <hr />
    <study-attributes-group
      attribute-alignment="vertical"
      :attributes="documentAttributes"/>
    <hr />
    <div class="row">
      <div class="card" id="desc-card">
        <div class="card-header">
          <h5 class="mb-0">
            <a class="text-dark text-decoration-none" href="#desc" @click.prevent="toggleDesc">
              {{ $t('details_label') }}
            </a>
          </h5>
        </div>
        <div id="desc" class="collapse" :class="{ show: descExpanded }">
          <div class="card-body">
            {{ getAttributeOrEmpty('description') }}
          </div>
        </div>
      </div>
    </div>
    <hr />
    <study-attributes-group
      attribute-alignment="horizontal"
      :attributes="clinicalAttributes"/>
    <hr />
    <div v-if="studyDetails && studyDetails.publication && studyDetails.publication.length > 0">
      <h4>{{ $t('publications_label') }}</h4>
      <div v-for="publication of studyDetails.publication" :key="publication.IRI">
        <b>{{ publication.author }}</b>: {{ publication.title }}
        <a :href="publication.IRI" target="_blank">
          {{ getDoiValue(publication.IRI) }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useStudiesStore } from '../stores/studies'
// import { useI18n } from 'vue-i18n'
import StudyAttributesGroup from './StudyAttributesGroup.vue'

const route = useRoute()
const studiesStore = useStudiesStore()
// const { t } = useI18n()

// const backHref = ref('/' + route.params.locale)
const studyId = ref(route.params.studyId)
// const catalogContact = ref(undefined)
const descExpanded = ref(false)

const studyDetails = ref({})
// const error = computed(() => store.getters.getError)

function getAttributeOrEmpty(attribute) {
  if (Object.prototype.hasOwnProperty.call(studyDetails.value, attribute)) {
    return studyDetails.value[attribute]
  }
  return ''
}

const headerAttributes = computed(() => {
  const organizationItems = [{
    name: 'organization_label',
    value: getAttributeOrEmpty('organization')
  }]

  if (studyDetails.value.hospital) {
    organizationItems.push({
      name: 'hospital_label',
      value: getAttributeOrEmpty('hospital')
    })
  }
  if (studyDetails.value.experimentationCenter) {
    organizationItems.push({
      name: 'experimentation_center_label',
      value: getAttributeOrEmpty('experimentationCenter')
    })
  }
  if (studyDetails.value.department) {
    organizationItems.push({
      name: 'department_label',
      value: getAttributeOrEmpty('department')
    })
  }
  if (studyDetails.value.ethicCommittee) {
    organizationItems.push({
      name: 'ethic_committee_label',
      value: getAttributeOrEmpty('ethicCommittee')
    })
  }
  return [
    {
      id: '0',
      items: organizationItems
    },
    {
      id: '1',
      items: [
        {
          name: 'status_label',
          value: getAttributeOrEmpty('status')
        },
        {
          name: 'start_date_label',
          value: getAttributeOrEmpty('startDate')
        },
        {
          name: 'end_date_label',
          value: getAttributeOrEmpty('endDate')
        }
      ]
    },
    {
      id: '2',
      header: 'persons_involved_label',
      items: [
        {
          name: 'principal_investigator_label',
          value: getAttributeOrEmpty('principalInvestigator')
        },
        {
          name: 'data_steward_label',
          value: getAttributeOrEmpty('dataSteward')
        },
        // {
        //   name: 'contacts_label',
        //   value: this.catalogContact
        //     ? [{
        //       type: 'span',
        //       value: this.$options.filters.i18n('ifair_team_label')
        //     }, {
        //       type: 'mailto',
        //       value: `${this.catalogContact.email}?subject=${this.$options.filters.i18n('email_subject_message')} ${this.studyDetails.label}`
        //     }] : ''
        // }
      ]
    }
  ]
})

const documentAttributes = computed(() => {
  // Add your implementation here
  return []
})

const clinicalAttributes = computed(() => {
  // Add your implementation here
  return []
})

// const getCatalogContact = async () => {
//   catalogContact.value = await store.dispatch('getCatalogContact')
// }

// const getStudyDetailsData = async () => {
//   if (Object.keys(studyDetails.value).length === 0 && studyDetails.value.constructor === Object) {
//     await store.dispatch('getStudyDetails', {
//       studyId: route.params.studyId,
//       router: null
//     })
//   }
// }

const toggleDesc = () => {
  descExpanded.value = !descExpanded.value
}

const getDoiValue = (doiLink) => {
  return doiLink ? doiLink.replace('https://doi.org/', 'doi:') : ''
}

// const getPersonName = (person) => {
//   const prefix = person.honorificPrefix ? person.honorificPrefix : ''
//   const suffix = person.honorificSuffix ? `, ${person.honorificSuffix}` : ''
//   return `${prefix} ${person.givenName} ${person.familyName}${suffix}`
// }

// getCatalogContact()
// getStudyDetailsData()

onBeforeUnmount(() => {
  studiesStore.resetStudyDetails()
})

onMounted(async () => {
  // if (
  //   Object.keys(this.studyDetails.value).length === 0 &&
  //   this.studyDetails.constructor === Object
  // ) {
  studyDetails.value = await studiesStore.fetchStudyDetails(studyId.value)
  // }
})

</script>

<style scoped>
#desc-card .collapse:not(.show) {
  max-height: 150px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
}

a[href="#desc"]:after {
  content: " +";
  float: right;
}

a[href="#desc"].collapsed:after {
  content: " -";
}
</style>
