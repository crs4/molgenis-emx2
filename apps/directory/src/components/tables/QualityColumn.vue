<template>
  <ul class="list-unstyled text-nowrap m-0">
    <li
      v-for="quality in qualities"
      :style="margin"
      :key="quality.id"
      class="d-flex"
    >
      <a
        :href="quality.certification_report"
        target="_blank"
        rel="noopener noreferrer"
        v-if="quality.certification_report"
      >
        <span v-if="!quality.certification_image_link">
          {{ quality.label }}
        </span>
        <span v-else>
          <img
            :src="quality.certification_image_link"
            class="quality-logo"
            :alt="generateQualityLabel(quality)"
          />
        </span>
      </a>
      <span v-else>
        <span
          v-if="
            !quality.certification_image_link && 'quality_standard' in quality
          "
        >
          {{ getQualityInfo(quality.quality_standard.name).label }}
        </span>
        <span v-else>
          <img
            v-if="quality.quality_standard"
            :src="quality.certification_image_link"
            class="quality-logo"
            :alt="generateQualityLabel(quality)"
          />
        </span>
      </span>
      <info-popover
        v-if="quality.quality_standard"
        class="ml-2"
        popover-placement="bottom"
      >
        <div class="popover-content">
          <div>
            <b> {{ getQualityInfo(quality.quality_standard.name)?.label }}</b>
          </div>
          <div>
            <span>
              {{ getQualityInfo(quality.quality_standard.name)?.definition }}
            </span>
          </div>
        </div>
      </info-popover>
    </li>
  </ul>
</template>

<script>
import { InfoPopover } from "molgenis-components";

export default {
  components: { InfoPopover },
  name: "quality-column",
  props: {
    qualities: {
      type: Array,
    },
    spacing: {
      type: Number,
    },
    qualityInfo: {
      type: Object,
    },
  },
  computed: {
    margin() {
      return `margin-top:${this.spacing}rem;margin-bottom:${this.spacing};`;
    },
  },
  methods: {
    generateQualityLabel(quality) {
      return quality.label !== "Others"
        ? this.getQualityInfo(quality.quality_standard.name).label
        : quality.certification_number;
    },
    getQualityInfo(key) {
      return this.qualityInfo[key];
    },
  },
};
</script>

<style scoped>
.popover-content {
  text-align: left;
  width: 14rem;
  max-width: 14rem;
  overflow-wrap: break-word;
  white-space: initial;
}

.quality-logo {
  max-width: 9rem;
  max-height: 4rem;
}

.fa-check {
  position: relative;
  top: 2px;
}
</style>
