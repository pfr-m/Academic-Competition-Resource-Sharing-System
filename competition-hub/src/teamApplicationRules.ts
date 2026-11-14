export type TeamApplicationRecord = {
  id?: number
  applicantId: number
  status: string
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

export function canApplyToTeam(input: {
  leaderId: number
  currentUserId: number
  hasActiveApplication: boolean
  isMember: boolean
}) {
  return input.leaderId !== input.currentUserId
    && !input.hasActiveApplication
    && !input.isMember
}
