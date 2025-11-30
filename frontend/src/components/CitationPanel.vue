<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Citation } from '@/types'
import { FileText, ChevronDown, ChevronUp, BookOpen } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'

interface Props {
  citations: Citation[]
  expanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  expanded: false
})

const isExpanded = ref(props.expanded)
const expandedCitations = ref<Set<string>>(new Set())

// Deduplicate citations by document
const groupedCitations = computed(() => {
  const groups = new Map<string, Citation[]>()
  
  for (const citation of props.citations) {
    const key = citation.documentId
    if (!groups.has(key)) {
      groups.set(key, [])
    }
    groups.get(key)!.push(citation)
  }
  
  // Sort chunks within each document by chunk order
  for (const chunks of groups.values()) {
    chunks.sort((a, b) => a.chunkOrder - b.chunkOrder)
  }
  
  return Array.from(groups.entries()).map(([docId, chunks]) => ({
    documentId: docId,
    documentName: chunks[0]?.documentName ?? 'Unknown Document',
    chunks: chunks
  }))
})

function toggleCitation(docId: string) {
  if (expandedCitations.value.has(docId)) {
    expandedCitations.value.delete(docId)
  } else {
    expandedCitations.value.add(docId)
  }
}
</script>

<template>
  <Collapsible v-model:open="isExpanded" class="mt-3">
    <CollapsibleTrigger asChild>
      <Button 
        variant="ghost" 
        size="sm"
        class="w-full justify-between h-8 px-3 text-xs font-medium text-muted-foreground hover:text-foreground"
      >
        <span class="flex items-center gap-1.5">
          <BookOpen class="w-3.5 h-3.5" />
          {{ citations.length }} source{{ citations.length !== 1 ? 's' : '' }} cited
        </span>
        <ChevronDown 
          v-if="!isExpanded" 
          class="w-4 h-4 transition-transform" 
        />
        <ChevronUp 
          v-else 
          class="w-4 h-4 transition-transform" 
        />
      </Button>
    </CollapsibleTrigger>
    
    <CollapsibleContent>
      <div class="mt-2 space-y-2">
        <div 
          v-for="group in groupedCitations" 
          :key="group.documentId"
          class="rounded-lg border bg-muted/30 overflow-hidden"
        >
          <!-- Document Header -->
          <button
            @click="toggleCitation(group.documentId)"
            class="w-full flex items-center gap-2 p-2.5 hover:bg-muted/50 transition-colors text-left"
          >
            <div class="shrink-0 w-6 h-6 rounded bg-primary/10 flex items-center justify-center">
              <FileText class="w-3.5 h-3.5 text-primary" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium truncate">
                {{ group.documentName }}
              </div>
              <div class="text-xs text-muted-foreground">
                {{ group.chunks.length }} relevant chunk{{ group.chunks.length !== 1 ? 's' : '' }}
              </div>
            </div>
            <ChevronDown 
              :class="[
                'w-4 h-4 text-muted-foreground transition-transform',
                expandedCitations.has(group.documentId) && 'rotate-180'
              ]"
            />
          </button>
          
          <!-- Expanded Chunks -->
          <div 
            v-if="expandedCitations.has(group.documentId)"
            class="border-t bg-background/50"
          >
            <div 
              v-for="(chunk, idx) in group.chunks" 
              :key="`${chunk.documentId}-${chunk.chunkOrder}`"
              :class="[
                'p-3 text-sm',
                idx !== group.chunks.length - 1 && 'border-b'
              ]"
            >
              <div class="flex items-center gap-2 mb-2">
                <span class="text-xs font-medium text-muted-foreground">
                  Source [{{ chunk.index }}] - Chunk #{{ chunk.chunkOrder + 1 }}
                </span>
              </div>
              <p 
                v-if="chunk.content" 
                class="text-muted-foreground text-xs leading-relaxed whitespace-pre-wrap"
              >
                {{ chunk.content }}
              </p>
              <p 
                v-else 
                class="text-muted-foreground/60 text-xs italic"
              >
                From: {{ chunk.documentName }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </CollapsibleContent>
  </Collapsible>
</template>
