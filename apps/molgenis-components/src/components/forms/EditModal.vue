<template>
  <LayoutModal :title="title" :show="isModalShown" @close="handleClose">
    <template #body>
      <div class="container" v-if="loaded && tableMetaData">
        <div class="row">
          <div
            class="overflow-auto mr-n3"
            :class="{ 'col-10': showChapters, 'col-12': !showChapters }"
            style="max-height: calc(100vh - 200px)"
          >
            <RowEdit
              :id="id"
              v-model="rowData"
              :pkey="pkey"
              :tableId="tableId"
              :tableMetaData="tableMetaData"
              :schemaMetaData="schemaMetaData"
              :visibleColumns="
                myUseChapters
                  ? columnsSplitByHeadings[currentPage - 1]
                  : visibleColumns
              "
              :clone="clone"
              :errorPerColumn="rowErrors"
              :applyDefaultValues="applyDefaultValues"
              @update:model-value="checkForErrors"
            />
          </div>
          <div
            v-if="showChapters"
            class="col-2 border-left chapter-menu overflow-auto"
            style="max-height: calc(100vh - 200px)"
          >
            <div class="mb-1">
              <b>Chapters</b>
            </div>
            <div v-for="(chapter, index) in columnsSplitByHeadings">
              <Tooltip
                :name="`chapter-${chapter[0]}-error-tooltip`"
                :value="
                  chapterStyleAndErrors[index].errorFields.length
                    ? `errors in:\n${chapterStyleAndErrors[index].errorFields}`
                    : ''
                "
                placement="left"
              >
                <button
                  type="button"
                  class="btn btn-link"
                  :class="{ 'font-weight-bold': index + 1 === currentPage }"
                  @click="setCurrentPage(index + 1)"
                  :style="chapterStyleAndErrors[index].style"
                >
                  {{ getHeadingLabel(chapter[0]) }}
                </button>
              </Tooltip>
            </div>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <RowEditFooter
        :id="id + '-footer'"
        :tableLabel="label"
        :errorMessage="errorMessage"
        :saveDisabledMessage="saveDisabledMessage"
        :saveDraftDisabledMessage="saveDraftDisabledMessage"
        @cancel="handleClose"
        @saveDraft="handleSaveDraftRequest"
        @save="handleSaveRequest"
      >
        <div class="mr-auto">
          <div v-if="showChapters">
            <ButtonAction
              @click="setCurrentPage(currentPage - 1)"
              :disabled="currentPage <= 1"
              class="mr-2 pr-3"
            >
              <i :class="'fas fa-fw fa-chevron-left'" /> Previous
            </ButtonAction>
            <ButtonAction
              @click="setCurrentPage(currentPage + 1)"
              :disabled="currentPage >= columnsSplitByHeadings.length"
              class="pl-3"
            >
              Next <i :class="'fas fa-fw fa-chevron-right'" />
            </ButtonAction>
          </div>
        </div>
      </RowEditFooter>
    </template>
  </LayoutModal>
</template>

<script lang="ts">
import type {
  IColumn,
  ISchemaMetaData,
  ISetting,
  ITableMetaData,
} from "meta-data-utils";
import type { IRow } from "../../Interfaces/IRow";
import { INewClient } from "../../client/IClient";
import Client from "../../client/client";
import constants from "../constants";
import LayoutModal from "../layout/LayoutModal.vue";
import { deepClone } from "../utils";
import ButtonAction from "./ButtonAction.vue";
import RowEdit from "./RowEdit.vue";
import RowEditFooter from "./RowEditFooter.vue";
import Tooltip from "./Tooltip.vue";
import {
  filterVisibleColumns,
  getChapterStyle,
  getRowErrors,
  getSaveDisabledMessage,
  isColumnVisible,
  removeKeyColumns,
  splitColumnIdsByHeadings,
} from "./formUtils/formUtils";
const { IS_CHAPTERS_ENABLED_FIELD_NAME } = constants;

