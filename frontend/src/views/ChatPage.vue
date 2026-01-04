<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Send, Loader2, Plus, Trash2, FileText, X, MessageSquare } from 'lucide-vue-next'
import { chatApi } from '@/services/api'
import type { Message, ChatResponse, Citation } from '@/types'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import CitationBadge from '@/components/CitationBadge.vue'
import CitationPanel from '@/components/CitationPanel.vue'
import DocumentUpload from '@/components/DocumentUpload.vue'
import DocumentList from '@/components/DocumentList.vue'
import { marked } from 'marked'

interface MessageWithCitations extends Message {
  citations?: Citation[]
}

interface ChatHistoryItem {
  id: string
  title: string
  timestamp: number
}

const conversationId = ref<string>('')
const messages = ref<MessageWithCitations[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const selectedCitation = ref<Citation | null>(null)
const chatHistory = ref<ChatHistoryItem[]>([])
const showDocuments = ref(false)
const documentListRef = ref<InstanceType<typeof DocumentList> | null>(null)

const startNewChat = () => {
  if (messages.value.length > 0) {
    // Save current chat to history
    chatHistory.value.unshift({
      id: conversationId.value,
      title: messages.value[0]?.content.substring(0, 30) + '...' || 'New Chat',
      timestamp: Date.now()
    })
  }
  
  conversationId.value = generateId()
  messages.value = []
  error.value = ''
}

const loadChat = (chatId: string) => {
  // In a real app, you'd load messages from backend
  conversationId.value = chatId
}

onMounted(() => {
  // Use 'default' to access documents uploaded from the Upload page
  conversationId.value = 'default'
})

const generateId = (): string => {
  return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

const scrollToBottom = async (): Promise<void> => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async (): Promise<void> => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage: MessageWithCitations = {
    id: generateId(),
    content: inputMessage.value,
    sender: 'user',
    timestamp: Date.now(),
  }

  messages.value.push(userMessage)
  const messageText = inputMessage.value
  inputMessage.value = ''
  isLoading.value = true
  error.value = ''
  
  await scrollToBottom()

  try {
    const response: ChatResponse = await chatApi.sendMessage({
      message: messageText,
      conversationId: conversationId.value,
    })

    const assistantMessage: MessageWithCitations = {
      id: generateId(),
      content: response.content,
      sender: 'assistant',
      timestamp: response.timestamp,
      citations: response.citations,
    }

    messages.value.push(assistantMessage)
    await scrollToBottom()
  } catch (err) {
    error.value = 'Failed to send message. Please try again.'
    console.error('Error sending message:', err)
  } finally {
    isLoading.value = false
  }
}

const clearChat = async (): Promise<void> => {
  try {
    await chatApi.clearConversation(conversationId.value)
    messages.value = []
    conversationId.value = generateId()
    error.value = ''
  } catch (err) {
    error.value = 'Failed to clear conversation.'
    console.error('Error clearing conversation:', err)
  }
}

const formatTime = (timestamp: number): string => {
  return new Date(timestamp).toLocaleTimeString([], { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

const renderMarkdown = (content: string): string => {
  return marked(content, { 
    breaks: true,
    gfm: true 
  }) as string
}

const handleCitationClick = (citation: Citation) => {
  selectedCitation.value = citation
}

const handleUploadComplete = () => {
  documentListRef.value?.loadDocuments()
}
</script>

<template>
  <StudyLayout>
    <div class="flex h-full overflow-hidden">
      <!-- Chat History Sidebar (Right side, inside main content) -->
      <aside class="w-64 border-r bg-white/70 backdrop-blur-md shrink-0 hidden lg:flex flex-col h-full overflow-hidden">
        <div class="p-4 border-b bg-white/50">
          <h2 class="font-semibold text-sm flex items-center gap-2">
            <MessageSquare class="w-4 h-4" />
            Chat History
          </h2>
        </div>
        
        <ScrollArea class="flex-1">
          <div class="p-2 space-y-1">
            <Button 
              variant="outline" 
              size="sm" 
              class="w-full justify-start gap-2"
              @click="startNewChat"
            >
              <Plus class="w-4 h-4" />
              New Chat
            </Button>
            
            <Separator class="my-2" />
            
            <button 
              v-for="chat in chatHistory" 
              :key="chat.id"
              :class="[
                'w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-left transition-colors',
                conversationId === chat.id 
                  ? 'bg-primary text-primary-foreground' 
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              ]"
              @click="loadChat(chat.id)"
            >
              <MessageSquare class="w-3 h-3 shrink-0" />
              <span class="truncate">{{ chat.title }}</span>
            </button>
            
            <div v-if="chatHistory.length === 0" class="px-3 py-4 text-xs text-muted-foreground text-center">
              No previous chats
            </div>
          </div>
        </ScrollArea>
        
        <div class="p-2 border-t">
          <Button 
            variant="ghost" 
            size="sm" 
            class="w-full justify-start gap-2"
            @click="showDocuments = true"
          >
            <FileText class="w-4 h-4" />
            Knowledge Base
          </Button>
        </div>
      </aside>
      
      <!-- Main Chat Area -->
      <div class="flex-1 flex flex-col min-w-0 h-full overflow-hidden bg-white/60 backdrop-blur-sm">
        <!-- Chat Header -->
        <header class="border-b bg-white/70 backdrop-blur-sm px-4 py-3 flex items-center shrink-0">
          <div class="flex-1 text-center min-w-0">
            <h1 class="text-xl font-bold bg-gradient-to-r from-[#3F5EFB] to-[#FC466B] bg-clip-text text-transparent">
              AI ChatBot 
            </h1>
            <p class="text-xs text-muted-foreground">Powered by Groq</p>
          </div>
          <Button 
            variant="outline" 
            size="sm" 
            @click="clearChat" 
            :disabled="messages.length === 0"
          >
            <Trash2 class="w-4 h-4 mr-2" />
            Clear
          </Button>
        </header>

        <!-- Messages (Only this area scrolls) -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 min-h-0">
          <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
            <div class="text-center space-y-2">
              <h2 class="text-xl font-semibold text-muted-foreground">Start a conversation</h2>
              <p class="text-sm text-muted-foreground">Type a message below to begin</p>
              <p class="text-xs text-muted-foreground/70 mt-4">
                💡 Tip: Upload documents via the Knowledge Base to enhance AI responses
              </p>
            </div>
          </div>

          <div class="space-y-4 max-w-4xl mx-auto">
            <div
              v-for="message in messages"
              :key="message.id"
              :class="message.sender === 'user' ? 'ml-auto max-w-[80%]' : 'mr-auto max-w-[80%]'"
            >
              <Card :class="message.sender === 'user' ? 'bg-primary text-primary-foreground' : ''">
                <CardContent class="p-3">
                  <div 
                    class="text-sm message-content prose prose-sm max-w-none dark:prose-invert" 
                    v-html="renderMarkdown(message.content)"
                  />
                  
                  <!-- Citation Badges -->
                  <div 
                    v-if="message.citations && message.citations.length > 0" 
                    class="inline-flex items-center gap-0.5 mt-1"
                  >
                    <CitationBadge
                      v-for="(citation, index) in message.citations"
                      :key="`${citation.documentId}-${citation.chunkOrder}`"
                      :citation="citation"
                      :index="index"
                      @click="handleCitationClick"
                    />
                  </div>
                  
                  <!-- Citation Panel -->
                  <CitationPanel
                    v-if="message.citations && message.citations.length > 0"
                    :citations="message.citations"
                  />
                  
                  <span class="text-xs opacity-70 mt-1 block">
                    {{ formatTime(message.timestamp) }}
                  </span>
                </CardContent>
              </Card>
            </div>

            <div v-if="isLoading" class="mr-auto max-w-[80%]">
              <Card>
                <CardContent class="p-3">
                  <Loader2 class="w-4 h-4 animate-spin" />
                </CardContent>
              </Card>
            </div>

            <Card v-if="error" class="border-destructive">
              <CardContent class="p-3 text-sm text-destructive">
                {{ error }}
              </CardContent>
            </Card>
          </div>
        </div>

        <!-- Input Footer -->
        <footer class="border-t bg-white/70 backdrop-blur-sm p-4 shrink-0">
          <form @submit.prevent="sendMessage" class="flex gap-2 max-w-4xl mx-auto">
            <Input
              v-model="inputMessage"
              placeholder="Type your message..."
              :disabled="isLoading"
              class="flex-1"
            />
            <Button type="submit" :disabled="!inputMessage.trim() || isLoading">
              <Send class="w-4 h-4" />
            </Button>
          </form>
        </footer>
      </div>
      
      <!-- Document Management Panel (Slide-over) -->
      <div 
        v-if="showDocuments" 
        class="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm"
        @click.self="showDocuments = false"
      >
        <div class="absolute right-0 top-0 h-full w-full max-w-md bg-card border-l shadow-xl overflow-y-auto">
          <div class="p-4 border-b sticky top-0 bg-card z-10">
            <div class="flex items-center justify-between">
              <h2 class="text-lg font-semibold flex items-center gap-2">
                <FileText class="w-5 h-5" />
                Knowledge Base
              </h2>
              <Button variant="ghost" size="icon" @click="showDocuments = false">
                <X class="w-4 h-4" />
              </Button>
            </div>
            <p class="text-sm text-muted-foreground mt-1">
              Upload documents to enhance AI responses with relevant context
            </p>
          </div>
          
          <div class="p-4 space-y-6">
            <DocumentUpload 
              :conversation-id="conversationId" 
              @upload-complete="handleUploadComplete"
            />
            
            <Separator />
            
            <DocumentList 
              ref="documentListRef"
              :conversation-id="conversationId" 
            />
          </div>
        </div>
      </div>
    </div>
  </StudyLayout>
</template>
