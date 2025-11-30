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
        
        // Success - add to notes
        notes.value.push(note)
        uploadQueue.value.set(uploadId, {
          id: uploadId,
          filename: file.name,
          progress: 100,
          status: 'complete'
        })
        
        persistNotes()
        results.push({ success: true, note })
        
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
