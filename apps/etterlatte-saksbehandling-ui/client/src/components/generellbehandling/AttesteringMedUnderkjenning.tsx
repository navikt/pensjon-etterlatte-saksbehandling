import { SidebarPanel } from '~shared/components/Sidebar'
import { Alert, Heading, Radio, RadioGroup } from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/attestering/styled'
import React, { useState } from 'react'
import { UnderkjenneModal } from '~components/generellbehandling/UnderkjenneModal'
import { Atteseringmodal } from '~components/generellbehandling/Atteseringmodal'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

type BeslutningerType = 'UNDERKJENN' | 'GODKJENN'
const Beslutning: Record<BeslutningerType, string> = {
  UNDERKJENN: 'Underkjenn',
  GODKJENN: 'Godkjenn',
}
export const AttesteringMedUnderkjenning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props

  //TODO: må matche oppgaven til kravpakke opp mot innlogget bruker- se https://jira.adeo.no/browse/EY-3149
  const oppgaveErTildeltInnloggetBruker = true
  const [beslutning, setBeslutning] = useState<BeslutningerType>()

  return (
    <SidebarPanel>
      {oppgaveErTildeltInnloggetBruker ? (
        <>
          <Alert variant="info" size="small">
            Kontroller opplysninger og faglige vurderinger gjort under behandling.
          </Alert>
          <br />
          <>
            <Heading size="xsmall">Beslutning</Heading>
            <RadioGroupWrapper>
              <RadioGroup
                disabled={false}
                legend=""
                size="small"
                className="radioGroup"
                onChange={(event) => setBeslutning(event as BeslutningerType)}
              >
                <div className="flex">
                  <Radio value={Beslutning.GODKJENN}>{Beslutning.GODKJENN}</Radio>
                  <Radio value={Beslutning.UNDERKJENN}>{Beslutning.UNDERKJENN}</Radio>
                </div>
              </RadioGroup>
            </RadioGroupWrapper>
            {beslutning === Beslutning.GODKJENN && <Atteseringmodal utlandsBehandling={utlandsBehandling} />}
            {beslutning === Beslutning.UNDERKJENN && <UnderkjenneModal utlandsBehandling={utlandsBehandling} />}
          </>
        </>
      ) : (
        <></>
      )}
    </SidebarPanel>
  )
}
