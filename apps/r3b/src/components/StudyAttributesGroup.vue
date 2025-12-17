<template>
  <div class="row">
    <div
      v-for="col in attributes"
      :key="col.id"
      :class="`col-sm-${columnSize}`">
      <h2 v-if="col.header">
        {{ $t(col.header) }}
      </h2>
      <div
        v-for="attribute in col.items"
        :key="attribute.name"
        class="pb-3">
        <study-attribute
          :name="attribute.name"
          :type="attribute.type"
          :value="attribute.value"
          :alignment="attributeAlignment"
          :link="attribute.link"
          :valueColor="attribute.valueColor"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StudyAttribute from './StudyAttribute.vue'

const props = defineProps({
  attributeAlignment: { type: String, default: 'vertical' },
  attributes: { type: Array, default: () => [] }
})

const columnSize = computed(() => {
  return 12 / props.attributes.length
})
</script>
