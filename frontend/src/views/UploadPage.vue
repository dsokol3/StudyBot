<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, FileText, X, CheckCircle2, AlertCircle, Loader2, BookOpen } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Alert, AlertDescription } from '@/components/ui/alert'
import StudyLayout from '@/layouts/StudyLayout.vue'
import { useNotesStore } from '@/stores/notes'

const router = useRouter()
const notesStore = useNotesStore()

const isDragging = ref(false)
const error = ref<string | null>(null)

const acceptedTypes = ['.pdf', '.txt', '.md', '.docx']
const maxFileSize = 10 * 1024 * 1024 // 10MB

const uploadQueue = computed(() => Array.from(notesStore.uploadQueue.values()))
const hasNotes = computed(() => notesStore.notes.length > 0)
const isUploading = computed(() => uploadQueue.value.some(u => u.status === 'uploading'))

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = true
}

const handleDragLeave = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = false
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = false
  
  const files = e.dataTransfer?.files
  if (files) {
    handleFiles(Array.from(files))
  }
}

const handleFileInput = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files) {
    handleFiles(Array.from(target.files))
  }
  target.value = '' // Reset input
}

const handleFiles = async (files: File[]) => {
  error.value = null
  
  const validFiles = files.filter(file => {
    // Check file extension
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!acceptedTypes.includes(ext)) {
      error.value = `File "${file.name}" has an unsupported format. Please use ${acceptedTypes.join(', ')}`
      return false
    }
    
    // Check file size
    if (file.size > maxFileSize) {
      error.value = `File "${file.name}" is too large. Maximum size is 10MB.`
      return false
    }
    
    return true
  })
  
  if (validFiles.length > 0) {
    await notesStore.uploadFiles(validFiles)
  }
}

const removeNote = (id: string) => {
  notesStore.removeNote(id)
}

const continueToDashboard = () => {
  router.push('/dashboard')
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'complete':
      return CheckCircle2
    case 'error':
      return AlertCircle
    default:
      return Loader2
  }
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'complete':
      return 'text-green-500'
    case 'error':
      return 'text-destructive'
    default:
      return 'text-muted-foreground'
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<template>
  <StudyLayout>
    <div class="min-h-full flex items-center justify-center p-6 bg-gradient-to-br from-background via-background to-muted/20">
      <div class="w-full max-w-2xl space-y-6">
        <!-- Header -->
        <div class="text-center space-y-2">
          <div class="flex justify-center mb-4">
            <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-500 to-fuchsia-500 flex items-center justify-center shadow-lg">
              <BookOpen class="w-8 h-8 text-white" />
            </div>
          </div>
          <h1 class="text-3xl font-bold tracking-tight">AI Study Guide</h1>
          <p class="text-muted-foreground">
            Upload your study materials to get started with AI-powered learning tools
          </p>
        </div>
        
        <!-- Upload Card -->
        <Card>
          <CardHeader>
            <CardTitle>Upload Study Materials</CardTitle>
            <CardDescription>
              Drag and drop your files or click to browse. Supports PDF, TXT, MD, and DOCX files.
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <!-- Drop Zone -->
            <div
              :class="[
                'relative border-2 border-dashed rounded-xl p-8 transition-all duration-200 cursor-pointer',
                isDragging 
                  ? 'border-primary bg-primary/5 scale-[1.02]' 
                  : 'border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/30'
              ]"
              @dragover="handleDragOver"
              @dragleave="handleDragLeave"
              @drop="handleDrop"
              @click="($refs.fileInput as HTMLInputElement)?.click()"
            >
              <input
                ref="fileInput"
                type="file"
                :accept="acceptedTypes.join(',')"
                multiple
                class="hidden"
                @change="handleFileInput"
              />
              
              <div class="flex flex-col items-center gap-3 text-center">
                <div :class="[
                  'w-12 h-12 rounded-full flex items-center justify-center transition-colors',
                  isDragging ? 'bg-primary/20' : 'bg-muted'
                ]">
                  <Upload :class="[
                    'w-6 h-6 transition-colors',
                    isDragging ? 'text-primary' : 'text-muted-foreground'
                  ]" />
                </div>
                <div>
                  <p class="font-medium">
                    {{ isDragging ? 'Drop files here' : 'Drag files here or click to browse' }}
                  </p>
                  <p class="text-sm text-muted-foreground mt-1">
                    PDF, TXT, MD, DOCX up to 10MB each
                  </p>
                </div>
              </div>
            </div>
            
            <!-- Error Alert -->
            <Alert v-if="error" variant="destructive">
              <AlertCircle class="h-4 w-4" />
              <AlertDescription>{{ error }}</AlertDescription>
            </Alert>
            
            <!-- Upload Queue -->
            <div v-if="uploadQueue.length > 0" class="space-y-2">
              <p class="text-sm font-medium">Uploading...</p>
              <div v-for="upload in uploadQueue" :key="upload.id" class="space-y-1">
                <div class="flex items-center justify-between text-sm">
                  <span class="truncate max-w-[70%]">{{ upload.filename }}</span>
                  <component 
                    :is="getStatusIcon(upload.status)" 
                    :class="['w-4 h-4', getStatusColor(upload.status), upload.status === 'uploading' && 'animate-spin']"
                  />
                </div>
                <Progress :model-value="upload.progress" class="h-1" />
              </div>
            </div>
            
            <!-- Uploaded Files -->
            <div v-if="notesStore.notes.length > 0" class="space-y-2">
              <p class="text-sm font-medium">Uploaded Files ({{ notesStore.notes.length }})</p>
              <div class="space-y-2">
                <div 
                  v-for="note in notesStore.notes" 
                  :key="note.id"
                  class="flex items-center gap-3 p-3 rounded-lg bg-muted/50 group"
                >
                  <div class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                    <FileText class="w-4 h-4 text-primary" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium truncate">{{ note.filename }}</p>
                    <p class="text-xs text-muted-foreground">
                      {{ formatFileSize(note.size) }} • {{ note.type.toUpperCase() }}
                    </p>
                  </div>
                  <Button 
                    variant="ghost" 
                    size="icon" 
                    class="opacity-0 group-hover:opacity-100 transition-opacity"
                    @click.stop="removeNote(note.id)"
                  >
                    <X class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
        
        <!-- Continue Button -->
        <div class="flex flex-col items-center gap-3">
          <Button 
            size="lg" 
            :disabled="!hasNotes || isUploading"
            class="min-w-[200px]"
            @click="continueToDashboard"
          >
            <template v-if="isUploading">
              <Loader2 class="w-4 h-4 mr-2 animate-spin" />
              Uploading...
            </template>
            <template v-else>
              Continue to Dashboard
            </template>
          </Button>
          
          <Button 
            variant="ghost" 
            size="sm"
            class="text-muted-foreground hover:text-foreground"
            @click="continueToDashboard"
          >
            Skip for now
          </Button>
        </div>
        
        <!-- Footer hint -->
        <p class="text-center text-sm text-muted-foreground">
          Your files will be processed securely to generate study materials
        </p>
      </div>
    </div>
  </StudyLayout>
</template>
