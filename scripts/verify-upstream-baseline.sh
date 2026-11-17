#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM="$ROOT/upstream"

declare -A EXPECTED=(
  ["morphe-manager"]="d9f56746d13890c0f8837c6d070dae6f210ad2a9"
  ["morphe-patcher"]="0aed87c4e3264cf9233feab7ed26cf2543a08b96"
  ["ARSCLib"]="50418a07a806f7fc6d8a567be81d951d6e360079"
  ["smali"]="bbabf5f9980028daba64c19366483bbddc05cf01"
)

git -C "$ROOT" submodule update --init --recursive

for name in "${!EXPECTED[@]}"; do
  actual="$(git -C "$UPSTREAM/$name" rev-parse HEAD)"
  if [[ "$actual" != "${EXPECTED[$name]}" ]]; then
    echo "ERROR: upstream/$name is $actual; expected ${EXPECTED[$name]}" >&2
    exit 1
  fi
  echo "Verified upstream/$name @ $actual"
done

bash "$ROOT/scripts/apply-vyra-morphe-overlay.sh"

# Validate the archive guard against the actual official Morphe release artifact.
# The bundle is downloaded only into the ephemeral CI workspace and is not committed.
FIXTURE_DIR="$ROOT/.ci-fixtures"
MORPHE_MPP="$FIXTURE_DIR/patches-2.3.0.mpp"
mkdir -p "$FIXTURE_DIR"
curl --fail --location --retry 3 --connect-timeout 15 --max-time 180 \
  --output "$MORPHE_MPP" \
  "https://github.com/MorpheApp/morphe-patches/releases/download/v2.3.0/patches-2.3.0.mpp"
echo "1c37549274a2b6cf7f2e3e099d9b8cb641880b8a7dac323a95638a06bb4509a9  $MORPHE_MPP" | sha256sum --check -
export VYRA_TEST_MPP="$MORPHE_MPP"

cd "$UPSTREAM/morphe-manager"
chmod +x ./gradlew
./gradlew :app:testDebugUnitTest :app:assembleDebug --stacktrace
