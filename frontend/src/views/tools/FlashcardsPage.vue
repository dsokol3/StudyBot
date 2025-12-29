<script setup lang="ts">
import { ref, computed } from 'vue'
import { Layers, Loader2, Download, RefreshCw, ChevronLeft, ChevronRight, RotateCcw, Shuffle } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { FlashcardsResult } from '@/types/study'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<FlashcardsResult>('flashcards')

const useFullNotes = ref(true)
const useDifficultyDistribution = ref(false)
const easyCount = ref(3)
const mediumCount = ref(4)
const hardCount = ref(3)
const currentIndex = ref(0)
const isFlipped = ref(false)
const studyMode = ref(false)

const currentCard = computed(() => {
  if (!result.value || !result.value.cards.length) return null
  return result.value.cards[currentIndex.value]
})

const progress = computed(() => {
  if (!result.value) return 0
  return ((currentIndex.value + 1) / result.value.cards.length) * 100
})

const totalCards = computed(() => {
  return easyCount.value + mediumCount.value + hardCount.value
})

const handleGenerate = async () => {
  // When using full notes, pass undefined to let generate() fetch from backend if needed
  const content = useFullNotes.value ? undefined : selectedContent.value
  
  // Build options object
  const options: any = { force: true }
  if (useDifficultyDistribution.value) {
    options.easyCount = easyCount.value
    options.mediumCount = mediumCount.value
    options.hardCount = hardCount.value
    console.log('Generating with custom difficulty:', {
      easy: easyCount.value,
      medium: mediumCount.value,
      hard: hardCount.value,
      total: totalCards.value
    })
  }
  
  await generate(content, options)
  currentIndex.value = 0
  isFlipped.value = false
}

const nextCard = () => {
  if (!result.value) return
  if (currentIndex.value < result.value.cards.length - 1) {
    currentIndex.value++
    isFlipped.value = false
  }
}

const prevCard = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    isFlipped.value = false
  }
}

const flipCard = () => {
  isFlipped.value = !isFlipped.value
}

const shuffleCards = () => {
  if (!result.value) return
  result.value.cards.sort(() => Math.random() - 0.5)
  currentIndex.value = 0
  isFlipped.value = false
}

const resetCards = () => {
  currentIndex.value = 0
  isFlipped.value = false
}

const getDifficultyColor = (difficulty: string) => {
  switch (difficulty) {
    case 'easy': return 'bg-green-500/10 text-green-500'
    case 'medium': return 'bg-yellow-500/10 text-yellow-500'
    case 'hard': return 'bg-red-500/10 text-red-500'
    default: return 'bg-muted text-muted-foreground'
  }
}

