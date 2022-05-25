import { differenceInYears, add, startOfMonth } from 'date-fns'
import { IPersonOpplysningFraPdl } from '../types'
import { WarningText } from '../../../shared/styled'
import { GyldigFramsattType, VurderingsResultat, IGyldighetproving } from '../../../store/reducers/BehandlingReducer'

export const sjekkDataFraSoeknadMotPdl = (dataFraPdl: string, dataFraSoeknad: string) => {
  return dataFraSoeknad === dataFraPdl || dataFraSoeknad === null ? (
    <span>{dataFraPdl}</span>
  ) : (
    <WarningText>
      <div>{dataFraPdl}</div>
      {dataFraSoeknad} (fra søknad)
    </WarningText>
  )
}

export const sjekkPersonFraSoeknadMotPdl = (personFraPdl: IPersonOpplysningFraPdl, personFraSoeknad: any) => {
  //TODO: hvis fnr er likt, men navn forskjellig, hva skal vises?
  return personFraSoeknad.foedselsnummer === personFraPdl.foedselsnummer ? (
    <span>
      {personFraPdl?.fornavn} {personFraPdl?.etternavn}
    </span>
  ) : (
    <WarningText>
      <div>
        {personFraPdl?.fornavn} {personFraPdl?.etternavn}
      </div>
      {personFraSoeknad?.fornavn} {personFraSoeknad?.etternavn} (fra søknad)
    </WarningText>
  )
}

export const sjekkAdresseGjenlevendeISoeknadMotPdl = (adresseFraSoeknad: string, adresseFraPdl: string): boolean => {
  return adresseFraPdl !== adresseFraSoeknad
}

export const hentAlderVedDoedsdato = (foedselsdato: string, doedsdato: string): string => {
  return Math.floor(differenceInYears(new Date(doedsdato), new Date(foedselsdato))).toString()
}

export const hentVirkningstidspunkt = (doedsdato: string, mottattDato: string): string => {
  if (sjekkDodsfallMerEnn3AarSiden(doedsdato, mottattDato)) {
    /*
    TODO se mere på utregning av virkningstidspunkt. for gjenlevende er det 3 måneder(?) og barnepensjon 1,
    men er det mere enn 3 år siden dødsfall, går man bare 3 år tilbake. Denne burde kanskje regnes ut i backend og ligge
    lagret på behandlingen, og så må saksbehandler ha mulighet til å endre den hvis det er behov, på vilkårssiden der den vises.
    */
    return startOfMonth(add(new Date(doedsdato), { years: 3 })).toString()
  }
  return startOfMonth(add(new Date(doedsdato), { months: 1 })).toString()
}

export const sjekkDodsfallMerEnn3AarSiden = (doedsdato: string, mottattDato: string): boolean => {
  return differenceInYears(new Date(mottattDato), new Date(doedsdato)) > 3
}

export function mapGyldighetstyperTilTekst(gyldig: IGyldighetproving): String | undefined {
  let svar

  if (gyldig.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'Nei. innsender har ikke forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'Mangler info'
    } else {
      svar = undefined
    }
  } else if (gyldig.navn === GyldigFramsattType.INNSENDER_ER_FORELDER) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'Nei, innsender er ikke gjenlevende forelder. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'Mangler info'
    } else {
      svar = undefined
    }
  } else {
    svar = undefined
  }
  return svar
}

export function hentGyldigBostedTekst(gyldig: IGyldighetproving): String | undefined {
  let svar
  if (gyldig.navn === GyldigFramsattType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'barn og gjenlevende forelder bor ikke på samme adresse'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'Mangler info'
    }
  } else {
    svar = undefined
  }
  return svar
}
