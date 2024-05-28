import { CogRotationIcon } from '@navikt/aksel-icons'
import { BodyShort, Box, Button, Heading, Modal, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { Journalpost, Journalstatus } from '~shared/types/Journalpost'
import { FeilregistrerJournalpost } from '~components/person/dokumenter/avvik/FeilregistrerJournalpost'
import { OpphevFeilregistreringJournalpost } from '~components/person/dokumenter/avvik/OpphevFeilregistreringJournalpost'
import { KnyttTilAnnenSak } from '~components/person/dokumenter/avvik/KnyttTilAnnenSak'
import { Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { kanEndreJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { KnyttTilAnnentBruker } from './KnyttTilAnnenBruker'
import { OpprettJournalfoeringsoppgave } from './OpprettJournalfoeringsoppgave'

enum AvvikHandling {
  UTGAAR = 'UTGAAR',
  OVERFOER = 'OVERFOER',
  FEILREGISTRER = 'FEILREGISTRER',
  OPPRETT_OPPGAVE = 'OPPRETT_OPPGAVE',
}

export const HaandterAvvikModal = ({
  journalpost,
  sakStatus,
}: {
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [aarsak, setAarsak] = useState<AvvikHandling>()

  return (
    <>
      <Button
        variant="secondary"
        size="small"
        icon={<CogRotationIcon />}
        title="Håndter avvik"
        onClick={() => setIsOpen(true)}
      />

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" width="medium">
        <Modal.Header>
          <Heading size="medium">Håndter avvik på journalpost</Heading>
        </Modal.Header>

        <Modal.Body>
          <Box borderWidth="1" padding="4" borderRadius="medium" borderColor="border-subtle" background="bg-subtle">
            <Heading size="xsmall" spacing>
              Journalpostdetaljer
            </Heading>

            <VStack gap="4">
              <Info label="Journalpost ID" tekst={journalpost.journalpostId} />
              <Info label="Bruker" tekst={`${journalpost.bruker?.id || '-'} (${journalpost.bruker?.type})`} />
              <Info label="Sakstype" tekst={journalpost.sak?.sakstype || '-'} />
              <Info label="FagsakId" tekst={journalpost.sak?.fagsakId || '-'} />
              <Info label="Fagsaksystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
              <Info label="Tema" tekst={journalpost.tema || '-'} />

              <Info
                label="Dokumenter"
                tekst={
                  <>
                    {journalpost.dokumenter.map((dok) => (
                      <BodyShort key={dok.dokumentInfoId}>{dok.tittel}</BodyShort>
                    ))}
                  </>
                }
              />
            </VStack>
          </Box>
          <br />

          <RadioGroup
            legend="Velg handling"
            value={aarsak || ''}
            onChange={(checked) => setAarsak(checked as AvvikHandling)}
          >
            <Radio value={AvvikHandling.OVERFOER}>Overfør til Gjenny</Radio>
            <Radio value={AvvikHandling.FEILREGISTRER}>
              {journalpost.journalstatus === Journalstatus.FEILREGISTRERT ? 'Opphev feilregistrering' : 'Feilregistrer'}
            </Radio>
            <Radio value={AvvikHandling.OPPRETT_OPPGAVE}>Opprett oppgave</Radio>
          </RadioGroup>

          <br />

          {aarsak === AvvikHandling.FEILREGISTRER &&
            (journalpost.journalstatus === Journalstatus.FEILREGISTRERT ? (
              <OpphevFeilregistreringJournalpost journalpost={journalpost} />
            ) : (
              <FeilregistrerJournalpost journalpost={journalpost} />
            ))}

          {aarsak === AvvikHandling.OVERFOER &&
            (kanEndreJournalpost(journalpost) ? (
              <KnyttTilAnnentBruker
                journalpost={journalpost}
                sakStatus={sakStatus}
                lukkModal={() => setIsOpen(false)}
              />
            ) : (
              <KnyttTilAnnenSak journalpost={journalpost} sakStatus={sakStatus} lukkModal={() => setIsOpen(false)} />
            ))}

          {aarsak === AvvikHandling.OPPRETT_OPPGAVE && (
            <OpprettJournalfoeringsoppgave journalpost={journalpost} sakStatus={sakStatus} />
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
