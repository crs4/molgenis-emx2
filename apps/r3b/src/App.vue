<template>
  <Molgenis v-model="session">
    <header class="header text-center text-white">
      <div class="float-end">
        <a href="/it" @click.prevent="changeLocale('it')">
          <img
            id="ita-lang"
            src="./assets/custom_icons/ita-lang.png"
            title="Ita"
            width="40"
            height="20"
            alt="Italian"/>
        </a>
        <a href="/en" @click.prevent="changeLocale('en')">
          <img
            id="uk-lang"
            class="mx-1"
            src="./assets/custom_icons/uk-lang.png"
            title="English"
            width="40"
            height="20"
            alt="English"/>
        </a>
      </div>
      <div class="p-4 rounded-0 bg-secondary">
        <h1 class="mb-3 display-4">{{ $t('header_label') }}</h1>
        <p class="h5">{{ $t('header_description') }}</p>
      </div>
    </header>
    <router-view :session="session" />
    <template #footer>
      <div class="text-center text-muted fst-italic mb-2">
        <img src="./assets/logos/ifair_program.png" class="me-4 fluid" width="50" alt="iFAIR"/>
        <img src="./assets/logos/sardegna_ricerche.png" class="me-4 fluid" width="90" alt="Sardegna Ricerche"/>
        <img src="./assets/logos/crs4.png" class="me-4 fluid" width="110" alt="CRS4"/>
        <img src="./assets/logos/unica.png" class="me-4 fluid" width="150" alt="UniCA"/>
        <img src="./assets/logos/uniss.png" class="me-4 fluid" width="110" alt="UNISS"/>
        <img src="./assets/logos/ue.png" class="me-4 fluid" width="100" alt="EU"/>
        <img src="./assets/logos/repubblica_italiana.svg" class="me-4 fluid" width="55" alt="Repubblica Italiana"/>
        <img src="./assets/logos/ras.svg" class="me-4 fluid" width="140" alt="RAS"/>
        <img src="./assets/logos/por-fesr.png" class="me-4 fluid" width="90" alt="POR-FESR"/>
      </div>
    </template>
  </Molgenis>

</template>

<script setup lang="ts">
//@ts-expect-error
import { Molgenis } from "molgenis-components";
import { useSettingsStore } from "./stores/settings"
import { useFavicon } from "@vueuse/core";
// import { storeToRefs } from "pinia"
import { ref, onMounted } from "vue";

const session = ref({});

// const errorModalShow = ref(false)
// const error = ref('')
// const errorWatcher = watch(error, async (error) => {
//   errorModalShow.value = error !== undefined
// })

const settingsStore = useSettingsStore()

// Use storeToRefs to keep 'locale' reactive if you need to display it
// const { locale } = storeToRefs(settingsStore)

const changeLocale = (lang) => {
  settingsStore.setLocale(lang)
}

function changeFavicon() {
  useFavicon("favicon.ico");
}

onMounted(() => {
  changeFavicon()
})

// export default {
//   data () {
//     return {
//       errorModalShow: false
//     }
//   },
//   // computed: {
//   //   ...mapGetters({
//   //     error: 'getError'
//   //   })
//   // },
//   watch: {
//     error: function () {
//       this.errorModalShow = this.error !== undefined
//     }
//   },
//   methods: {
//     // ...mapMutations(['setError']),
//     // changeLocale: function (event) {
//     //   const splittedCurrentPath = getSplittedCurrentPath()
//     //   const language = event.target.id === 'ita-lang' ? 'it' : 'en'
//     //   if (splittedCurrentPath[1] !== language) {
//     //     splittedCurrentPath[1] = language
//     //   }
//     //   setCurrentPath(splittedCurrentPath.join('/'))
//     // }
//   }
// }
</script>

<!-- <style src="vue-multiselect/dist/vue-multiselect.min.css"></style> -->
<style scoped>
.fs-5 {
  font-size: 0.80rem;
}
</style>

