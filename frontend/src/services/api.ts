import axios from 'axios'
import type { ChatRequest, ChatResponse } from '@/types'

const API_BASE_URL = 'http://localhost:8080/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
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

export type { ChatRequest, ChatResponse }
