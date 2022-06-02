import { Label } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'

export const Foreldreansvar = ({
  innsenderHarForeldreansvar,
}: {
  innsenderHarForeldreansvar: IGyldighetproving | undefined
}) => {
  const navn = innsenderHarForeldreansvar?.basertPaaOpplysninger?.innsender?.navn
  return (
    <DetailWrapper>
      {innsenderHarForeldreansvar?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Label size="small">Foreldreansvar</Label>
          {navn}
          <div>(gjenlevende forelder)</div>
        </div>
      )}

      {innsenderHarForeldreansvar?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Foreldreansvar
          </Label>
          <span className="warningText">Innsender har ikke foreldreansvar</span>
        </div>
      )}

      {innsenderHarForeldreansvar?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Foreldreansvar
          </Label>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
