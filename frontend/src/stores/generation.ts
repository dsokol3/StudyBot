import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { 
  GenerationResult, 
  ToolType, 
  GenerationHistoryItem
} from '@/types/study'
import { studyApi } from '@/services/studyApi'

export const useGenerationStore = defineStore('generation', () => {
  // State
  const history = ref<GenerationHistoryItem[]>([])
  const activeGenerations = ref<Map<string, { toolType: ToolType; startTime: number }>>(new Map())
  const cache = ref<Map<string, GenerationResult>>(new Map())
  const lastError = ref<string | null>(null)
  
  // Getters
  const isGenerating = computed(() => activeGenerations.value.size > 0)
  const generationCount = computed(() => history.value.length)
  
  // Hydrate from localStorage
  function hydrate() {
    const stored = localStorage.getItem('study-generation-history')
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        history.value = parsed.slice(0, 50) // Keep last 50
      } catch {
        localStorage.removeItem('study-generation-history')
      }
    }
  }
  
  // Persist to localStorage
  function persist() {
    localStorage.setItem('study-generation-history', JSON.stringify(history.value.slice(0, 50)))
  }
  
  // Generate content hash for caching
  function hashContent(content: string): string {
    let hash = 0
    for (let i = 0; i < Math.min(content.length, 1000); i++) {
      hash = ((hash << 5) - hash) + content.charCodeAt(i)
      hash = hash & hash
    }
    return hash.toString(36)
  }
  
  // Actions
  async function generate<T extends GenerationResult>(
    toolType: ToolType,
    content: string,
    options?: { force?: boolean; additionalParams?: Record<string, unknown> }
  ): Promise<T> {
    const cacheKey = `${toolType}:${hashContent(content)}`
    
    // Check cache first (unless forced)
    if (!options?.force && cache.value.has(cacheKey)) {
      return cache.value.get(cacheKey) as T
    }
    
    const generationId = crypto.randomUUID()
    activeGenerations.value.set(generationId, {
      toolType,
      startTime: Date.now()
    })
    lastError.value = null
    
    try {
      let result: GenerationResult
      
      switch (toolType) {
        case 'summary':
          result = await studyApi.generateSummary(content)
          break
        case 'flashcards':
          result = await studyApi.generateFlashcards(content)
          break
        case 'questions':
          result = await studyApi.generateQuestions(content)
          break
        case 'essay-prompts':
          result = await studyApi.generateEssayPrompts(content)
          break
        case 'explanations':
          result = await studyApi.explainText(content)
          break
        case 'diagrams':
          result = await studyApi.generateDiagram(content)
          break
        case 'study-plan':
          result = await studyApi.generateStudyPlan(
            options?.additionalParams?.examDate as string || '',
            content,
            options?.additionalParams?.hoursPerDay as number || 2
          )
          break
        default:
          throw new Error(`Unknown tool type: ${toolType}`)
      }
      
      // Cache result
      cache.value.set(cacheKey, result)
      
      // Add to history
      history.value.unshift({
        id: generationId,
        toolType,
        input: content.slice(0, 200),
        result,
        createdAt: new Date().toISOString()
      })
      
      // Persist
      persist()
      
      return result as T
    } catch (error) {
      lastError.value = error instanceof Error ? error.message : 'Generation failed'
      throw error
    } finally {
      activeGenerations.value.delete(generationId)
    }
  }
  
  function getFromHistory(toolType: ToolType): GenerationHistoryItem | undefined {
    return history.value.find(h => h.toolType === toolType)
  }
  
  function clearCache() {
    cache.value.clear()
  }
  
  function clearHistory() {
    history.value = []
    localStorage.removeItem('study-generation-history')
  }
  
  // Initialize
  hydrate()
  
  return {
    history,
    activeGenerations,
    isGenerating,
    generationCount,
    lastError,
    generate,
    getFromHistory,
    clearCache,
    clearHistory
  }
})
