import { CogRotationIcon } from '@navikt/aksel-icons'
import { BodyShort, Box, Button, Heading, Modal, Radio, RadioGroup } from '@navikt/ds-react'
import { useState } from 'react'
import { Journalpost, Journalstatus } from '~shared/types/Journalpost'
import { FeilregistrerJournalpost } from '~components/person/dokumenter/avvik/FeilregistrerJournalpost'
import { OpphevFeilregistreringJournalpost } from '~components/person/dokumenter/avvik/OpphevFeilregistreringJournalpost'
import { FlyttJournalpost } from '~components/person/dokumenter/avvik/FlyttJournalpost'
import { Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

enum Aarsak {
  UTGAAR = 'UTGAAR',
  OVERFOER = 'OVERFOER',
  FEILREGISTRER = 'FEILREGISTRER',
}

export const HaandterAvvikModal = ({
  journalpost,
  sakStatus,
}: {
  journalpost: Journalpost
  sakStatus: Result<SakMedBehandlinger>
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [aarsak, setAarsak] = useState<Aarsak>()

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
              Journalpostdetailjer
            </Heading>

            <InfoWrapper>
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
            </InfoWrapper>
          </Box>
          <br />

          <RadioGroup legend="Velg handling" value={aarsak || ''} onChange={(checked) => setAarsak(checked as Aarsak)}>
            <Radio value={Aarsak.OVERFOER}>Overfør til annen sak</Radio>
            <Radio value={Aarsak.FEILREGISTRER}>
              {journalpost.journalstatus === Journalstatus.FEILREGISTRERT ? 'Opphev feilregistrering' : 'Feilregistrer'}
            </Radio>

            {/* TODO: Skal lage støtte for den fortløpende, men prioriterer overføring til annen sak */}
            {/*<option value={Aarsak.UTGAAR}>Sett til utgår</option>*/}
          </RadioGroup>

          <br />

          {aarsak === Aarsak.UTGAAR && <>{/*TODO*/}</>}

          {aarsak === Aarsak.FEILREGISTRER &&
            (journalpost.journalstatus === Journalstatus.FEILREGISTRERT ? (
              <OpphevFeilregistreringJournalpost journalpost={journalpost} />
            ) : (
              <FeilregistrerJournalpost journalpost={journalpost} />
            ))}

          {aarsak === Aarsak.OVERFOER && (
            <FlyttJournalpost journalpost={journalpost} sakStatus={sakStatus} lukkModal={() => setIsOpen(false)} />
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
