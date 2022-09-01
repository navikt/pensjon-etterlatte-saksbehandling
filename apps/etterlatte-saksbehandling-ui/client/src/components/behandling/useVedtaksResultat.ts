import { useContext } from 'react'
import { AppContext } from '../../store/AppContext'
import { IBehandlingsType, VurderingsResultat } from '../../store/reducers/BehandlingReducer'

// Dette er en midlertidig workaround i frontend fram til vi får noe mer rimelig rundt hva er vedtaksresultat i backend
// Laget som en felles greie slik at
//      1. steder som har noe forhold til vedtaksresultat kan bruke samme kilde
//      2. når vi har noe bedre i backend kan vi endre det her
export type VedtakResultat = 'opphoer' | 'innvilget' | 'avslag' | 'endring' | 'uavklart'

export function useVedtaksResultat(): VedtakResultat {
  const { state } = useContext(AppContext)
  const behandlingType = state.behandlingReducer?.behandlingType
  const vilkaarsresultat =
    state.behandlingReducer?.vilkårsprøving?.resultat ?? VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING

  switch (vilkaarsresultat) {
    case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
      return 'uavklart'
    case VurderingsResultat.OPPFYLT:
      return behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? 'innvilget' : 'endring'
    case VurderingsResultat.IKKE_OPPFYLT:
      return behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? 'avslag' : 'opphoer'
  }
}
