import { BodyLong, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import React, { useContext, useState } from 'react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { ConfigContext } from '~clientConfig'

export const AktivitetspliktInfoModal = ({ fnr }: { fnr: string | null }) => {
  const [visModal, setVisModal] = useState(false)
  const configContext = useContext(ConfigContext)

  return (
    <>
      <Button size="small" onClick={() => setVisModal(true)}>
        Se oppgave
      </Button>
      {visModal && (
        <Modal
          open={visModal}
          onClose={() => setVisModal(false)}
          header={{ label: 'Oppfølging av aktivitetsplikt', heading: 'Send brev og opprett oppgave for oppfølging' }}
        >
          <Modal.Body>
            <HStack gap="12">
              <div>
                <Heading size="small" spacing>
                  Opprett varsel- eller informasjonbrev rundt aktivitetsplikt til bruker
                </Heading>
                <BodyLong spacing>
                  Den etterlatte skal informeres om aktivitetskravet som vil tre i kraft 6 måneder etter dødsfallet. Det
                  skal opprettes et manuelt varsels- eller informasjonsbrev som skal bli sendt 3-4 måneder etter
                  dødsfallet.
                </BodyLong>
                <Button
                  variant="primary"
                  size="small"
                  as="a"
                  href={`/person/${fnr?.toString()}?fane=BREV`}
                  target="_blank"
                >
                  Opprett manuelt brev
                </Button>
              </div>

              <div>
                <Heading size="small" spacing>
                  Lag intern oppfølgingsoppgave med frist
                </Heading>
                <BodyLong spacing>
                  Den etterlatte skal påminnes og følges opp rundt aktivitetskravet både ved 3-4 måneder etter
                  dødsfallet og på nytt ved 9-10 måneder. Andre frister bør vurderes hvis den etterlatte har andre
                  ytelser eller andre grunner som krever alternativ oppfølging.
                </BodyLong>
                <Button
                  variant="primary"
                  size="small"
                  as="a"
                  href={`${configContext['gosysUrl']}/personoversikt/fnr=${fnr?.toString()}`}
                  target="_blank"
                >
                  Lag oppfølgingsoppgave i Gosys <ExternalLinkIcon />
                </Button>
              </div>
            </HStack>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setVisModal(false)}>
              Lukk
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </>
  )
}
