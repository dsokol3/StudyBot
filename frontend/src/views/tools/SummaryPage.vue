<script setup lang="ts">
import { ref } from 'vue'
import { BookOpen, Loader2, Download, RefreshCw, Copy, Check, List } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { SummaryResult } from '@/types/study'

const { 
  isLoading, 
  isLoadingContent,
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<SummaryResult>('summary')

const useFullNotes = ref(true)
const copied = ref(false)

const handleGenerate = async () => {
  // When using full notes, pass undefined to let generate() fetch from backend if needed
  const content = useFullNotes.value ? undefined : selectedContent.value
  await generate(content, { force: true })
}

const copyToClipboard = async () => {
  if (!result.value) return
  
  const text = `# Summary\n\n${result.value.summary}\n\n## Key Points\n\n${result.value.keyPoints.map(p => `- ${p}`).join('\n')}`
  
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

const downloadAsMarkdown = () => {
  if (!result.value) return
  
  const text = `# Summary\n\n${result.value.summary}\n\n## Key Points\n\n${result.value.keyPoints.map(p => `- ${p}`).join('\n')}`
  
  const blob = new Blob([text], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'study-summary.md'
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
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center">
            <BookOpen class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Generate Summary</h1>
            <p class="text-muted-foreground">Create a concise summary with key points from your notes</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Content to Summarize</CardTitle>
            <CardDescription>
              Use all your uploaded notes or paste specific content below
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
              placeholder="Paste or type the content you want to summarize..."
              class="min-h-[200px] resize-y"
            />
            
            <div v-else class="p-4 rounded-lg bg-muted/50 text-sm text-muted-foreground">
              <template v-if="isLoadingContent">
                <Loader2 class="w-4 h-4 inline animate-spin mr-2" />
                Loading document content...
              </template>
              <template v-else-if="notesContent && notesContent.trim()">
                Will use content from {{ notesContent.split('\n').length }} lines of uploaded notes
              </template>
              <template v-else>
                No documents uploaded yet. Upload files on the Upload page or paste content below.
              </template>
            </div>
            
            <!-- Error Alert -->
            <Alert v-if="error" variant="destructive">
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
            <Button 
              :disabled="isLoading" 
              @click="handleGenerate"
              class="w-full sm:w-auto"
            >
              <template v-if="isLoading">
                <Loader2 class="w-4 h-4 mr-2 animate-spin" />
                Generating Summary...
              </template>
              <template v-else>
                <RefreshCw class="w-4 h-4 mr-2" />
                Generate Summary
              </template>
            </Button>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <Card v-if="isLoading">
          <CardHeader>
            <Skeleton class="h-6 w-32" />
          </CardHeader>
          <CardContent class="space-y-4">
            <Skeleton class="h-4 w-full" />
            <Skeleton class="h-4 w-full" />
            <Skeleton class="h-4 w-3/4" />
            <Skeleton class="h-4 w-5/6" />
            <div class="pt-4 space-y-2">
              <Skeleton class="h-4 w-24" />
              <Skeleton class="h-3 w-full" />
              <Skeleton class="h-3 w-full" />
              <Skeleton class="h-3 w-2/3" />
            </div>
          </CardContent>
        </Card>
        
        <!-- Result Section -->
        <Card v-else-if="result">
          <CardHeader>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <CardTitle class="text-lg">Summary</CardTitle>
                <Badge variant="secondary">
                  {{ result.wordCount }} words
                </Badge>
              </div>
              <div class="flex items-center gap-2">
                <Button variant="ghost" size="icon" @click="copyToClipboard">
                  <Check v-if="copied" class="w-4 h-4 text-green-500" />
                  <Copy v-else class="w-4 h-4" />
                </Button>
                <Button variant="ghost" size="icon" @click="downloadAsMarkdown">
                  <Download class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent class="space-y-6">
            <!-- Main Summary -->
            <div class="prose prose-sm max-w-none dark:prose-invert">
              <p class="text-base leading-relaxed">{{ result.summary }}</p>
            </div>
            
            <!-- Key Points -->
            <div class="space-y-3">
              <div class="flex items-center gap-2">
                <List class="w-4 h-4 text-primary" />
                <h3 class="font-semibold">Key Points</h3>
              </div>
              <ul class="space-y-2">
                <li 
                  v-for="(point, index) in result.keyPoints" 
                  :key="index"
                  class="flex items-start gap-3 p-3 rounded-lg bg-muted/50"
                >
                  <span class="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary text-sm font-medium flex items-center justify-center">
                    {{ index + 1 }}
                  </span>
                  <span class="text-sm">{{ point }}</span>
                </li>
              </ul>
            </div>
          </CardContent>
        </Card>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <BookOpen class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Summary Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Summary" to create a summary of your study materials
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
