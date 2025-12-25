import axios from 'axios'
import type { ChatRequest, ChatResponse, Document, DocumentStatus, DocumentStatusResponse } from '@/types'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

export const chatApi = {
  sendMessage: async (request: ChatRequest): Promise<ChatResponse> => {
    const response = await apiClient.post<ChatResponse>('/chat/message', request)
    return response.data
  },

  clearConversation: async (conversationId: string): Promise<void> => {
    await apiClient.delete(`/chat/conversation/${conversationId}`)
  },

  healthCheck: async (): Promise<string> => {
    const response = await apiClient.get<string>('/chat/health')
    return response.data
  },
}

export const documentApi = {
  uploadDocument: async (
    file: File, 
    conversationId: string,
    onProgress?: (progress: number) => void
  ): Promise<Document> => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('conversationId', conversationId)
    
    const response = await apiClient.post<Document>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      },
    })
    return response.data
  },

  getDocumentStatus: async (documentId: string): Promise<DocumentStatusResponse> => {
    const response = await apiClient.get<DocumentStatusResponse>(`/documents/${documentId}/status`)
    return response.data
  },

  getDocument: async (documentId: string): Promise<Document> => {
    const response = await apiClient.get<Document>(`/documents/${documentId}`)
    return response.data
  },

  getDocumentsByConversation: async (conversationId: string): Promise<Document[]> => {
    const response = await apiClient.get<Document[]>(`/documents/conversation/${conversationId}`)
    return response.data
  },

  deleteDocument: async (documentId: string): Promise<void> => {
    await apiClient.delete(`/documents/${documentId}`)
  },
}

export type { ChatRequest, ChatResponse, Document, DocumentStatus, DocumentStatusResponse }
