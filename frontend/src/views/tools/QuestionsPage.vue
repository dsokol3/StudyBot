<script setup lang="ts">
import { ref, computed } from 'vue'
import { HelpCircle, Loader2, Download, RefreshCw, Check, X, ChevronDown } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { QuestionsResult } from '@/types/study'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<QuestionsResult>('questions')

const useFullNotes = ref(true)
const answers = ref<Map<string, number>>(new Map())
const showResults = ref(false)
const expandedExplanations = ref<Set<string>>(new Set())

const score = computed(() => {
  if (!result.value) return { correct: 0, total: 0, percentage: 0 }
  
  let correct = 0
  result.value.questions.forEach(q => {
    if (answers.value.get(q.id) === q.correctAnswer) {
      correct++
    }
  })
  
  return {
    correct,
    total: result.value.questions.length,
    percentage: Math.round((correct / result.value.questions.length) * 100)
  }
})

const handleGenerate = async () => {
  const content = useFullNotes.value ? notesContent.value : selectedContent.value
  await generate(content, { force: true })
  answers.value.clear()
  showResults.value = false
  expandedExplanations.value.clear()
}

const selectAnswer = (questionId: string, optionIndex: number) => {
  if (showResults.value) return
  answers.value.set(questionId, optionIndex)
}

const submitQuiz = () => {
  showResults.value = true
}

const resetQuiz = () => {
  answers.value.clear()
  showResults.value = false
  expandedExplanations.value.clear()
}

const toggleExplanation = (questionId: string) => {
  if (expandedExplanations.value.has(questionId)) {
    expandedExplanations.value.delete(questionId)
  } else {
    expandedExplanations.value.add(questionId)
  }
}

const isCorrect = (questionId: string) => {
  const question = result.value?.questions.find(q => q.id === questionId)
  return question && answers.value.get(questionId) === question.correctAnswer
}

