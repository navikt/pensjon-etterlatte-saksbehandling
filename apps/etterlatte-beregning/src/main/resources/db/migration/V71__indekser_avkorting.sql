CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortet_ytelse_aarsoppgjoer_type ON avkortet_ytelse (aarsoppgjoer_id, type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortet_ytelse_inntektsgrunnlag_type ON avkortet_ytelse (inntektsgrunnlag, type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortet_ytelse_behandling ON avkortet_ytelse (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsperioder_inntektsgrunnlag ON avkortingsperioder (inntektsgrunnlag);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsperioder_behandling ON avkortingsperioder (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ytelse_foer_avkorting_aarsoppgjoer ON avkorting_aarsoppgjoer_ytelse_foer_avkorting (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ytelse_foer_avkorting_behandling ON avkorting_aarsoppgjoer_ytelse_foer_avkorting (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_aarsoppgjoer_restanse_aarsoppgjoer ON avkorting_aarsoppgjoer_restanse (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_aarsoppgjoer_restanse_behandling ON avkorting_aarsoppgjoer_restanse (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_forventet_aarsoppgjoer ON avkortingsgrunnlag_forventet (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_forventet_behandling ON avkortingsgrunnlag_forventet (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_faktisk_aarsoppgjoer ON avkortingsgrunnlag_faktisk (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_faktisk_behandling ON avkortingsgrunnlag_faktisk (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inntekt_innvilget_grunnlag ON inntekt_innvilget (grunnlag_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inntekt_innvilget_behandling ON inntekt_innvilget (behandling_id);
