<script setup lang="ts">
import { ref } from 'vue'
import { Send, Trash2, FileText, Upload } from 'lucide-vue-next'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import DocumentUpload from '@/components/DocumentUpload.vue'
import DocumentList from '@/components/DocumentList.vue'

interface ChatHistory {
  id: string
  title: string
  timestamp: number
}

defineProps<{
  canClearChat: boolean
  conversationId: string
}>()

const chatHistory = ref<ChatHistory[]>([])
const showDocuments = ref(false)
const documentListRef = ref<InstanceType<typeof DocumentList> | null>(null)

const emit = defineEmits<{
  newChat: []
  loadChat: [chatId: string]
  clearChat: []
}>()

const startNewChat = () => {
  emit('newChat')
}

const loadChat = (chatId: string) => {
  emit('loadChat', chatId)
}

const clearChat = () => {
  emit('clearChat')
}

const toggleDocuments = () => {
  showDocuments.value = !showDocuments.value
}

const handleUploadComplete = () => {
  // Refresh the document list when upload completes
  documentListRef.value?.loadDocuments()
}

defineExpose({
  chatHistory,
})
</script>

<template>
  <SidebarProvider>
    <Sidebar collapsible="icon">
      <SidebarContent>
        <!-- Chat History Section -->
        <SidebarGroup>
          <SidebarGroupLabel>Chat History</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton @click="startNewChat">
                  <Send class="w-4 h-4" />
                  <span>New Chat</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
              
              <SidebarMenuItem v-for="chat in chatHistory" :key="chat.id">
                <SidebarMenuButton @click="loadChat(chat.id)">
                  <span class="truncate">{{ chat.title }}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <Separator class="my-2" />

        <!-- Documents Section -->
        <SidebarGroup>
          <SidebarGroupLabel>
            <div class="flex items-center gap-2">
              <FileText class="w-4 h-4" />
              <span>Knowledge Base</span>
            </div>
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton @click="toggleDocuments">
                  <Upload class="w-4 h-4" />
                  <span>Manage Documents</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>

    <SidebarInset>
      <div class="flex flex-col h-screen w-full">
        <!-- Header -->
        <header class="border-b bg-card px-4 py-3 flex items-center shrink-0">
          <div class="flex items-center gap-2 min-w-0">
            <SidebarTrigger />
          </div>
          <div class="flex-1 text-center min-w-0">
            <h1 class="text-xl font-bold bg-linear-to-r from-[#3F5EFB] to-[#FC466B] bg-clip-text text-transparent">
              AI ChatBot 
            </h1>
            <p class="text-xs text-muted-foreground font-lato">Made by Devora Sokol</p>
            <p class="text-xs text-muted-foreground font-medium">⚡ Powered by Groq + Local Embeddings</p>
          </div>
          <div class="flex items-center gap-2 min-w-0">
            <div class="relative p-0.5 rounded-md bg-linear-to-r from-[#3F5EFB] to-[#FC466B]">
              <Button 
                variant="outline" 
                size="sm" 
                @click="clearChat" 
                :disabled="!canClearChat"
                class="bg-card"
              >
                <Trash2 class="w-4 h-4 mr-2" />
                Clear Chat
              </Button>
            </div>
          </div>
        </header>

        <!-- Document Management Panel (Slide-over) -->
        <div 
          v-if="showDocuments" 
          class="absolute inset-0 z-50 bg-background/80 backdrop-blur-sm"
          @click.self="showDocuments = false"
        >
          <div class="absolute right-0 top-0 h-full w-full max-w-md bg-card border-l shadow-xl overflow-y-auto">
            <div class="p-4 border-b sticky top-0 bg-card z-10">
              <div class="flex items-center justify-between">
                <h2 class="text-lg font-semibold flex items-center gap-2">
                  <FileText class="w-5 h-5" />
                  Knowledge Base
                </h2>
                <Button variant="ghost" size="sm" @click="showDocuments = false">
                  ✕
                </Button>
              </div>
              <p class="text-sm text-muted-foreground mt-1">
                Upload documents to enhance AI responses with relevant context
              </p>
            </div>
            
            <div class="p-4 space-y-6">
              <!-- Upload Section -->
              <DocumentUpload 
                :conversation-id="conversationId" 
                @upload-complete="handleUploadComplete"
              />
              
              <Separator />
              
              <!-- Document List Section -->
              <DocumentList 
                ref="documentListRef"
                :conversation-id="conversationId" 
              />
            </div>
          </div>
        </div>

        <!-- Main Content Slot -->
        <main class="flex-1 overflow-y-auto">
          <slot />
        </main>

        <!-- Footer Slot -->
        <footer class="border-t shrink-0">
          <slot name="footer" />
        </footer>
      </div>
    </SidebarInset>
  </SidebarProvider>
</template>
