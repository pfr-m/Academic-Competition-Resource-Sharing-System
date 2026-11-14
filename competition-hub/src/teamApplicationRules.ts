export type TeamStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type TeamApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type TeamApplicationRecord = {
  id?: number
  applicantId: number
  status: TeamApplicationStatus
}

export type TeamMemberRecord = {
  userId: number
}

export function getCurrentTeamParticipation(
  applications: TeamApplicationRecord[],
  members: TeamMemberRecord[],
  currentUserId: number,
) {
  const activeApplication = applications.find((application) =>
    application.applicantId === currentUserId
      && (application.status === 'PENDING' || application.status === 'APPROVED'),
  )
  return {
    hasActiveApplication: Boolean(activeApplication),
    applicationId: activeApplication?.id,
    isMember: members.some((member) => member.userId === currentUserId),
  }
}

export function getPendingApprovalTarget<T extends TeamApplicationRecord>(applications: T[]) {
  return applications.find((application) => application.status === 'PENDING')
}

export function canApplyToTeam(input: {
  teamStatus: TeamStatus
  leaderId: number
  currentUserId: number
  hasActiveApplication: boolean
  isMember: boolean
}) {
  return input.teamStatus === 'APPROVED'
    && input.leaderId !== input.currentUserId
    && !input.hasActiveApplication
    && !input.isMember
}
