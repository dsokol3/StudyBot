import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UploadedNote, UploadProgress } from '@/types/study'
import { studyApi } from '@/services/studyApi'

function getFileTypeFromFilename(filename: string): 'pdf' | 'txt' | 'md' | 'docx' {
  const extension = filename.split('.').pop()?.toLowerCase()
  switch (extension) {
    case 'pdf': return 'pdf'
    case 'docx':
    case 'doc': return 'docx'
    case 'md':
    case 'markdown': return 'md'
    default: return 'txt'
  }
}

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
  
  // Sync documents from backend
  async function syncFromBackend(conversationId: string = 'default') {
    try {
      const backendDocs = await studyApi.getAllDocuments(conversationId)
      
      if (!backendDocs || backendDocs.length === 0) {
        // No documents on backend, keep local notes
        return true
      }
      
      // Merge with local notes, preferring backend as source of truth
      const backendIds = new Set(backendDocs.map(d => d.id))
      
      // Remove local notes that no longer exist on backend
      notes.value = notes.value.filter(note => backendIds.has(note.id))
      
      // Add or update notes from backend
      for (const backendDoc of backendDocs) {
        const existingIndex = notes.value.findIndex(n => n.id === backendDoc.id)
        if (existingIndex >= 0) {
          // Update existing note
          notes.value[existingIndex] = { 
            ...notes.value[existingIndex], 
            ...backendDoc,
            // Ensure required properties
            type: backendDoc.type || getFileTypeFromFilename(backendDoc.filename) || 'txt',
            size: backendDoc.size || backendDoc.fileSizeBytes || 0,
            uploadedAt: backendDoc.uploadedAt || backendDoc.createdAt || new Date().toISOString()
          }
        } else {
          // Add new note from backend
          notes.value.push({
            ...backendDoc,
            // Ensure required properties
            type: backendDoc.type || getFileTypeFromFilename(backendDoc.filename) || 'txt',
            size: backendDoc.size || backendDoc.fileSizeBytes || 0,
            uploadedAt: backendDoc.uploadedAt || backendDoc.createdAt || new Date().toISOString()
          })
        }
        
        // Fetch content if completed and missing
        if (backendDoc.status === 'COMPLETED' && !backendDoc.content) {
          try {
            const content = await studyApi.getDocumentContent(backendDoc.id)
            const note = notes.value.find(n => n.id === backendDoc.id)
            if (note) {
              note.content = content
            }
          } catch (err) {
            console.warn(`Could not fetch content for ${backendDoc.id}:`, err)
          }
        }
      }
      
      persistNotes()
      return true
    } catch (error) {
      console.warn('Could not sync documents from backend:', error)
      return false
    }
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
        
        // Ensure the note has all required properties
        const completeNote: UploadedNote = {
          ...note,
          type: note.type || getFileTypeFromFilename(note.filename) || 'txt',
          size: note.size || note.fileSizeBytes || file.size,
          uploadedAt: note.uploadedAt || new Date().toISOString()
        }
        
        // Add to notes immediately
        notes.value.push(completeNote)
        
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
    // Ensure the note has all required properties
    const completeNote: UploadedNote = {
      ...note,
      type: note.type || getFileTypeFromFilename(note.filename) || 'txt',
      size: note.size || note.fileSizeBytes || 0,
      uploadedAt: note.uploadedAt || new Date().toISOString()
    }
    notes.value.push(completeNote)
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
        const parsedNotes = JSON.parse(stored)
        // Migrate old notes to ensure they have required properties
        notes.value = parsedNotes.map((note: any) => ({
          ...note,
          type: note.type || getFileTypeFromFilename(note.filename) || 'txt',
          size: note.size || note.fileSizeBytes || 0,
          uploadedAt: note.uploadedAt || note.createdAt || new Date().toISOString()
        }))
      } catch {
        localStorage.removeItem('study-notes')
      }
    }
    isHydrated.value = true
    
    // Sync from backend in the background (non-blocking)
    setTimeout(() => {
      syncFromBackend().catch(err => {
        console.warn('Background document sync skipped:', err)
      })
    }, 1000)
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
    hydrate,
    syncFromBackend
  }
})
