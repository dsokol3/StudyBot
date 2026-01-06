import { ref, computed, onMounted } from 'vue'
import { useGenerationStore } from '@/stores/generation'
import { useNotesStore } from '@/stores/notes'
import { studyApi } from '@/services/studyApi'
import type { ToolType, GenerationResult } from '@/types/study'

export function useStudyTool<T extends GenerationResult>(toolType: ToolType) {
  const generationStore = useGenerationStore()
  const notesStore = useNotesStore()
  
  const isLoading = ref(false)
  const isLoadingContent = ref(true)
  const error = ref<string | null>(null)
  const result = ref<T | null>(null)
  const selectedContent = ref('')
  const fetchedContent = ref('')
  
  // Get combined content from all notes (local cache)
  const notesContent = computed(() => {
    // If we have fetched content from backend, prefer that
    if (fetchedContent.value?.trim()) {
      return fetchedContent.value
    }
    
    // Fallback to local content
    return notesStore.notes
      .map(note => note.content || '')
      .filter(c => c.trim())
      .join('\n\n---\n\n')
  })
  
  // Fetch content from backend
  async function fetchContentFromBackend(): Promise<string> {
    try {
      isLoadingContent.value = true
      const content = await studyApi.getAllDocumentContents('default')
      fetchedContent.value = content
      return content
    } catch (err) {
      console.error('Failed to fetch content from backend:', err)
      return ''
    } finally {
      isLoadingContent.value = false
    }
  }
  
  // Auto-fetch content on composable mount
  onMounted(() => {
    fetchContentFromBackend()
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
    options?: { 
      force?: boolean; 
      easyCount?: number;
      mediumCount?: number;
      hardCount?: number;
      additionalParams?: Record<string, unknown> 
    }
  ): Promise<T | null> {
    isLoading.value = true
    error.value = null
    
    try {
      let inputContent = content || selectedContent.value
      
      // If no content provided, fetch from backend
      if (!inputContent?.trim()) {
        inputContent = await fetchContentFromBackend()
      }
      
      // Fallback to local notes content
      if (!inputContent?.trim()) {
        inputContent = notesContent.value
      }
      
      // Truncate content if too long (backend limit is around 12,000 chars for prompt)
      const MAX_CONTENT_LENGTH = 8000 // Leave room for prompt overhead
      if (inputContent.length > MAX_CONTENT_LENGTH) {
        console.warn(`Content too long (${inputContent.length} chars), truncating to ${MAX_CONTENT_LENGTH}`)
        inputContent = inputContent.substring(0, MAX_CONTENT_LENGTH) + '\n\n[Content truncated for processing...]'
      }
      
      if (!inputContent?.trim()) {
        error.value = 'Please upload some documents first, or paste content to generate from'
        return null
      }
      
      // Merge difficulty params into additionalParams
      const additionalParams = { ...(options?.additionalParams || {}) }
      if (options?.easyCount !== undefined) additionalParams.easyCount = options.easyCount
      if (options?.mediumCount !== undefined) additionalParams.mediumCount = options.mediumCount
      if (options?.hardCount !== undefined) additionalParams.hardCount = options.hardCount
      
      const generatedResult = await generationStore.generate<T>(
        toolType, 
        inputContent,
        {
          force: options?.force,
          additionalParams
        }
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
    isLoadingContent,
    error,
    result,
    selectedContent,
    notesContent,
    previousResult,
    generate,
    fetchContentFromBackend,
    clearError,
    clearResult
  }
}
