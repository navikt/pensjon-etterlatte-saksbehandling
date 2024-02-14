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
  tilbakestillSynlig: boolean
}

export const TilbakestillOgLagreRad = ({
  lagretStatus,
  lagre,
  tilbakestill,
  tilbakestillManuellPayloadStatus,
  lagreManuellPayloadStatus,
  tilbakestillSynlig,
}: TilbakestillOgLagreRadProps): ReactNode => {
  const [isOpen, setIsOpen] = useState<boolean>(false)

  return (
    <>
      <ButtonRow>
        {tilbakestillSynlig && (
          <Button
            icon={<FileResetIcon aria-hidden />}
            variant="secondary"
            onClick={() => setIsOpen(true)}
            disabled={!lagretStatus}
            loading={tilbakestillManuellPayloadStatus}
          >
            Tilbakestill brev
          </Button>
        )}
        <LagreEndringer>
          {lagretStatus.beskrivelse && (
            <Detail as="span" style={{ marginRight: '5px' }}>
              {lagretStatus.beskrivelse}
            </Detail>
          )}
          <Button
            icon={<FloppydiskIcon aria-hidden />}
            variant="primary"
            onClick={lagre}
            disabled={!lagretStatus}
            loading={lagreManuellPayloadStatus}
          >
            Lagre endringer
          </Button>
        </LagreEndringer>
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

const LagreEndringer = styled.div`
  display: flex;
  align-items: end;
`

const ButtonRow = styled.div`
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding: 1rem;
`
