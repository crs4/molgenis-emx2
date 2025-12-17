<template>
  <div :class="{ 'd-flex': alignment === 'horizontal' }">
    <div v-if="type !== 'button'" class="fw-bold" :class="alignment">
      {{ $t(name) }}
    </div>
    <button
      v-if="type === 'button'"
      :disabled="value === '' || value === undefined"
      class="btn btn-secondary btn-sm"
      @click="navigateTo(value)">
      {{ $t(name) }}
    </button>
    <span v-else-if="type === 'span'">
      {{ value }}
    </span>
    <a v-else-if="type === 'mailto'" :href="`mailto:${value}`">
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
        class="badge rounded-pill me-1 mb-1"
        :class="badgeColor()"
        :title="$t('iri_tooltip_message')">{{ item[getI18nLabel] }}</a>
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

<script setup>
import { computed } from 'vue'
import { getOntologyI18nLabelItem } from '../utils'

const props = defineProps({
  type: { type: String, default: 'div' },
  name: { type: String, default: '' },
  value: { type: [String, Array], default: '' },
  alignment: { type: String, default: 'vertical' },
  link: { type: String, default: '' },
  valueColor: { type: String, default: 'primary' }
})

const valueType = computed(() => {
  return typeof props.value
})

const getI18nLabel = getOntologyI18nLabelItem

const badgeColor = () => {
  return `bg-${props.valueColor}`
}

const navigateTo = (url) => {
  if (url) window.location.href = url
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
