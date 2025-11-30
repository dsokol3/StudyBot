// ==========================================
// Study Guide Types
// ==========================================

// File Upload Types
export interface UploadedNote {
  id: string
  filename: string
  content: string
  uploadedAt: string
  size: number
  type: 'pdf' | 'txt' | 'md' | 'docx'
}

export interface UploadProgress {
  id: string
  filename: string
  progress: number
  status: 'pending' | 'uploading' | 'processing' | 'complete' | 'error'
  error?: string
}

// Tool Types
export type ToolType = 
  | 'summary' 
  | 'flashcards' 
  | 'questions' 
  | 'essay-prompts' 
  | 'explanations' 
  | 'diagrams' 
  | 'study-plan'

export interface ToolInfo {
  id: ToolType
  name: string
  description: string
  icon: string
  color: string
  route: string
}

// Generation Result Types
export interface SummaryResult {
  type: 'summary'
  summary: string
  keyPoints: string[]
  wordCount: number
}

export interface Flashcard {
  id: string
  front: string
  back: string
  difficulty: 'easy' | 'medium' | 'hard'
}

export interface FlashcardsResult {
  type: 'flashcards'
  cards: Flashcard[]
  totalCards: number
}

export interface Question {
  id: string
  question: string
  options: string[]
  correctAnswer: number
  explanation: string
}

export interface QuestionsResult {
  type: 'questions'
  questions: Question[]
  totalQuestions: number
}

export interface EssayPrompt {
  id: string
  prompt: string
  suggestedLength: string
  keyPointsToAddress: string[]
  difficulty: 'beginner' | 'intermediate' | 'advanced'
}

export interface EssayPromptsResult {
  type: 'essay-prompts'
  prompts: EssayPrompt[]
  totalPrompts: number
}

export interface Explanation {
  term: string
  definition: string
  example?: string
  relatedTerms?: string[]
}

export interface ExplanationsResult {
  type: 'explanations'
  explanations: Explanation[]
  simplifiedText: string
}

export interface DiagramNode {
  id: string
  label: string
  type: 'concept' | 'detail' | 'example'
}

export interface DiagramEdge {
  from: string
  to: string
  label?: string
}

export interface DiagramsResult {
  type: 'diagrams'
  mermaidCode: string
  nodes: DiagramNode[]
  edges: DiagramEdge[]
  description: string
}

export interface StudySession {
  id: string
  date: string
  topic: string
  duration: number // minutes
  activities: string[]
}

export interface StudyPlanResult {
  type: 'study-plan'
  sessions: StudySession[]
  totalHours: number
  examDate: string
  recommendations: string[]
}

// Union type for all generation results
export type GenerationResult = 
  | SummaryResult 
  | FlashcardsResult 
  | QuestionsResult 
  | EssayPromptsResult 
  | ExplanationsResult 
  | DiagramsResult 
  | StudyPlanResult

// Generation History
export interface GenerationHistoryItem {
  id: string
  toolType: ToolType
  input: string
  result: GenerationResult
  createdAt: string
}

// API Request Types
export interface GenerateRequest {
  content: string
  options?: Record<string, unknown>
}

export interface StudyPlanRequest {
  content: string
  examDate: string
  hoursPerDay?: number
}

// API Response wrapper
export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
}

// Export/Download Types
export type ExportFormat = 'pdf' | 'json' | 'markdown' | 'anki'

export interface ExportOptions {
  format: ExportFormat
  includeMetadata?: boolean
  filename?: string
}
