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

export const validerFnrValgfri = (fnr: string | null): string | undefined => {
  if (fnr && !fnrErGyldig(fnr)) {
    return 'Fødselsnummer er på ugyldig format'
  }
  return undefined
}

export const validateFnrObligatorisk = (fnr: string | undefined): string | undefined => {
  if (!fnr) {
    return 'Fødselsnummer må være satt'
  } else if (!fnrErGyldig(fnr)) {
    return 'Fødselsnummer er på ugyldig format'
  }
  return undefined
}

export const validerStringNumber = (stringNumber: string | undefined): string | undefined => {
  if (Number.isNaN(stringNumber)) return 'Må være heltall'
  return undefined
}
