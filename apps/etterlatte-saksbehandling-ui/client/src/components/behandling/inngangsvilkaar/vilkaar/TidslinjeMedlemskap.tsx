import styled from 'styled-components'
import { format, sub } from 'date-fns'
import { aarIProsent, startdatoOffsetProsent } from './tidslinjeUtils'
import { Tidsperiode } from './Tidsperiode'
import {
  IAdresser,
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning, mapTilPerioderSeksAarFoerDoedsdato } from '../../felles/utils'

export interface IPeriode {
  periodeType: string
  innhold: IPeriodeInnhold
  kilde: any
}

export interface IPeriodeInnhold {
  fraDato: string
  tilDato?: string
  beskrivelse: string
  adresseINorge: boolean
  land?: string
}

export const TidslinjeMedlemskap = ({ vilkaar }: { vilkaar: IVilkaarsproving }) => {
  const avdoedDoedsdato: string | null = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.DOEDSDATO
  )?.opplysning?.doedsdato

  if (avdoedDoedsdato == null) {
    return <div style={{ color: 'red' }}>Dødsdato manger, kan ikke lage tidslinje</div>
  }

  const doedsdato = format(Date.parse(avdoedDoedsdato), 'yyyy-MM-dd')
  const seksAarTidligere = format(sub(Date.parse(doedsdato), { years: 6 }), 'yyyy-MM-dd')
  const femAarTidligere = format(sub(Date.parse(doedsdato), { years: 5 }), 'yyyy-MM-dd')
  const navnOgProsentPaaDatoer = aarIProsent(seksAarTidligere, doedsdato)
  const prosentStartDato = startdatoOffsetProsent(femAarTidligere, seksAarTidligere)

  console.log(navnOgProsentPaaDatoer)

  const adresseOpplysning: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.ADRESSER
  )
  const adresser: IAdresser | undefined = adresseOpplysning?.opplysning

  console.log('adresseOpplysning', adresseOpplysning)
  console.log('bosted', adresser?.bostedadresse)

  const perioderBosted: IPeriode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresseOpplysning?.opplysning?.bostedadresse,
    'Bostedsadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const perioderOpphold: IPeriode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresseOpplysning?.opplysning?.oppholdadresse,
    'Oppholdsadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const perioderKontakt: IPeriode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresseOpplysning?.opplysning?.kontaktadresse,
    'Kontaktadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const adresseperioder = [perioderBosted, perioderOpphold, perioderKontakt]
    .flat()
    .sort((a, b) => (new Date(b.innhold.fraDato) < new Date(a.innhold.fraDato) ? 1 : -1))

  console.log(adresseperioder)

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
      <div>
        {adresseperioder.map((periode, index) => (
          <Tidsperiode key={index} doedsdato={doedsdato} seksAarTidligere={seksAarTidligere} periode={periode} />
        ))}
      </div>
    </Tidslinje>
  )
}

export const Tidslinje = styled.div`
  margin-top: 110px;
  width: 100%;
  position: relative;
  margin-bottom: 30px;
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
