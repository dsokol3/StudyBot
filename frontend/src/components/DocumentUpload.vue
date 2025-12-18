<script setup lang="ts">
import { ref, computed } from 'vue'
import { Upload, X, FileText, FileWarning } from 'lucide-vue-next'
import { documentApi } from '@/services/api'
import type { Document } from '@/types'

const props = defineProps<{
  conversationId: string
}>()

const emit = defineEmits<{
  (e: 'uploaded', document: Document): void
  (e: 'upload-complete'): void
  (e: 'error', message: string): void
}>()

const isDragging = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const selectedFile = ref<File | null>(null)
const error = ref<string | null>(null)

const ALLOWED_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'text/markdown'
]

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB

const fileIcon = computed(() => {
  if (!selectedFile.value) return FileText
  return FileText
})

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const validateFile = (file: File): string | null => {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return 'Unsupported file type. Please upload PDF, DOCX, TXT, or MD files.'
  }
  if (file.size > MAX_FILE_SIZE) {
    return 'File too large. Maximum size is 50MB.'
  }
  return null
}

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = false
  
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file) selectFile(file)
  }
}

const handleFileInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    const file = input.files[0]
    if (file) selectFile(file)
  }
}

const selectFile = (file: File) => {
  error.value = null
  const validationError = validateFile(file)
  if (validationError) {
    error.value = validationError
    emit('error', validationError)
    return
  }
  selectedFile.value = file
}

const clearSelection = () => {
  selectedFile.value = null
  error.value = null
  uploadProgress.value = 0
}

const uploadFile = async () => {
  if (!selectedFile.value) return
  
  isUploading.value = true
  error.value = null
  uploadProgress.value = 0
  
  try {
    const document = await documentApi.uploadDocument(
      selectedFile.value,
      props.conversationId,
      (progress) => {
        uploadProgress.value = progress
      }
    )
    
    emit('uploaded', document)
    emit('upload-complete')
    clearSelection()
  } catch (err: any) {
    const message = err.response?.data?.error || err.message || 'Upload failed'
    error.value = message
    emit('error', message)
  } finally {
    isUploading.value = false
  }
}
</script>

<template>
  <div class="document-upload">
    <!-- Drop zone -->
    <div
      class="drop-zone"
      :class="{ 
        'drag-over': isDragging, 
        'has-file': selectedFile,
        'uploading': isUploading 
      }"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <input
        type="file"
        ref="fileInput"
        accept=".pdf,.docx,.txt,.md"
        @change="handleFileInput"
        hidden
      />
      
      <!-- Empty state -->
      <div v-if="!selectedFile && !isUploading" class="upload-prompt">
        <Upload class="icon" :size="32" />
        <p class="title">Drop files here or <button @click="($refs.fileInput as HTMLInputElement).click()">browse</button></p>
        <span class="hint">PDF, DOCX, TXT, MD up to 50MB</span>
      </div>
      
      <!-- File selected -->
      <div v-else-if="selectedFile && !isUploading" class="file-preview">
        <component :is="fileIcon" class="file-icon" :size="24" />
        <div class="file-info">
          <span class="filename">{{ selectedFile.name }}</span>
          <span class="filesize">{{ formatFileSize(selectedFile.size) }}</span>
        </div>
        <button class="remove-btn" @click.stop="clearSelection">
          <X :size="16" />
        </button>
      </div>
      
      <!-- Uploading -->
      <div v-else-if="isUploading" class="upload-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        </div>
        <span class="progress-text">Uploading... {{ uploadProgress }}%</span>
      </div>
    </div>
    
    <!-- Error message -->
    <div v-if="error" class="error-message">
      <FileWarning :size="16" />
      <span>{{ error }}</span>
    </div>
    
    <!-- Upload button -->
    <button
      v-if="selectedFile && !isUploading"
      class="upload-btn"
      @click="uploadFile"
    >
      <Upload :size="16" />
      Upload Document
    </button>
  </div>
</template>

<style scoped>
.document-upload {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.drop-zone {
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 0.75rem;
  padding: 1.5rem;
  text-align: center;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
}

.drop-zone:hover,
.drop-zone.drag-over {
  border-color: rgba(99, 102, 241, 0.5);
  background: rgba(99, 102, 241, 0.05);
}

.drop-zone.has-file {
  border-style: solid;
  border-color: rgba(99, 102, 241, 0.3);
}

.upload-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: rgba(255, 255, 255, 0.6);
}

.upload-prompt .icon {
  color: rgba(255, 255, 255, 0.4);
}

.upload-prompt .title {
  font-size: 0.875rem;
}

.upload-prompt button {
  color: rgb(99, 102, 241);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
}

.upload-prompt .hint {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.4);
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-align: left;
}

.file-icon {
  color: rgb(99, 102, 241);
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.filename {
  display: block;
  font-size: 0.875rem;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.filesize {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

.remove-btn {
  padding: 0.25rem;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 0.25rem;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s;
}

.remove-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: rgb(239, 68, 68);
}

.upload-progress {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3F5EFB, #FC466B);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 0.5rem;
  color: rgb(239, 68, 68);
  font-size: 0.75rem;
}

.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: linear-gradient(135deg, #3F5EFB, #FC466B);
  border: none;
  border-radius: 0.5rem;
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.upload-btn:hover {
  opacity: 0.9;
}
</style>
