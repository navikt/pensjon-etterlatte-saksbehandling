import { BodyShort, Detail, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import {
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import styled from 'styled-components'
import { formaterStringDato } from '~utils/formattering'

export const AktivitetspliktVurderingVisning = ({ vurdering }: { vurdering: IAktivitetspliktVurdering }) => {
  return (
    <AktivitetspliktVurderingWrapper>
      <div>
        <HStack gap="12">
          {vurdering && (
            <VStack gap="4">
              {vurdering.unntak && (
                <>
                  <Label>Unntak</Label>
                  <BodyShort>{tekstAktivitetspliktUnntakType[vurdering.unntak.unntak]}</BodyShort>

                  {vurdering.unntak.tom && (
                    <>
                      <Label>Sluttdato</Label>
                      <BodyShort>{vurdering.unntak.tom}</BodyShort>
                    </>
                  )}

                  <Label>Vurdering</Label>
                  <BodyShort>{vurdering.unntak.beskrivelse}</BodyShort>

                  <Detail>
                    Vurdering ble utført {formaterStringDato(vurdering.unntak.opprettet.tidspunkt)} av saksbehandler{' '}
                    {vurdering.unntak.opprettet.ident}
                  </Detail>
                </>
              )}

              {vurdering.aktivitet && (
                <>
                  <Label>Aktivitetsgrad</Label>
                  <BodyShort>{tekstAktivitetspliktVurderingType[vurdering.aktivitet.aktivitetsgrad]}</BodyShort>

                  <Label>Vurdering</Label>
                  <BodyShort>{vurdering.aktivitet.beskrivelse}</BodyShort>

                  <Detail>
                    Vurdering ble utført {formaterStringDato(vurdering.aktivitet.opprettet.tidspunkt)} av saksbehandler{' '}
                    {vurdering.aktivitet.opprettet.ident}
                  </Detail>
                </>
              )}
            </VStack>
          )}
        </HStack>
      </div>
    </AktivitetspliktVurderingWrapper>
  )
}

const AktivitetspliktVurderingWrapper = styled.div`
  max-width: 500px;
`
