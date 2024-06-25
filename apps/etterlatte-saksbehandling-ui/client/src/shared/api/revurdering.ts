import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { RevurderingInfo, RevurderinginfoMedIdOgOpprettet } from '~shared/types/RevurderingInfo'
import { SakType } from '~shared/types/sak'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const lagreRevurderingInfo = ({
  behandlingId,
  begrunnelse,
  revurderingInfo,
}: {
  behandlingId: string
  begrunnelse?: string
  revurderingInfo: RevurderingInfo
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/revurdering/${behandlingId}/revurderinginfo`, {
    begrunnelse: begrunnelse ? begrunnelse : null,
    info: revurderingInfo,
  })
}
export const opprettRevurdering = async ({
  sakId,
  aarsak,
  paaGrunnAvHendelseId,
  paaGrunnAvOppgaveId,
  begrunnelse,
  fritekstAarsak,
}: {
  sakId: number
  aarsak: Revurderingaarsak
  paaGrunnAvHendelseId?: string
  paaGrunnAvOppgaveId?: string
  begrunnelse?: string
  fritekstAarsak?: string
}): Promise<ApiResponse<string>> => {
  return apiClient.post(`/revurdering/${sakId}`, {
    aarsak: aarsak,
    paaGrunnAvHendelseId: paaGrunnAvHendelseId,
    paaGrunnAvOppgaveId: paaGrunnAvOppgaveId,
    begrunnelse: begrunnelse,
    fritekstAarsak: fritekstAarsak,
  })
}

export const hentStoettedeRevurderinger = async ({
  sakType,
}: {
  sakType: SakType
}): Promise<ApiResponse<Array<Revurderingaarsak>>> => {
  return apiClient.get(`/stoettederevurderinger/${sakType}`)
}

export const hentRevurderingerForSakMedAarsak = async ({
  sakId,
  revurderingsaarsak,
}: {
  sakId: number
  revurderingsaarsak: Revurderingaarsak
}): Promise<ApiResponse<Array<RevurderinginfoMedIdOgOpprettet>>> => {
  return apiClient.get(`/revurdering/${sakId}/${revurderingsaarsak}`)
}

export const opprettOmgjoeringKlage = async (args: {
  sakId: number
  oppgaveId: string
}): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.post(`/revurdering/${args.sakId}/omgjoering-klage`, { oppgaveIdOmgjoering: args.oppgaveId })
}

export const opprettOmgjoeringFoerstegangsbehandling = async (args: {
  sakId: number
}): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.post(`/behandling/omgjoer-avslag-avbrudd/${args.sakId}`, {})
}
