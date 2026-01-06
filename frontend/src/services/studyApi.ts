import axios from 'axios'
import type {
  UploadedNote,
  SummaryResult,
  FlashcardsResult,
  QuestionsResult,
  EssayPromptsResult,
  ExplanationsResult,
  DiagramsResult,
  DiagramType,
  StudyPlanResult
} from '@/types/study'

const api = axios.create({
  baseURL: '/api/study',
  timeout: 120000, // 2 minute timeout for AI generation
  headers: {
    'Content-Type': 'application/json'
  }
})

// Rate limiting helper
let lastRequestTime = 0
const MIN_REQUEST_INTERVAL = 2000 // 2 seconds between requests to be safe

async function throttledRequest<T>(requestFn: () => Promise<T>): Promise<T> {
  const now = Date.now()
  const timeSinceLastRequest = now - lastRequestTime
  
  if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
    await new Promise(resolve => 
      setTimeout(resolve, MIN_REQUEST_INTERVAL - timeSinceLastRequest)
    )
  }
  
  lastRequestTime = Date.now()
  return requestFn()
}

export const studyApi = {
  // File Upload
  async uploadNotes(file: File, conversationId?: string): Promise<UploadedNote> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('conversationId', conversationId || 'default')
    
    const response = await axios.post('/api/documents/upload', formData, {
      timeout: 120000,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    // Map backend response to frontend UploadedNote type
    const data = response.data
    const fileExtension = file.name.split('.').pop()?.toLowerCase() || 'txt'
    
    return {
      id: data.id,
      filename: data.filename || file.name,
      content: '', // Content will be fetched separately after processing
      contentType: data.contentType,
      fileSizeBytes: data.fileSizeBytes,
      status: data.status,
      chunkCount: data.chunkCount,
      errorMessage: data.errorMessage,
      createdAt: data.createdAt,
      processedAt: data.processedAt,
      uploadedAt: data.createdAt || new Date().toISOString(),
      size: data.fileSizeBytes || file.size,
      type: fileExtension as 'pdf' | 'txt' | 'md' | 'docx'
    }
  },
  
  // Get document status
  async getDocumentStatus(documentId: string): Promise<{ status: string; chunkCount: number }> {
    const response = await axios.get(`/api/documents/${documentId}/status`)
    return response.data
  },
  
  // Get document content after processing
  async getDocumentContent(documentId: string): Promise<string> {
    const response = await axios.get(`/api/documents/${documentId}/content`)
    return response.data.content || ''
  },
  
  // Get all document contents for study tools
  async getAllDocumentContents(conversationId: string = 'default'): Promise<string> {
    const response = await axios.get(`/api/documents/conversation/${conversationId}/content`)
    return response.data.content || ''
  },
  
  // Get all documents metadata for a conversation
  async getAllDocuments(conversationId: string = 'default'): Promise<UploadedNote[]> {
    try {
      const response = await axios.get(`/api/documents/conversation/${conversationId}`)
      return response.data.map((doc: any) => ({
        id: doc.id,
        filename: doc.filename,
        content: '', // Content needs to be fetched separately
        contentType: doc.contentType,
        fileSizeBytes: doc.fileSizeBytes,
        status: doc.status,
        chunkCount: doc.chunkCount,
        errorMessage: doc.errorMessage,
        createdAt: doc.createdAt,
        processedAt: doc.processedAt,
        uploadedAt: doc.createdAt || new Date().toISOString(),
        size: doc.fileSizeBytes,
        type: doc.contentType?.includes('pdf') ? 'pdf' : 
              doc.contentType?.includes('word') ? 'docx' : 
              doc.contentType?.includes('markdown') ? 'md' : 'txt'
      }))
    } catch (error) {
      console.error('Failed to fetch documents:', error)
      return []
    }
  },
  
  // Summary Generation
  async generateSummary(content: string): Promise<SummaryResult> {
    return throttledRequest(async () => {
      const response = await api.post<SummaryResult>('/generate/summary', {
        content
      })
      return { ...response.data, type: 'summary' as const }
    })
  },
  
  // Flashcards Generation
  async generateFlashcards(
    content: string, 
    count?: number,
    options?: { easyCount?: number; mediumCount?: number; hardCount?: number }
  ): Promise<FlashcardsResult> {
    return throttledRequest(async () => {
      const requestData: any = { content }
      
      // If difficulty distribution is provided, use it
      if (options?.easyCount !== undefined || options?.mediumCount !== undefined || options?.hardCount !== undefined) {
        requestData.easyCount = options.easyCount || 0
        requestData.mediumCount = options.mediumCount || 0
        requestData.hardCount = options.hardCount || 0
      } else {
        // Otherwise use total count
        requestData.count = count || 10
      }
      
      const response = await api.post<FlashcardsResult>('/generate/flashcards', requestData)
      return { ...response.data, type: 'flashcards' as const }
    })
  },
  
  // Questions Generation
  async generateQuestions(content: string, count?: number): Promise<QuestionsResult> {
    return throttledRequest(async () => {
      const response = await api.post<QuestionsResult>('/generate/questions', {
        content,
        count: count || 5
      })
      return { ...response.data, type: 'questions' as const }
    })
  },
  
  // Essay Prompts Generation
  async generateEssayPrompts(content: string, count?: number): Promise<EssayPromptsResult> {
    return throttledRequest(async () => {
      const response = await api.post<EssayPromptsResult>('/generate/essay-prompts', {
        content,
        count: count || 3
      })
      return { ...response.data, type: 'essay-prompts' as const }
    })
  },
  
  // Text Explanation
  async explainText(content: string): Promise<ExplanationsResult> {
    return throttledRequest(async () => {
      const response = await api.post<ExplanationsResult>('/generate/explain', {
        content
      })
      return { ...response.data, type: 'explanations' as const }
    })
  },
  
  // Diagram Generation
  async generateDiagram(content: string, diagramType: DiagramType = 'concept-map'): Promise<DiagramsResult> {
    return throttledRequest(async () => {
      const response = await api.post<DiagramsResult>('/generate/diagram', {
        content,
        diagramType
      })
      return { ...response.data, type: 'diagrams' as const, diagramType }
    })
  },
  
  // Study Plan Generation
  async generateStudyPlan(
    examDate: string, 
    content: string,
    hoursPerDay?: number
  ): Promise<StudyPlanResult> {
    return throttledRequest(async () => {
      const response = await api.post<StudyPlanResult>('/generate/study-plan', {
        content,
        examDate,
        hoursPerDay: hoursPerDay || 2
      })
      return { ...response.data, type: 'study-plan' as const }
    })
  },
  
  // Export functionality
  async exportResult(
    resultType: string, 
    result: unknown, 
    format: 'pdf' | 'json' | 'markdown' | 'anki'
  ): Promise<Blob> {
    const response = await api.post('/export', {
      resultType,
      result,
      format
    }, {
      responseType: 'blob'
    })
    
    return response.data
  }
}

// Error interceptor
api.interceptors.response.use(
  response => {
    // Log successful responses for debugging
    console.log('API Response:', response.config.url, response.data)
    return response
  },
  error => {
    console.error('API Error:', error.response?.status, error.response?.data, error.message)
    if (error.response?.status === 429) {
      // Rate limited
      console.warn('Rate limited. Please wait before making another request.')
    }
    return Promise.reject(error)
  }
)

export default studyApi
