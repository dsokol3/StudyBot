<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  BookOpen, 
  Layers, 
  HelpCircle, 
  FileText, 
  Lightbulb, 
  GitBranch, 
  Calendar,
  FileUp,
  Sparkles
} from 'lucide-vue-next'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useNotesStore } from '@/stores/notes'
import { useGenerationStore } from '@/stores/generation'

const router = useRouter()
const notesStore = useNotesStore()
const generationStore = useGenerationStore()

const tools = [
  {
    id: 'summary',
    name: 'Summary',
    description: 'Generate a concise summary of your study materials with key points',
    icon: BookOpen,
    color: 'from-blue-500 to-cyan-500',
    route: '/tools/summary'
  },
  {
    id: 'flashcards',
    name: 'Flashcards',
    description: 'Create interactive flashcards for effective memorization',
    icon: Layers,
    color: 'from-violet-500 to-purple-500',
    route: '/tools/flashcards'
  },
  {
    id: 'questions',
    name: 'Practice Questions',
    description: 'Generate multiple-choice questions to test your knowledge',
    icon: HelpCircle,
    color: 'from-green-500 to-emerald-500',
    route: '/tools/questions'
  },
  {
    id: 'essay-prompts',
    name: 'Essay Prompts',
    description: 'Get essay prompts with suggested points to address',
    icon: FileText,
    color: 'from-orange-500 to-amber-500',
    route: '/tools/essay-prompts'
  },
  {
    id: 'explanations',
    name: 'Explanations',
    description: 'Break down complex concepts into simple explanations',
    icon: Lightbulb,
    color: 'from-yellow-500 to-orange-500',
    route: '/tools/explanations'
  },
  {
    id: 'diagrams',
    name: 'Concept Diagrams',
    description: 'Visualize relationships between concepts with diagrams',
    icon: GitBranch,
    color: 'from-pink-500 to-rose-500',
    route: '/tools/diagrams'
  },
  {
    id: 'study-plan',
    name: 'Study Plan',
    description: 'Create a personalized study schedule leading up to your exam',
    icon: Calendar,
    color: 'from-indigo-500 to-blue-500',
    route: '/tools/study-plan'
  }
]

const uploadedFilesCount = computed(() => notesStore.notes.length)
const totalGenerations = computed(() => generationStore.generationCount)

const navigateToTool = (route: string) => {
  router.push(route)
}

const navigateToUpload = () => {
  router.push('/')
}

const hasHistoryForTool = (toolId: string) => {
  return generationStore.history.some(h => h.toolType === toolId)
}
</script>

<template>
  <StudyLayout>
    <div class="min-h-full p-6 bg-gradient-to-br from-background via-background to-muted/20">
      <div class="max-w-6xl mx-auto space-y-8">
        <!-- Header -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 class="text-3xl font-bold tracking-tight">Study Dashboard</h1>
            <p class="text-muted-foreground mt-1">
              Choose a study tool to generate learning materials from your notes
            </p>
          </div>
          
          <div class="flex items-center gap-3">
            <Badge variant="secondary" class="gap-1">
              <FileUp class="w-3 h-3" />
              {{ uploadedFilesCount }} {{ uploadedFilesCount === 1 ? 'file' : 'files' }} uploaded
            </Badge>
            <Badge v-if="totalGenerations > 0" variant="outline" class="gap-1">
              <Sparkles class="w-3 h-3" />
              {{ totalGenerations }} generations
            </Badge>
          </div>
        </div>
        
        <!-- Quick Stats -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Card>
            <CardContent class="p-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                  <FileText class="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p class="text-2xl font-bold">{{ uploadedFilesCount }}</p>
                  <p class="text-xs text-muted-foreground">Study Materials</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent class="p-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                  <Sparkles class="w-5 h-5 text-green-500" />
                </div>
                <div>
                  <p class="text-2xl font-bold">{{ totalGenerations }}</p>
                  <p class="text-xs text-muted-foreground">AI Generations</p>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <Card>
            <CardContent class="p-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-violet-500/10 flex items-center justify-center">
                  <Layers class="w-5 h-5 text-violet-500" />
                </div>
                <div>
                  <p class="text-2xl font-bold">7</p>
                  <p class="text-xs text-muted-foreground">Study Tools</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        
        <!-- Tools Grid -->
        <div>
          <h2 class="text-lg font-semibold mb-4">Study Tools</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            <Card 
              v-for="tool in tools" 
              :key="tool.id"
              class="group cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-[1.02] hover:border-primary/50"
              @click="navigateToTool(tool.route)"
            >
              <CardHeader class="pb-2">
                <div class="flex items-start justify-between">
                  <div 
                    :class="[
                      'w-10 h-10 rounded-lg flex items-center justify-center bg-gradient-to-br',
                      tool.color
                    ]"
                  >
                    <component :is="tool.icon" class="w-5 h-5 text-white" />
                  </div>
                  <Badge 
                    v-if="hasHistoryForTool(tool.id)" 
                    variant="outline" 
                    class="text-xs"
                  >
                    Used
                  </Badge>
                </div>
                <CardTitle class="text-base mt-3 group-hover:text-primary transition-colors">
                  {{ tool.name }}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <CardDescription class="text-sm line-clamp-2">
                  {{ tool.description }}
                </CardDescription>
              </CardContent>
            </Card>
          </div>
        </div>
        
        <!-- Upload More Section -->
        <Card class="border-dashed">
          <CardContent class="p-6">
            <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-muted flex items-center justify-center">
                  <FileUp class="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                  <p class="font-medium">Need to add more materials?</p>
                  <p class="text-sm text-muted-foreground">
                    Upload additional study notes to enhance your learning
                  </p>
                </div>
              </div>
              <Button variant="outline" @click="navigateToUpload">
                Upload More Files
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
