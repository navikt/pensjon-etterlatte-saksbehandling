import { Alert, Detail, Heading, HStack, Label, Radio, RadioGroup } from '@navikt/ds-react'
import React, { useState } from 'react'
import { UnderkjenneModal } from '~components/generellbehandling/UnderkjenneModal'
import { Attesteringmodal } from '~components/generellbehandling/Attesteringmodal'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'

type BeslutningsTyper = 'UNDERKJENN' | 'GODKJENN'
const Beslutning: Record<BeslutningsTyper, string> = {
  UNDERKJENN: 'Underkjenn',
  GODKJENN: 'Godkjenn',
}

export const AttesteringMedUnderkjenning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
  oppgaveErTildeltInnloggetBruker: boolean
}) => {
  const { utlandsBehandling, oppgaveErTildeltInnloggetBruker } = props
  const [beslutning, setBeslutning] = useState<BeslutningsTyper>()

  return (
    <>
      {oppgaveErTildeltInnloggetBruker && (
        <>
          <Alert variant="info" size="small">
            Kontroller opplysninger og faglige vurderinger gjort under behandling.
          </Alert>
          <br />
          <>
            <Heading size="xsmall">Beslutning</Heading>
            <HStack gap="space-4" justify="space-between">
              <div>
                <Label size="small">Saksbehandler</Label>
                {utlandsBehandling.behandler?.saksbehandler ? (
                  <Detail>{utlandsBehandling.behandler?.saksbehandler}</Detail>
                ) : (
                  <Alert variant="error">Saksbehandler mangler og du vil da ikke få attestert behandlingen</Alert>
                )}
              </div>

              <div>
                <Label size="small">Behandlet dato</Label>
                <Detail>
                  {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.behandler?.tidspunkt)}
                </Detail>
              </div>
            </HStack>

            <RadioGroup
              disabled={false}
              legend=""
              size="small"
              className="radioGroup"
              onChange={(event) => setBeslutning(event as BeslutningsTyper)}
            >
              <HStack gap="space-4" wrap={false} justify="space-between">
                <Radio value={Beslutning.GODKJENN}>{Beslutning.GODKJENN}</Radio>
                <Radio value={Beslutning.UNDERKJENN}>{Beslutning.UNDERKJENN}</Radio>
              </HStack>
            </RadioGroup>

            {beslutning === Beslutning.GODKJENN && <Attesteringmodal utlandsBehandling={utlandsBehandling} />}
            {beslutning === Beslutning.UNDERKJENN && <UnderkjenneModal utlandsBehandling={utlandsBehandling} />}
          </>
        </>
      )}
    </>
  )
}
