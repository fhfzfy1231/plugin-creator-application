import { definePlugin } from '@halo-dev/ui-shared'
import HomeView from './views/HomeView.vue'
import { IconPlug } from '@halo-dev/components'
import { markRaw } from 'vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/creator-applications',
        name: 'CreatorApplications',
        component: HomeView,
        meta: {
          title: '创作者申请',
          searchable: true,
          menu: {
            name: '创作者申请',
            icon: markRaw(IconPlug),
            priority: 0,
          },
          permissions: ['plugin:creator-application:manage'],
        },
      },
    },
  ],
  extensionPoints: {},
})
