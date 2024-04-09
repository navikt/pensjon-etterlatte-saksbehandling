import React, { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { hentNavkontorForPerson } from '~shared/api/sak'
import { BodyShort, Heading, ReadMore, Tag } from '@navikt/ds-react'
import { FlexRow, SpaceChildren } from '~shared/styled'
import { tagColors } from '~shared/Tags'
import { formaterEnumTilLesbarString } from '~utils/formattering'
import { Buildings3Icon, LocationPinIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { EndreEnhet } from '~components/person/sakOgBehandling/EndreEnhet'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'

export const SakOversiktHeader = ({ sak, fnr }: { sak: ISakMedUtlandstilknytning; fnr: string }) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [navkontorResult, hentNavkontor] = useApiCall(hentNavkontorForPerson)

  useEffect(() => {
    hentNavkontor(fnr)
  }, [])

  return (
    <SpaceChildren>
      <Heading size="large">Saksnummer {sak.id}</Heading>
      <SpaceChildren direction="row">
        <Tag variant={tagColors[sak.sakType]}>{formaterEnumTilLesbarString(sak.sakType)}</Tag>
        {!!sak.utlandstilknytning?.type && (
          <Tag variant={tagColors[sak.utlandstilknytning?.type]}>
            {formaterEnumTilLesbarString(sak.utlandstilknytning?.type)}
          </Tag>
        )}
      </SpaceChildren>
      <SpaceChildren direction="row">
        <FlexRow align="center">
          <LocationPinIcon aria-hidden width="1.75rem" height="1.75rem" />
          <BodyShort>
            Navkontor:{' '}
            {mapResult(navkontorResult, {
              pending: <Spinner visible label="" />,
              error: <>Kunne ikke hente kontor</>,
              success: (navKontor) => <>{navKontor.navn}</>,
            })}
          </BodyShort>
        </FlexRow>
      </SpaceChildren>
      <SpaceChildren direction="row" gap="0">
        <FlexRow align="center">
          <Buildings3Icon aria-hidden width="1.75rem" height="1.75rem" />
          <BodyShort>Denne saken tilhører enhet {sak.enhet}</BodyShort>
          {enhetErSkrivbar(sak.enhet, innloggetSaksbehandler.skriveEnheter) && <EndreEnhet sakId={sak.id} />}
        </FlexRow>
      </SpaceChildren>
      {enhetErSkrivbar(sak.enhet, innloggetSaksbehandler.skriveEnheter) && (
        <ByttEnhetReadMore header="Ønsker du å bytte enhet?">
          <ol>
            <li>
              Skriv i kommentarfeltet i sjekklisten inne i behandlingen hvilken enhet saken overføres til og hvorfor.
            </li>
            <li>Deretter går du til saksoversikten og klikker på knappen ovenfor for å endre enhet.</li>
            <li>Overfør til riktig behandlende enhet.</li>
          </ol>
        </ByttEnhetReadMore>
      )}
    </SpaceChildren>
  )
}

const ByttEnhetReadMore = styled(ReadMore)`
  max-width: 32.5rem;
`
