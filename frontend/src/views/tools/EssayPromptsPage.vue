<script setup lang="ts">
import { ref } from 'vue'
import { FileText, Loader2, Download, RefreshCw, Copy, Check, ChevronDown } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { EssayPromptsResult } from '@/types/study'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  generate
} = useStudyTool<EssayPromptsResult>('essay-prompts')

const useFullNotes = ref(true)
const expandedPrompts = ref<Set<string>>(new Set())
const copiedId = ref<string | null>(null)

const handleGenerate = async () => {
  // When using full notes, pass undefined to let generate() fetch from backend if needed
  const content = useFullNotes.value ? undefined : selectedContent.value
  await generate(content, { force: true })
  expandedPrompts.value.clear()
}

const togglePrompt = (id: string) => {
  if (expandedPrompts.value.has(id)) {
    expandedPrompts.value.delete(id)
  } else {
    expandedPrompts.value.add(id)
  }
}

const copyPrompt = async (prompt: string, id: string) => {
  await navigator.clipboard.writeText(prompt)
  copiedId.value = id
  setTimeout(() => { copiedId.value = null }, 2000)
}

const getDifficultyColor = (difficulty: string) => {
  switch (difficulty) {
    case 'beginner': return 'bg-green-500/10 text-green-600'
    case 'intermediate': return 'bg-yellow-500/10 text-yellow-600'
    case 'advanced': return 'bg-red-500/10 text-red-600'
    default: return 'bg-muted text-muted-foreground'
  }
}

const downloadAsMarkdown = () => {
  if (!result.value) return
  
  let text = '# Essay Prompts\n\n'
  result.value.prompts.forEach((prompt, index) => {
    text += `## Prompt ${index + 1}: ${prompt.difficulty}\n\n`
    text += `${prompt.prompt}\n\n`
    text += `**Suggested Length:** ${prompt.suggestedLength}\n\n`
    text += `**Key Points to Address:**\n${prompt.keyPointsToAddress.map(p => `- ${p}`).join('\n')}\n\n---\n\n`
  })
  
  const blob = new Blob([text], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'essay-prompts.md'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <StudyLayout>
    <div class="min-h-full p-6">
      <div class="max-w-4xl mx-auto space-y-6">
        <!-- Header -->
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-500 to-amber-500 flex items-center justify-center">
            <FileText class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Essay Prompts</h1>
            <p class="text-muted-foreground">Generate essay prompts with suggested points to address</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Generate Essay Prompts</CardTitle>
            <CardDescription>
              Create essay topics based on your study materials
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex items-center gap-4">
              <Button 
                :variant="useFullNotes ? 'default' : 'outline'"
                size="sm"
                @click="useFullNotes = true"
              >
                Use All Notes
              </Button>
              <Button 
                :variant="!useFullNotes ? 'default' : 'outline'"
                size="sm"
                @click="useFullNotes = false"
              >
                Custom Content
              </Button>
            </div>
            
            <Textarea 
              v-if="!useFullNotes"
              v-model="selectedContent"
              placeholder="Paste or type the content to create essay prompts from..."
              class="min-h-[150px] resize-y"
            />
            
            <Alert v-if="error" variant="destructive">
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
            <div class="flex items-center gap-2">
              <Button 
                :disabled="isLoading" 
                @click="handleGenerate"
              >
                <template v-if="isLoading">
                  <Loader2 class="w-4 h-4 mr-2 animate-spin" />
                  Generating...
                </template>
                <template v-else>
                  <RefreshCw class="w-4 h-4 mr-2" />
                  Generate Prompts
                </template>
              </Button>
              
              <Button 
                v-if="result" 
                variant="outline"
                @click="downloadAsMarkdown"
              >
                <Download class="w-4 h-4 mr-2" />
                Export
              </Button>
            </div>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <div v-if="isLoading" class="space-y-4">
          <Card v-for="i in 3" :key="i">
            <CardHeader>
              <div class="flex items-center justify-between">
                <Skeleton class="h-5 w-24" />
                <Skeleton class="h-5 w-16" />
              </div>
            </CardHeader>
            <CardContent class="space-y-3">
              <Skeleton class="h-4 w-full" />
              <Skeleton class="h-4 w-5/6" />
              <Skeleton class="h-4 w-4/6" />
            </CardContent>
          </Card>
        </div>
        
        <!-- Results -->
        <div v-else-if="result && result.prompts.length > 0" class="space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold">Generated Prompts</h2>
            <Badge variant="secondary">{{ result.prompts.length }} prompts</Badge>
          </div>
          
          <div class="space-y-4">
            <Card 
              v-for="(prompt, index) in result.prompts" 
              :key="prompt.id"
              class="overflow-hidden"
            >
              <CardHeader class="pb-3">
                <div class="flex items-start justify-between gap-4">
                  <div class="space-y-1">
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-medium text-muted-foreground">
                        Prompt {{ index + 1 }}
                      </span>
                      <Badge :class="getDifficultyColor(prompt.difficulty)">
                        {{ prompt.difficulty }}
                      </Badge>
                    </div>
                    <CardTitle class="text-base leading-relaxed">
                      {{ prompt.prompt }}
                    </CardTitle>
                  </div>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    @click="copyPrompt(prompt.prompt, prompt.id)"
                  >
                    <Check v-if="copiedId === prompt.id" class="w-4 h-4 text-green-500" />
                    <Copy v-else class="w-4 h-4" />
                  </Button>
                </div>
              </CardHeader>
              
              <CardContent class="pt-0">
                <div class="text-sm text-muted-foreground mb-3">
                  <span class="font-medium">Suggested Length:</span> {{ prompt.suggestedLength }}
                </div>
                
                <Collapsible>
                  <CollapsibleTrigger asChild>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      class="w-full justify-between px-0 hover:bg-transparent"
                      @click="togglePrompt(prompt.id)"
                    >
                      <span class="text-sm font-medium">Key Points to Address</span>
                      <ChevronDown :class="[
                        'w-4 h-4 transition-transform',
                        expandedPrompts.has(prompt.id) && 'rotate-180'
                      ]" />
                    </Button>
                  </CollapsibleTrigger>
                  <CollapsibleContent>
                    <ul class="mt-2 space-y-2">
                      <li 
                        v-for="(point, pIndex) in prompt.keyPointsToAddress" 
                        :key="pIndex"
                        class="flex items-start gap-2 text-sm"
                      >
                        <span class="flex-shrink-0 w-5 h-5 rounded-full bg-primary/10 text-primary text-xs font-medium flex items-center justify-center mt-0.5">
                          {{ pIndex + 1 }}
                        </span>
                        <span>{{ point }}</span>
                      </li>
                    </ul>
                  </CollapsibleContent>
                </Collapsible>
              </CardContent>
            </Card>
          </div>
        </div>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <FileText class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Essay Prompts Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Prompts" to create essay topics from your materials
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
