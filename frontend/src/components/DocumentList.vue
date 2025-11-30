<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { FileText, Trash2, Loader2, CheckCircle, XCircle, RefreshCw } from 'lucide-vue-next'
import { documentApi } from '@/services/api'
import type { Document } from '@/types'

const props = defineProps<{
  conversationId: string
}>()

const documents = ref<Document[]>([])
const isLoading = ref(false)
const pollingIntervals = ref<Map<string, number>>(new Map())

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'COMPLETED': return CheckCircle
    case 'FAILED': return XCircle
    case 'PROCESSING': return Loader2
    default: return RefreshCw
  }
}

const getStatusColor = (status: string): string => {
  switch (status) {
    case 'COMPLETED': return 'text-green-500'
    case 'FAILED': return 'text-red-500'
    case 'PROCESSING': return 'text-yellow-500'
    default: return 'text-gray-500'
  }
}

const loadDocuments = async () => {
  if (!props.conversationId) return
  
  isLoading.value = true
  try {
    documents.value = await documentApi.getDocumentsByConversation(props.conversationId)
    
    // Start polling for processing documents
    documents.value.forEach(doc => {
      if (doc.status === 'PENDING' || doc.status === 'PROCESSING') {
        startPolling(doc.id)
      }
    })
  } catch (err) {
    console.error('Failed to load documents:', err)
  } finally {
    isLoading.value = false
  }
}

const startPolling = (documentId: string) => {
  if (pollingIntervals.value.has(documentId)) return
  
  const intervalId = window.setInterval(async () => {
    try {
      const status = await documentApi.getDocumentStatus(documentId)
      const docIndex = documents.value.findIndex(d => d.id === documentId)
      
      if (docIndex !== -1 && documents.value[docIndex]) {
        const existingDoc = documents.value[docIndex]
        documents.value[docIndex] = {
          id: existingDoc.id,
          filename: existingDoc.filename,
          contentType: existingDoc.contentType,
          fileSizeBytes: existingDoc.fileSizeBytes,
          createdAt: existingDoc.createdAt,
          status: status.status,
          chunkCount: status.chunkCount,
          errorMessage: status.errorMessage
        }
        
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          stopPolling(documentId)
        }
      }
    } catch (err) {
      console.error('Polling failed:', err)
      stopPolling(documentId)
    }
  }, 2000)
  
  pollingIntervals.value.set(documentId, intervalId)
}

const stopPolling = (documentId: string) => {
  const intervalId = pollingIntervals.value.get(documentId)
  if (intervalId) {
    clearInterval(intervalId)
    pollingIntervals.value.delete(documentId)
  }
}

const deleteDocument = async (documentId: string) => {
  try {
    await documentApi.deleteDocument(documentId)
    documents.value = documents.value.filter(d => d.id !== documentId)
    stopPolling(documentId)
  } catch (err) {
    console.error('Failed to delete document:', err)
  }
}

const addDocument = (document: Document) => {
  documents.value.unshift(document)
  if (document.status === 'PENDING' || document.status === 'PROCESSING') {
    startPolling(document.id)
  }
}

// Watch for conversation changes
watch(() => props.conversationId, () => {
  // Stop all polling
  pollingIntervals.value.forEach((_, id) => stopPolling(id))
  loadDocuments()
})

onMounted(() => {
  loadDocuments()
})

onUnmounted(() => {
  // Clean up all polling intervals
  pollingIntervals.value.forEach((_, id) => stopPolling(id))
})

defineExpose({
  addDocument,
  loadDocuments
})
</script>

<template>
  <div class="document-list">
    <div v-if="isLoading" class="loading">
      <Loader2 class="animate-spin" :size="20" />
      <span>Loading documents...</span>
    </div>
    
    <div v-else-if="documents.length === 0" class="empty">
      <FileText :size="32" class="opacity-30" />
      <p>No documents uploaded yet</p>
    </div>
    
    <div v-else class="documents">
      <div 
        v-for="doc in documents" 
        :key="doc.id"
        class="document-item"
      >
        <div class="doc-icon">
          <FileText :size="20" />
        </div>
        
        <div class="doc-info">
          <span class="doc-name" :title="doc.filename">{{ doc.filename }}</span>
          <div class="doc-meta">
            <span>{{ formatFileSize(doc.fileSizeBytes) }}</span>
            <span v-if="doc.status === 'COMPLETED'">• {{ doc.chunkCount }} chunks</span>
          </div>
        </div>
        
        <div class="doc-status" :class="getStatusColor(doc.status)">
          <component 
            :is="getStatusIcon(doc.status)" 
            :size="16"
            :class="{ 'animate-spin': doc.status === 'PROCESSING' }"
          />
          <span class="status-text">{{ doc.status.toLowerCase() }}</span>
        </div>
        
        <button 
          class="delete-btn"
          @click="deleteDocument(doc.id)"
          title="Delete document"
        >
          <Trash2 :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.document-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.loading,
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.documents {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 0.5rem;
  transition: background 0.2s;
}

.document-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.doc-icon {
  color: rgba(99, 102, 241, 0.8);
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  display: block;
  font-size: 0.875rem;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

.doc-status {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  flex-shrink: 0;
}

.status-text {
  text-transform: capitalize;
}

.text-green-500 {
  color: rgb(34, 197, 94);
}

.text-red-500 {
  color: rgb(239, 68, 68);
}

.text-yellow-500 {
  color: rgb(234, 179, 8);
}

.text-gray-500 {
  color: rgb(107, 114, 128);
}

.delete-btn {
  padding: 0.375rem;
  background: transparent;
  border: none;
  border-radius: 0.25rem;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.2s;
  opacity: 0;
}

.document-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: rgb(239, 68, 68);
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
