import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { useBehandling } from '~components/behandling/useBehandling'

// Dette er en midlertidig workaround i frontend fram til vi får noe mer rimelig rundt hva er vedtaksresultat i backend
// Laget som en felles greie slik at
//      1. steder som har noe forhold til vedtaksresultat kan bruke samme kilde
//      2. når vi har noe bedre i backend kan vi endre det her
export type VedtakResultat = 'opphoer' | 'innvilget' | 'avslag' | 'endring' | 'uavklart'

export function useVedtaksResultat(): VedtakResultat {
  const behandling = useBehandling()
  const behandlingType = behandling?.behandlingType
  const vilkaarsresultat = behandling?.vilkårsprøving?.resultat?.utfall

  switch (vilkaarsresultat) {
    case VilkaarsvurderingResultat.OPPFYLT:
      return behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? 'innvilget' : 'endring'
    case VilkaarsvurderingResultat.IKKE_OPPFYLT:
      return behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? 'avslag' : 'opphoer'
    default:
      return 'uavklart'
  }
}

export function formaterVedtaksResultat(resultat: VedtakResultat, virkningsdatoFormatert?: string): string {
  switch (resultat) {
    case 'innvilget':
      return `Innvilget fra ${virkningsdatoFormatert}`
    case 'avslag':
      return `Avslag`
    case 'opphoer':
      return `Opphør fra ${virkningsdatoFormatert}`
    case 'uavklart':
      return `Mangler opplysninger`
    case 'endring':
      return `Endring fra ${virkningsdatoFormatert}`
  }
}
