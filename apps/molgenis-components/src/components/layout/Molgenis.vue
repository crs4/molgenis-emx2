<template>
  <div style="background-color: #f4f4f4">
    <CookieWall
      v-if="analyticsId"
      :analyticsId="analyticsId"
      :htmlContentString="cookieWallContent"
    />
    <div style="min-height: calc(100vh - 70px)">
      <MolgenisMenu
        :logo="logoURLorDefault"
        active="My search"
        :items="menu"
        :session="session"
      >
        <MolgenisSession
          v-model="session"
          :key="timestamp"
          @error="$emit('error', $event)"
        />
      </MolgenisMenu>
      <Breadcrumb
        v-if="showCrumbs && Object.keys(crumbs).length > 1"
        :crumbs="crumbs"
        :dropdown="schemaUrlsForCrumbs"
      />
      <div class="container-fluid p-3" style="padding-bottom: 50px">
        <h1 v-if="title">{{ title }}</h1>
        <slot />
      </div>
    </div>
    <MolgenisFooter>
      <span v-if="session?.manifest">
        Software version:
        <a
          :href="
            'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
            session.manifest.SpecificationVersion
          "
        >
          {{ session.manifest.SpecificationVersion }} </a
        >.
        <span v-if="session.manifest.DatabaseVersion">
          Database version: {{ session.manifest.DatabaseVersion }}.
        </span>
      </span>
    </MolgenisFooter>
  </div>
</template>

<script lang="ts">
import MolgenisMenu from "./MolgenisMenu.vue";
import MolgenisSession from "../account/MolgenisSession.vue";
import MolgenisFooter from "./MolgenisFooter.vue";
import Breadcrumb from "./Breadcrumb.vue";
import CookieWall from "./CookieWall.vue";
import { request, gql } from "graphql-request";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: {
    MolgenisSession,
    MolgenisMenu,
    MolgenisFooter,
    Breadcrumb,
    CookieWall,
  },
  props: {
    menuItems: {
      type: Array,
      default: () => [
        { label: "Tables", href: "tables", role: "Viewer" },
        {
          label: "Schema",
          href: "schema",
          role: "Viewer",
        },
        {
          label: "Up/Download",
          href: "updownload",
          role: "Viewer",
        },
        {
          label: "Reports",
          href: "reports",
          role: "Viewer",
        },
        {
          label: "Jobs & Scripts",
          href: "tasks",
          role: "Manager",
        },
        {
          label: "Graphql",
          href: "graphql-playground",
          role: "Viewer",
        },
        {
          label: "Settings",
          href: "settings",
          role: "Manager",
        },
        {
          label: "Help",
          href: "docs",
          role: "Viewer",
        },
      ],
    },
    title: String,
    showCrumbs: {
      type: Boolean,
      default: true,
    },
  },
  data: function () {
    return {
      session: {} as Record<string, any>,
      logoURL: null,
      fullscreen: false,
      timestamp: Date.now(),
      analyticsId: null,
      cookieWallContent: null,
    };
  },
  computed: {
    schemaUrlsForCrumbs() {
      let result: Record<string, any> = {
        "list all databases": "/apps/central/",
      };
      //all databases
      if (this.session?.schemas) {
        this.session.schemas.forEach((schema: string) => {
          result[schema] = "../../" + schema; // all paths are of form /:schema/:app
        });
      }
      return result;
    },
    crumbs() {
      let result: Record<string, any> = {};
      if (window && location) {
        let path = decodeURI(
          window.location.pathname.replace(location.search, "")
        ).split("/");
        let url = "/";
        if (window.location.pathname != "/apps/central/") {
          path.forEach((el) => {
            if (el !== "") {
              url += el + "/";
              result[el] = url;
            }
          });
        }
        if (this.$route) {
          path = decodeURI(location.hash.split("?")[0]).substr(1).split("/");
          url += "#";
          path.forEach((el) => {
            if (el !== "") {
              url += "/" + el;
              result[el] = url;
            }
          });
        }
      }
      return result;
    },
    logoURLorDefault() {
      return (
        this.logoURL ||
        "/apps/molgenis-components/assets/img/molgenis_logo_white.png"
      );
    },
    menu() {
      if (this.session?.settings?.menu) {
        return this.session.settings.menu;
      } else {
        return this.menuItems;
      }
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        if (this.session?.settings?.logoURL) {
          this.logoURL = this.session.settings.logoURL;
        }
        this.$emit("update:modelValue", this.session);
      },
    },
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    },
  },
  emits: ["update:modelValue", "error"],
  created() {
    request(
      "graphql",
      gql`
        {
          _settings {
            key
            value
          }
        }
      `
    ).then((data: any) => {
      const analyticsSetting = data._settings.find(
        (setting: Record<string, any>) => setting.key === "ANALYTICS_ID"
      );
      this.analyticsId = analyticsSetting ? analyticsSetting.value : null;
      const analyticsCookieWallContentSetting = data._settings.find(
        (setting: Record<string, any>) =>
          setting.key === "ANALYTICS_COOKIE_WALL_CONTENT"
      );
      this.cookieWallContent = analyticsCookieWallContentSetting
        ? analyticsCookieWallContentSetting.value
        : null;
    });
  },
};
</script>

<docs>
<template>
  <Molgenis :menuItems="[
        {label:'Home',href:'/'},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]" title="My title" v-model="molgenis">
    <template>
      <p>Some contents and I can see the molgenis state via v-model = {{ JSON.stringify(molgenis) }}</p>
    </template>
  </Molgenis>
</template>
<script>
  export default {
    data() {
      return {
        molgenis: null
      }
    }
  }
</script>
</docs>
