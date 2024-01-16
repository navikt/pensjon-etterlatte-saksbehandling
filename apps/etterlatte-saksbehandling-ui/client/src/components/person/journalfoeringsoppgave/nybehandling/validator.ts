import { fnrErGyldig } from '~utils/fnr'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'

export const gyldigBehandlingRequest = (request?: NyBehandlingRequest) => {
  const gyldigDatoOgSpraak = !!request?.mottattDato && !!request?.spraak

  return gyldigDatoOgSpraak && gyldigPersongalleri(request)
}

const gyldigPersongalleri = (request: NyBehandlingRequest) => {
  const persongalleri = request?.persongalleri

  if (!persongalleri) {
    return false
  }

  const avdoede = persongalleri.avdoed?.filter((fnr) => fnrErGyldig(fnr)) || []

  const gyldigAvdoed = avdoede.length >= 1

  const gyldigInnsender = !persongalleri.innsender || fnrErGyldig(persongalleri.innsender)

  return fnrErGyldig(persongalleri.soeker) && gyldigAvdoed && gyldigInnsender
}
