import { apiClient } from '~shared/api/apiClient'
import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'

export const hentSjekkliste = async (behandlingId: string) => apiClient.get<ISjekkliste>(`/sjekkliste/${behandlingId}`)

export const opprettSjekkliste = async (behandlingId: string) =>
  apiClient.post<ISjekkliste>(`/sjekkliste/${behandlingId}`, {})

export const oppdaterSjekkliste = async (sjekkliste: ISjekkliste) =>
  apiClient.put<ISjekkliste>(`/sjekkliste/${sjekkliste.id}`, {
    kommentar: sjekkliste.kommentar,
    adresseForBrev: sjekkliste.adresseForBrev,
    kontonrRegistrert: sjekkliste.kontonrRegistrert,
    bekreftet: sjekkliste.bekreftet,
    versjon: sjekkliste.versjon,
  })

export const oppdaterSjekklisteItem = async (args: { behandlingId: string; item: ISjekklisteItem; checked: boolean }) =>
  apiClient.post<ISjekklisteItem>(`/sjekkliste/${args.behandlingId}/item/${args.item.id}`, {
    avkrysset: args.checked,
    versjon: args.item.versjon,
  })
