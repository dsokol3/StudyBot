<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Send, Trash2, Loader2 } from 'lucide-vue-next'
import { chatApi } from '@/services/api'
import type { Message, ChatResponse } from '@/types'

const conversationId = ref<string>('')
const messages = ref<Message[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

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

  const userMessage: Message = {
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

    const assistantMessage: Message = {
      id: generateId(),
      content: response.message,
      sender: 'assistant',
      timestamp: response.timestamp,
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
</script>

<template>
  <div class="flex flex-col h-screen bg-background">
    <!-- Header -->
    <header class="border-b bg-card px-6 py-4">
      <div class="max-w-4xl mx-auto flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold text-foreground">AI ChatBot</h1>
          <p class="text-sm text-muted-foreground">Powered by Java & Vue</p>
        </div>
        <button
          @click="clearChat"
          class="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-destructive text-destructive-foreground hover:bg-destructive/90 h-10 px-4 py-2"
          :disabled="messages.length === 0"
        >
          <Trash2 class="w-4 h-4 mr-2" />
          Clear Chat
        </button>
      </div>
    </header>

    <!-- Messages Area -->
    <main ref="messagesContainer" class="flex-1 overflow-y-auto px-6 py-6">
      <div class="max-w-4xl mx-auto space-y-4">
        <div
          v-if="messages.length === 0"
          class="flex items-center justify-center h-full text-center"
        >
          <div class="space-y-2">
            <h2 class="text-xl font-semibold text-muted-foreground">
              Start a conversation
            </h2>
            <p class="text-sm text-muted-foreground">
              Type a message below to begin chatting with the AI assistant
            </p>
          </div>
        </div>

        <div
          v-for="message in messages"
          :key="message.id"
          :class="[
            'flex',
            message.sender === 'user' ? 'justify-end' : 'justify-start',
          ]"
        >
          <div
            :class="[
              'max-w-[80%] rounded-lg px-4 py-2',
              message.sender === 'user'
                ? 'bg-primary text-primary-foreground'
                : 'bg-muted text-muted-foreground',
            ]"
          >
            <p class="text-sm">{{ message.content }}</p>
            <span class="text-xs opacity-70 mt-1 block">
              {{ formatTime(message.timestamp) }}
            </span>
          </div>
        </div>

        <div v-if="isLoading" class="flex justify-start">
          <div class="bg-muted text-muted-foreground max-w-[80%] rounded-lg px-4 py-2">
            <Loader2 class="w-4 h-4 animate-spin" />
          </div>
        </div>

        <div v-if="error" class="bg-destructive/10 text-destructive rounded-lg px-4 py-2 text-sm">
          {{ error }}
        </div>
      </div>
    </main>

    <!-- Input Area -->
    <footer class="border-t bg-card px-6 py-4">
      <form @submit.prevent="sendMessage" class="max-w-4xl mx-auto">
        <div class="flex gap-2">
          <input
            v-model="inputMessage"
            type="text"
            placeholder="Type your message..."
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="isLoading"
          />
          <button
            type="submit"
            class="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-4 py-2"
            :disabled="!inputMessage.trim() || isLoading"
          >
            <Send class="w-4 h-4" />
          </button>
        </div>
      </form>
    </footer>
  </div>
</template>
