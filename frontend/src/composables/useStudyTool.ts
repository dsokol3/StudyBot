import { ref, computed } from 'vue'
import { useGenerationStore } from '@/stores/generation'
import { useNotesStore } from '@/stores/notes'
import type { ToolType, GenerationResult } from '@/types/study'

export function useStudyTool<T extends GenerationResult>(toolType: ToolType) {
  const generationStore = useGenerationStore()
  const notesStore = useNotesStore()
  
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const result = ref<T | null>(null)
  const selectedContent = ref('')
  
  // Get combined content from all notes
  const notesContent = computed(() => {
    return notesStore.notes.map(note => note.content).join('\n\n---\n\n')
  })
  
  // Check if there's cached history for this tool
  const previousResult = computed(() => {
    return generationStore.getFromHistory(toolType)
  })
  
  // Initialize with previous result if available
  if (previousResult.value) {
    result.value = previousResult.value.result as T
  }
  
  async function generate(
    content?: string,
    options?: { force?: boolean; additionalParams?: Record<string, unknown> }
  ): Promise<T | null> {
    isLoading.value = true
    error.value = null
    
    try {
      const inputContent = content || selectedContent.value || notesContent.value
      
      if (!inputContent.trim()) {
        error.value = 'Please provide some content to generate from'
        return null
      }
      
      const generatedResult = await generationStore.generate<T>(
        toolType, 
        inputContent,
        options
      )
      
      result.value = generatedResult
      return generatedResult
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to generate content'
      return null
    } finally {
      isLoading.value = false
    }
  }
  
  function clearError() {
    error.value = null
  }
  
  function clearResult() {
    result.value = null
  }
  
  return {
    isLoading,
    error,
    result,
    selectedContent,
    notesContent,
    previousResult,
    generate,
    clearError,
    clearResult
  }
}
