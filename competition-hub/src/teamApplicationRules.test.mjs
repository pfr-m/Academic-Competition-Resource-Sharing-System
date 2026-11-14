import test from 'node:test'
import assert from 'node:assert/strict'

test('administrator can apply to another user team even when contact is visible', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false }), true)
})

test('team leader cannot apply to own team', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ leaderId: 20, currentUserId: 20, hasActiveApplication: false, isMember: false }), false)
})

test('another users application does not block current user', async () => {
  const { getCurrentTeamParticipation } = await import('./teamApplicationRules.ts')
  assert.deepEqual(
    getCurrentTeamParticipation([{ id: 1, applicantId: 99, status: 'PENDING' }], [], 20),
    { hasActiveApplication: false, applicationId: undefined, isMember: false },
  )
})

test('current users pending application blocks another application', async () => {
  const { getCurrentTeamParticipation } = await import('./teamApplicationRules.ts')
  assert.deepEqual(
    getCurrentTeamParticipation([{ id: 7, applicantId: 20, status: 'PENDING' }], [], 20),
    { hasActiveApplication: true, applicationId: 7, isMember: false },
  )
})

test('current team member cannot apply again', async () => {
  const { getCurrentTeamParticipation, canApplyToTeam } = await import('./teamApplicationRules.ts')
  const participation = getCurrentTeamParticipation([], [{ userId: 20 }], 20)
  assert.equal(canApplyToTeam({ leaderId: 10, currentUserId: 20, ...participation }), false)
})
