import { OfficeIcon } from '../../../../../shared/icons/officeIcon'
import { HomeIcon } from '../../../../../shared/icons/homeIcon'
import styled from 'styled-components'
import { InformationIcon } from '../../../../../shared/icons/informationIcon'
import { IPeriode } from './TidslinjeMedlemskap'
import { useRef, useState } from 'react'
import { Popover } from '@navikt/ds-react'
import { formaterStringDato } from '../../../../../utils/formattering'
import { hentKildenavn } from '../utils'
import { CloseIcon } from '../../../../../shared/icons/closeIcon'
import '../../../../../index.css'

export const Tidsperiode = ({
  periode,
  lengde,
  startOffset,
}: {
  periode: IPeriode
  lengde: string
  startOffset: string
}) => {
  const buttonRef = useRef(null)
  const [open, setOpen] = useState(false)
  const land = periode.innhold.adresseINorge ? ' (Norge)' : ` (${periode.innhold.land})`
  const isGap = periode.periodeType === 'Bostedgap'

  return (
    <Rad style={{ left: startOffset, width: lengde }} isGap={isGap}>
      {!isGap && (
        <InnholdWrapper>
          <Ikon>{periode.periodeType === 'jobb' ? <OfficeIcon /> : <HomeIcon />}</Ikon>
          <PeriodeType>
            {periode.periodeType} {land}
          </PeriodeType>
        </InnholdWrapper>
      )}
      <InfoIkonWrapper ref={buttonRef} onClick={() => setOpen(true)}>
        <InformationIcon />
      </InfoIkonWrapper>

      <Popover
        className={'breddepopover'}
        open={open}
        onClose={() => setOpen(false)}
        anchorEl={buttonRef.current}
        placement="top"
      >
        <Popover.Content>
          <Close onClick={() => setOpen(false)}>
            <CloseIcon />
          </Close>
          <InnholdTittel>Detaljer</InnholdTittel>
          {!isGap && (
            <InnholdKilde>
              Kilde: {hentKildenavn(periode.kilde.type)}{' '}
              {periode.kilde.tidspunktForInnhenting && formaterStringDato(periode.kilde.tidspunktForInnhenting)}
            </InnholdKilde>
          )}
          <div>
            {periode.periodeType} {!isGap && ': ' + periode.innhold.beskrivelse}
          </div>
          <div>{periode.innhold.land}</div>
          <div>
            Periode: {formaterStringDato(periode.innhold.fraDato)} -{' '}
            {periode.innhold.tilDato ? formaterStringDato(periode.innhold.tilDato) : ''}
          </div>
        </Popover.Content>
      </Popover>
    </Rad>
  )
}

const Rad = styled.div<{ isGap: boolean }>`
  background-color: ${(props) => (props.isGap ? '#E180714C' : '#CCE2F0')};
  position: relative;
  margin-bottom: 10px;
  margin-right: 1px;
  border-radius: 4px;
  display: flex;
  justify-content: ${(props) => (props.isGap ? 'center' : 'space-between')};
  align-items: center;
`

const InnholdWrapper = styled.div`
  display: flex;
  justify-content: left;
  align-items: center;
  width: 80%;
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
  opacity: 1;
  align-items: center;

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
