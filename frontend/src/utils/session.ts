let identityKey = 'initializing';
export function getSessionId() { return identityKey; }
export function setSessionIdentity(value: string) { identityKey = value; }
