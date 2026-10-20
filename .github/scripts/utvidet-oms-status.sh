#!/usr/bin/env bash
# Viser status for de aktive branchene til utvidet omstillingsstønad på tvers av repoene.
# Branchene vedlikeholdes i .github/utvidet-oms/veikart.md og i lista under.
# Bruk: .github/scripts/utvidet-oms-status.sh [--fetch]

set -uo pipefail

HJEMREPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ROT="$(dirname "$HJEMREPO")"

BRANCHER=(
  "pensjon-etterlatte:utvidet-soknad-struktur-ny-app"
  "pensjon-etterlatte:NAV-31200-sende-inn-utvidet-oms-soeknad"
  "pensjon-etterlatte-saksbehandling:nav-31200-ta-imot-soknad-utvidet-oms"
)

finn_repo() {
  local navn="$1"
  if [[ -d "$ROT/$navn/.git" || -f "$ROT/$navn/.git" ]]; then
    echo "$ROT/$navn"
  elif [[ "$navn" == "pensjon-etterlatte-saksbehandling" ]]; then
    echo "$HJEMREPO"
  fi
}

if [[ "${1:-}" == "--fetch" ]]; then
  for repo in pensjon-etterlatte-felles pensjon-etterlatte pensjon-etterlatte-saksbehandling; do
    sti="$(finn_repo "$repo")"
    [[ -n "$sti" ]] && git -C "$sti" fetch --quiet origin 2>/dev/null
  done
fi

har_gh=false
command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1 && har_gh=true

printf "%-36s %-42s %-14s %s\n" "REPO" "BRANCH" "FORAN/BAK" "PR"
for oppfoering in "${BRANCHER[@]}"; do
  repo="${oppfoering%%:*}"
  branch="${oppfoering#*:}"
  sti="$(finn_repo "$repo")"

  if [[ -z "$sti" ]]; then
    printf "%-36s %-42s %s\n" "$repo" "$branch" "(repo ikke funnet i $ROT)"
    continue
  fi

  ref="origin/$branch"
  if ! git -C "$sti" rev-parse --verify --quiet "$ref" >/dev/null; then
    ref="$branch"
  fi

  if git -C "$sti" rev-parse --verify --quiet "$ref" >/dev/null; then
    foran_bak="$(git -C "$sti" rev-list --left-right --count "$ref...origin/main" 2>/dev/null | awk '{print "+"$1"/-"$2}')"
  else
    foran_bak="(finnes ikke)"
  fi

  pr="-"
  if $har_gh; then
    pr="$(gh pr list --repo "navikt/$repo" --head "$branch" --state all --limit 1 \
      --json number,state,isDraft --jq '.[0] | if . == null then "-" else "#\(.number) \(.state)\(if .isDraft then " (draft)" else "" end)" end' 2>/dev/null || echo "?")"
  fi

  printf "%-36s %-42s %-14s %s\n" "$repo" "$branch" "$foran_bak" "$pr"
done

echo
echo "Veikart: $HJEMREPO/.github/utvidet-oms/veikart.md"
