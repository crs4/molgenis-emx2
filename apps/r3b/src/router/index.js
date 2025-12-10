import StudySearchContainer from '../components/StudySearchContainer.vue'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: StudySearchContainer
      // },
      // {
      //   path: 'studies/:studyId',
      //   name: 'study',
      //   component: StudyDetails
    }
  ]
})

export default router


