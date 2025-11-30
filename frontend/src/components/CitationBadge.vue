<script setup lang="ts">
import type { Citation } from '@/types'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'

interface Props {
  citation: Citation
  index: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [citation: Citation]
}>()

function handleClick() {
  emit('click', props.citation)
}
</script>

<template>
  <TooltipProvider>
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          @click="handleClick"
          :class="[
            'inline-flex items-center justify-center',
            'min-w-5 h-5 px-1.5 mx-0.5',
            'text-[10px] font-semibold text-white',
            'rounded-full bg-linear-to-r from-blue-500 to-blue-600 shadow-sm',
            'hover:scale-110 hover:shadow-md',
            'transition-all duration-200 ease-out',
            'cursor-pointer select-none'
          ]"
        >
          {{ citation.index }}
        </button>
      </TooltipTrigger>
      <TooltipContent 
        side="top" 
        :sideOffset="5"
        class="max-w-xs"
      >
        <div class="space-y-1">
          <div class="font-medium text-sm truncate">
            {{ citation.documentName }}
          </div>
          <div class="text-xs text-muted-foreground">
            Source [{{ citation.index }}] - Chunk #{{ citation.chunkOrder + 1 }}
          </div>
        </div>
      </TooltipContent>
    </Tooltip>
  </TooltipProvider>
</template>