const downloadAsJSON = () => {
  if (!result.value) return
  
  const blob = new Blob([JSON.stringify(result.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'practice-questions.json'
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
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500 to-emerald-500 flex items-center justify-center">
            <HelpCircle class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Practice Questions</h1>
            <p class="text-muted-foreground">Test your knowledge with multiple-choice questions</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Generate Questions</CardTitle>
            <CardDescription>
              Create practice questions to test your understanding
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
              placeholder="Paste or type the content to create questions from..."
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
                  Generate Questions
                </template>
              </Button>
              
              <Button 
                v-if="result && showResults" 
                variant="outline"
                @click="resetQuiz"
              >
                Try Again
              </Button>
            </div>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <Card v-if="isLoading">
          <CardContent class="p-6 space-y-4">
            <Skeleton class="h-6 w-48" />
            <div class="space-y-3">
              <Skeleton class="h-12 w-full" />
              <Skeleton class="h-12 w-full" />
              <Skeleton class="h-12 w-full" />
              <Skeleton class="h-12 w-full" />
            </div>
          </CardContent>
        </Card>
        
        <!-- Results Banner -->
        <Card v-else-if="result && showResults" :class="score.percentage >= 70 ? 'border-green-500/50 bg-green-50/50 dark:bg-green-950/20' : 'border-orange-500/50 bg-orange-50/50 dark:bg-orange-950/20'">
          <CardContent class="p-6">
            <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div class="text-center sm:text-left">
                <h3 class="text-lg font-semibold">Quiz Complete!</h3>
                <p class="text-muted-foreground">
                  You scored {{ score.correct }} out of {{ score.total }} ({{ score.percentage }}%)
                </p>
              </div>
              <div class="flex items-center gap-2">
                <Button variant="outline" size="sm" @click="resetQuiz">
                  Try Again
                </Button>
                <Button variant="outline" size="sm" @click="downloadAsJSON">
                  <Download class="w-4 h-4 mr-2" />
                  Export
                </Button>
              </div>
            </div>
            <Progress :model-value="score.percentage" class="h-2 mt-4" />
          </CardContent>
        </Card>
        
        <!-- Questions List -->
        <div v-if="result && result.questions.length > 0" class="space-y-4">
          <div 
            v-for="(question, qIndex) in result.questions" 
            :key="question.id"
          >
            <Card :class="[
              showResults && (isCorrect(question.id) ? 'border-green-500/50' : 'border-red-500/50')
            ]">
              <CardHeader class="pb-2">
                <div class="flex items-start justify-between">
                  <CardTitle class="text-base">
                    <span class="text-muted-foreground mr-2">Q{{ qIndex + 1 }}.</span>
                    {{ question.question }}
                  </CardTitle>
                  <Badge 
                    v-if="showResults"
                    :variant="isCorrect(question.id) ? 'default' : 'destructive'"
                  >
                    {{ isCorrect(question.id) ? 'Correct' : 'Incorrect' }}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent class="space-y-2">
                <div 
                  v-for="(option, oIndex) in question.options" 
                  :key="oIndex"
                  :class="[
                    'p-3 rounded-lg border cursor-pointer transition-all',
                    answers.get(question.id) === oIndex && !showResults && 'border-primary bg-primary/5',
                    showResults && oIndex === question.correctAnswer && 'border-green-500 bg-green-50 dark:bg-green-950/30',
                    showResults && answers.get(question.id) === oIndex && oIndex !== question.correctAnswer && 'border-red-500 bg-red-50 dark:bg-red-950/30',
                    !showResults && 'hover:border-primary/50'
                  ]"
                  @click="selectAnswer(question.id, oIndex)"
                >
                  <div class="flex items-center gap-3">
                    <div :class="[
                      'w-6 h-6 rounded-full border-2 flex items-center justify-center flex-shrink-0',
                      answers.get(question.id) === oIndex 
                        ? 'border-primary bg-primary text-primary-foreground' 
                        : 'border-muted-foreground/30'
                    ]">
                      <template v-if="showResults && oIndex === question.correctAnswer">
                        <Check class="w-4 h-4 text-green-600" />
                      </template>
                      <template v-else-if="showResults && answers.get(question.id) === oIndex && oIndex !== question.correctAnswer">
                        <X class="w-4 h-4 text-red-600" />
                      </template>
                      <template v-else-if="answers.get(question.id) === oIndex">
                        <div class="w-2 h-2 rounded-full bg-current" />
                      </template>
                    </div>
                    <span class="text-sm">{{ option }}</span>
                  </div>
                </div>
                
                <!-- Explanation (collapsible, shown after results) -->
                <Collapsible v-if="showResults" class="mt-4">
                  <CollapsibleTrigger asChild>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      class="w-full justify-between"
                      @click="toggleExplanation(question.id)"
                    >
                      <span>View Explanation</span>
                      <ChevronDown :class="[
                        'w-4 h-4 transition-transform',
                        expandedExplanations.has(question.id) && 'rotate-180'
                      ]" />
                    </Button>
                  </CollapsibleTrigger>
                  <CollapsibleContent>
                    <div class="p-3 mt-2 rounded-lg bg-muted/50 text-sm">
                      {{ question.explanation }}
                    </div>
                  </CollapsibleContent>
                </Collapsible>
              </CardContent>
            </Card>
          </div>
          
          <!-- Submit Button -->
          <div v-if="!showResults" class="flex justify-center pt-4">
            <Button 
              size="lg"
              :disabled="answers.size < result.questions.length"
              @click="submitQuiz"
            >
              Submit Answers
              <Badge variant="secondary" class="ml-2">
                {{ answers.size }}/{{ result.questions.length }}
              </Badge>
            </Button>
          </div>
        </div>
        
        <!-- Empty State -->
        <Card v-else-if="!isLoading" class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <HelpCircle class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Questions Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Questions" to create practice questions from your materials
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
