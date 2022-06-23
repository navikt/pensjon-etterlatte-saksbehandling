import { startdatoOffsetProsent, tidsperiodeProsent } from './tidslinjeUtils'
import { OfficeIcon } from '../../../../shared/icons/officeIcon'
import { HomeIcon } from '../../../../shared/icons/homeIcon'
import styled from 'styled-components'
import { InformationIcon } from '../../../../shared/icons/informationIcon'
import { IPeriode } from './TidslinjeMedlemskap'

export const Tidsperiode = ({
  doedsdato,
  seksAarTidligere,
  periode,
}: {
  doedsdato: string
  seksAarTidligere: string
  periode: IPeriode
}) => {
  const lengdePeriode = tidsperiodeProsent(
    periode.innhold.fraDato,
    periode.innhold.tilDato,
    doedsdato,
    seksAarTidligere
  )
  const startDatoOffset = startdatoOffsetProsent(periode.innhold.fraDato, seksAarTidligere)

  //const colors = ['#a6cbdc', '#a5a5d7', '#c4adde', '#d7a9c6']
  //backgroundColor: colors[index % colors.length]

  return (
    <Rad style={{ left: startDatoOffset, width: lengdePeriode, backgroundColor: '#CCE2F0' }}>
      <InnholdWrapper>
        <Ikon>{periode.periodeType === 'jobb' ? <OfficeIcon /> : <HomeIcon />}</Ikon>
        <PeriodeType>{periode.periodeType}</PeriodeType>
      </InnholdWrapper>
      <InfoIkonWrapper>
        <InformationIcon />
      </InfoIkonWrapper>
    </Rad>
  )
}

//<InnholdDatoFraTil>
//    {format(new Date(periode.innhold.fraDato), 'MM.yyyy')} -{' '}
//    {format(new Date(periode.innhold.tilDato), 'MM.yyyy')}
//</InnholdDatoFraTil>
//<InnholdBeskrivelse>{periode.innhold.beskrivelse}</InnholdBeskrivelse>
//<InnholdKilde>{periode.innhold.kilde}</InnholdKilde>

const Rad = styled.div`
  position: relative;
  margin-bottom: 10px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const InnholdWrapper = styled.div`
  display: flex;
  justify-content: left;
  align-items: center;
`

const Ikon = styled.div`
  display: flex;
  justify-content: left;
  padding-left: 5px;

  svg {
    width: 1em;
    height: 1em;
  }
`

const PeriodeType = styled.div`
  padding: 5px;
  font-weight: bold;
`

const InfoIkonWrapper = styled.div`
  display: flex;
  padding-right: 5px;

  svg {
    width: 1.2em;
    height: 1.2em;
  }
`

/*
const InnholdDatoFraTil = styled.div`
  padding-top: 5px;
  padding-left: 5px;
  font-weight: bold;
`
const InnholdBeskrivelse = styled.div`
  padding-left: 5px;
`

const InnholdKilde = styled.div`
  padding-left: 5px;
  color: #676363;
`
*/
