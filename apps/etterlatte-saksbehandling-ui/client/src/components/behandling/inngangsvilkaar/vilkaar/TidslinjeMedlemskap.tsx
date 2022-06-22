import styled from 'styled-components'
import { format, sub } from 'date-fns'
import { aarIProsent } from './tidslinjeUtils'
import { Tidsperiode } from './Tidsperiode'
import {
  IAdresser,
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning, mapTilPerioderSeksAarFoerDoedsdato } from '../../felles/utils'

export interface Periode {
  periodeType: string
  innhold: any
  kilde: any
}

export const TidslinjeMedlemskap = ({ vilkaar }: { vilkaar: IVilkaarsproving }) => {
  const avdoedDoedsdato: string | null = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.DOEDSDATO
  )?.opplysning?.doedsdato

  if (avdoedDoedsdato == null) {
    return <div>Dødsdato manger, kan ikke lage tidslinje</div>
  }

  const doedsdato = format(Date.parse(avdoedDoedsdato), 'yyyy-MM-dd')
  const seksAarTidligere = format(sub(Date.parse(doedsdato), { years: 6 }), 'yyyy-MM-dd')
  const navnOgProsentPaaDatoer = aarIProsent(seksAarTidligere!!, doedsdato)

  const adresseOpplysning: IKriterieOpplysning | undefined = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR,
    KriterieOpplysningsType.ADRESSER
  )
  const adresser: IAdresser | undefined = adresseOpplysning?.opplysning?.adresser

  const perioderBosted: Periode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresser?.bostedadresse,
    'Bostedsadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const perioderOpphold: Periode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresser?.oppholdadresse,
    'Oppholdsadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const perioderKontakt: Periode[] = mapTilPerioderSeksAarFoerDoedsdato(
    adresser?.kontaktadresse,
    'Kontaktadresse',
    seksAarTidligere,
    adresseOpplysning?.kilde
  )

  const adresseperioder = [perioderBosted, perioderOpphold, perioderKontakt].flat()

  console.log(adresseperioder)

  return (
    <Tidslinje>
      <Grid>
        <GridBorder style={{ top: '10px' }}>&nbsp;</GridBorder>
        <GridDatoer leftmargin={'0px'}>{format(Date.parse(seksAarTidligere), 'MM.yyyy')}</GridDatoer>
        {navnOgProsentPaaDatoer.map((aar) => (
          <GridDatoerWrapper prosent={aar[1]} key={aar[1].toString()}>
            <GridDatoer leftmargin={'-16px'}>{aar[0]}</GridDatoer>
            <GridGraaSirkel>&nbsp;</GridGraaSirkel>
          </GridDatoerWrapper>
        ))}
        <GridDoedsdato>
          <GridDatoer leftmargin={'-150px'}>dødsdato: {format(Date.parse(doedsdato), 'MM.yyyy')}</GridDatoer>
        </GridDoedsdato>
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
  margin-top: 60px;
  width: 100%;
  position: relative;
  margin-bottom: 30px;
`

export const GridGraaSirkel = styled.div`
  width: 20px;
  height: 20px;
  background: #ccc;
  border-radius: 99px;
  margin-left: -10px;
`

export const Grid = styled.div`
  width: 100%;
  font-weight: bold;
  margin-bottom: 40px;
`

const GridDatoerWrapper = styled.div<{ prosent: string }>`
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

const GridDoedsdato = styled.div`
  left: 100%;
  position: absolute;
  top: 30px;
  bottom: 0;
`

const GridBorder = styled.div`
  position: absolute;
  left: 0;
  right: 0;
  border-bottom: 1px solid #ccc;
`
