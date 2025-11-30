<script setup lang="ts">
import { ref } from 'vue'
import { Lightbulb, Loader2, Download, RefreshCw, Copy, Check, BookOpen, Link } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { ExplanationsResult } from '@/types/study'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<ExplanationsResult>('explanations')

const useFullNotes = ref(true)
const copied = ref(false)
const activeTab = ref('simplified')

const handleGenerate = async () => {
  const content = useFullNotes.value ? notesContent.value : selectedContent.value
  await generate(content, { force: true })
}

const copyToClipboard = async () => {
  if (!result.value) return
  
  let text = '# Explanations\n\n## Simplified Text\n\n'
  text += result.value.simplifiedText + '\n\n'
  text += '## Term Definitions\n\n'
  result.value.explanations.forEach(exp => {
    text += `### ${exp.term}\n\n${exp.definition}\n\n`
    if (exp.example) text += `**Example:** ${exp.example}\n\n`
  })
  
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

const downloadAsMarkdown = () => {
  if (!result.value) return
  
  let text = '# Explanations\n\n## Simplified Text\n\n'
  text += result.value.simplifiedText + '\n\n'
  text += '## Term Definitions\n\n'
  result.value.explanations.forEach(exp => {
    text += `### ${exp.term}\n\n${exp.definition}\n\n`
    if (exp.example) text += `**Example:** ${exp.example}\n\n`
    if (exp.relatedTerms?.length) {
      text += `**Related:** ${exp.relatedTerms.join(', ')}\n\n`
    }
  })
  
  const blob = new Blob([text], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'explanations.md'
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
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-yellow-500 to-orange-500 flex items-center justify-center">
            <Lightbulb class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Explanations</h1>
            <p class="text-muted-foreground">Break down complex concepts into simple explanations</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Explain Content</CardTitle>
            <CardDescription>
              Get simplified explanations and key term definitions
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
              placeholder="Paste or type complex content that needs explaining..."
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
                  Generate Explanations
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
              
              <Button 
                v-if="result" 
                variant="ghost"
                size="icon"
                @click="copyToClipboard"
              >
                <Check v-if="copied" class="w-4 h-4 text-green-500" />
                <Copy v-else class="w-4 h-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <Card v-if="isLoading">
          <CardContent class="p-6 space-y-4">
            <Skeleton class="h-6 w-48" />
            <Skeleton class="h-4 w-full" />
            <Skeleton class="h-4 w-full" />
            <Skeleton class="h-4 w-3/4" />
            <div class="pt-4 space-y-3">
              <Skeleton class="h-24 w-full rounded-lg" />
              <Skeleton class="h-24 w-full rounded-lg" />
            </div>
          </CardContent>
        </Card>
        
        <!-- Results -->
        <Tabs v-else-if="result" v-model="activeTab" class="w-full">
          <TabsList class="grid w-full grid-cols-2">
            <TabsTrigger value="simplified">Simplified Text</TabsTrigger>
            <TabsTrigger value="terms">
              Key Terms
              <Badge variant="secondary" class="ml-2">{{ result.explanations.length }}</Badge>
            </TabsTrigger>
          </TabsList>
          
          <!-- Simplified Text Tab -->
          <TabsContent value="simplified">
            <Card>
              <CardHeader>
                <div class="flex items-center gap-2">
                  <BookOpen class="w-4 h-4 text-primary" />
                  <CardTitle class="text-lg">Simplified Explanation</CardTitle>
                </div>
                <CardDescription>
                  Your content explained in simpler terms
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div class="prose prose-sm max-w-none dark:prose-invert">
                  <p class="text-base leading-relaxed whitespace-pre-wrap">
                    {{ result.simplifiedText }}
                  </p>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
          
          <!-- Key Terms Tab -->
          <TabsContent value="terms">
            <div class="space-y-4">
              <Card 
                v-for="explanation in result.explanations" 
                :key="explanation.term"
              >
                <CardHeader class="pb-2">
                  <CardTitle class="text-base flex items-center gap-2">
                    <span class="w-2 h-2 rounded-full bg-primary" />
                    {{ explanation.term }}
                  </CardTitle>
                </CardHeader>
                <CardContent class="space-y-3">
                  <p class="text-sm">{{ explanation.definition }}</p>
                  
                  <div v-if="explanation.example" class="p-3 rounded-lg bg-primary/5 border border-primary/10">
                    <p class="text-xs font-medium text-primary mb-1">Example</p>
                    <p class="text-sm">{{ explanation.example }}</p>
                  </div>
                  
                  <div v-if="explanation.relatedTerms?.length" class="flex items-center gap-2 flex-wrap">
                    <Link class="w-3 h-3 text-muted-foreground" />
                    <span class="text-xs text-muted-foreground">Related:</span>
                    <Badge 
                      v-for="term in explanation.relatedTerms" 
                      :key="term"
                      variant="outline"
                      class="text-xs"
                    >
                      {{ term }}
                    </Badge>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <Lightbulb class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Explanations Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Explanations" to simplify complex content
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
