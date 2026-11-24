CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_aarsoppgjoer_restanse_behandling ON avkorting_aarsoppgjoer_restanse (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_forventet_aarsoppgjoer ON avkortingsgrunnlag_forventet (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_forventet_behandling ON avkortingsgrunnlag_forventet (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_faktisk_aarsoppgjoer ON avkortingsgrunnlag_faktisk (aarsoppgjoer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_avkortingsgrunnlag_faktisk_behandling ON avkortingsgrunnlag_faktisk (behandling_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inntekt_innvilget_grunnlag ON inntekt_innvilget (grunnlag_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inntekt_innvilget_behandling ON inntekt_innvilget (behandling_id);