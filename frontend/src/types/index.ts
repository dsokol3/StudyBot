// API Types
export interface ChatRequest {
  message: string
  conversationId: string
}

export interface Citation {
  index: number
  documentId: string
  documentName: string
  chunkOrder: number
  // Extended fields for UI display (populated on frontend)
  content?: string
  similarity?: number
}

export interface ChatResponse {
  id: string
  content: string
  sender: string
  timestamp: number
  conversationId: string
  citations: Citation[]
}

// Document Types
export interface Document {
  id: string
  filename: string
  contentType: string
  fileSizeBytes: number
  status: DocumentStatus
  chunkCount: number
  errorMessage?: string
  createdAt: string
  processedAt?: string
}

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

export interface DocumentStatusResponse {
  id: string
  status: DocumentStatus
  chunkCount: number
  errorMessage?: string
}

// UI Types
export interface Message {
  id: string
  content: string
  sender: 'user' | 'assistant'
  timestamp: number
  citations?: Citation[]
}

export type MessageSender = 'user' | 'assistant'
