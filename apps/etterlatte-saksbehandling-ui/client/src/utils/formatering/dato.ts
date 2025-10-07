import { add, format } from 'date-fns'
import { nb } from 'date-fns/locale'

export enum DatoFormat {
  AAR_MAANED_DAG = 'yyyy-MM-dd',
  DAG_MAANED_AAR = 'dd.MM.yyyy',
  MAANED_AAR = 'MM.yyyy',
  MAANEDNAVN_AAR = 'MMMM yyyy',
}

export const formaterDato = (dato: string | Date) => format(dato, DatoFormat.DAG_MAANED_AAR).toString()

export const maanedNavn = (dato: string | Date) => format(dato, 'LLLL', { locale: nb }).toString()

export const formaterDatoMedFallback = (dato?: string | Date, fallback?: string) =>
  dato ? format(dato, DatoFormat.DAG_MAANED_AAR).toString() : fallback

export const formaterTilISOString = (date: Date | string): string => {
  return format(date, DatoFormat.AAR_MAANED_DAG)
}

export const formaterMaanedAar = (dato: string | Date) => format(dato, DatoFormat.MAANED_AAR).toString()

export const formaterMaanednavnAar = (dato: string | Date) => format(dato, DatoFormat.MAANEDNAVN_AAR).toString()

export const formaterKanskjeStringDato = (dato?: string): string =>
  formaterKanskjeStringDatoMedFallback('Ukjent dato', dato)

export const formaterDatoStrengTilLocaleDateTime = (dato: string) => new Date(dato).toISOString().replace('Z', '')

export const formaterKanskjeStringDatoMedFallback = (fallback: string, dato?: string): string =>
  dato ? formaterDato(dato) : fallback

export const formaterTidspunktTimeMinutterSekunder = (dato: Date) => format(new Date(dato), 'HH:mm:ss').toString()

export const formaterDatoMedTidspunkt = (dato: Date) => format(new Date(dato), 'dd.MM.yyyy HH:mm').toString()

export const formaterDatoMedKlokkeslett = (dato: Date | string) =>
  format(new Date(dato), "dd.MM.yyyy 'kl.' HH:mm").toString()

export const aarFraDatoString = (dato: string) => new Date(dato).getFullYear()

export const maanedFraDatoString = (dato: string) => new Date(dato).getMonth()

export const kanskjeMaanedFraDatoString = (dato?: string) => (dato ? maanedFraDatoString(dato) : undefined)

export const datoIMorgen = (): Date => {
  return add(new Date(), { days: 1 })
}

export const datoToAarFramITid = (): Date => {
  return add(new Date(), { years: 2 })
}
