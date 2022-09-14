import styled from 'styled-components'
import { format, sub } from 'date-fns'
import { aarIProsent, numberToProsentString, startdatoOffsetProsent } from './tidslinjeUtils'
import {
  IAdresse,
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../../../store/reducers/BehandlingReducer'

import { TidslinjeRad } from './TidslinjeRad'
import { ITidslinjePeriode, IVurdertPeriode } from '../../types'
import {
  mapAdresseTilPerioderSeksAarFoerDoedsdato,
  mapGapsTilPerioder,
  mapYtelseTilPerioderSeksAarFoerDoedsdato,
} from '../../utils'
import { hentKriterierMedOpplysning, hentNorskeAdresser, hentUtlandskeAdresser } from '../../../felles/utils'

export const TidslinjeMedlemskap = ({ vilkaar }: { vilkaar: IVilkaarsproving }) => {
  const avdoedDoedsdato: string | null = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.DOEDSDATO
  )?.opplysning?.doedsdato

  if (avdoedDoedsdato == null) {
    return <div style={{ color: 'red' }}>Dødsdato manger, kan ikke lage tidslinje</div>
  }

  const doedsdato = format(Date.parse(avdoedDoedsdato), 'yyyy-MM-dd')
  const seksAarTidligere = format(sub(Date.parse(doedsdato), { years: 6 }), 'yyyy-MM-dd')
  const femAarTidligere = format(sub(Date.parse(doedsdato), { years: 5 }), 'yyyy-MM-dd')
  const navnOgProsentPaaDatoer = aarIProsent(seksAarTidligere, doedsdato)
  const prosentStartDato = numberToProsentString(startdatoOffsetProsent(femAarTidligere, seksAarTidligere))

  const bostedsadresseOpplysning: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.ADRESSELISTE
  )

  const adressegaps: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.ADRESSE_GAPS
  )

  const perioderOpplysning: IVurdertPeriode[] = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_OPPFYLLER_MEDLEMSKAP,
    KriterieOpplysningsType.AVDOED_MEDLEMSKAP
  )?.opplysning?.perioder

  const bostedadresser: IAdresse[] | undefined = bostedsadresseOpplysning?.opplysning?.adresser
  const norskeBostedsadresser = hentNorskeAdresser(bostedadresser)
  const utlandskeBostedsadresser = hentUtlandskeAdresser(bostedadresser)
  const periodeNorskeBostedGaps: ITidslinjePeriode[] = mapGapsTilPerioder(adressegaps?.opplysning, adressegaps?.kilde)
  const perioderNorskeBosted: ITidslinjePeriode[] = mapAdresseTilPerioderSeksAarFoerDoedsdato(
    norskeBostedsadresser,
    'Bostedsadresse',
    seksAarTidligere,
    bostedsadresseOpplysning?.kilde
  )
  const perioderNorskeBostedMedGaps = [perioderNorskeBosted, periodeNorskeBostedGaps]
    .flat()
    .sort((a, b) => (new Date(b.innhold.fraDato) < new Date(a.innhold.fraDato) ? 1 : -1))

  const perioderUtlandskeBosted: ITidslinjePeriode[] = mapAdresseTilPerioderSeksAarFoerDoedsdato(
    utlandskeBostedsadresser,
    'Bostedsadresse',
    seksAarTidligere,
    bostedsadresseOpplysning?.kilde
  )

  const oppholdsadresseOpplysning: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER,
    KriterieOpplysningsType.ADRESSELISTE
  )

  const kontaktadresseOpplysning: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_KUN_NORSKE_KONTAKTADRESSER,
    KriterieOpplysningsType.ADRESSELISTE
  )

  const perioderOpphold: ITidslinjePeriode[] = mapAdresseTilPerioderSeksAarFoerDoedsdato(
    oppholdsadresseOpplysning?.opplysning?.adresser,
    'Oppholdsadresse',
    seksAarTidligere,
    oppholdsadresseOpplysning?.kilde
  )

  const perioderKontakt: ITidslinjePeriode[] = mapAdresseTilPerioderSeksAarFoerDoedsdato(
    kontaktadresseOpplysning?.opplysning?.adresser,
    'Kontaktadresse',
    seksAarTidligere,
    kontaktadresseOpplysning?.kilde
  )

  const medlemskapsPerioder: ITidslinjePeriode[] = mapYtelseTilPerioderSeksAarFoerDoedsdato(
    perioderOpplysning,
    seksAarTidligere
  )

  return (
    <Tidslinje>
      <Grid>
        <GridStartSluttdato style={{ left: prosentStartDato }}>
          <GridDatoer leftmargin={'-30px'}>{format(Date.parse(femAarTidligere), 'MM.yyyy')}</GridDatoer>
          <GridSirkel>&nbsp;</GridSirkel>
        </GridStartSluttdato>
        <GridStartSluttdato style={{ left: '100%' }}>
          <GridDatoer leftmargin={'-30px'}>{format(Date.parse(doedsdato), 'MM.yyyy')}</GridDatoer>
          <GridSirkel>&nbsp;</GridSirkel>
        </GridStartSluttdato>
        <GridBorder style={{ top: '6px' }}>&nbsp;</GridBorder>
        <GridDatoer leftmargin={'0px'}>{format(Date.parse(seksAarTidligere), 'MM.yyyy')}</GridDatoer>
        {navnOgProsentPaaDatoer.map((aar) => (
          <GridDatoerAarstallWrapper prosent={aar[1]} key={aar[1].toString()}>
            <GridDatoer leftmargin={'-16px'}>{aar[0]}</GridDatoer>
          </GridDatoerAarstallWrapper>
        ))}

        <GridBorder style={{ bottom: '-30px' }}>&nbsp;</GridBorder>
      </Grid>
      <TidslinjeRad perioder={perioderNorskeBostedMedGaps} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} />
      <TidslinjeRad perioder={perioderUtlandskeBosted} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} />
      <TidslinjeRad perioder={perioderOpphold} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} />
      <TidslinjeRad perioder={perioderKontakt} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} />
      {medlemskapsPerioder.map((periode, index) => (
        <TidslinjeRad perioder={[periode]} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} key={index} />
      ))}
    </Tidslinje>
  )
}

export const Tidslinje = styled.div`
  margin-top: 110px;
  width: 100%;
  position: relative;
  margin-bottom: 50px;
`

export const GridSirkel = styled.div`
  width: 20px;
  height: 20px;
  background: #005b82;
  border-radius: 99px;
  margin-left: -10px;
`

export const Grid = styled.div`
  width: 100%;
  font-weight: bold;
  margin-bottom: 40px;
`

const GridDatoerAarstallWrapper = styled.div<{ prosent: string }>`
  left: ${(props) => props.prosent};
  position: absolute;
  top: 30px;
  bottom: -30px;
  border-left: thin solid #ccc;
`

const GridDatoer = styled.div<{ leftmargin: string }>`
  margin-top: -30px;
  margin-left: ${(props) => props.leftmargin};
`

const GridStartSluttdato = styled.div`
  position: absolute;
  top: -30px;
  bottom: -30px;
  border-left: thin solid #005b82;
`

const GridBorder = styled.div`
  position: absolute;
  left: 0;
  right: 0;
  border-bottom: 1px solid #ccc;
`
