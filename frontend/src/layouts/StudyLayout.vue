<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Home, 
  Upload, 
  BookOpen, 
  Layers, 
  HelpCircle, 
  FileText, 
  Lightbulb, 
  GitBranch, 
  Calendar,
  ArrowLeft,
  MessageSquare
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useNotesStore } from '@/stores/notes'

const router = useRouter()
const route = useRoute()
const notesStore = useNotesStore()

const tools = [
  { id: 'summary', name: 'Summary', icon: BookOpen, route: '/tools/summary' },
  { id: 'flashcards', name: 'Flashcards', icon: Layers, route: '/tools/flashcards' },
  { id: 'questions', name: 'Questions', icon: HelpCircle, route: '/tools/questions' },
  { id: 'essay-prompts', name: 'Essay Prompts', icon: FileText, route: '/tools/essay-prompts' },
  { id: 'explanations', name: 'Explanations', icon: Lightbulb, route: '/tools/explanations' },
  { id: 'diagrams', name: 'Diagrams', icon: GitBranch, route: '/tools/diagrams' },
  { id: 'study-plan', name: 'Study Plan', icon: Calendar, route: '/tools/study-plan' },
]

const currentRoute = computed(() => route.path)

const navigateTo = (path: string) => {
  router.push(path)
}

const goBack = () => {
  router.back()
}

const isActive = (path: string) => currentRoute.value === path
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <!-- Sidebar -->
    <aside class="w-64 border-r bg-white/80 backdrop-blur-md shrink-0 hidden md:flex flex-col h-full">
      <!-- Header -->
      <div class="p-4 border-b">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-fuchsia-500 flex items-center justify-center">
            <BookOpen class="w-4 h-4 text-white" />
          </div>
          <span class="font-semibold text-lg">AI Study Guide</span>
        </div>
      </div>
      
      <!-- Navigation -->
      <ScrollArea class="flex-1">
        <nav class="p-2 space-y-1">
          <!-- Main Navigation -->
          <button 
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              isActive('/') 
                ? 'bg-primary text-primary-foreground' 
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            ]"
            @click="navigateTo('/')"
          >
            <Upload class="w-4 h-4" />
            <span>Upload Notes</span>
          </button>
          
          <button 
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              isActive('/dashboard') 
                ? 'bg-primary text-primary-foreground' 
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            ]"
            @click="navigateTo('/dashboard')"
          >
            <Home class="w-4 h-4" />
            <span>Dashboard</span>
          </button>
          
          <Separator class="my-3" />
          
          <!-- Study Tools -->
          <div class="px-3 py-2">
            <span class="text-xs font-medium text-muted-foreground uppercase tracking-wider">
              Study Tools
            </span>
          </div>
          
          <button 
            v-for="tool in tools" 
            :key="tool.id"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              isActive(tool.route) 
                ? 'bg-primary text-primary-foreground' 
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            ]"
            @click="navigateTo(tool.route)"
          >
            <component :is="tool.icon" class="w-4 h-4" />
            <span>{{ tool.name }}</span>
          </button>
          
          <Separator class="my-3" />
          
          <!-- Chat Link -->
          <button 
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              isActive('/chat') 
                ? 'bg-primary text-primary-foreground' 
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            ]"
            @click="navigateTo('/chat')"
          >
            <MessageSquare class="w-4 h-4" />
            <span>AI Chat</span>
          </button>
        </nav>
      </ScrollArea>
    </aside>
    
    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0 h-full overflow-hidden bg-white/60 backdrop-blur-sm">
      <!-- Header with back button for tool pages -->
      <header 
        v-if="route.path.startsWith('/tools/')" 
        class="flex items-center gap-2 p-4 border-b bg-white/80 backdrop-blur shrink-0"
      >
        <Button variant="ghost" size="icon" @click="goBack">
          <ArrowLeft class="w-4 h-4" />
        </Button>
        <span class="text-sm text-muted-foreground">Back to Dashboard</span>
      </header>
      
      <!-- Page Content -->
      <main class="flex-1 overflow-hidden">
        <slot />
      </main>
    </div>
  </div>
</template>
