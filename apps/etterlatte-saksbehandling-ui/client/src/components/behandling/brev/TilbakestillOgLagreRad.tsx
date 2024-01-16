import React, { ReactNode, useState } from 'react'
import { LagretStatus } from '~components/behandling/brev/RedigerbartBrev'
import styled from 'styled-components'
import { Button, Detail } from '@navikt/ds-react'
import { FileResetIcon, FloppydiskIcon } from '@navikt/aksel-icons'
import { GeneriskModal } from '~shared/modal/modal'

interface TilbakestillOgLagreRadProps {
  lagretStatus: LagretStatus
  lagre: () => void
  tilbakestill: () => void
  tilbakestillManuellPayloadStatus: boolean
  lagreManuellPayloadStatus: boolean
}

export const TilbakestillOgLagreRad = ({
  lagretStatus,
  lagre,
  tilbakestill,
  tilbakestillManuellPayloadStatus,
  lagreManuellPayloadStatus,
}: TilbakestillOgLagreRadProps): ReactNode => {
  const [isOpen, setIsOpen] = useState<boolean>(false)

  return (
    <>
      <ButtonRow>
        <Button
          icon={<FileResetIcon aria-hidden />}
          variant="secondary"
          onClick={() => setIsOpen(true)}
          disabled={!lagretStatus}
          loading={tilbakestillManuellPayloadStatus}
        >
          Tilbakestill brev
        </Button>
        <div>
          {lagretStatus.beskrivelse && <Detail as="span">{lagretStatus.beskrivelse}</Detail>}
          <Button
            icon={<FloppydiskIcon aria-hidden />}
            variant="primary"
            onClick={lagre}
            disabled={!lagretStatus}
            loading={lagreManuellPayloadStatus}
          >
            Lagre endringer
          </Button>
        </div>
      </ButtonRow>
      <GeneriskModal
        tittel="Tilbakestill brevet"
        beskrivelse="Ønsker du å tilbakestille brevet og hente inn ny informasjon? Dette vil slette tidligere endringer, så husk å kopiere tekst du vil beholde først."
        tekstKnappJa="Ja, hent alt innhold på nytt"
        tekstKnappNei="Nei"
        onYesClick={tilbakestill}
        setModalisOpen={setIsOpen}
        open={isOpen}
      />
    </>
  )
}

const ButtonRow = styled.div`
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding: 1rem;
`
