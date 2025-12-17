import StudySearchContainer from '../components/StudySearchContainer.vue'
import StudyDetails from '../components/StudyDetails.vue'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: StudySearchContainer
    },
    {
      path: '/:studyId',
      name: 'study',
      component: StudyDetails
    }
  ]
})

export default router


