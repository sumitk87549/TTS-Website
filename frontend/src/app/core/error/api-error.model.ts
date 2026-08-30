/**
 * Structured error response from the Spring Boot backend.
 * GlobalExceptionHandler produces this shape for all errors.
 */
export interface ApiError {
  /** Machine-readable code — Angular maps this to the UX analogy + quote */
  code: string;
  /** HTTP status integer */
  status: number;
  /** Short technical message (used for logging, not displayed to user) */
  message: string;
  /** Field-level validation errors — present only for VALIDATION_ERROR */
  details?: Record<string, string>;
  /** ISO timestamp */
  timestamp?: string;
}

/**
 * User-facing error — what we actually show in the UI.
 * Converted from ApiError by the error interceptor.
 */
export interface UserFacingError {
  /** The underlying backend code */
  code: string;
  /** Short, friendly title shown prominently */
  title: string;
  /** Plain-English analogy explaining the issue without technical jargon */
  analogy: string;
  /** Thematic quote to soften the experience */
  quote: string;
  /** Quote attribution */
  quoteAuthor: string;
  /** Suggested action label for the CTA button */
  actionLabel: string;
  /** Whether to offer a Retry action */
  canRetry: boolean;
  /** Original HTTP status for programmatic use */
  httpStatus: number;
  /** Field validation errors if applicable */
  fieldErrors?: Record<string, string>;
}

/** Maps backend error codes to user-facing copy + analogy + quote */
export const ERROR_UX_MAP: Record<string, Omit<UserFacingError, 'code' | 'httpStatus' | 'fieldErrors'>> = {
  TTS_ENGINE_UNAVAILABLE: {
    title: 'Voice Studio Is Taking a Short Break',
    analogy: 'Think of our voice studio like a recording booth — right now the artist just stepped out for chai. They\'ll be back any second!',
    quote: 'Every pause has a purpose.',
    quoteAuthor: 'Anonymous',
    actionLabel: 'Try Again',
    canRetry: true,
  },
  TTS_ENGINE_BUSY: {
    title: 'Studio Is Fully Booked Right Now',
    analogy: 'Our recording booth is like a popular studio — all three rooms are occupied. Just wait a moment and a slot will open up!',
    quote: 'Good things come to those who wait.',
    quoteAuthor: 'Unknown',
    actionLabel: 'Try Again in a Moment',
    canRetry: true,
  },
  TTS_ENGINE_TIMEOUT: {
    title: 'That Took a Bit Too Long',
    analogy: 'Generating speech is like baking — sometimes a very large loaf takes longer than expected and the oven timer goes off. Try a shorter piece of text!',
    quote: 'Great things are not done by impulse, but by a series of small things brought together.',
    quoteAuthor: 'Vincent Van Gogh',
    actionLabel: 'Try With Shorter Text',
    canRetry: true,
  },
  DAILY_LIMIT_EXCEEDED: {
    title: 'You\'ve Used Your Daily Words!',
    analogy: 'Think of it like a notebook — you\'ve filled today\'s pages beautifully. A fresh page is waiting for you tomorrow at midnight!',
    quote: 'Rest is not idleness, it is the work of tomorrow.',
    quoteAuthor: 'John Lubbock',
    actionLabel: 'Come Back Tomorrow',
    canRetry: false,
  },
  TEXT_TOO_LONG: {
    title: 'That\'s a Whole Novel!',
    analogy: 'Our voice studio works best with shorter sessions — like reading one chapter at a time rather than the whole book in one sitting. Try splitting your text into smaller parts.',
    quote: 'Brevity is the soul of wit.',
    quoteAuthor: 'William Shakespeare',
    actionLabel: 'Edit Text',
    canRetry: true,
  },
  RATE_LIMIT_EXCEEDED: {
    title: 'Slow Down, Superstar!',
    analogy: 'You\'re on fire! But even the best DJs play one track at a time — we need a short cooldown before the next request. Try again in a little while.',
    quote: 'Quality over quantity — always.',
    quoteAuthor: 'Anonymous',
    actionLabel: 'Try Later',
    canRetry: false,
  },
  VALIDATION_ERROR: {
    title: 'Something Needs Your Attention',
    analogy: 'Think of this like proofreading before sending an email — just a small detail needs fixing before we can continue.',
    quote: 'Details make perfection, and perfection is not a detail.',
    quoteAuthor: 'Leonardo da Vinci',
    actionLabel: 'Fix & Retry',
    canRetry: true,
  },
  EMAIL_ALREADY_EXISTS: {
    title: 'This Email Is Already Taken',
    analogy: 'Like choosing a username — this one is already someone else\'s. Try signing in instead, or use a different email.',
    quote: 'Every new beginning comes from some other beginning\'s end.',
    quoteAuthor: 'Seneca',
    actionLabel: 'Try Logging In',
    canRetry: false,
  },
  INVALID_CREDENTIALS: {
    title: 'That Password Didn\'t Match',
    analogy: 'Like using the wrong key for a lock — the right one is somewhere in your memory! Try again or reset your password.',
    quote: 'Persistence is the key to unlocking every door.',
    quoteAuthor: 'Anonymous',
    actionLabel: 'Try Again',
    canRetry: true,
  },
  WRONG_CURRENT_PASSWORD: {
    title: 'Current Password Is Incorrect',
    analogy: 'For your security, we need to verify it\'s really you before changing anything — like confirming your identity at a bank.',
    quote: 'Security is not a product, but a process.',
    quoteAuthor: 'Bruce Schneier',
    actionLabel: 'Try Again',
    canRetry: true,
  },
  RESOURCE_NOT_FOUND: {
    title: 'We Couldn\'t Find That',
    analogy: 'Like looking for a book that\'s been returned to the library — it may have been moved or deleted.',
    quote: 'Not all who wander are lost — but this one might be.',
    quoteAuthor: 'J.R.R. Tolkien (adapted)',
    actionLabel: 'Go Back',
    canRetry: false,
  },
  ACCESS_DENIED: {
    title: 'This Area Is Off-Limits',
    analogy: 'Like walking into a backstage area without a pass — this section is for authorised crew only.',
    quote: 'Respect the boundaries that protect everyone.',
    quoteAuthor: 'Anonymous',
    actionLabel: 'Go Back',
    canRetry: false,
  },
  INTERNAL_ERROR: {
    title: 'Something Broke Backstage',
    analogy: 'Like a mic dropping mid-concert — something unexpected happened behind the scenes. Our team has been notified and is already on it.',
    quote: 'Every expert was once a beginner. We\'re getting there.',
    quoteAuthor: 'Helen Hayes',
    actionLabel: 'Try Again',
    canRetry: true,
  },
  UNKNOWN_ERROR: {
    title: 'Oops — That Was Unexpected',
    analogy: 'Sometimes technology has a hiccup — like autocorrect changing the wrong word. Let\'s try that again.',
    quote: 'The only way to do great work is to keep going.',
    quoteAuthor: 'Steve Jobs',
    actionLabel: 'Try Again',
    canRetry: true,
  },
};

/** Converts a backend ApiError into a display-ready UserFacingError */
export function mapApiErrorToUserFacing(apiError: ApiError): UserFacingError {
  const ux = ERROR_UX_MAP[apiError.code] ?? ERROR_UX_MAP['UNKNOWN_ERROR'];
  return {
    code: apiError.code,
    httpStatus: apiError.status,
    fieldErrors: apiError.details,
    ...ux,
  };
}
