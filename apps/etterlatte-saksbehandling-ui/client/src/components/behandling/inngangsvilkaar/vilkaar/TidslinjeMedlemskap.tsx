import styled from 'styled-components'
import moment from 'moment'
import { aarIProsent } from './utilities'

export const TidslinjeMedlemskap = () => {
  const doedsdato = moment('2022-06-01', 'YYYY-MM-DD').format('DD.MM.YYYY').toString()
  const femAarTidligere = moment('2022-06-01', 'YYYY-MM-DD').subtract(5, 'years').format('DD.MM.YYYY').toString()

  const navnOgProsentPaaDatoer = aarIProsent(femAarTidligere, doedsdato)

  return (
    <Tidslinje>
      <Grid>
        <GridUnderline style={{ top: '10px' }}>&nbsp;</GridUnderline>
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
        <GridUnderline style={{ bottom: '-30px' }}>&nbsp;</GridUnderline>
      </Grid>
      <div style={{ paddingTop: '60px' }}>
        <Rad style={{ position: 'relative', left: '10%', width: '70%', marginBottom: '10px' }}>
          <DatoFraTil>jan. 2019 - okt. 2021</DatoFraTil>
          <Adresse>Plogveien 54, 0458 Oslo</Adresse>
          <Kilde>Folkeregisteret 11.11.2021</Kilde>
        </Rad>
        <Rad style={{ position: 'relative', left: '3%', width: '50%', marginBottom: '10px' }}>
          <DatoFraTil>jan. 2019 - okt. 2021</DatoFraTil>
          <Adresse>Plogveien 54, 0458 Oslo</Adresse>
          <Kilde>Folkeregisteret 11.11.2021</Kilde>
        </Rad>
        <Rad style={{ position: 'relative', left: '60%', width: '40%', marginBottom: '10px' }}>
          <DatoFraTil>jan. 2019 - okt. 2021</DatoFraTil>
          <Adresse>Plogveien 54, 0458 Oslo</Adresse>
          <Kilde>Folkeregisteret 11.11.2021</Kilde>
        </Rad>
      </div>
    </Tidslinje>
  )
}

export const Tidslinje = styled.div`
  margin-top: 40px;
  width: 100%;
  position: relative;
  margin-bottom: 30px;
`

export const Rad = styled.div`
  position: relative;
  width: 70%;
  left: 10%;
  background-color: #88c3de;
  border-radius: 10px;
`

export const DatoFraTil = styled.div`
  padding-top: 10px;
  padding-left: 20px;
  font-weight: bold;
`
export const Adresse = styled.div`
  padding-left: 20px;
`

export const Kilde = styled.div`
  padding-left: 20px;
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

const GridUnderline = styled.div`
  position: absolute;
  left: 0;
  right: 0;
  border-bottom: 1px solid #ccc;
`
