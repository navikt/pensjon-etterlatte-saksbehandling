import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const skalViseTrygdeavtale = (behandling: IDetaljertBehandling): boolean => {
  console.log(
    'behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale: ',
    behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale
  )
  console.log(
    'behandling.behandlingType === IBehandlingsType.REVURDERING: ',
    behandling.behandlingType === IBehandlingsType.REVURDERING
  )
  console.log(
    'behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND: ',
    behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND
  )
  return (
    behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale ||
    (behandling.behandlingType === IBehandlingsType.REVURDERING &&
      behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND)
  )
}
