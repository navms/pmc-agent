const USER_KEY = 'pmc-agent-user-id'

export function getUserId(): string {
  const existing = localStorage.getItem(USER_KEY)
  if (existing) {
    return existing
  }
  const id = crypto.randomUUID()
  localStorage.setItem(USER_KEY, id)
  return id
}
