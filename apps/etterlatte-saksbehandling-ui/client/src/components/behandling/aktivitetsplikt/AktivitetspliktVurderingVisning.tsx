import { BodyShort, Button, Detail, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import {
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { formaterStringDato } from '~utils/formattering'
import { PencilIcon } from '@navikt/aksel-icons'

export const AktivitetspliktVurderingVisning = ({
  vurdering,
  visForm,
  erRedigerbar,
}: {
  vurdering?: IAktivitetspliktVurdering
  visForm: () => void
  erRedigerbar: boolean
}) => {
  return (
    <VStack gap="4">
      <>
        {vurdering ? (
          <>
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
                  Vurdering ble utført {formaterStringDato(vurdering.aktivitet.endret.tidspunkt)} av saksbehandler{' '}
                  {vurdering.aktivitet.endret.ident}
                </Detail>
              </>
            )}
          </>
        ) : (
          <BodyShort>Ingen vurdering</BodyShort>
        )}

        {erRedigerbar && (
          <div>
            <Button
              size="small"
              variant={vurdering ? 'tertiary' : 'secondary'}
              icon={<PencilIcon aria-hidden />}
              iconPosition="left"
              onClick={() => visForm()}
            >
              {vurdering ? 'Rediger vurdering' : 'Legg til vurdering'}
            </Button>
          </div>
        )}
      </>
    </VStack>
  )
}
