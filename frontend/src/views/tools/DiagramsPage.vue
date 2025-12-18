<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { GitBranch, Loader2, Download, RefreshCw, Copy, Check, ZoomIn, ZoomOut, Maximize } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useStudyTool } from '@/composables/useStudyTool'
import type { DiagramsResult } from '@/types/study'
import mermaid from 'mermaid'

const { 
  isLoading, 
  error, 
  result, 
  selectedContent, 
  notesContent,
  generate
} = useStudyTool<DiagramsResult>('diagrams')

const useFullNotes = ref(true)
const copied = ref(false)
const diagramContainer = ref<HTMLElement | null>(null)
const fullscreenContainer = ref<HTMLElement | null>(null)
const zoom = ref(1)
const isFullscreen = ref(false)

// Initialize mermaid
onMounted(() => {
  mermaid.initialize({
    startOnLoad: false,
    theme: 'neutral',
    securityLevel: 'loose',
    flowchart: {
      useMaxWidth: true,
      htmlLabels: true,
      curve: 'basis'
    }
  })
})

// Render diagram when result changes
watch(() => result.value, async (newResult) => {
  if (newResult?.mermaidCode) {
    await nextTick()
    await renderDiagram()
  }
}, { immediate: true })

const renderDiagram = async () => {
  if (!result.value?.mermaidCode || !diagramContainer.value) return
  
  try {
    const { svg } = await mermaid.render('diagram-svg', result.value.mermaidCode)
    diagramContainer.value.innerHTML = svg
    
    // Also render in fullscreen container if open
    if (fullscreenContainer.value) {
      const { svg: fullSvg } = await mermaid.render('diagram-svg-full', result.value.mermaidCode)
      fullscreenContainer.value.innerHTML = fullSvg
    }
  } catch (err) {
    console.error('Mermaid render error:', err)
  }
}

const handleGenerate = async () => {
  // When using full notes, pass undefined to let generate() fetch from backend if needed
  const content = useFullNotes.value ? undefined : selectedContent.value
  await generate(content, { force: true })
}

