import { ref } from 'vue';

// Explicit navigation intent only; normal page updates must not replay the title.
export const newReviewAnimationRequest = ref(0);
