import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { useBehandling } from '~components/behandling/useBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

// Dette er en midlertidig workaround i frontend fram til vi får noe mer rimelig rundt hva er vedtaksresultat i backend
// Laget som en felles greie slik at
//      1. steder som har noe forhold til vedtaksresultat kan bruke samme kilde
//      2. når vi har noe bedre i backend kan vi endre det her
export type VedtakResultat = 'opphoer' | 'innvilget' | 'avslag' | 'endring'

export function useVedtaksResultat(): VedtakResultat | null {
  const behandling = useBehandling()
  const behandlingType = behandling?.behandlingType
  const vilkaarsresultat = behandling?.vilkaarsvurdering?.resultat?.utfall
  const foerstegangsbehandling = behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING

  switch (vilkaarsresultat) {
    case VilkaarsvurderingResultat.OPPFYLT:
      return foerstegangsbehandling ? 'innvilget' : 'endring'
    case VilkaarsvurderingResultat.IKKE_OPPFYLT:
      const revurderingNySoeknad =
        behandlingType === IBehandlingsType.REVURDERING &&
        behandling?.revurderingsaarsak == Revurderingaarsak.NY_SOEKNAD

      if (foerstegangsbehandling || revurderingNySoeknad) return 'avslag'
      else return 'opphoer'
    default:
      return null
  }
}

export function formaterVedtaksResultat(
  resultat: VedtakResultat | null,
  virkningsdatoFormatert?: string
): string | null {
  switch (resultat) {
    case 'innvilget':
      return `Innvilget fra ${virkningsdatoFormatert}`
    case 'avslag':
      return `Avslag`
    case 'opphoer':
      return `Opphør fra ${virkningsdatoFormatert}`
    case 'endring':
      return `Endring fra ${virkningsdatoFormatert}`
    case null:
      return 'Mangler vedtak resultat'
  }
}
