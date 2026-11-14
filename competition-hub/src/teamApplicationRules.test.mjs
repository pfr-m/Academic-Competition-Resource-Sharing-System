import test from 'node:test'
import assert from 'node:assert/strict'

test('administrator can apply to another user team even when contact is visible', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'APPROVED', leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false }), true)
})

test('approved team permits an otherwise eligible user to apply', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'APPROVED', leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false }), true)
})

test('pending team does not permit an otherwise eligible user to apply', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'PENDING', leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false }), false)
})

test('rejected team does not permit an otherwise eligible user to apply', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'REJECTED', leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false }), false)
})

test('super-admin-equivalent role is irrelevant to pure team application eligibility', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'APPROVED', leaderId: 10, currentUserId: 20, hasActiveApplication: false, isMember: false, role: 'super-admin' }), true)
})

test('team leader cannot apply to own team', async () => {
  const { canApplyToTeam } = await import('./teamApplicationRules.ts')
  assert.equal(canApplyToTeam({ teamStatus: 'APPROVED', leaderId: 20, currentUserId: 20, hasActiveApplication: false, isMember: false }), false)
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

test('current users approved application blocks another application', async () => {
  const { getCurrentTeamParticipation } = await import('./teamApplicationRules.ts')
  assert.deepEqual(
    getCurrentTeamParticipation([{ id: 8, applicantId: 20, status: 'APPROVED' }], [], 20),
    { hasActiveApplication: true, applicationId: 8, isMember: false },
  )
})

test('another users pending application remains an approval target without blocking current user apply state', async () => {
  const { getCurrentTeamParticipation, getPendingApprovalTarget } = await import('./teamApplicationRules.ts')
  const applications = [{ id: 9, applicantId: 99, status: 'PENDING' }]

  assert.deepEqual(getPendingApprovalTarget(applications), applications[0])
  assert.deepEqual(
    getCurrentTeamParticipation(applications, [], 20),
    { hasActiveApplication: false, applicationId: undefined, isMember: false },
  )
})

test('current team member cannot apply again', async () => {
  const { getCurrentTeamParticipation, canApplyToTeam } = await import('./teamApplicationRules.ts')
  const participation = getCurrentTeamParticipation([], [{ userId: 20 }], 20)
  assert.equal(canApplyToTeam({ teamStatus: 'APPROVED', leaderId: 10, currentUserId: 20, ...participation }), false)
})
