import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const skalViseTrygdeavtale = (behandling: IDetaljertBehandling): boolean => {
  return behandling.boddEllerArbeidetUtlandet?.vurdereAvdoedesTrygdeavtale === true
}
