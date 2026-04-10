# syntax=docker/dockerfile:1.6
#
# Custom Rundeck image for local (ARM Mac) + Azure AKS (amd64).
#
# For local dev (arm64), build from source with build.sh:
#   ./build.sh dev
#
# For AKS (multi-arch), push to ACR:
#   ./build.sh push <acr-name> <tag>
#
# The base image is built from source (docker/) so native ARM64
# binaries (Remco, Tini, JRE) are used — no Rosetta emulation.

ARG RUNDECK_IMAGE=rundeck/rundeck:local
FROM ${RUNDECK_IMAGE}

# Teams notification plugin.
# There is no "official" rundeck-plugins org Teams plugin — this is the only one
# with a published GitHub release asset. Note: tag is v0.7, asset file is 0.6.jar.
#   https://github.com/bageera/rundeck-team-webhook
ARG TEAMS_PLUGIN_URL=https://github.com/bageera/rundeck-team-webhook/releases/download/v0.7/rundeck-team-incoming-webhook-plugin-0.6.jar

# NOTE: There is NO Azure Key Vault plugin for Rundeck. For Key Vault secrets on
# AKS, use the Azure Key Vault Provider for Secrets Store CSI Driver to mount
# secrets into the pod — see README.md section "Azure Key Vault on AKS".

USER root

# curl + ca-certificates for plugin download and TLS to Azure
RUN set -eux; \
    if command -v apt-get >/dev/null 2>&1; then \
      apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/*; \
    fi

# Drop plugins into the Rundeck libext dir (picked up automatically on startup).
RUN set -eux; \
    mkdir -p /home/rundeck/libext; \
    curl -fsSL -o /home/rundeck/libext/rundeck-team-incoming-webhook-plugin.jar "${TEAMS_PLUGIN_URL}"; \
    chown -R rundeck:root /home/rundeck/libext

# Overlay: server/config/* fragments go under /home/rundeck, aclpolicy files go to /home/rundeck/etc
COPY --chown=rundeck:root overlay/server/ /home/rundeck/server/
COPY --chown=rundeck:root overlay/etc/ /home/rundeck/etc/

USER rundeck
