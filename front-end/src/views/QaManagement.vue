<template>
  <div class="page-card">
    <div class="page-toolbar">
      <span class="page-title">问答管理</span>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="问题内容" min-width="220" show-overflow-tooltip>
        <template #default="{row}">
          <span :class="{ hidden: row.status === 0 }">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提问人" width="130">
        <template #default="{row}">{{ row.authorName }}<span v-if="row.authorStage" class="stage">{{ row.authorStage }}</span></template>
      </el-table-column>
      <el-table-column prop="answerCount" label="回答数" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已隐藏' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提问时间" width="160" />
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button link type="primary" @click="showAnswers(row)">回答</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="hideQuestion(row)">隐藏</el-button>
          <el-button v-else link type="success" @click="showQuestion(row)">恢复</el-button>
          <el-button link type="warning" @click="openAnswer(row)">老师答</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > size" layout="total, prev, pager, next" :total="total"
                   :page-size="size" :current-page="page" @current-change="turnPage" style="margin-top:12px" />

    <!-- 回答列表 -->
    <el-dialog v-model="answerVisible" title="回答列表" width="640px">
      <el-table :data="answers" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="内容" min-width="200" show-overflow-tooltip>
          <template #default="{row}"><span :class="{ hidden: row.status === 0 }">{{ row.content }}</span></template>
        </el-table-column>
        <el-table-column label="回答人" width="130">
          <template #default="{row}">
            <el-tag v-if="row.isTeacher" type="primary" size="small">{{ row.teacherName || '老师' }}</el-tag>
            <span v-else>{{ row.authorName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" />
        <el-table-column label="操作" width="80">
          <template #default="{row}">
            <el-button v-if="row.status === 1" link type="danger" @click="hideAnswer(row)">隐藏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 老师回答 -->
    <el-dialog v-model="answerFormVisible" title="以老师身份回答" width="480px">
      <el-form label-width="80px">
        <el-form-item label="问题">
          <div class="q-preview">{{ currentQuestion && currentQuestion.content }}</div>
        </el-form-item>
        <el-form-item label="老师">
          <el-select v-model="answerForm.teacherId" placeholder="选择老师" style="width:100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回答内容">
          <el-input v-model="answerForm.content" type="textarea" :rows="4" placeholder="请输入回答内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="answerFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAnswer">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qaApi, teacherApi } from '@/api'

const list = ref([]); const page = ref(1); const size = ref(20); const total = ref(0)
const answers = ref([]); const answerVisible = ref(false)
const currentQuestion = ref(null)
const teachers = ref([]); const answerFormVisible = ref(false); const answerForm = ref({})

function load() {
  qaApi.questions({ page: page.value, size: size.value }).then((d) => {
    list.value = (d && d.list) || []; total.value = (d && d.total) || 0
  })
}
function turnPage(p) { page.value = p; load() }

function showAnswers(row) {
  currentQuestion.value = row
  qaApi.answers(row.id).then((d) => { answers.value = d || []; answerVisible.value = true })
}
function hideQuestion(row) {
  ElMessageBox.confirm('隐藏该问题？将连带隐藏其全部回答。', '隐藏问题')
    .then(() => qaApi.hideQuestion(row.id).then(() => { ElMessage.success('已隐藏'); load() })).catch(() => {})
}
function showQuestion(row) {
  qaApi.showQuestion(row.id).then(() => { ElMessage.success('已恢复（原被隐藏的回答仍保持隐藏）'); load() })
}
function hideAnswer(row) {
  qaApi.hideAnswer(row.id).then(() => { ElMessage.success('已隐藏'); showAnswers(currentQuestion.value) })
}
function openAnswer(row) {
  currentQuestion.value = row
  answerForm.value = { teacherId: null, content: '' }
  answerFormVisible.value = true
}
function submitAnswer() {
  if (!answerForm.value.content) { ElMessage.warning('请输入回答内容'); return }
  qaApi.teacherAnswer(currentQuestion.value.id, answerForm.value).then(() => {
    ElMessage.success('已回答'); answerFormVisible.value = false; load()
  })
}

onMounted(() => {
  teacherApi.list().then((d) => teachers.value = d || [])
  load()
})
</script>

<style scoped>
.hidden { color: #c0c4cc; text-decoration: line-through; }
.stage { color: #c0c4cc; font-size: 12px; margin-left: 4px; }
.q-preview { color: #c0c4cc; font-size: 13px; line-height: 1.5; }
</style>
