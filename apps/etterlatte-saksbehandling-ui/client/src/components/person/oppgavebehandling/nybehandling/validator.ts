import { GYLDIG_FNR } from '~utils/fnr'
import { SakType } from '~shared/types/sak'
import { Persongalleri } from '~shared/types/Person'

export const gyldigPersongalleri = (sakType: SakType, persongalleri?: Persongalleri) => {
  if (!persongalleri) {
    return false
  }

  const avdoede = persongalleri.avdoed?.filter((fnr) => GYLDIG_FNR(fnr)) || []
  const gjenlevende = persongalleri.gjenlevende?.filter((fnr) => GYLDIG_FNR(fnr)) || []
  const antallGjenlevOgAvdoed = avdoede.length + gjenlevende.length

  let gyldigGjenlevOgAvdoed = false
  if (sakType === SakType.BARNEPENSJON) {
    gyldigGjenlevOgAvdoed = antallGjenlevOgAvdoed === 2
  } else if (sakType === SakType.OMSTILLINGSSTOENAD) {
    gyldigGjenlevOgAvdoed = antallGjenlevOgAvdoed === 1
  }

  return (
    !!persongalleri && GYLDIG_FNR(persongalleri.soeker) && GYLDIG_FNR(persongalleri.innsender) && gyldigGjenlevOgAvdoed
  )
}
