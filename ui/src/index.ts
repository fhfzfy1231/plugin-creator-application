import { definePlugin, utils } from '@halo-dev/ui-shared'
import ApplyView from './views/ApplyView.vue'
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
        name: 'CreatorApplicationsRoot',
        redirect: () =>
          utils.permission.has(['plugin:creator-application:review'])
            ? '/creator-applications/review'
            : '/creator-applications/apply',
        meta: {
          title: '创作者申请',
          searchable: true,
          permissions: (permissions: string[]) =>
            permissions.includes('*') ||
            permissions.includes('plugin:creator-application:apply') ||
            permissions.includes('plugin:creator-application:review'),
          menu: {
            name: '创作者申请',
            group: 'tool',
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
        children: [
          {
            path: 'apply',
            name: 'CreatorApplicationApply',
            component: ApplyView,
            meta: {
              title: '创作者申请',
              searchable: true,
              permissions: ['plugin:creator-application:apply'],
              menu: {
                name: '申请',
                priority: 0,
              },
            },
          },
          {
            path: 'review',
            name: 'CreatorApplicationReview',
            component: HomeView,
            meta: {
              title: '申请审核',
              searchable: true,
              permissions: ['plugin:creator-application:review'],
              menu: {
                name: '审核',
                priority: 1,
              },
            },
          },
        ],
      },
    },
  ],
  extensionPoints: {},
})
