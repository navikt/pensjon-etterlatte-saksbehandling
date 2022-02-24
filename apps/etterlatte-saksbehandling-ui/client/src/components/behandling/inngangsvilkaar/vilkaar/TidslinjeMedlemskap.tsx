import styled from 'styled-components'
import moment from 'moment'
import { aarIProsent, tidsperiodeProsent, startdatoOffsetProsent } from './tidslinjeUtils'
import { HomeIcon } from '../../../../shared/icons/homeIcon'
import { OfficeIcon } from '../../../../shared/icons/officeIcon'

export const TidslinjeMedlemskap = () => {
  const doedsdato = moment('2022-06-01', 'YYYY-MM-DD').format('DD.MM.YYYY').toString()
  const femAarTidligere = moment('2022-06-01', 'YYYY-MM-DD').subtract(5, 'years').format('DD.MM.YYYY').toString()

  const navnOgProsentPaaDatoer = aarIProsent(femAarTidligere, doedsdato)

  const mockdata = [
    {
      periodeType: 'jobb',
      innhold: {
        fraDato: '01.02.2019',
        tilDato: '01.11.2021',
        beskrivelse: 'Tonsenhagenskole, 100% stilling',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
    {
      periodeType: 'bosted',
      innhold: {
        fraDato: '01.02.2016',
        tilDato: '01.03.2021',
        beskrivelse: 'Plogveien 54, 0458 Oslo',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
    {
      periodeType: 'bosted',
      innhold: {
        fraDato: '01.02.2020',
        tilDato: '01.03.2023',
        beskrivelse: 'Plogveien 54, 0458 Oslo',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
  ]

  return (
    <Tidslinje>
      <Grid>
        <GridBorder style={{ top: '10px' }}>&nbsp;</GridBorder>
        <GridDatoer leftmargin={'0px'}>{femAarTidligere}</GridDatoer>
        {navnOgProsentPaaDatoer.map((aar) => (
          <GridDatoerWrapper prosent={aar[1]} key={aar[1].toString()}>
            <GridDatoer leftmargin={'-16px'}>{aar[0]}</GridDatoer>
            <GridGraaSirkel>&nbsp;</GridGraaSirkel>
          </GridDatoerWrapper>
        ))}
        <GridDoedsdato>
          <GridDatoer leftmargin={'-150px'}>dødsdato: {doedsdato}</GridDatoer>
        </GridDoedsdato>
        <GridBorder style={{ bottom: '-30px' }}>&nbsp;</GridBorder>
      </Grid>
      <div>
        {mockdata.map((periode, index) => (
          <Periode
            key={index}
            doedsdato={doedsdato}
            femAarTidligere={femAarTidligere}
            periode={periode}
            index={index}
          />
        ))}
      </div>
    </Tidslinje>
  )
}

const Periode = ({
  doedsdato,
  femAarTidligere,
  periode,
  index,
}: {
  doedsdato: string
  femAarTidligere: string
  periode: any
  index: number
}) => {
  const lengdePeriode = tidsperiodeProsent(periode.innhold.fraDato, periode.innhold.tilDato, doedsdato, femAarTidligere)
  const startDatoOffset = startdatoOffsetProsent(periode.innhold.fraDato, femAarTidligere)

  const colors = ['#a6cbdc', '#a5a5d7', '#c4adde', '#d7a9c6']

  return (
    <Rad style={{ left: startDatoOffset, width: lengdePeriode, backgroundColor: colors[index % colors.length] }}>
      <InnholdWrapper>
        <Ikon>{periode.periodeType === 'jobb' ? <OfficeIcon /> : <HomeIcon />}</Ikon>
        <div>
          <InnholdDatoFraTil>
            {periode.innhold.fraDato} - {periode.innhold.tilDato}
          </InnholdDatoFraTil>
          <InnholdBeskrivelse>{periode.innhold.beskrivelse}</InnholdBeskrivelse>
          <InnholdKilde>{periode.innhold.kilde}</InnholdKilde>
        </div>
      </InnholdWrapper>
    </Rad>
  )
}

export const Ikon = styled.div`
  display: flex;
  justify-content: left;
  padding: 10px;

  svg {
    width: 1.8em;
    height: 1.8em;
  }
`

export const InnholdWrapper = styled.div`
  display: flex;
  justify-content: left;
`

export const Tidslinje = styled.div`
  margin-top: 40px;
  width: 100%;
  position: relative;
  margin-bottom: 30px;
`

export const Rad = styled.div`
  position: relative;
  margin-bottom: 10px;
  border-radius: 4px;
  padding-bottom: 10px;
`

export const InnholdDatoFraTil = styled.div`
  padding-top: 5px;
  padding-left: 10px;
  font-weight: bold;
`
export const InnholdBeskrivelse = styled.div`
  padding-left: 5px;
`

export const InnholdKilde = styled.div`
  padding-left: 5px;
  color: #676363;
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
