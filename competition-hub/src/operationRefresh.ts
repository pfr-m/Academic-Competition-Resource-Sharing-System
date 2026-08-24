export interface MutationRefreshResult<T> {
  value: T
  refreshFailed: boolean
}

export async function runMutationAndRefresh<T>(
  mutation: () => Promise<T>,
  refreshTasks: Array<() => Promise<unknown>>,
): Promise<MutationRefreshResult<T>> {
  const value = await mutation()
  const refreshResults = await Promise.allSettled(refreshTasks.map((task) => task()))
  return {
    value,
    refreshFailed: refreshResults.some((result) => result.status === 'rejected'),
  }
}
