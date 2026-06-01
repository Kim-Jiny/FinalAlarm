export type AppErrorCode =
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'CONFLICT'
  | 'TEAM_NOT_MEMBER'
  | 'INVITE_EXPIRED'
  | 'INVITE_INVALID'
  | 'WINDOW_NOT_ACTIVE'
  | 'REQUEST_EXPIRED'
  | 'REQUEST_INVALID_STATE'
  | 'EVENT_INVALID_STATE'
  | 'MISSION_FAILED'
  | 'RATE_LIMITED';

const STATUS: Record<AppErrorCode, number> = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  VALIDATION_ERROR: 400,
  CONFLICT: 409,
  TEAM_NOT_MEMBER: 403,
  INVITE_EXPIRED: 410,
  INVITE_INVALID: 400,
  WINDOW_NOT_ACTIVE: 409,
  REQUEST_EXPIRED: 410,
  REQUEST_INVALID_STATE: 409,
  EVENT_INVALID_STATE: 409,
  MISSION_FAILED: 400,
  RATE_LIMITED: 429,
};

export class AppError extends Error {
  readonly code: AppErrorCode;
  readonly status: number;
  readonly details?: unknown;

  constructor(code: AppErrorCode, message: string, details?: unknown) {
    super(message);
    this.code = code;
    this.status = STATUS[code];
    this.details = details;
  }
}
