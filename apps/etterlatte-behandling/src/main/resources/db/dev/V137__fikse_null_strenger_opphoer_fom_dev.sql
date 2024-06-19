-- Ble introdusert BehandlingDao på grunn av objectMapper.writeValueAsString(fom) hvor fom er nullable
update behandling set opphoer_fom = null where opphoer_fom = 'null';
