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

  const gyldigAvdoed = !persongalleri.avdoed?.length || persongalleri.avdoed.every((fnr) => fnrErGyldig(fnr))

  const gyldigInnsender = !persongalleri.innsender || fnrErGyldig(persongalleri.innsender)

  return fnrErGyldig(persongalleri.soeker) && gyldigAvdoed && gyldigInnsender
}
