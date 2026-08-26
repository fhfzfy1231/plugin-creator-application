<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'

type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
type ReviewScope = 'pending' | 'reviewed'
type Application = {
  metadata: { name: string; creationTimestamp: string }
  spec: {
    displayName: string
    username: string
    stage: 'CONTRIBUTOR' | 'AUTHOR'
    status: ApplicationStatus
    reason?: string
    qqScreenshot?: string
    articleTitle?: string
    articleUrl?: string
  }
}
type PagedApplications = {
  items: Application[]
  total: number
  pendingTotal: number
  reviewedTotal: number
  page: number
  size: number
  totalPages: number
}

const items = ref<Application[]>([])
const loading = ref(false)
const error = ref('')
const activeScope = ref<ReviewScope>('pending')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(1)
const pendingTotal = ref(0)
const reviewedTotal = ref(0)
const pageSizes = [10, 20, 50, 100]

const visiblePages = computed(() => {
  const start = Math.max(1, Math.min(page.value - 2, totalPages.value - 4))
  const end = Math.min(totalPages.value, start + 4)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get<PagedApplications>(
      '/apis/api.creator.jnoasa.cn/v1alpha1/applications/paged',
      {
        params: {
          scope: activeScope.value,
          page: page.value,
          size: pageSize.value,
        },
      },
    )
    items.value = response.data.items
    total.value = response.data.total
    pendingTotal.value = response.data.pendingTotal
    reviewedTotal.value = response.data.reviewedTotal
    page.value = response.data.page
    pageSize.value = response.data.size
    totalPages.value = response.data.totalPages
  } catch (caught: unknown) {
    error.value = axios.isAxiosError(caught)
      ? caught.response?.data?.detail || '加载失败'
      : '加载失败'
  } finally {
    loading.value = false
  }
}

async function switchScope(scope: ReviewScope) {
  if (activeScope.value === scope) return
  activeScope.value = scope
  page.value = 1
  await load()
}

async function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  await load()
}

async function changePageSize() {
  page.value = 1
  await load()
}

