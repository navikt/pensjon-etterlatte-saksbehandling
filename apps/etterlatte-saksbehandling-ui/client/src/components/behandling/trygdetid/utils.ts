import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const skalViseTrygdeavtale = (behandling: IDetaljertBehandling): boolean => {
  return (
    behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale ||
    (behandling.behandlingType === IBehandlingsType.REVURDERING &&
      behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND)
  )
}
