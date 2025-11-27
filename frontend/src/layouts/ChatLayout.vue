<script setup lang="ts">
import { ref } from 'vue'
import { Send, Trash2 } from 'lucide-vue-next'
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

interface ChatHistory {
  id: string
  title: string
  timestamp: number
}

const chatHistory = ref<ChatHistory[]>([])

const emit = defineEmits<{
  newChat: []
  loadChat: [chatId: string]
  clearChat: []
}>()

defineProps<{
  canClearChat: boolean
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

defineExpose({
  chatHistory,
})
</script>

<template>
  <SidebarProvider>
    <Sidebar collapsible="icon">
      <SidebarContent>
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
            <p class="text-xs text-muted-foreground font-medium">Powered by Ollama</p>
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
