import { Label } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { IPersonOpplysning } from '../../../types'

export const Innsender = ({
  innsenderHarForeldreAnsvar,
  innsender,
}: {
  innsenderHarForeldreAnsvar: IGyldighetproving | undefined
  innsender: IPersonOpplysning
}) => {
  return (
    <DetailWrapper>
      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Label size="small">Innsender</Label>
          {innsender?.fornavn} {innsender?.etternavn}
          <div>(gjenlevende forelder)</div>
        </div>
      )}

      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Label>
          <span>
            {innsender?.fornavn} {innsender?.etternavn}
          </span>
          <div className="warningText"> Ikke gjenlevende forelder</div>
        </div>
      )}

      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Label>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
