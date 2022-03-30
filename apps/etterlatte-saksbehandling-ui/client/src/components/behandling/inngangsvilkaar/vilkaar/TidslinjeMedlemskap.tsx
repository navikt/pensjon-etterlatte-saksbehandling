import styled from 'styled-components'
import moment from 'moment'
import { aarIProsent } from './tidslinjeUtils'
import { Tidsperiode } from './Tidsperiode'

export const TidslinjeMedlemskap = () => {
  const doedsdato = moment('2022-06-01', 'YYYY-MM-DD')
  const femAarTidligere = moment('2022-06-01', 'YYYY-MM-DD').subtract(5, 'years')

  const navnOgProsentPaaDatoer = aarIProsent(femAarTidligere, doedsdato)

  const mockdata = [
    {
      periodeType: 'jobb',
      innhold: {
        fraDato: '2019-02-01',
        tilDato: '2021-01-11',
        beskrivelse: 'Tonsenhagenskole, 100% stilling',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
    {
      periodeType: 'bosted',
      innhold: {
        fraDato: '2016-02-01',
        tilDato: '2021-03-01',
        beskrivelse: 'Plogveien 54, 0458 Oslo',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
    {
      periodeType: 'bosted',
      innhold: {
        fraDato: '2020-02-01',
        tilDato: '2023-03-01',
        beskrivelse: 'Plogveien 54, 0458 Oslo',
        kilde: 'Folkeregisteret 11.11.2021',
      },
    },
  ]

  return (
    <Tidslinje>
      <Grid>
        <GridBorder style={{ top: '10px' }}>&nbsp;</GridBorder>
        <GridDatoer leftmargin={'0px'}>{moment(femAarTidligere).format('DD.MM.YYYY')}</GridDatoer>
        {navnOgProsentPaaDatoer.map((aar) => (
          <GridDatoerWrapper prosent={aar[1]} key={aar[1].toString()}>
            <GridDatoer leftmargin={'-16px'}>{aar[0]}</GridDatoer>
            <GridGraaSirkel>&nbsp;</GridGraaSirkel>
          </GridDatoerWrapper>
        ))}
        <GridDoedsdato>
          <GridDatoer leftmargin={'-150px'}>dødsdato: {moment(doedsdato).format('DD.MM.YYYY')}</GridDatoer>
        </GridDoedsdato>
        <GridBorder style={{ bottom: '-30px' }}>&nbsp;</GridBorder>
      </Grid>
      <div>
        {mockdata.map((periode, index) => (
          <Tidsperiode
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
