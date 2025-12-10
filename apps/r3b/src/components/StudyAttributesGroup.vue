<template>
  <b-row>
    <b-col
      v-for="col in attributes"
      :key="col.id"
      :sm="columnSize">
      <h2 v-if="col.header">
        {{ col.header | i18n }}
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
    </b-col>
  </b-row>
</template>

<script>
import StudyAttribute from './StudyAttribute.vue'
export default {
  components: { StudyAttribute },
  props: {
    attributeAlignment: { type: String, default: 'vertical' },
    attributes: { type: Array, default: () => [] }
  },
  computed: {
    columnSize () {
      return 12 / this.attributes.length
    }
  }
}
</script>
