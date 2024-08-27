import { BodyShort, Button, Detail, HStack, Label, VStack } from '@navikt/ds-react'
import React from 'react'
import {
  IAktivitetspliktVurdering,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { formaterDato } from '~utils/formatering/dato'
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

                <div>
                  <Label>Unntaksperiode</Label>
                  <HStack gap="4">
                    <div>
                      <Label size="small">Sluttdato</Label>
                      <BodyShort>
                        {vurdering.unntak.fom ? formaterDato(vurdering.unntak.fom) : 'Mangler startdato'}
                      </BodyShort>
                    </div>
                    <div>
                      <Label size="small">Sluttdato</Label>
                      <BodyShort>
                        {vurdering.unntak.tom ? formaterDato(vurdering.unntak.tom) : 'Mangler sluttdato'}
                      </BodyShort>
                    </div>
                  </HStack>
                </div>

                <Label>Vurdering</Label>
                <BodyShort>{vurdering.unntak.beskrivelse}</BodyShort>

                <Detail>
                  Vurdering ble utført {formaterDato(vurdering.unntak.opprettet.tidspunkt)} av saksbehandler{' '}
                  {vurdering.unntak.opprettet.ident}
                </Detail>
              </>
            )}

            {vurdering.aktivitet && (
              <>
                <div>
                  <Label>Aktivitetsgrad</Label>
                  <BodyShort>{tekstAktivitetspliktVurderingType[vurdering.aktivitet.aktivitetsgrad]}</BodyShort>
                </div>

                <div>
                  <Label>Aktivitetsgradsperiode</Label>
                  <HStack gap="4">
                    <div>
                      <Label size="small">Startdato</Label>
                      <BodyShort>{formaterDato(vurdering.aktivitet.fom)}</BodyShort>
                    </div>
                    <div>
                      <Label size="small">Sluttdato</Label>
                      <BodyShort>
                        {vurdering.aktivitet.tom ? formaterDato(vurdering.aktivitet.tom) : 'Mangler sluttdato'}
                      </BodyShort>
                    </div>
                  </HStack>
                </div>

                <div>
                  <Label>Vurdering</Label>
                  <BodyShort>{vurdering.aktivitet.beskrivelse}</BodyShort>
                </div>

                <Detail>
                  Vurdering ble utført {formaterDato(vurdering.aktivitet.endret.tidspunkt)} av saksbehandler{' '}
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
