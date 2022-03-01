import moment, { Moment } from 'moment'
import { startdatoOffsetProsent, tidsperiodeProsent } from './tidslinjeUtils'
import { OfficeIcon } from '../../../../shared/icons/officeIcon'
import { HomeIcon } from '../../../../shared/icons/homeIcon'
import styled from 'styled-components'

export const Tidsperiode = ({
  doedsdato,
  femAarTidligere,
  periode,
  index,
}: {
  doedsdato: Moment
  femAarTidligere: Moment
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
            {moment(periode.innhold.fraDato).format('MM.YYYY')} - {moment(periode.innhold.tilDato).format('MM.YYYY')}
          </InnholdDatoFraTil>
          <InnholdBeskrivelse>{periode.innhold.beskrivelse}</InnholdBeskrivelse>
          <InnholdKilde>{periode.innhold.kilde}</InnholdKilde>
        </div>
      </InnholdWrapper>
    </Rad>
  )
}

const Rad = styled.div`
  position: relative;
  margin-bottom: 10px;
  border-radius: 4px;
  padding-bottom: 10px;
`

const InnholdWrapper = styled.div`
  display: flex;
  justify-content: left;
`

const Ikon = styled.div`
  display: flex;
  justify-content: left;
  padding: 10px;

  svg {
    width: 1.8em;
    height: 1.8em;
  }
`

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