export default {
  name: "EditModal",
  components: {
    LayoutModal,
    RowEditFooter,
    RowEdit,
    ButtonAction,
    Tooltip,
  },
  data() {
    return {
      rowData: {} as Record<string, any>,
      rowErrors: {} as Record<string, string | undefined>,
      tableMetaData: null as unknown as ITableMetaData,
      schemaMetaData: null as unknown as ISchemaMetaData,
      client: null as unknown as INewClient,
      errorMessage: "",
      loaded: false,
      currentPage: 1,
      myUseChapters: this.useChapters,
      saveDisabledMessage: "",
    };
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    isModalShown: {
      type: Boolean,
      required: true,
    },
    schemaId: {
      type: String,
      required: true,
    },
    pkey: {
      type: Object,
      default: () => null,
    },
    clone: {
      type: Boolean,
      default: () => false,
    },
    visibleColumns: {
      type: Array,
      default: () => null,
    },
    defaultValue: {
      type: Object,
      default: () => null,
    },
    useChapters: {
      type: Boolean,
      default: () => null,
    },
    applyDefaultValues: {
      type: Boolean,
    },
  },
  computed: {
    title() {
      return `${this.titlePrefix} into table: ${this.label} (${this.tableId})`;
    },
    label() {
      if (this.tableMetaData) {
        return this.tableMetaData.label;
      }
    },
    titlePrefix() {
      if (this.pkey && this.clone) {
        return "copy";
      } else if (this.pkey) {
        return "update";
      } else {
        return "insert";
      }
    },
    columnsSplitByHeadings(): string[][] {
      const filteredByVisibilityFilters = filterVisibleColumns(
        this.tableMetaData?.columns || [],
        this.visibleColumns as string[]
      );
      const filteredByVisibilityExpressions =
        filteredByVisibilityFilters.filter((column: IColumn) => {
          try {
            return isColumnVisible(column, this.rowData, this.tableMetaData);
          } catch (error: any) {
            this.rowErrors[column.id] = error;
            return true;
          }
        });
      const withoutMetadataColumns = filteredByVisibilityExpressions.filter(
        (column: IColumn) => !column.id.startsWith("mg_")
      );
      const splitByHeadings = splitColumnIdsByHeadings(withoutMetadataColumns);
      const filteredEmptyHeadings = splitByHeadings.filter(
        (chapter: string[]) => chapter.length > 1
      );
      return filteredEmptyHeadings;
    },
    chapterStyleAndErrors(): IChapterInfo[] {
      return this.columnsSplitByHeadings.map((page: string[]): IChapterInfo => {
        const errorFields = page.filter((fieldsInPage: string) =>
          Boolean(this.rowErrors[fieldsInPage])
        );
        return {
          style: getChapterStyle(page, this.rowErrors),
          errorFields,
        };
      });
    },
    saveDraftDisabledMessage() {
      const hasPrimaryKeyValue = this.tableMetaData?.columns.some(
        (column) =>
          column.key === 1 &&
          column.columnType !== "AUTO_ID" &&
          !this.rowData[column.id]
      );
      if (hasPrimaryKeyValue) {
        return "Cannot save draft: primary key is required";
      } else {
        return "";
      }
    },
    showChapters() {
      return this.myUseChapters && this.columnsSplitByHeadings.length > 1;
    },
  },
  methods: {
    setCurrentPage(newPage: number) {
      this.currentPage = newPage;
    },
    handleSaveRequest() {
      this.save({ ...this.rowData, mg_draft: false });
    },
    handleSaveDraftRequest() {
      this.save({ ...this.rowData, mg_draft: true });
    },
    async save(formData: IRow) {
      this.errorMessage = "";
      let result;
      if (this.pkey && !this.clone) {
        result = await this.client
          .updateDataRow(formData, this.tableId, this.schemaId)
          .catch(this.handleSaveError);
      } else {
        result = await this.client
          .insertDataRow(formData, this.tableId, this.schemaId)
          .catch(this.handleSaveError);
      }
      if (result) {
        this.$emit("update:newRow", formData);
        this.handleClose();
      }
    },
    handleSaveError(error: any) {
      if (error.response?.status === 403) {
        this.errorMessage =
          "Schema doesn't exist or permission denied. Do you need to Sign In?";
      } else {
        this.errorMessage =
          error.response?.data?.errors[0]?.message ||
          "An Error occurred during save";
      }
    },
    async fetchRowData() {
      const result = await this.client?.fetchRowData(this.tableId, this.pkey);
      if (!result) {
        this.errorMessage = `Error, unable to fetch data for this row (${this.pkey})`;
      } else {
        return result;
      }
    },
    handleClose() {
      this.errorMessage = "";
      this.$emit("close");
    },
    checkForErrors() {
      this.rowErrors = getRowErrors(this.tableMetaData, this.rowData);
      this.saveDisabledMessage = getSaveDisabledMessage(this.rowErrors);
    },
    getHeadingLabel(headingId: string) {
      const column = this.tableMetaData.columns.find(
        (column) => column.id === headingId
      );
      return column?.label || column?.id || headingId;
    },
  },
  async mounted() {
    this.loaded = false;
    this.client = Client.newClient(this.schemaId);
    this.schemaMetaData = await this.client.fetchSchemaMetaData();
    const settings: ISetting[] = await this.client.fetchSettings();

    if (this.useChapters === null) {
      this.myUseChapters =
        settings.find(
          (item: ISetting) => item.key === IS_CHAPTERS_ENABLED_FIELD_NAME
        )?.value !== "false";
    }

    this.tableMetaData = await this.client.fetchTableMetaData(this.tableId);

    if (this.pkey) {
      const rowData = await this.fetchRowData();

      if (this.clone) {
        this.rowData = removeKeyColumns(this.tableMetaData, rowData);
      } else {
        this.rowData = rowData;
      }
    }

    this.rowData = { ...this.rowData, ...deepClone(this.defaultValue) };
    this.checkForErrors();
    this.loaded = true;
  },
};

