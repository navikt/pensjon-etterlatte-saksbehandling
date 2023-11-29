import { fnrErGyldig } from '~utils/fnr'
import { SakType } from '~shared/types/sak'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'

export const gyldigBehandlingRequest = (request?: NyBehandlingRequest) => {
  const gyldigDatoOgSpraak = !!request?.mottattDato && !!request?.spraak

  return gyldigDatoOgSpraak && gyldigPersongalleri(request)
}

const gyldigPersongalleri = (request: NyBehandlingRequest) => {
  const sakType = request.sakType
  const persongalleri = request?.persongalleri

  if (!persongalleri) {
    return false
  }

  const avdoede = persongalleri.avdoed?.filter((fnr) => fnrErGyldig(fnr)) || []
  const gjenlevende = persongalleri.gjenlevende?.filter((fnr) => fnrErGyldig(fnr)) || []
  const antallGjenlevOgAvdoed = avdoede.length + gjenlevende.length

  let gyldigGjenlevOgAvdoed = false
  if (sakType === SakType.BARNEPENSJON) {
    gyldigGjenlevOgAvdoed = antallGjenlevOgAvdoed === 2
  } else if (sakType === SakType.OMSTILLINGSSTOENAD) {
    gyldigGjenlevOgAvdoed = antallGjenlevOgAvdoed === 1
  }

  const gyldigInnsender = !persongalleri.innsender || fnrErGyldig(persongalleri.innsender)

  return fnrErGyldig(persongalleri.soeker) && gyldigGjenlevOgAvdoed && gyldigInnsender
}