async function review(item: Application, approved: boolean) {
  const message = approved ? '' : prompt('请输入驳回原因') || ''
  if (!approved && !message) return
  await axios.post(
    `/apis/api.creator.jnoasa.cn/v1alpha1/applications/${item.metadata.name}/review`,
    { approved, message },
  )
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header>
      <div>
        <h1>申请审核</h1>
        <p>审核未审核作者与已审核作者申请；用户组由管理员手动分配</p>
      </div>
      <button class="refresh" :disabled="loading" @click="load">刷新</button>
    </header>

    <nav class="tabs" aria-label="审核状态筛选">
      <button
        :class="['tab', { active: activeScope === 'pending' }]"
        @click="switchScope('pending')"
      >
        未审核 <span>{{ pendingTotal }}</span>
      </button>
      <button
        :class="['tab', { active: activeScope === 'reviewed' }]"
        @click="switchScope('reviewed')"
      >
        已审核 <span>{{ reviewedTotal }}</span>
      </button>
    </nav>

    <div class="toolbar">
      <span>共 {{ total }} 条</span>
      <label>
        每页
        <select v-model.number="pageSize" :disabled="loading" @change="changePageSize">
          <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
        </select>
        条
      </label>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="state">加载中…</div>
    <div v-else class="list">
      <article v-for="item in items" :key="item.metadata.name" class="card">
        <div class="row">
          <div>
            <h2>
              {{ item.spec.displayName }} <small>@{{ item.spec.username }}</small>
            </h2>
            <span class="tag">{{
              item.spec.stage === 'CONTRIBUTOR' ? '申请未审核作者' : '申请已审核作者'
            }}</span>
            <span :class="['status', item.spec.status.toLowerCase()]">{{ item.spec.status }}</span>
          </div>
          <time>{{ new Date(item.metadata.creationTimestamp).toLocaleString() }}</time>
        </div>
        <p v-if="item.spec.reason">申请理由：{{ item.spec.reason }}</p>
        <a v-if="item.spec.articleUrl" :href="item.spec.articleUrl" target="_blank"
          >{{ item.spec.articleTitle }} ↗</a
        >
        <img v-if="item.spec.qqScreenshot" :src="item.spec.qqScreenshot" alt="QQ 群截图" />
        <div v-if="item.spec.status === 'PENDING'" class="actions">
          <button class="reject" @click="review(item, false)">驳回</button>
          <button class="approve" @click="review(item, true)">标记为通过</button>
        </div>
      </article>
      <p v-if="!items.length" class="state">
        {{ activeScope === 'pending' ? '暂无未审核申请' : '暂无已审核申请' }}
      </p>
    </div>

    <nav v-if="totalPages > 1" class="pagination" aria-label="申请列表分页">
      <button :disabled="page === 1 || loading" @click="changePage(page - 1)">上一页</button>
      <button
        v-for="pageNumber in visiblePages"
        :key="pageNumber"
        :class="{ current: pageNumber === page }"
        :disabled="loading"
        @click="changePage(pageNumber)"
      >
        {{ pageNumber }}
      </button>
      <button :disabled="page === totalPages || loading" @click="changePage(page + 1)">
        下一页
      </button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
    </nav>
  </section>
</template>

<style scoped>
.page {
  padding: 24px;
  max-width: 1100px;
  margin: auto;
}
header,
.row,
.actions,
.toolbar,
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
h1 {
  font-size: 24px;
  margin: 0;
}
header p,
small,
time,
.toolbar,
.pagination span {
  color: #667085;
}
button,
select {
  font: inherit;
}
.refresh,
.pagination button {
  border: 1px solid #d0d5dd;
  background: white;
  border-radius: 8px;
  padding: 8px 13px;
  cursor: pointer;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.tabs {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  border-bottom: 1px solid #e4e7ec;
}
.tab {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  padding: 10px 14px;
  color: #667085;
  cursor: pointer;
}
.tab.active {
  border-bottom-color: #2859d9;
  color: #2859d9;
  font-weight: 600;
}
.tab span {
  display: inline-block;
  min-width: 20px;
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 99px;
  background: #eef2ff;
  font-size: 12px;
}
.toolbar {
  margin-top: 16px;
}
.toolbar label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.toolbar select {
  border: 1px solid #d0d5dd;
  border-radius: 7px;
  padding: 5px 28px 5px 8px;
  background: white;
}
.list {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}
.card {
  background: white;
  border: 1px solid #e4e7ec;
  border-radius: 14px;
  padding: 18px;
}
.card h2 {
  margin: 0 0 10px;
  font-size: 18px;
}
.tag,
.status {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 99px;
  background: #eef2ff;
  font-size: 12px;
}
.pending {
  background: #fff3cd;
}
.approved {
  background: #d1fadf;
}
.rejected {
  background: #fee4e2;
}
.card img {
  display: block;
  max-width: 420px;
  max-height: 300px;
  border-radius: 8px;
  margin-top: 12px;
}
.actions {
  justify-content: flex-end;
  margin-top: 16px;
}
.actions button {
  border: 0;
  border-radius: 8px;
  padding: 8px 13px;
  cursor: pointer;
}
.approve {
  background: #2859d9;
  color: white;
}
.reject {
  background: #fee4e2;
  color: #b42318;
}
.pagination {
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 20px;
}
.pagination button.current {
  border-color: #2859d9;
  background: #2859d9;
  color: white;
}
.state {
  padding: 28px 0;
  text-align: center;
  color: #667085;
}
.error {
  color: #b42318;
}
@media (max-width: 720px) {
  .page {
    padding: 16px;
  }
  header,
  .row {
    align-items: flex-start;
  }
  .row {
    flex-direction: column;
  }
  .card img {
    max-width: 100%;
  }
}
</style>