interface IChapterInfo {
  style: { color?: "red" };
  errorFields: string[];
}
</script>

<style scoped>
.chapter-menu {
  padding: 1rem;
  margin: -1rem -1rem -1rem 1rem;
  max-width: 16rem;
  overflow: hidden;
}

.chapter-menu button {
  padding: 0;
  max-width: 14rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
}
</style>

<docs>
<template>
  <DemoItem label="Edit Modal">
    <p>
      This component can be used in chapter mode split the form into multiple
      chapter based on headings.
    </p>
    <div class="mt-2 mb-3">
      Use the "isChaptersEnabled" schema setting to switch mode.
      <div class="font-weight-bold">
        isChaptersEnabled:
        <input
          :disabled="loadFromBackend"
          type="checkbox"
          id="checkbox"
          v-model="useChapters"
        />
      </div>
      <div class="font-weight-bold">
        load from backend:
        <input type="checkbox" id="checkbox" v-model="loadFromBackend" />
      </div>
    </div>

    <button class="btn btn-primary" @click="isModalShown = !isModalShown">
      Show {{ demoMode }} {{ tableId }}
    </button>
    <label for="table-selector" class="ml-5 pr-1">table</label>
    <select id="table-selector" v-model="tableId">
      <option>Pet</option>
      <option>Order</option>
      <option>Category</option>
      <option>User</option>
    </select>
    <input
      type="radio"
      id="insert"
      value="insert"
      v-model="demoMode"
      class="ml-5"
    />
    <label for="insert" class="pl-1">Insert</label>
    <input
      type="radio"
      id="update"
      value="update"
      v-model="demoMode"
      class="ml-1 pr-1"
    />
    <label for="update" class="pl-1">Update</label>
    <input
      type="radio"
      id="clone"
      value="clone"
      v-model="demoMode"
      class="ml-1 pr-1"
    />
    <label for="clone" class="pl-1">Clone</label>
    <EditModal
      :key="tableId + demoKey + demoMode + useChapters"
      id="edit-modal"
      :tableId="tableId"
      :pkey="demoKey"
      :clone="demoMode === 'clone'"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      :useChapters="useChapters"
      @close="isModalShown = false"
    />
  </DemoItem>
</template>

<script>
export default {
  data: function () {
    return {
      schemaId: "pet store",
      tableId: "Pet",
      demoMode: "insert", // one of [insert, update, clone]
      demoKey: null, // empty in case of insert
      isModalShown: false,
      useChapters: true,
      loadFromBackend: false,
    };
  },
  methods: {
    async reload() {
      const client = this.$Client.newClient(this.schemaId);
      const rowData = await client.fetchTableDataValues(this.tableId, {});
      this.demoKey =
        this.demoMode === "insert"
          ? null
          : await client.convertRowToPrimaryKey(rowData[0], this.tableId);
      const settings = await client.fetchSettings();
      if (this.loadFromBackend) {
        this.useChapters =
          settings.find(
            (item) =>
              item.key === this.$constants.IS_CHAPTERS_ENABLED_FIELD_NAME
          )?.value !== "false";
      }
    },
  },
  watch: {
    demoMode() {
      this.reload();
    },
    useChapters() {
      this.reload();
    },
    loadFromBackend() {
      this.reload();
    },
  },
  mounted() {
    this.reload();
  },
};
</script>
</docs>
