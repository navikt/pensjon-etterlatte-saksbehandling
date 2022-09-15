import { OfficeIcon } from '../../../../../shared/icons/officeIcon'
import { HomeIcon } from '../../../../../shared/icons/homeIcon'
import styled from 'styled-components'
import { InformationIcon } from '../../../../../shared/icons/informationIcon'

import { useRef, useState } from 'react'
import { Popover } from '@navikt/ds-react'
import { formaterEnumTilLesbarString, formaterStringDato } from '../../../../../utils/formattering'
import { hentKildenavn, norskeBostaver } from '../tekstUtils'
import { CloseIcon } from '../../../../../shared/icons/closeIcon'
import '../../../../../index.css'
import {
  IAdresseTidslinjePeriodeInnhold,
  IReturnertPeriodeType,
  ISaksbehandlerKilde,
  ITidslinjePeriode,
  IVurdertPeriode,
  TidslinjePeriodeType,
} from '../../types'
import { KildeType } from '../../../../../store/reducers/BehandlingReducer'

export const Tidsperiode = ({
  periode,
  lengde,
  startOffset,
}: {
  periode: ITidslinjePeriode
  lengde: string
  startOffset: string
}) => {
  const buttonRef = useRef(null)
  const [open, setOpen] = useState(false)

  const isAdresseInnhold = 'land' in periode.innhold
  const isYtelseInnhold = 'godkjentPeriode' in periode.innhold
  const innholdAdresse = periode.innhold as IAdresseTidslinjePeriodeInnhold
  const innholdYtelse = periode.innhold as IVurdertPeriode
  const isGap = periode.type === TidslinjePeriodeType.GAP
  const isSaksbehandlerPeriode = isYtelseInnhold ? innholdYtelse.kilde.type === KildeType.saksbehandler : false
  const trengerAvklaring = isAdresseInnhold ? !innholdAdresse.adresseINorge : false

  function hentTittel() {
    if (isYtelseInnhold) {
      return innholdYtelse.periodeType === IReturnertPeriodeType.offentlig_ytelse
        ? formaterEnumTilLesbarString(innholdYtelse.beskrivelse ? innholdYtelse.beskrivelse : '')
        : formaterEnumTilLesbarString(norskeBostaver(innholdYtelse.periodeType))
    } else {
      return innholdAdresse.adresseINorge ? innholdAdresse.typeAdresse : innholdAdresse.typeAdresse + ' utland'
    }
  }

  function getColor() {
    if (
      isGap ||
      (isAdresseInnhold && innholdAdresse.typeAdresse === 'Bostedsadresse' && !innholdAdresse.adresseINorge)
    ) {
      return '#E180714C'
    } else if (isSaksbehandlerPeriode) {
      return '#E0D8E9'
    } else if (trengerAvklaring) {
      return '#FFECCC'
    } else {
      return '#CCE2F0'
    }
  }

  return (
    <Rad style={{ left: startOffset, width: lengde }} periodeColor={getColor()} isGap={isGap}>
      {!isGap && (
        <InnholdWrapper>
          <Ikon>{periode.type !== TidslinjePeriodeType.ADRESSE ? <OfficeIcon /> : <HomeIcon />}</Ikon>
          <PeriodeType>{hentTittel()}</PeriodeType>
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

          {isAdresseInnhold && !isGap && (
            <>
              <InnholdTittel>{hentTittel()}</InnholdTittel>
              <InnholdKilde>
                Kilde: {hentKildenavn(periode.kilde.type)}{' '}
                {periode.kilde.tidspunktForInnhenting && formaterStringDato(periode.kilde.tidspunktForInnhenting)}
              </InnholdKilde>
              <div>
                <BoldTekst>Adresse: </BoldTekst>
                <span>{innholdAdresse.beskrivelse}</span>
              </div>
              <div>{innholdAdresse?.land}</div>
            </>
          )}

          {isAdresseInnhold && isGap && <InnholdTittel>Gap i bostedsadresse i Norge</InnholdTittel>}

          {isYtelseInnhold && isSaksbehandlerPeriode && (
            <>
              <InnholdTittel>{hentTittel()}</InnholdTittel>
              <InnholdKilde>
                Kilde: {hentKildenavn(innholdYtelse.kilde.type)}{' '}
                {innholdYtelse.kilde.tidspunkt && formaterStringDato(innholdYtelse.kilde.tidspunkt)}
                <div>Lagt inn av {(innholdYtelse.kilde as ISaksbehandlerKilde).ident}</div>
              </InnholdKilde>
              {innholdYtelse.periodeType === IReturnertPeriodeType.arbeidsperiode && (
                <>
                  <div>
                    <BoldTekst>Arbeidsgiver: </BoldTekst> <span>{innholdYtelse.arbeidsgiver}</span>
                  </div>
                  <div>
                    <BoldTekst>Stillingsprosent: </BoldTekst>
                    <span>{innholdYtelse.stillingsprosent}</span>
                  </div>
                </>
              )}
              <div>
                <BoldTekst>Begrunnelse: </BoldTekst> <span>{innholdYtelse.begrunnelse}</span>
              </div>
              <div>
                <BoldTekst>Oppgitt kilde: </BoldTekst> <span>{innholdYtelse.oppgittKilde}</span>
              </div>
              <div>
                <BoldTekst>Perioden teller for medlemskap: </BoldTekst>
                <span>{innholdYtelse.godkjentPeriode ? 'Ja' : 'Nei'}</span>
              </div>
            </>
          )}

          {isYtelseInnhold && !isSaksbehandlerPeriode && (
            <>
              <InnholdTittel>{hentTittel()}</InnholdTittel>
              <InnholdKilde>
                Kilde: {hentKildenavn(innholdYtelse.kilde.type)}{' '}
                {innholdYtelse.kilde.tidspunkt && formaterStringDato(innholdYtelse.kilde.tidspunkt)}
              </InnholdKilde>
              {innholdYtelse.periodeType === IReturnertPeriodeType.arbeidsperiode && (
                <>
                  <div>
                    <BoldTekst>Arbeidsgiver: </BoldTekst> <span>{innholdYtelse.arbeidsgiver}</span>
                  </div>
                  <div>
                    <BoldTekst>Stillingsprosent: </BoldTekst>
                    <span>{innholdYtelse.stillingsprosent} </span>
                  </div>
                </>
              )}
              <div>
                <BoldTekst>Perioden teller for medlemskap: </BoldTekst>
                <span>{innholdYtelse.godkjentPeriode ? 'Ja' : 'Nei'}</span>
              </div>
            </>
          )}

          <div>
            <BoldTekst>Periode: </BoldTekst>
            <span> {formaterStringDato(periode.innhold.fraDato)} - </span>
            {periode.innhold.tilDato ? formaterStringDato(periode.innhold.tilDato) : ''}
          </div>
        </Popover.Content>
      </Popover>
    </Rad>
  )
}

const Rad = styled.div<{ periodeColor: string; isGap: boolean }>`
  background-color: ${(props) => props.periodeColor};
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

const BoldTekst = styled.span`
  font-weight: bold;
`
