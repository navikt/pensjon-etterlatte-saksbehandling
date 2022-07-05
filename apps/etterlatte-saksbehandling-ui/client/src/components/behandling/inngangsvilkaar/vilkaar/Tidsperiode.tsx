import { startdatoOffsetProsent, tidsperiodeProsent } from './tidslinjeUtils'
import { OfficeIcon } from '../../../../shared/icons/officeIcon'
import { HomeIcon } from '../../../../shared/icons/homeIcon'
import styled from 'styled-components'
import { InformationIcon } from '../../../../shared/icons/informationIcon'
import { IPeriode } from './TidslinjeMedlemskap'
import { useRef, useState } from 'react'
import { Popover } from '@navikt/ds-react'
import { formatterStringDato } from '../../../../utils'
import { hentKildenavn } from './utils'
import { CloseIcon } from '../../../../shared/icons/closeIcon'

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
  const buttonRef = useRef(null)
  const [open, setOpen] = useState(false)
  const land = periode.innhold.adresseINorge ? ' (Norge)' : ` (${periode.innhold.land})`

  return (
    <Rad style={{ left: startDatoOffset, width: lengdePeriode, backgroundColor: '#CCE2F0' }}>
      <InnholdWrapper>
        <Ikon>{periode.periodeType === 'jobb' ? <OfficeIcon /> : <HomeIcon />}</Ikon>
        <PeriodeType>
          {periode.periodeType} {land} og lengre tekst her
        </PeriodeType>
      </InnholdWrapper>
      <InfoIkonWrapper ref={buttonRef} onClick={() => setOpen(true)}>
        <InformationIcon />
      </InfoIkonWrapper>

      <Popover open={open} onClose={() => setOpen(false)} anchorEl={buttonRef.current} placement="top">
        <Popover.Content>
          <Close onClick={() => setOpen(false)}>
            <CloseIcon />
          </Close>
          <InnholdTittel>Detaljer</InnholdTittel>
          <InnholdKilde>
            Kilde: {hentKildenavn(periode.kilde.type)} {formatterStringDato(periode.kilde.tidspunktForInnhenting)}
          </InnholdKilde>
          <div>
            {periode.periodeType}: {periode.innhold.beskrivelse}
          </div>
          <div>
            Periode: {formatterStringDato(periode.innhold.fraDato)} -{' '}
            {periode.innhold.tilDato ? formatterStringDato(periode.innhold.tilDato) : ''}
          </div>
        </Popover.Content>
      </Popover>
    </Rad>
  )
}

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
  width: 85%;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

const InfoIkonWrapper = styled.div`
  display: flex;
  padding-right: 5px;

  svg {
    width: 1.2em;
    height: 1.2em;

    :hover {
      font-size: 1.2em;
    }
  }
`

const Close = styled.div`
  display: flex;
  justify-content: flex-end;
  cursor: pointer;
`

const InnholdTittel = styled.div`
  font-weight: bold;
`

const InnholdKilde = styled.div`
  color: #676363;
  margin-bottom: 10px;
`
