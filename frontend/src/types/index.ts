// API Types
export interface ChatRequest {
  message: string
  conversationId: string
}

export interface ChatResponse {
  message: string
  conversationId: string
  timestamp: number
}

// UI Types
export interface Message {
  id: string
  content: string
  sender: 'user' | 'assistant'
  timestamp: number
}

export type MessageSender = 'user' | 'assistant'
