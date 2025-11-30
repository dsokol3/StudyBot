<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Send, Loader2 } from 'lucide-vue-next'
import { chatApi } from '@/services/api'
import type { Message, ChatResponse, Citation } from '@/types'
import ChatLayout from '@/layouts/ChatLayout.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import CitationBadge from '@/components/CitationBadge.vue'
import CitationPanel from '@/components/CitationPanel.vue'
import { marked } from 'marked'

interface MessageWithCitations extends Message {
  citations?: Citation[]
}

const conversationId = ref<string>('')
const messages = ref<MessageWithCitations[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const chatLayoutRef = ref<InstanceType<typeof ChatLayout> | null>(null)
const selectedCitation = ref<Citation | null>(null)

const startNewChat = () => {
  if (messages.value.length > 0 && chatLayoutRef.value) {
    // Save current chat to history
    chatLayoutRef.value.chatHistory.unshift({
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
  // For now, just show it's selected
  conversationId.value = chatId
}

onMounted(() => {
  conversationId.value = generateId()
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
</script>

<template>
  <ChatLayout
    ref="chatLayoutRef"
    :can-clear-chat="messages.length > 0"
    :conversation-id="conversationId"
    @new-chat="startNewChat"
    @load-chat="loadChat"
    @clear-chat="clearChat"
  >
    <!-- Messages -->
    <div ref="messagesContainer" class="p-4 h-full">
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <div class="text-center space-y-2">
          <h2 class="text-xl font-semibold text-muted-foreground">Start a conversation</h2>
          <p class="text-sm text-muted-foreground">Type a message below to begin</p>
          <p class="text-xs text-muted-foreground/70 mt-4">
            💡 Tip: Upload documents via the sidebar to enhance AI responses with your own knowledge base
          </p>
        </div>
      </div>

      <div class="space-y-4 max-w-4xl mx-auto">
        <div
          v-for="message in messages"
          :key="message.id"
          :class="message.sender === 'user' ? 'ml-auto max-w-[80%]' : 'mr-auto max-w-[80%]'"
          class="relative p-0.5 rounded-xl bg-linear-to-r from-[#3F5EFB] to-[#FC466B]"
        >
          <Card class="relative bg-card">
            <CardContent class="p-3">
              <div 
                class="text-sm message-content prose prose-sm max-w-none dark:prose-invert" 
                v-html="renderMarkdown(message.content)"
              />
              
              <!-- Citation Badges (inline at end of message) -->
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
              
              <!-- Citation Panel (expandable details) -->
              <CitationPanel
                v-if="message.citations && message.citations.length > 0"
                :citations="message.citations"
              />
              
              <span class="text-xs text-muted-foreground mt-1 block">
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
    <template #footer>
      <div class="p-4">
        <form @submit.prevent="sendMessage" class="flex gap-2 max-w-4xl mx-auto">
          <Input
            v-model="inputMessage"
            placeholder="Type your message..."
            :disabled="isLoading"
          />
          <Button type="submit" :disabled="!inputMessage.trim() || isLoading">
            <Send class="w-4 h-4" />
          </Button>
        </form>
      </div>
    </template>
  </ChatLayout>
</template>
