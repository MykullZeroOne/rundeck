#!/usr/bin/env bash
# Build + push the custom Rundeck image for local dev and AKS.
#
# Usage:
#   ./build.sh dev                       # Build from source for arm64, load as rundeck:dev
#   ./build.sh push <acr-name> <tag>     # Build from source multi-arch, push to ACR
#
# The "dev" target builds the full stack from the repo source so that
# native ARM64 binaries are produced (Remco, Tini, JRE). This avoids
# the SIGSEGV that occurs when Docker Desktop emulates the amd64-only
# Docker Hub image via Rosetta on Apple Silicon.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODE="${1:-dev}"

LOCAL_BASE_TAG="rundeck/rundeck:local"

# Override the PagerDuty/Cloudsmith private npm registry that ships in the
# repo's .npmrc files. We don't have a CLOUDSMITH_NPM_TOKEN and don't need
# one — no @pagerduty scoped packages are actually used.
override_npmrc() {
  for rc in \
    "$REPO_ROOT/rundeckapp/grails-spa/packages/ui-trellis/.npmrc" \
    "$REPO_ROOT/rundeckapp/grails-app/assets/javascripts/_package-manager/.npmrc"; do
    if [[ -f "$rc" ]]; then
      echo ">> Patching $rc to use public npm registry"
      cat > "$rc" <<'NPMRC'
registry=https://registry.npmjs.org/
legacy-peer-deps=true
NPMRC
    fi
  done

  # If package-lock.json is missing or still points to a private registry,
  # regenerate it from the public registry.
  local trellis_dir="$REPO_ROOT/rundeckapp/grails-spa/packages/ui-trellis"
  if [[ ! -f "$trellis_dir/package-lock.json" ]] || grep -q 'npm.artifacts.pd-internal.com' "$trellis_dir/package-lock.json"; then
    echo ">> Regenerating package-lock.json from public registry"
    (cd "$trellis_dir" && npm install --package-lock-only)
  fi
}

# Build the WAR and base Docker images from source.
# Args: platform (e.g. "linux/arm64" or "linux/amd64,linux/arm64")
build_from_source() {
  local platform="$1"

  override_npmrc

  echo ">> Building Rundeck WAR from source..."
  cd "$REPO_ROOT"
  ./gradlew :rundeckapp:bootWar

  # Build Remco on the host to avoid Docker BuildKit DNS issues.
  local remco_bin="$REPO_ROOT/docker/ubuntu-base/remco"
  if [[ ! -x "$remco_bin" ]]; then
    echo ">> Building Remco from source (host)..."
    local remco_commit="dd15086e958dd83f334fb857ecd29bb1615fa179"
    local remco_tmp
    remco_tmp=$(mktemp -d)
    git clone https://github.com/rundeck/remco.git "$remco_tmp"
    (cd "$remco_tmp" && git checkout "$remco_commit" && \
      GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -o "$remco_bin" ./cmd/remco)
    rm -rf "$remco_tmp"
  fi

  echo ">> Building ubuntu-base image..."
  cd "$REPO_ROOT/docker/ubuntu-base"
  docker build \
    --build-arg JRE_VERSION=openjdk-17-jre-headless \
    -t rundeck/ubuntu-base \
    .

  echo ">> Building official Rundeck image..."
  # Stage the WAR where the official Dockerfile expects it.
  local official_dir="$REPO_ROOT/docker/official"
  mkdir -p "$official_dir/.build"

  local war_file
  war_file=$(find "$REPO_ROOT/rundeckapp/build/libs" -name 'rundeck-*.war' -not -name '*-plain.war' | head -1)
  if [[ -z "$war_file" ]]; then
    echo "ERROR: WAR not found in rundeckapp/build/libs/" >&2
    exit 1
  fi
  cp "$war_file" "$official_dir/.build/rundeck.war"

  docker build \
    --platform "$platform" \
    -t "$LOCAL_BASE_TAG" \
    "$official_dir"

  # Clean up staged WAR
  rm -rf "$official_dir/.build"
}

case "$MODE" in
  dev)
    echo ">> Building for local ARM64 Mac..."
    build_from_source "linux/arm64"

    echo ">> Building custom image (rundeck:dev)..."
    cd "$SCRIPT_DIR"
    docker build \
      --platform linux/arm64 \
      --build-arg RUNDECK_IMAGE="$LOCAL_BASE_TAG" \
      -t rundeck:dev \
      .

    echo ""
    echo "Done! Run with:  docker compose up"
    ;;
  push)
    ACR="${2:?usage: build.sh push <acr-name> <tag>}"
    TAG="${3:?usage: build.sh push <acr-name> <tag>}"
    IMAGE="${ACR}.azurecr.io/rundeck:${TAG}"

    # Ensure a buildx builder exists (idempotent).
    if ! docker buildx inspect multi >/dev/null 2>&1; then
      docker buildx create --name multi --driver docker-container --use
      docker buildx inspect --bootstrap
    else
      docker buildx use multi
    fi

    override_npmrc

    echo ">> Building WAR from source..."
    cd "$REPO_ROOT"
    ./gradlew :rundeckapp:bootWar

    # Stage the WAR
    local_official_dir="$REPO_ROOT/docker/official"
    mkdir -p "$local_official_dir/.build"
    war_file=$(find "$REPO_ROOT/rundeckapp/build/libs" -name 'rundeck-*.war' -not -name '*-plain.war' | head -1)
    cp "$war_file" "$local_official_dir/.build/rundeck.war"

    echo ">> Logging in to ACR ${ACR}"
    az acr login --name "${ACR}"

    # For multi-arch push we need a combined Dockerfile that includes
    # the base layers, since buildx can't reference local-only images.
    # Build ubuntu-base and push to ACR, then official, then custom.
    echo ">> Building + pushing ubuntu-base (multi-arch)..."
    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      --build-arg JRE_VERSION=openjdk-17-jre-headless \
      -t "${ACR}.azurecr.io/rundeck-base:${TAG}" \
      --push \
      "$REPO_ROOT/docker/ubuntu-base"

    echo ">> Building + pushing official image (multi-arch)..."
    # Temporarily update the FROM to use ACR base
    local tmp_dockerfile
    tmp_dockerfile=$(mktemp)
    sed "s|FROM rundeck/ubuntu-base|FROM ${ACR}.azurecr.io/rundeck-base:${TAG}|" \
      "$local_official_dir/Dockerfile" > "$tmp_dockerfile"

    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      -f "$tmp_dockerfile" \
      -t "${ACR}.azurecr.io/rundeck-official:${TAG}" \
      --push \
      "$local_official_dir"
    rm "$tmp_dockerfile"

    echo ">> Building + pushing custom image (multi-arch)..."
    cd "$SCRIPT_DIR"
    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      --build-arg "RUNDECK_IMAGE=${ACR}.azurecr.io/rundeck-official:${TAG}" \
      -t "${IMAGE}" \
      --push \
      .

    # Clean up
    rm -rf "$local_official_dir/.build"

    echo ""
    echo "Done! Pushed: ${IMAGE}"
    ;;
  *)
    echo "Unknown mode: $MODE" >&2
    echo "Usage: $0 dev | push <acr-name> <tag>" >&2
    exit 2
    ;;
esac