const copyMermaidCode = async () => {
  if (!result.value?.mermaidCode) return
  await navigator.clipboard.writeText(result.value.mermaidCode)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

const downloadAsSVG = () => {
  if (!diagramContainer.value) return
  
  const svg = diagramContainer.value.querySelector('svg')
  if (!svg) return
  
  const blob = new Blob([svg.outerHTML], { type: 'image/svg+xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'concept-diagram.svg'
  a.click()
  URL.revokeObjectURL(url)
}

const downloadAsPNG = async () => {
  if (!diagramContainer.value) return
  
  const svg = diagramContainer.value.querySelector('svg')
  if (!svg) return
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  const img = new Image()
  const svgBlob = new Blob([svg.outerHTML], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  
  img.onload = () => {
    canvas.width = img.width * 2
    canvas.height = img.height * 2
    ctx.scale(2, 2)
    ctx.fillStyle = 'white'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0)
    
    const a = document.createElement('a')
    a.href = canvas.toDataURL('image/png')
    a.download = 'concept-diagram.png'
    a.click()
    
    URL.revokeObjectURL(url)
  }
  
  img.src = url
}

const zoomIn = () => {
  zoom.value = Math.min(zoom.value + 0.25, 3)
}

const zoomOut = () => {
  zoom.value = Math.max(zoom.value - 0.25, 0.5)
}

const openFullscreen = async () => {
  isFullscreen.value = true
  await nextTick()
  if (result.value?.mermaidCode && fullscreenContainer.value) {
    try {
      const { svg } = await mermaid.render('diagram-svg-full-' + Date.now(), result.value.mermaidCode)
      fullscreenContainer.value.innerHTML = svg
    } catch (err) {
      console.error('Mermaid fullscreen render error:', err)
    }
  }
}
</script>

<template>
  <StudyLayout>
    <div class="min-h-full p-6">
      <div class="max-w-5xl mx-auto space-y-6">
        <!-- Header -->
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-pink-500 to-rose-500 flex items-center justify-center">
            <GitBranch class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold">Concept Diagrams</h1>
            <p class="text-muted-foreground">Visualize relationships between concepts</p>
          </div>
        </div>
        
        <!-- Input Section -->
        <Card>
          <CardHeader>
            <CardTitle class="text-lg">Generate Diagram</CardTitle>
            <CardDescription>
              Create a visual representation of concepts and their relationships
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
              placeholder="Paste or type content to visualize as a diagram..."
              class="min-h-[150px] resize-y"
            />
            
            <Alert v-if="error" variant="destructive">
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
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
                Generate Diagram
              </template>
            </Button>
          </CardContent>
        </Card>
        
        <!-- Loading State -->
        <Card v-if="isLoading">
          <CardContent class="p-8">
            <div class="flex items-center justify-center">
              <Skeleton class="h-64 w-full max-w-2xl rounded-xl" />
            </div>
          </CardContent>
        </Card>
        
        <!-- Result Section -->
        <Card v-else-if="result">
          <CardHeader>
            <div class="flex items-center justify-between">
              <div>
                <CardTitle class="text-lg">Concept Map</CardTitle>
                <CardDescription>{{ result.description }}</CardDescription>
              </div>
              <div class="flex items-center gap-2">
                <Button variant="ghost" size="icon" @click="zoomOut">
                  <ZoomOut class="w-4 h-4" />
                </Button>
                <span class="text-sm text-muted-foreground">{{ Math.round(zoom * 100) }}%</span>
                <Button variant="ghost" size="icon" @click="zoomIn">
                  <ZoomIn class="w-4 h-4" />
                </Button>
                
                <Dialog v-model:open="isFullscreen">
                  <DialogTrigger asChild>
                    <Button variant="ghost" size="icon" @click="openFullscreen">
                      <Maximize class="w-4 h-4" />
                    </Button>
                  </DialogTrigger>
                  <DialogContent class="max-w-[90vw] max-h-[90vh] overflow-auto">
                    <DialogHeader>
                      <DialogTitle>Concept Diagram</DialogTitle>
                    </DialogHeader>
                    <div 
                      ref="fullscreenContainer" 
                      class="w-full overflow-auto flex items-center justify-center p-4"
                    />
                  </DialogContent>
                </Dialog>
                
                <Button variant="ghost" size="icon" @click="copyMermaidCode">
                  <Check v-if="copied" class="w-4 h-4 text-green-500" />
                  <Copy v-else class="w-4 h-4" />
                </Button>
                <Button variant="outline" size="sm" @click="downloadAsSVG">
                  <Download class="w-4 h-4 mr-2" />
                  SVG
                </Button>
                <Button variant="outline" size="sm" @click="downloadAsPNG">
                  <Download class="w-4 h-4 mr-2" />
                  PNG
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div class="overflow-auto border rounded-xl bg-white dark:bg-zinc-900 p-6">
              <div 
                ref="diagramContainer"
                :style="{ transform: `scale(${zoom})`, transformOrigin: 'center top' }"
                class="flex items-center justify-center transition-transform duration-200"
              />
            </div>
            
            <!-- Diagram Legend -->
            <div class="mt-4 p-4 rounded-lg bg-muted/50">
              <p class="text-sm font-medium mb-2">Diagram Elements</p>
              <div class="flex flex-wrap gap-4 text-sm">
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 rounded bg-blue-100 border border-blue-300" />
                  <span class="text-muted-foreground">Concepts</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 rounded bg-green-100 border border-green-300" />
                  <span class="text-muted-foreground">Details</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="w-4 h-4 rounded bg-yellow-100 border border-yellow-300" />
                  <span class="text-muted-foreground">Examples</span>
                </div>
              </div>
            </div>
            
            <!-- Mermaid Code (collapsed) -->
            <details class="mt-4">
              <summary class="text-sm text-muted-foreground cursor-pointer hover:text-foreground">
                View Mermaid Code
              </summary>
              <pre class="mt-2 p-4 rounded-lg bg-muted text-xs overflow-x-auto">{{ result.mermaidCode }}</pre>
            </details>
          </CardContent>
        </Card>
        
        <!-- Empty State -->
        <Card v-else class="border-dashed">
          <CardContent class="p-12 text-center">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mx-auto mb-4">
              <GitBranch class="w-6 h-6 text-muted-foreground" />
            </div>
            <h3 class="font-semibold mb-1">No Diagram Yet</h3>
            <p class="text-sm text-muted-foreground">
              Click "Generate Diagram" to visualize concepts from your materials
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </StudyLayout>
</template>
