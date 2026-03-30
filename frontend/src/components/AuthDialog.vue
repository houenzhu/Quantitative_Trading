<template>
  <el-dialog
    v-model="visible"
    :title="isLogin ? '登录' : '注册'"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="0"
    >
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          placeholder="用户名"
          prefix-icon="User"
          size="large"
        />
      </el-form-item>
      
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          prefix-icon="Lock"
          size="large"
          show-password
        />
      </el-form-item>
      
      <el-form-item v-if="!isLogin" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="确认密码"
          prefix-icon="Lock"
          size="large"
          show-password
        />
      </el-form-item>
      
      <el-form-item v-if="!isLogin" prop="nickname">
        <el-input
          v-model="form.nickname"
          placeholder="昵称（可选）"
          prefix-icon="UserFilled"
          size="large"
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
          {{ isLogin ? '登录' : '注册' }}
        </el-button>
        <div class="switch-mode">
          <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
          <el-button type="primary" link @click="switchMode">
            {{ isLogin ? '立即注册' : '立即登录' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'login'
  }
})

const emit = defineEmits(['update:modelValue', 'login-success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isLogin = ref(true)
const loading = ref(false)
const formRef = ref(null)

watch(() => props.modelValue, (val) => {
  if (val) {
    isLogin.value = props.mode === 'login'
    resetForm()
  }
})

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!isLogin.value) {
    if (!value) {
      callback(new Error('请确认密码'))
    } else if (value !== form.password) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const switchMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      let res
      if (isLogin.value) {
        res = await authApi.login({
          username: form.username,
          password: form.password
        })
      } else {
        res = await authApi.register({
          username: form.username,
          password: form.password,
          nickname: form.nickname
        })
      }
      
      if (res.code === 200) {
        const { token, userInfo } = res.data
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        ElMessage.success(isLogin.value ? '登录成功' : '注册成功')
        emit('login-success', userInfo)
        visible.value = false
        resetForm()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '网络错误')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.nickname = ''
}

const handleClose = () => {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.dialog-footer .el-button {
  width: 100%;
}

.switch-mode {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
}

</style>
