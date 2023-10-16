import { apiClient } from '~shared/api/apiClient'
import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'

export const hentSjekkliste = async (behandlingId: string) =>
  apiClient.get<ISjekkliste>(`/behandling/${behandlingId}/sjekkliste`)

export const opprettSjekkliste = async (behandlingId: string) =>
  apiClient.post<ISjekkliste>(`/behandling/${behandlingId}/sjekkliste`, {})

export const oppdaterSjekklisteItem = async (args: { behandlingId: string; item: ISjekklisteItem; checked: boolean }) =>
  apiClient.post<ISjekklisteItem>(`/behandling/${args.behandlingId}/sjekkliste/${args.item.id}`, {
    avkrysset: args.checked,
    versjon: args.item.versjon,
  })
