<script setup lang="ts">
import axios from 'axios'
import { onMounted, ref } from 'vue'
type Application = {
  metadata: { name: string; creationTimestamp: string }
  spec: {
    displayName: string
    username: string
    stage: 'CONTRIBUTOR' | 'AUTHOR'
    status: string
    reason?: string
    qqScreenshot?: string
    articleTitle?: string
    articleUrl?: string
  }
}
const items = ref<Application[]>([]),
  loading = ref(false),
  error = ref('')
async function load() {
  loading.value = true
  try {
    items.value = (await axios.get('/apis/api.creator.jnoasa.cn/v1alpha1/applications')).data
  } catch (e: any) {
    error.value = e?.response?.data?.detail || '加载失败'
  } finally {
    loading.value = false
  }
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
      <button @click="load">刷新</button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading">加载中…</div>
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
        ><img v-if="item.spec.qqScreenshot" :src="item.spec.qqScreenshot" alt="QQ 群截图" />
        <div v-if="item.spec.status === 'PENDING'" class="actions">
          <button class="reject" @click="review(item, false)">驳回</button
          ><button class="approve" @click="review(item, true)">标记为通过</button>
        </div>
      </article>
      <p v-if="!items.length">暂无申请</p>
    </div>
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
.actions {
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
time {
  color: #667085;
}
.list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
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
.actions button,
header button {
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
.error {
  color: #b42318;
}
</style>
