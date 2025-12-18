<script setup lang="ts">
import { ref, computed } from 'vue'
import { Calendar, Loader2, Download, RefreshCw, Clock, BookOpen, CheckCircle2 } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ScrollArea } from '@/components/ui/scroll-area'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { StudyPlanResult } from '@/types/study'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<StudyPlanResult>('study-plan')

const useFullNotes = ref(true)
const examDate = ref('')
const hoursPerDay = ref(2)

// Calculate minimum date (tomorrow)
const minDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
})

const handleGenerate = async () => {
  if (!examDate.value) {
    return
  }
  
  // When using full notes, pass undefined to let generate() fetch from backend if needed
  const content = useFullNotes.value ? undefined : selectedContent.value
  await generate(content, { 
    force: true,
    additionalParams: {
      examDate: examDate.value,
      hoursPerDay: hoursPerDay.value
    }
  })
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', { 
    weekday: 'short',
    month: 'short', 
    day: 'numeric' 
  })
}

const formatDuration = (minutes: number) => {
  if (minutes < 60) return `${minutes}min`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

const downloadAsMarkdown = () => {
  if (!result.value) return
  
  let text = `# Study Plan\n\n`
  text += `**Exam Date:** ${formatDate(result.value.examDate)}\n`
  text += `**Total Study Time:** ${result.value.totalHours} hours\n\n`
  
  text += `## Schedule\n\n`
  result.value.sessions.forEach(session => {
    text += `### ${formatDate(session.date)} - ${session.topic}\n\n`
    text += `**Duration:** ${formatDuration(session.duration)}\n\n`
    text += `**Activities:**\n${session.activities.map(a => `- ${a}`).join('\n')}\n\n`
  })
  
  text += `## Recommendations\n\n`
  text += result.value.recommendations.map(r => `- ${r}`).join('\n')
  
  const blob = new Blob([text], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'study-plan.md'
  a.click()
  URL.revokeObjectURL(url)
}

const downloadAsICS = () => {
  if (!result.value) return
  
  let ics = `BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//AI Study Guide//EN\n`
  
  result.value.sessions.forEach(session => {
    const date = new Date(session.date)
    const startDate = date.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z'
    const endDate = new Date(date.getTime() + session.duration * 60000)
      .toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z'
    
    ics += `BEGIN:VEVENT\n`
    ics += `DTSTART:${startDate}\n`
    ics += `DTEND:${endDate}\n`
    ics += `SUMMARY:Study: ${session.topic}\n`
    ics += `DESCRIPTION:${session.activities.join('\\n')}\n`
    ics += `END:VEVENT\n`
  })
  
  ics += `END:VCALENDAR`
  
  const blob = new Blob([ics], { type: 'text/calendar' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'study-plan.ics'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <StudyLayout>
    <div class="min-h-full p-6">
      <div class="max-w-5xl mx-auto space-y-6">
        <!-- Header -->
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-blue-500 flex items-center justify-center">
            <Calendar class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Study Plan</h1>
            <p class="text-muted-foreground">Create a personalized study schedule for your exam</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Generate Study Plan</CardTitle>
            <CardDescription>
              Set your exam date and available study time to get a customized schedule
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label htmlFor="examDate">Exam Date</Label>
                <Input 
                  id="examDate"
                  type="date"
                  v-model="examDate"
                  :min="minDate"
                />
              </div>
              <div class="space-y-2">
                <Label htmlFor="hoursPerDay">Hours per Day</Label>
                <Input 
                  id="hoursPerDay"
                  type="number"
                  v-model.number="hoursPerDay"
                  min="1"
                  max="12"
                />
              </div>
            </div>
            
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
                Custom Topics
              </Button>
            </div>
            
            <Textarea 
              v-if="!useFullNotes"
              v-model="selectedContent"
              placeholder="List the topics you need to cover..."
              class="min-h-[100px] resize-y"
            />
            
            <Alert v-if="error" variant="destructive">
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
            <div class="flex items-center gap-2">
              <Button 
                :disabled="isLoading || !examDate" 
                @click="handleGenerate"
              >
                <template v-if="isLoading">
                  <Loader2 class="w-4 h-4 mr-2 animate-spin" />
                  Generating...
                </template>
                <template v-else>
                  <RefreshCw class="w-4 h-4 mr-2" />
                  Generate Plan
                </template>
              </Button>
              
              <Button 
                v-if="result" 
                variant="outline"
                @click="downloadAsMarkdown"
              >
                <Download class="w-4 h-4 mr-2" />
                Markdown
              </Button>
              
              <Button 
                v-if="result" 
                variant="outline"
                @click="downloadAsICS"
              >
                <Calendar class="w-4 h-4 mr-2" />
                Calendar
              </Button>
            </div>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <div v-if="isLoading" class="space-y-4">
          <Card>
            <CardContent class="p-6">
              <div class="flex items-center gap-4 mb-4">
                <Skeleton class="h-12 w-12 rounded-lg" />
                <div class="space-y-2">
                  <Skeleton class="h-5 w-32" />
                  <Skeleton class="h-4 w-24" />
                </div>
              </div>
              <Skeleton class="h-40 w-full rounded-lg" />
            </CardContent>
          </Card>
        </div>
        
        <!-- Results -->
        <div v-else-if="result" class="space-y-6">
          <!-- Overview Stats -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Card>
              <CardContent class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                    <Calendar class="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <p class="text-sm text-muted-foreground">Exam Date</p>
                    <p class="font-semibold">{{ formatDate(result.examDate) }}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            <Card>
              <CardContent class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                    <Clock class="w-5 h-5 text-green-500" />
                  </div>
                  <div>
                    <p class="text-sm text-muted-foreground">Total Study Time</p>
                    <p class="font-semibold">{{ result.totalHours }} hours</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            
            <Card>
              <CardContent class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg bg-violet-500/10 flex items-center justify-center">
                    <BookOpen class="w-5 h-5 text-violet-500" />
                  </div>
                  <div>
                    <p class="text-sm text-muted-foreground">Study Sessions</p>
                    <p class="font-semibold">{{ result.sessions.length }} sessions</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
          
          <!-- Timeline -->
          <Card>
            <CardHeader>
              <CardTitle class="text-lg">Study Schedule</CardTitle>
              <CardDescription>
                Your personalized study timeline
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ScrollArea class="h-[400px] pr-4">
                <div class="relative">
                  <!-- Timeline line -->
                  <div class="absolute left-4 top-0 bottom-0 w-0.5 bg-border" />
                  
                  <div class="space-y-6">
                    <div 
                      v-for="(session, index) in result.sessions" 
                      :key="session.id"
                      class="relative pl-10"
                    >
                      <!-- Timeline dot -->
                      <div class="absolute left-2.5 w-3 h-3 rounded-full bg-primary border-2 border-background" />
                      
                      <Card>
                        <CardHeader class="pb-2">
                          <div class="flex items-start justify-between">
                            <div>
                              <Badge variant="outline" class="mb-2">
                                Day {{ index + 1 }} • {{ formatDate(session.date) }}
                              </Badge>
                              <CardTitle class="text-base">{{ session.topic }}</CardTitle>
                            </div>
                            <Badge variant="secondary">
                              <Clock class="w-3 h-3 mr-1" />
                              {{ formatDuration(session.duration) }}
                            </Badge>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <ul class="space-y-2">
                            <li 
                              v-for="(activity, aIndex) in session.activities" 
                              :key="aIndex"
                              class="flex items-start gap-2 text-sm"
                            >
                              <CheckCircle2 class="w-4 h-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                              <span>{{ activity }}</span>
                            </li>
                          </ul>
                        </CardContent>
                      </Card>
                    </div>
                  </div>
                </div>
              </ScrollArea>
            </CardContent>
          </Card>
          
          <!-- Recommendations -->
          <Card>
            <CardHeader>
              <CardTitle class="text-lg">Study Tips</CardTitle>
              <CardDescription>
                Recommendations for effective studying
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ul class="space-y-3">
                <li 
                  v-for="(rec, index) in result.recommendations" 
                  :key="index"
                  class="flex items-start gap-3"
                >
                  <span class="flex-shrink-0 w-6 h-6 rounded-full bg-primary/10 text-primary text-sm font-medium flex items-center justify-center">
                    {{ index + 1 }}
                  </span>
                  <span class="text-sm">{{ rec }}</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <Calendar class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Study Plan Yet</h3>
            <p class="text-sm text-muted-foreground">
              Set your exam date and click "Generate Plan" to create your schedule
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
