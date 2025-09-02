import { Alert, BodyShort, Button, HStack, Modal, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { erOppgaveRedigerbar, OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { isPending } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato } from '~utils/formatering/dato'

type Props = {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}

export const EtteroppgjoerSvarfristUtloeptModal = ({ oppgave, oppdaterStatus }: Props) => {
  const [open, setOpen] = useState(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [ferdigstillStatus, ferdigstill] = useApiCall(ferdigstillOppgaveMedMerknad)

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const avslutt = () => {
    ferdigstill({ id: oppgave.id, merknad: oppgave.merknad }, () => {
      oppdaterStatus(oppgave.id, Oppgavestatus.FERDIGSTILT)
      setOpen(false)
    })
  }

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal
        open={open}
        aria-labelledby="etteroppgjoer-svarfrist-utloept-heading"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Etteroppgjør – svarfrist utløpt' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <HStack gap="4">
              <Info label="Opprettet" tekst={formaterDato(oppgave.opprettet)} />
              <Info label="Frist" tekst={formaterDato(oppgave.frist)} />
            </HStack>

            {oppgave.merknad && <Alert variant="info">{oppgave.merknad}</Alert>}

            {kanRedigeres && !erTildeltSaksbehandler && (
              <BodyShort>Du må tildele deg oppgaven for å endre den.</BodyShort>
            )}

            <BodyShort>Etteroppgjøret kan ferdigstilles</BodyShort>

            <HStack gap="4" justify="end">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillStatus)}>
                Lukk
              </Button>

              {kanRedigeres && erTildeltSaksbehandler && (
                <Button variant="danger" onClick={avslutt} loading={isPending(ferdigstillStatus)}>
                  Opprett revurdering
                </Button>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
