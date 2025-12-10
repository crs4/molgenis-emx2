<template>
  <div :class="{ 'd-flex': alignment === 'horizontal' }">
    <div v-if="type !== 'button'" class="font-weight-bold" :class="alignment">
      {{ name | i18n }}
    </div>
    <b-button
      v-if="type === 'button'"
      :disabled="value === '' || value === undefined"
      :href="value"
      class="btn btn-secondary btn-sm">
      {{ name | i18n }}
    </b-button>
    <span v-else-if="type === 'span'">
      {{ value }}
    </span>
    <a v-else-if="type === 'mailto'" :href="value">
      <i class="fa fa-envelope" aria-hidden="true" />
    </a>
    <div
      v-else-if="type === 'div' && valueType === 'string'"
      :class="alignment">
        {{ value }}
    </div>
    <div v-else-if="type === 'list'">
      <a
        v-for="(item, index) in value"
        :key="`${item}-${index}`"
        :href="item.IRI"
        target="_blank"
      class="badge badge-pill mr-1 mb-1"
      :class="badgeColor()"
      data-toggle="tooltip"
      data-placement="top"
      :title="'iri_tooltip_message' | i18n">{{ item[getI18nLabel] }}</a>
    </div>
    <div v-else :class="alignment">
      <study-attribute
        v-for="(item, index) in value"
        :key="'subitem' + index"
        :type="item.type"
        :value="item.value"
        :name="item.name"
        :alignment="alignment"/>
    </div>
  </div>
</template>

<script>
import { getI18nLabel } from '../utils'

export default {
  name: 'StudyAttribute',
  props: {
    type: { type: String, default: 'div' },
    name: { type: String, default: '' },
    value: { type: [String, Array], default: '' },
    alignment: { type: String, default: 'vertical' },
    link: { type: String, default: '' },
    valueColor: { type: String, default: 'primary' }
  },
  computed: {
    valueType () {
      return typeof this.value
    },
    getI18nLabel: getI18nLabel
  },
  methods: {
    badgeColor () {
      // return `badge-${this.valueColor}`
      return 'badge-red'
    }
  }
}
</script>

<style>
.horizontal {
  display: inline-block;
  min-width: 250px;
}
.vertical {
  display: block;
}
</style>
