<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'

type Stage = 'CONTRIBUTOR' | 'AUTHOR'

const stage = ref<Stage>('CONTRIBUTOR')
const username = ref('')
const reason = ref('')
const screenshot = ref<string | null>(null)
const screenshotName = ref('')
const articleTitle = ref('')
const articleUrl = ref('')
const message = ref('')
const submitting = ref(false)
const messageType = ref<'success' | 'error' | ''>('')

const isContributor = computed(() => stage.value === 'CONTRIBUTOR')

onMounted(async () => {
  try {
    const { data } = await axios.get('/apis/api.creator.jnoasa.cn/v1alpha1/me')
    username.value = data.username || ''
  } catch {
    messageType.value = 'error'
    message.value = '无法读取当前登录用户，请刷新页面后重试。'
  }
})

function selectStage(value: Stage) {
  stage.value = value
  message.value = ''
  messageType.value = ''
}

function handleScreenshot(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  screenshot.value = null
  screenshotName.value = ''
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    input.value = ''
    messageType.value = 'error'
    message.value = '截图不能超过 2 MB。'
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    screenshot.value = String(reader.result)
    screenshotName.value = file.name
    message.value = ''
    messageType.value = ''
  }
  reader.onerror = () => {
    messageType.value = 'error'
    message.value = '截图读取失败，请重新选择。'
  }
  reader.readAsDataURL(file)
}

async function submit() {
  message.value = ''
  messageType.value = ''

  if (isContributor.value && (!screenshot.value || !reason.value.trim())) {
    messageType.value = 'error'
    message.value = '请上传 QQ 群截图并填写申请理由。'
    return
  }
  if (!isContributor.value && (!articleTitle.value.trim() || !articleUrl.value.trim())) {
    messageType.value = 'error'
    message.value = '请填写文章名称和链接。'
    return
  }

  submitting.value = true
  try {
    await axios.post('/apis/api.creator.jnoasa.cn/v1alpha1/applications', {
      stage: stage.value,
      reason: reason.value,
      qqScreenshot: screenshot.value,
      articleTitle: articleTitle.value,
      articleUrl: articleUrl.value,
    })
    reason.value = ''
    screenshot.value = null
    screenshotName.value = ''
    articleTitle.value = ''
    articleUrl.value = ''
    const fileInput = document.querySelector<HTMLInputElement>('#qq-screenshot')
    if (fileInput) fileInput.value = ''
    messageType.value = 'success'
    message.value = '申请已提交，请等待管理员审核。'
  } catch (error: unknown) {
    messageType.value = 'error'
    message.value = axios.isAxiosError(error)
      ? error.response?.data?.detail || error.response?.data?.message || '提交失败，请稍后重试。'
      : '提交失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="page">
    <div class="card">
      <header>
        <div>
          <h1>创作者申请</h1>
          <p>成为创作者需要分两步完成，请根据当前阶段提交申请。</p>
        </div>
        <span v-if="username" class="account">当前账号：{{ username }}</span>
      </header>

      <div class="tabs" role="tablist" aria-label="申请阶段">
        <button
          type="button"
          :class="{ active: isContributor }"
          @click="selectStage('CONTRIBUTOR')"
        >
          第一步 · 申请未审核作者
        </button>
        <button type="button" :class="{ active: !isContributor }" @click="selectStage('AUTHOR')">
          第二步 · 申请已审核作者
        </button>
      </div>

      <form @submit.prevent="submit">
        <template v-if="isContributor">
          <div class="field">
            <label for="qq-screenshot">QQ 群成员截图</label>
            <input
              id="qq-screenshot"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              @change="handleScreenshot"
            />
            <p class="hint">支持 PNG、JPEG、WebP，最大 2 MB，仅供管理员审核。</p>
            <p v-if="screenshotName" class="selected">已选择：{{ screenshotName }}</p>
          </div>

          <div class="field">
            <label for="reason">申请理由</label>
            <textarea
              id="reason"
              v-model="reason"
              maxlength="1000"
              placeholder="请告诉管理员你为什么希望成为创作者"
            />
          </div>
        </template>

        <template v-else>
          <div class="field">
            <label for="article-title">文章名称</label>
            <input
              id="article-title"
              v-model="articleTitle"
              maxlength="200"
              placeholder="请输入已完成文章的名称"
            />
          </div>

          <div class="field">
            <label for="article-url">文章链接</label>
            <input
              id="article-url"
              v-model="articleUrl"
              type="url"
              placeholder="https://jnoasa.cn/archives/..."
            />
          </div>
        </template>

        <div class="nickname-note">网站昵称将在提交时自动读取，无需手动填写。</div>
        <button class="submit" type="submit" :disabled="submitting">
          {{ submitting ? '正在提交…' : '提交申请' }}
        </button>
        <p v-if="message" :class="['message', messageType]">{{ message }}</p>
      </form>
    </div>
  </section>
</template>

<style scoped>
.page {
  padding: 24px;
  max-width: 900px;
  margin: auto;
}
.card {
  background: #fff;
  border: 1px solid #e4e7ec;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px #1018280d;
}
header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
h1 {
  font-size: 24px;
  margin: 0 0 6px;
}
header p {
  margin: 0;
  color: #667085;
}
.account {
  white-space: nowrap;
  border-radius: 999px;
  background: #f2f4f7;
  padding: 6px 10px;
  color: #475467;
  font-size: 13px;
}
.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 24px;
}
.tabs button {
  border: 1px solid #d0d5dd;
  border-radius: 10px;
  background: #fff;
  color: #475467;
  padding: 11px 14px;
  cursor: pointer;
}
.tabs button.active {
  border-color: #2859d9;
  background: #eef4ff;
  color: #1849a9;
  font-weight: 600;
}
.field {
  margin-bottom: 18px;
}
.field label {
  display: block;
  font-weight: 600;
  margin-bottom: 7px;
}
.field input,
.field textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d5dd;
  border-radius: 9px;
  padding: 10px 12px;
  font: inherit;
  color: #101828;
  background: #fff;
}
.field textarea {
  min-height: 130px;
  resize: vertical;
}
.hint,
.selected {
  font-size: 13px;
  color: #667085;
  margin: 7px 0 0;
}
.selected {
  color: #2859d9;
}
.nickname-note {
  border-radius: 9px;
  background: #f8f9fc;
  padding: 10px 12px;
  color: #667085;
  font-size: 13px;
}
.submit {
  width: 100%;
  margin-top: 18px;
  border: 0;
  border-radius: 9px;
  background: #2859d9;
  color: #fff;
  padding: 11px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}
.submit:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.message {
  margin: 14px 0 0;
}
.message.success {
  color: #067647;
}
.message.error {
  color: #b42318;
}
@media (max-width: 640px) {
  .page {
    padding: 14px;
  }
  .card {
    padding: 18px;
  }
  header {
    display: block;
  }
  .account {
    display: inline-block;
    margin-top: 12px;
  }
  .tabs {
    grid-template-columns: 1fr;
  }
}
</style>
