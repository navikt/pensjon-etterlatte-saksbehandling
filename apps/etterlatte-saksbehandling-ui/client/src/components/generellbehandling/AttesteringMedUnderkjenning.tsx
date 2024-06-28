import { Alert, Heading, Radio, RadioGroup } from '@navikt/ds-react'
import { Info, RadioGroupWrapper, Tekst } from '~components/behandling/attestering/styled'
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
            <div className="flex">
              <div>
                <Info>Saksbehandler</Info>
                {utlandsBehandling.behandler?.saksbehandler ? (
                  <Tekst>{utlandsBehandling.behandler?.saksbehandler}</Tekst>
                ) : (
                  <Alert variant="error">Saksbehandler mangler og du vil da ikke få attestert behandlingen</Alert>
                )}
              </div>
              <Info>Behandlet dato</Info>
              <Tekst>
                {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.behandler?.tidspunkt)}
              </Tekst>
            </div>
            <RadioGroupWrapper>
              <RadioGroup
                disabled={false}
                legend=""
                size="small"
                className="radioGroup"
                onChange={(event) => setBeslutning(event as BeslutningsTyper)}
              >
                <div className="flex">
                  <Radio value={Beslutning.GODKJENN}>{Beslutning.GODKJENN}</Radio>
                  <Radio value={Beslutning.UNDERKJENN}>{Beslutning.UNDERKJENN}</Radio>
                </div>
              </RadioGroup>
            </RadioGroupWrapper>
            {beslutning === Beslutning.GODKJENN && <Attesteringmodal utlandsBehandling={utlandsBehandling} />}
            {beslutning === Beslutning.UNDERKJENN && <UnderkjenneModal utlandsBehandling={utlandsBehandling} />}
          </>
        </>
      )}
    </>
  )
}