const downloadAsAnki = () => {
  if (!result.value) return
  
  // Create Anki-compatible format (tab-separated front/back)
  const text = result.value.cards.map(card => `${card.front}\t${card.back}`).join('\n')
  
  const blob = new Blob([text], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'flashcards-anki.txt'
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
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 flex items-center justify-center">
            <Layers class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Flashcards</h1>
            <p class="text-muted-foreground">Create interactive flashcards for memorization</p>
          </div>
        </div>
        
        <!-- Input Section (hidden in study mode) -->
        <Card v-if="!studyMode">
          <CardHeader>
            <CardTitle class="text-lg">Generate Flashcards</CardTitle>
            <CardDescription>
              Create flashcards from your notes for effective studying
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex flex-wrap items-center gap-3">
              <Button 
                :variant="useFullNotes ? 'default' : 'outline'"
                size="sm"
                @click="useFullNotes = true"
                class="transition-all hover:scale-105 shadow-sm"
              >
                📚 Use All Notes
              </Button>
              <Button 
                :variant="!useFullNotes ? 'default' : 'outline'"
                size="sm"
                @click="useFullNotes = false"
                class="transition-all hover:scale-105 shadow-sm"
              >
                ✏️ Custom Content
              </Button>
            </div>
            
            <Textarea 
              v-if="!useFullNotes"
              v-model="selectedContent"
              placeholder="Paste or type the content to create flashcards from..."
              class="min-h-[150px] resize-y"
            />
            
            <!-- Difficulty Distribution Controls -->
            <div class="space-y-4 pt-4 border-t">
              <div class="flex flex-wrap items-center gap-3">
                <Button 
                  :variant="!useDifficultyDistribution ? 'default' : 'outline'"
                  size="sm"
                  @click="useDifficultyDistribution = false"
                  class="transition-all hover:scale-105 shadow-sm"
                >
                  🎲 Auto Difficulty
                </Button>
                <Button 
                  :variant="useDifficultyDistribution ? 'default' : 'outline'"
                  size="sm"
                  @click="useDifficultyDistribution = true"
                  class="transition-all hover:scale-105 shadow-sm"
                >
                  ⚙️ Custom Difficulty
                </Button>
              </div>
              
              <div v-if="useDifficultyDistribution" class="space-y-4">
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="space-y-2 p-4 rounded-lg border-2 border-green-200 dark:border-green-900 bg-gradient-to-br from-green-50 to-transparent dark:from-green-950/20 transition-all hover:shadow-md">
                    <Label for="easy-count" class="flex items-center gap-2 font-semibold">
                      <Badge :class="getDifficultyColor('easy')" class="text-xs">Easy</Badge>
                      Cards
                    </Label>
                    <Input 
                      id="easy-count"
                      v-model.number="easyCount"
                      type="number"
                      min="0"
                      max="50"
                      class="w-full text-lg font-semibold border-green-300 focus:border-green-500"
                    />
                  </div>
                  
                  <div class="space-y-2 p-4 rounded-lg border-2 border-yellow-200 dark:border-yellow-900 bg-gradient-to-br from-yellow-50 to-transparent dark:from-yellow-950/20 transition-all hover:shadow-md">
                    <Label for="medium-count" class="flex items-center gap-2 font-semibold">
                      <Badge :class="getDifficultyColor('medium')" class="text-xs">Medium</Badge>
                      Cards
                    </Label>
                    <Input 
                      id="medium-count"
                      v-model.number="mediumCount"
                      type="number"
                      min="0"
                      max="50"
                      class="w-full text-lg font-semibold border-yellow-300 focus:border-yellow-500"
                    />
                  </div>
                  
                  <div class="space-y-2 p-4 rounded-lg border-2 border-red-200 dark:border-red-900 bg-gradient-to-br from-red-50 to-transparent dark:from-red-950/20 transition-all hover:shadow-md">
                    <Label for="hard-count" class="flex items-center gap-2 font-semibold">
                      <Badge :class="getDifficultyColor('hard')" class="text-xs">Hard</Badge>
                      Cards
                    </Label>
                    <Input 
                      id="hard-count"
                      v-model.number="hardCount"
                      type="number"
                      min="0"
                      max="50"
                      class="w-full text-lg font-semibold border-red-300 focus:border-red-500"
                    />
                  </div>
                </div>
                
                <div class="flex items-center justify-center gap-2 p-3 rounded-lg bg-gradient-to-r from-violet-100 to-purple-100 dark:from-violet-950/30 dark:to-purple-950/30 border border-violet-200 dark:border-violet-800">
                  <span class="text-sm font-medium text-muted-foreground">Total cards:</span>
                  <Badge variant="secondary" class="text-lg font-bold px-3 py-1">{{ totalCards }}</Badge>
                </div>
              </div>
            </div>
            
            <Alert v-if="error" variant="destructive">
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
            <div class="flex items-center gap-2">
              <Button 
                size="lg"
                :disabled="isLoading" 
                @click="handleGenerate"
                class="bg-gradient-to-r from-violet-600 to-purple-600 hover:from-violet-700 hover:to-purple-700 text-white shadow-lg hover:shadow-xl transition-all hover:scale-105"
              >
                <template v-if="isLoading">
                  <Loader2 class="w-5 h-5 mr-2 animate-spin" />
                  Generating...
                </template>
                <template v-else>
                  <RefreshCw class="w-5 h-5 mr-2" />
                  ✨ Generate Flashcards
                </template>
              </Button>
              
              <Button 
                v-if="result" 
                variant="outline"
                size="lg"
                @click="studyMode = true"
                class="shadow-md hover:shadow-lg transition-all hover:scale-105"
              >
                🎯 Study Mode
              </Button>
            </div>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <Card v-if="isLoading">
          <CardContent class="p-8">
            <div class="space-y-4">
              <Skeleton class="h-40 w-full rounded-xl" />
              <div class="flex justify-center gap-2">
                <Skeleton class="h-10 w-24" />
                <Skeleton class="h-10 w-24" />
              </div>
            </div>
          </CardContent>
        </Card>
        
        <!-- Flashcard Study View -->
        <div v-else-if="result && result.cards.length > 0">
          <!-- Study Mode Controls -->
          <div v-if="studyMode" class="flex items-center justify-between">
            <Button variant="ghost" @click="studyMode = false">
              Exit Study Mode
            </Button>
            <div class="flex items-center gap-2">
              <Button variant="ghost" size="icon" @click="shuffleCards">
                <Shuffle class="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="icon" @click="resetCards">
                <RotateCcw class="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="icon" @click="downloadAsAnki">
                <Download class="w-4 h-4" />
              </Button>
            </div>
          </div>
          
          <!-- Progress Bar -->
          <div class="space-y-2">
            <div class="flex justify-between text-sm text-muted-foreground">
              <span>Card {{ currentIndex + 1 }} of {{ result.cards.length }}</span>
              <span>{{ Math.round(progress) }}% complete</span>
            </div>
            <Progress :model-value="progress" class="h-2" />
          </div>
          
          <!-- Flashcard -->
          <div 
            class="perspective-1000 cursor-pointer"
            @click="flipCard"
          >
            <Card 
              :class="[
                'min-h-[300px] transition-all duration-500 transform-style-3d relative',
                isFlipped && 'flashcard-flipped'
              ]"
            >
              <CardContent class="p-8 h-full min-h-[300px]">
                <!-- Front -->
                <div 
                  :class="[
                    'absolute inset-0 flex flex-col items-center justify-center p-8 backface-hidden transition-all duration-500 flashcard-front',
                  ]"
                  :style="{ opacity: isFlipped ? 0 : 1, pointerEvents: isFlipped ? 'none' : 'auto' }"
                >
                  <Badge 
                    v-if="currentCard"
                    :class="['mb-4', getDifficultyColor(currentCard.difficulty)]"
                  >
                    {{ currentCard.difficulty }}
                  </Badge>
                  <p class="text-xl font-medium">{{ currentCard?.front }}</p>
                  <p class="text-sm text-muted-foreground mt-4">Click to flip</p>
                </div>
                
                <!-- Back -->
                <div 
                  :class="[
                    'absolute inset-0 flex flex-col items-center justify-center p-8 backface-hidden transition-all duration-500 flashcard-back',
                  ]"
                  :style="{ opacity: isFlipped ? 1 : 0, pointerEvents: isFlipped ? 'auto' : 'none' }"
                >
                  <Badge class="mb-4 bg-green-500/10 text-green-600">Answer</Badge>
                  <p class="text-lg text-center">{{ currentCard?.back }}</p>
                  <p class="text-sm text-muted-foreground mt-4">Click to flip back</p>
                </div>
              </CardContent>
            </Card>
          </div>
          
          <!-- Navigation -->
          <div class="flex items-center justify-center gap-4">
            <Button 
              variant="outline" 
              size="lg"
              :disabled="currentIndex === 0"
              @click="prevCard"
            >
              <ChevronLeft class="w-5 h-5 mr-1" />
              Previous
            </Button>
            
            <Button 
              variant="outline"
              size="lg"
              :disabled="currentIndex === result.cards.length - 1"
              @click="nextCard"
            >
              Next
              <ChevronRight class="w-5 h-5 ml-1" />
            </Button>
          </div>
          
          <!-- All Cards Overview (not in study mode) -->
          <Card v-if="!studyMode">
            <CardHeader>
              <CardTitle class="text-lg">All Flashcards</CardTitle>
              <CardDescription>
                {{ result.cards.length }} cards generated
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div 
                  v-for="(card, index) in result.cards" 
                  :key="card.id"
                  :class="[
                    'p-4 rounded-lg border cursor-pointer transition-colors',
                    currentIndex === index 
                      ? 'border-primary bg-primary/5' 
                      : 'hover:border-primary/50'
                  ]"
                  @click="currentIndex = index; isFlipped = false"
                >
                  <div class="flex items-start justify-between gap-2">
                    <p class="text-sm font-medium line-clamp-2">{{ card.front }}</p>
                    <Badge 
                      variant="outline"
                      :class="getDifficultyColor(card.difficulty)"
                      class="text-xs flex-shrink-0"
                    >
                      {{ card.difficulty }}
                    </Badge>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <Layers class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Flashcards Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Flashcards" to create study cards from your materials
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>

<style scoped>
.perspective-1000 {
  perspective: 1000px;
}

.transform-style-3d {
  transform-style: preserve-3d;
}

.backface-hidden {
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.rotate-y-180 {
  transform: rotateY(180deg);
}

/* Fix for the flashcard flip - the back side needs to be pre-rotated */
.flashcard-front {
  transform: rotateY(0deg);
}

.flashcard-back {
  transform: rotateY(180deg);
}

.flashcard-flipped .flashcard-front {
  transform: rotateY(180deg);
}

.flashcard-flipped .flashcard-back {
  transform: rotateY(360deg);
}
</style>
