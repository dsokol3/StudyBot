import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UploadedNote, UploadProgress } from '@/types/study'
import { studyApi } from '@/services/studyApi'

export const useNotesStore = defineStore('notes', () => {
  // State
  const notes = ref<UploadedNote[]>([])
  const uploadQueue = ref<Map<string, UploadProgress>>(new Map())
  const isHydrated = ref(false)
  
  // Getters
  const hasNotes = computed(() => notes.value.length > 0)
  const totalNotes = computed(() => notes.value.length)
  const combinedContent = computed(() => 
    notes.value.map(n => n.content || '').join('\n\n---\n\n')
  )
  
  // Poll for document processing status
  async function pollDocumentStatus(noteId: string, maxAttempts = 30): Promise<boolean> {
    for (let i = 0; i < maxAttempts; i++) {
      try {
        const status = await studyApi.getDocumentStatus(noteId)
        
        if (status.status === 'COMPLETED') {
          // Fetch the content
          const content = await studyApi.getDocumentContent(noteId)
          
          // Update the note with content
          const noteIndex = notes.value.findIndex(n => n.id === noteId)
          const note = notes.value[noteIndex]
          if (noteIndex !== -1 && note) {
            note.content = content
            note.status = 'COMPLETED'
            persistNotes()
          }
          return true
        } else if (status.status === 'FAILED') {
          return false
        }
        
        // Wait 1 second before next poll
        await new Promise(resolve => setTimeout(resolve, 1000))
      } catch {
        // Continue polling on error
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
    }
    return false
  }
  
  // Actions
  async function uploadFiles(files: File[]) {
    const results: Array<{ success: boolean; note?: UploadedNote; error?: string }> = []
    
    for (const file of files) {
      const uploadId = crypto.randomUUID()
      
      // Track progress
      uploadQueue.value.set(uploadId, {
        id: uploadId,
        filename: file.name,
        progress: 0,
        status: 'uploading'
      })
      
      try {
        const note = await studyApi.uploadNotes(file)
        
        // Add to notes immediately
        notes.value.push(note)
        
        // Update queue to processing
        uploadQueue.value.set(uploadId, {
          id: uploadId,
          filename: file.name,
          progress: 50,
          status: 'processing'
        })
        
        // Poll for completion and fetch content
        const success = await pollDocumentStatus(note.id)
        
        if (success) {
          uploadQueue.value.set(uploadId, {
            id: uploadId,
            filename: file.name,
            progress: 100,
            status: 'complete'
          })
          results.push({ success: true, note })
        } else {
          uploadQueue.value.set(uploadId, {
            id: uploadId,
            filename: file.name,
            progress: 0,
            status: 'error',
            error: 'Processing failed'
          })
          results.push({ success: false, error: 'Processing failed' })
        }
        
        persistNotes()
        
        // Clear from queue after delay
        setTimeout(() => {
          uploadQueue.value.delete(uploadId)
        }, 2000)
      } catch (error) {
        const errorMsg = error instanceof Error ? error.message : 'Upload failed'
        uploadQueue.value.set(uploadId, {
          id: uploadId,
          filename: file.name,
          progress: 0,
          status: 'error',
          error: errorMsg
        })
        results.push({ success: false, error: errorMsg })
      }
    }
    
    return results
  }
  
  function addLocalNote(note: UploadedNote) {
    notes.value.push(note)
    persistNotes()
  }
  
  function removeNote(noteId: string) {
    const index = notes.value.findIndex(n => n.id === noteId)
    if (index !== -1) {
      notes.value.splice(index, 1)
      persistNotes()
    }
  }
  
  function clearUploadQueue() {
    uploadQueue.value.clear()
  }
  
  function clearAll() {
    notes.value = []
    uploadQueue.value.clear()
    localStorage.removeItem('study-notes')
  }
  
  // Persistence
  function persistNotes() {
    localStorage.setItem('study-notes', JSON.stringify(notes.value))
  }
  
  function hydrate() {
    if (isHydrated.value) return
    
    const stored = localStorage.getItem('study-notes')
    if (stored) {
      try {
        notes.value = JSON.parse(stored)
      } catch {
        localStorage.removeItem('study-notes')
      }
    }
    isHydrated.value = true
  }
  
  // Hydrate on store creation
  hydrate()
  
  return {
    notes,
    uploadQueue,
    hasNotes,
    totalNotes,
    combinedContent,
    uploadFiles,
    addLocalNote,
    removeNote,
    clearUploadQueue,
    clearAll,
    hydrate
  }
})
